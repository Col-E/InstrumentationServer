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
public class LiveTest {
	private static final String SERVER = "Server";
	private static Path agentJarPath;

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
			String agent = "-javaagent:" + agentJarPath.toString().replace("\\", "/");
			ProcessBuilder pb = new ProcessBuilder("java", agent, "-cp", "src/test/resources", "Runner");
			pb.inheritIO();
			start = pb.start();

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