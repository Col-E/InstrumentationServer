package software.coley.instrument;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import software.coley.instrument.command.impl.*;
import software.coley.instrument.util.DescUtil;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.Streams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
			Path classes = target.resolve("classes");
			agentJarPath = target.resolve("instrumentation-server-SNAPSHOT.jar");
			Files.deleteIfExists(agentJarPath);
			pack(classes, agentJarPath);
		} catch (IOException ex) {
			fail("Could not setup agent jar", ex);
		}
	}


	@Test
	@ResourceLock(SERVER) // Use this lock on other tests if they get split later
	public void test() throws Exception {
		Process start = null;
		try {
			// Start the java-agent on the 'Runner' example application
			String agent = "-javaagent:" + agentJarPath.toString().replace("\\", "/");
			ProcessBuilder pb = new ProcessBuilder("java", agent, "-cp", "src/test/resources", "Runner");
			pb.inheritIO();
			start = pb.start();
			// Setup our local client
			Thread.sleep(1500);
			Client client = new Client();
			assertTrue(client.connect());

			// Update one key
			client.send(new SetPropertyCommand("key", "new_value"), reply -> {
				System.out.println("> Key updated");
			});

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			// Update another
			client.send(new SetPropertyCommand("alt-key", "alt_key_value"), reply -> {
				System.out.println("> Alt-key updated");
			});

			// Request static field value
			client.send(new GetFieldCommand("Runner", "key", DescUtil.STRING_DESC), reply -> {
				GetFieldCommand getFieldCommand = (GetFieldCommand) reply;
				assertEquals("key", getFieldCommand.getValueText());
			});

			// Set static field value to different value
			client.send(new SetFieldCommand("Runner", "key", DescUtil.STRING_DESC, "alt-key"), null);

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			byte[] code = Files.readAllBytes(Paths.get("src/test/resources/Runner-instrumented.class"));
			client.send(new RedefineClassCommand("Runner", code), null);

			// Let runner app run to show the print output is different
			Thread.sleep(2000);

			// Request loaded class names
			client.send(new LoadedClassesCommand(), reply -> {
				LoadedClassesCommand loadedClassesCommand = (LoadedClassesCommand) reply;
				System.out.println("There are " + loadedClassesCommand.getClassNames().size() + " classes");
			});
		} finally {
			// Kill the remote process and delete the agent jar
			if (start != null)
				start.destroyForcibly();
			Thread.sleep(1000);
			Files.deleteIfExists(agentJarPath);
		}
	}

	private static void pack(Path source, Path target) throws IOException {
		String agentClassName = Agent.class.getName();
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new Attributes.Name("Premain-Class"), agentClassName);
		manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), agentClassName);
		manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
		manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
		try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(target), manifest)) {
			Files.walk(source)
					.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".class"))
					.forEach(path -> {
						JarEntry jarEntry = new JarEntry(source.relativize(path).toString().replace('\\', '/'));
						try {
							jar.putNextEntry(jarEntry);
							Files.copy(path, jar);
							jar.closeEntry();
						} catch (IOException ex) {
							fail(ex);
						}
					});
		}
	}
}