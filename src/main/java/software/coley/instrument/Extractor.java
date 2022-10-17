package software.coley.instrument;

import software.coley.instrument.util.Streams;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Tool to extract self at runtime in a bundled environment.
 *
 * @author Matt Coley
 */
public class Extractor {
	public static void main(String[] args) throws IOException {
		if (args.length > 0)
			extractToPath(Paths.get(args[0]));
		else
			System.err.println("Provide a target path to extract to");
	}

	public static void extractToPath(Path path) throws IOException {
		// Get self location
		List<Item> items = collectSelfItems();
		// Write to jar
		writeItems(items, path);
	}

	public static List<Item> collectSelfItems() throws IOException {
		URL selfLocation = Extractor.class.getProtectionDomain().getCodeSource().getLocation();
		String selfFilePath = selfLocation.getFile();
		if (selfFilePath.startsWith("/"))
			selfFilePath = selfFilePath.substring(1);
		Path selfPath = Paths.get(selfFilePath);
		// Get self-classes
		List<Item> list = new ArrayList<>();
		String prefix = Extractor.class.getPackage().getName().replace('.', '/');
		if (Files.isRegularFile(selfPath)) {
			// Read self as zip/jar
			try (ZipFile file = new ZipFile(selfPath.toFile())) {
				Enumeration<? extends ZipEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory())
						continue;
					String name = entry.getName();
					if (name.startsWith(prefix))
						list.add(new Item(entry.getName(), Streams.readStream(file.getInputStream(entry))));
				}
			}
		} else {
			Files.walk(selfPath)
					.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".class"))
					.forEach(path -> {
						String localName = selfPath.relativize(path).toString().replace('\\', '/');
						if (localName.startsWith(prefix)) {
							try {
								list.add(new Item(localName, Files.readAllBytes(path)));
							} catch (IOException ex) {
								throw new RuntimeException("Could not read class: " + localName, ex);
							}
						}
					});
		}
		return list;
	}

	public static void writeItems(List<Item> items, Path path) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new Attributes.Name("Premain-Class"), Agent.class.getName());
		manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), Agent.class.getName());
		manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
		manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
		try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path), manifest)) {
			for (Item item : items) {
				JarEntry jarEntry = new JarEntry(item.getPath());
				jar.putNextEntry(jarEntry);
				jar.write(item.getContent());
				jar.closeEntry();
			}
		}
	}

	public static class Item {
		private final String path;
		private final byte[] content;

		private Item(String path, byte[] content) {
			this.path = path;
			this.content = content;
		}

		public String getPath() {
			return path;
		}

		public byte[] getContent() {
			return content;
		}
	}
}
