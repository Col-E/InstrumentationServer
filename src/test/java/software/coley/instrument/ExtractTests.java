package software.coley.instrument;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtractTests {
	@Test
	public void testExtractFromDirectory() throws Exception {
		// Tests will run with the CodeSource being from 'target/classes'
		List<Extractor.Item> extractedItems = Extractor.collectSelfItems();
		assertTrue(extractedItems.size() > 0, "Extractor did not find any items");
	}
}