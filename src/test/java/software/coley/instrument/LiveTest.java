package software.coley.instrument;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.parallel.ResourceLock;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.instrument.data.MemberData;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.message.request.*;
import software.coley.instrument.util.DescUtil;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo setup using the {@code Runner} example class.
 */
//@DisabledIf("checkIsCiServer")
public class LiveTest {
	private static final String SERVER = "Server";
	private static Path agentJarPath;

//	public static boolean checkIsCiServer() {
//		// I have no idea why attach doesn't work on CI.
//		// It works locally on Java 8-17 just fine.
//		System.out.println("System.getProperty('user.dir'): " + System.getProperty("user.dir"));
//		return System.getProperty("user.dir").contains("/home/runner/");
//	}

	@BeforeAll
	public static void setup() {
		// Setup client logging (server will use defaults)
		Logger.level = Logger.INFO;
		// Create server jar from the latest compilation
		try {
			Path target = Paths.get("target");
			agentJarPath = target.resolve("instrumentation-server-SNAPSHOT.jar");
			Files.deleteIfExists(agentJarPath);
			Extractor.extractToPath(agentJarPath);
		} catch (IOException ex) {
			fail("Could not setup agent jar", ex);
		}
	}

	@Test
	@Timeout(15)
	@ResourceLock(SERVER) // Use this lock on other tests if they get split later
	public void test() throws Exception {
		Process start = null;
		Client client = null;
		try {
			// Start the java-agent on the 'Runner' example application
			String agent = "-javaagent:" + agentJarPath.toString().replace("\\", "/");
//			ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\AdoptOpenJDK\\jdk-17\\bin\\java", agent, "-cp", "src/test/resources", "Runner");
			ProcessBuilder pb = new ProcessBuilder("java", agent, "-cp", "src/test/resources", "Runner");
//			pb.environment().replace("_", "/usr/lib/jvm/java-17-openjdk/bin/java");
//			ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-8-openjdk/jre/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-11-openjdk/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-17-openjdk/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-19-openjdk/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/home/wolf/.jdks/corretto-18.0.2/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/home/wolf/.jdks/corretto-19.0.2/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/home/wolf/.jdks/openjdk-20/bin/java", agent, "-cp", "src/test/resources", "Runner");
//			ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "java", agent, "-cp", "src/test/resources", "Runner");
			System.out.println("pb: " + pb);
			System.out.println("pb.directory(): " + pb.directory());
			// map view of this process builder's environment
			Map<String, String> envMap = pb.environment();
			// checking map view of environment
			for (Map.Entry<String, String> entry :
					envMap.entrySet()) {
				// checking key and value separately
				System.out.println(entry.getKey()
						+ ": "
						+ entry.getValue());
			}
			System.out.println("---");
			System.out.println("_: " + pb.environment().get("_"));
			pb.inheritIO();
			start = pb.start();
			System.out.println("pb: " + pb);
			System.out.println("pb.directory(): " + pb.directory());

			// Setup our local client
			Thread.sleep(1500);
			int[] broadcastCounter = new int[1];
			client = new Client("localhost", Server.DEFAULT_PORT, ByteBufferAllocator.HEAP, MessageFactory.create());
			client.setBroadcastListener((type, message) -> broadcastCounter[0]++);
			assertTrue(client.connect());

			// Get the classloaders
			List<ClassLoaderInfo> loaders = new ArrayList<>();
			client.sendBlocking(new RequestClassloadersMessage(), reply -> {
				for (ClassLoaderInfo loaderInfo : reply.getClassLoaders()) {
					System.out.println("> Classloader: " + loaderInfo.getName());
					if (loaderInfo.isBootstrap())
						continue;
					loaders.add(loaderInfo);
				}
			});

			// Request one class
			client.sendBlocking(new RequestClassMessage(ApiConstants.SYSTEM_CLASSLOADER_ID, "Runner"),
					replyClassMessage -> {
						System.out.println("Runner class: " + replyClassMessage.getData());
					});

			// Update one key
			client.sendBlocking(new RequestSetPropertyMessage("key", "new_value"), reply -> {
				System.out.println("> Key updated");
			});

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			// Update another
			client.sendBlocking(new RequestSetPropertyMessage("alt-key", "alt_key_value"), reply -> {
				System.out.println("> Alt-key updated");
			});

			// Request static field value
			MemberData memberData = new MemberData("Runner", "key", DescUtil.STRING_DESC);
			client.sendBlocking(new RequestFieldGetMessage(memberData), reply -> {
				System.out.println("Get: " + reply.getValueText());
				assertEquals("key", reply.getValueText());
			});

			// Set static field value to different value
			client.sendBlocking(new RequestFieldSetMessage(memberData, "alt-key"), null);

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			byte[] code = Files.readAllBytes(Paths.get("src/test/resources/Runner-instrumented.class"));
			client.sendBlocking(new RequestRedefineMessage(ApiConstants.SYSTEM_CLASSLOADER_ID, "Runner", code), null);

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			// Request loaded class names in the system classloader
			client.sendBlocking(new RequestClassloaderClassesMessage(ApiConstants.SYSTEM_CLASSLOADER_ID), reply -> {
				System.out.println("There are " + reply.getClasses().size() + " total classes in the SCL");
			});

			int updated = broadcastCounter[0];
			System.out.println("There were " + updated + " broadcasted class updates");
			assertTrue(updated > 0);
			assertTrue(loaders.size() > 0);
		} finally {
			if (client != null)
				client.close();
			// Kill the remote process and delete the agent jar
			if (start != null)
				start.destroyForcibly();
			Thread.sleep(1000);
			Files.deleteIfExists(agentJarPath);
		}
	}
}