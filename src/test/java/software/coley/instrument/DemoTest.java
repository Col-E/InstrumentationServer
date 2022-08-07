package software.coley.instrument;

import org.junit.jupiter.api.Test;
import software.coley.instrument.util.DescUtil;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Demo class.
 */
public class DemoTest implements ClientListener {
	static {
		// Setup client logging (server will use defaults)
		Logger.level = Logger.DEBUG;
		Logger.isServer = false;
	}

	@Test
	public void test() throws Exception {
		// Create server jar from the latest compilation
		Path target = Paths.get("target");
		Path classes = target.resolve("classes");
		Path agentJarPath = target.resolve("instrumentation-server-SNAPSHOT.jar");
		Files.deleteIfExists(agentJarPath);
		pack(classes, agentJarPath);
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
			client.setListener(this);
			client.startInputLoop();
			// Send some requests
			client.requestProperties();
			Thread.sleep(500);
			client.requestSetProperty("key", "new_value");
			Thread.sleep(2000);
			client.requestSetProperty("alt-key", "alt_key_value");
			Thread.sleep(1000);
			client.requestGetStaticField("Runner", "key", DescUtil.STRING_DESC);
			client.requestGetStaticField(ClassLoader.class.getName(), "scl", DescUtil.getDescriptor(ClassLoader.class));
			Thread.sleep(500);
			client.requestSetStaticField("Runner", "key", DescUtil.STRING_DESC, "alt-key");
			Thread.sleep(1000);
			client.requestLoadedClasses();
			Thread.sleep(3000);
		} finally {
			// Kill the remote process and delete the agent jar
			if (start != null)
				start.destroyForcibly();
			Thread.sleep(1000);
			Files.deleteIfExists(agentJarPath);
		}
	}

	public static void pack(Path source, Path target) throws IOException {
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

	@Override
	public void onReceiveProperties(Map<String, String> properties) {
		System.out.println("[Demo] Properties[" + properties.size() + "]");
	}

	@Override
	public void onReceiveLoadedClasses(String[] classNames) {
		System.out.println("[Demo] Loaded classes: " + classNames.length);
	}

	@Override
	public void onReceiveStaticFieldValue(String owner, String name, String desc, String valueText) {
		System.out.println("[Demo] Static field " + owner + "." + name + " = " + valueText);
	}
}