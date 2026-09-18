package cn.shopex.ecshopx.theme.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PcTemplateStorageUrlNormalizerTest {

	private final PcTemplateStorageUrlNormalizer normalizer =
			new PcTemplateStorageUrlNormalizer(new ObjectMapper(), "http://ecshopx.test");

	@Test
	void rewritesLocalhostStorageUrlToAppOrigin() {
		String config =
				"{\"logo\":\"http://localhost:8080/storage/a/b.png\",\"x\":1}";
		String out = normalizer.normalize(config);
		assertTrue(out.contains("http://ecshopx.test/storage/a/b.png"));
		assertTrue(out.contains("\"x\":1"));
	}

	@Test
	void leavesExternalHostUnchanged() {
		String config = "{\"logo\":\"https://cdn.example.com/storage/a/b.png\"}";
		assertEquals(config, normalizer.normalize(config));
	}

	@Test
	void normalizeAppOriginStripsPath() {
		assertEquals(
				"http://ecshopx.test",
				PcTemplateStorageUrlNormalizer.normalizeAppOrigin("http://ecshopx.test/api"));
	}
}
