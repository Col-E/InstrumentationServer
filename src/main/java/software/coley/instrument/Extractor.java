package software.coley.instrument;

import software.coley.instrument.util.Streams;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
	private static final List<Path> extractionPaths = new CopyOnWriteArrayList<>();
	private static boolean isTest;

	public static void main(String[] args) throws IOException {
		if (args.length > 0)
			extractToPath(Paths.get(args[0]));
		else
			System.err.println("Provide a target path to extract to");
	}

	public static void markTestEnv() {
		isTest = true;
	}

	public static void extractToPath(Path path) throws IOException {
		// Get self location
		List<Item> items = collectSelfItems();
		// Write to jar
		writeItems(items, path);
	}

	public static void addExtractionContext(Class<?> context) throws IOException {
		// Get code-src location of the context class, which is the jar or directory that contains it.
		Path path;
		try {
			path = Paths.get(context.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException syntaxException) {
			throw new IOException(syntaxException);
		}
		if (!Files.exists(path))
			throw new IOException("Source-path of extraction context does not exist: " + path);

		// Don't want duplicates.
		if (extractionPaths.contains(path))
			return;
		extractionPaths.add(path);
	}

	public static List<Item> collectSelfItems() throws IOException {
		addExtractionContext(Extractor.class);
		return getItems();
	}

	private static List<Item> getItems() throws IOException {
		// Get self-classes + ASM classes.
		//  - Real usage: They are shaded
		//  - Test usage: They're in their own ASM dependency jar
		List<Item> list = new ArrayList<>();
		String asmPath = isTest ?
				"org/objectweb/asm" :
				"software/coley/instrumement/shadedasm";
		String[] prefixes = {
				Extractor.class.getPackage().getName().replace('.', '/'),
				asmPath
		};
		for (Path pathEntry : Extractor.extractionPaths) {
			if (Files.isRegularFile(pathEntry)) {
				// Read self as zip/jar
				try (ZipFile file = new ZipFile(pathEntry.toFile())) {
					Enumeration<? extends ZipEntry> entries = file.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						if (entry.isDirectory())
							continue;
						String name = entry.getName();
						if (hasMatchingPrefix(name, prefixes))
							list.add(new Item(entry.getName(), Streams.readStream(file.getInputStream(entry))));
					}
				}
			} else if (Files.isDirectory(pathEntry)) {
				Files.walk(pathEntry)
						.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".class"))
						.forEach(path -> {
							String localName = pathEntry.relativize(path).toString().replace('\\', '/');
							if (hasMatchingPrefix(localName, prefixes)) {
								try {
									list.add(new Item(localName, Files.readAllBytes(path)));
								} catch (IOException ex) {
									throw new RuntimeException("Could not read class: " + localName, ex);
								}
							}
						});
			} else {
				throw new IOException("Source-path of extractor exists, but is not a file or directory: " + pathEntry);
			}
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

	private static boolean hasMatchingPrefix(String name, String[] prefixes) {
		for (String prefix : prefixes)
			if (name.startsWith(prefix))
				return true;
		return false;
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
