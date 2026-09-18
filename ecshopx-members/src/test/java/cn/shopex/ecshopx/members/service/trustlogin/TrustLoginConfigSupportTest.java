package cn.shopex.ecshopx.members.service.trustlogin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrustLoginConfigSupportTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mergeTrustLoginConfig_appendsMissingTouchSocialRows() {
		Map<String, Object> stored = new LinkedHashMap<>();
		List<Map<String, Object>> touch = new ArrayList<>();
		touch.add(row("weixin", "false"));
		stored.put("touch", touch);

		Map<String, Object> defaults = defaultRoot();
		Map<String, Object> merged = TrustLoginConfigSupport.mergeTrustLoginConfig(stored, defaults);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> touchOut = (List<Map<String, Object>>) merged.get("touch");
		assertEquals(5, touchOut.size());
		assertTrue(touchOut.stream().anyMatch(r -> "apple".equals(r.get("type"))));
		assertTrue(touchOut.stream().anyMatch(r -> "facebook".equals(r.get("type"))));
	}

	@Test
	void mergeExtraConfigOnSave_preservesPrivateKeyWhenMasked() throws Exception {
		String existing =
				objectMapper.writeValueAsString(Map.of(
						"team_id", "T1",
						"key_id", "K1",
						"private_key", "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----"));
		String incoming = objectMapper.writeValueAsString(Map.of(
				"team_id", "T2",
				"key_id", "K2",
				"private_key", "***"));
		String merged = TrustLoginConfigSupport.mergeExtraConfigOnSave(existing, incoming, objectMapper);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = objectMapper.readValue(merged, Map.class);
		assertEquals("T2", map.get("team_id"));
		assertEquals("K2", map.get("key_id"));
		assertTrue(String.valueOf(map.get("private_key")).contains("BEGIN PRIVATE KEY"));
	}

	@Test
	void mergeExtraConfigOnSave_partialIncomingDoesNotRestoreMissingKeys() throws Exception {
		String existing =
				objectMapper.writeValueAsString(Map.of(
						"team_id", "T1",
						"key_id", "K1",
						"private_key", "pem"));
		String incoming = objectMapper.writeValueAsString(Map.of("team_id", "T2"));
		String merged = TrustLoginConfigSupport.mergeExtraConfigOnSave(existing, incoming, objectMapper);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = objectMapper.readValue(merged, Map.class);
		assertEquals("T2", map.get("team_id"));
		assertFalse(map.containsKey("private_key"));
	}

	@Test
	void assertAppleExtraConfigValid_rejectsMissingPrivateKey() throws Exception {
		String bad = objectMapper.writeValueAsString(Map.of("team_id", "T", "key_id", "K"));
		assertThrows(
				ResourceException.class,
				() -> TrustLoginConfigSupport.assertAppleExtraConfigValid(bad, objectMapper));
	}

	@Test
	void sanitizeConfigRow_adminMasksPrivateKey() throws Exception {
		String extra = objectMapper.writeValueAsString(Map.of(
				"team_id", "T",
				"key_id", "K",
				"private_key", "pem"));
		Map<String, Object> row = row("apple", "false");
		row.put("secret", "sec");
		row.put("extra_config", extra);
		Map<String, Object> out = TrustLoginConfigSupport.sanitizeConfigRow(row, false);
		assertEquals("sec", out.get("secret"));
		@SuppressWarnings("unchecked")
		Map<String, Object> extraOut = objectMapper.readValue(String.valueOf(out.get("extra_config")), Map.class);
		assertEquals("***", extraOut.get("private_key"));
		assertEquals("T", extraOut.get("team_id"));
	}

	@Test
	void sanitizeConfigRow_frontRemovesSecrets() throws Exception {
		String extra = objectMapper.writeValueAsString(Map.of(
				"team_id", "T",
				"key_id", "K",
				"private_key", "pem"));
		Map<String, Object> row = row("apple", "false");
		row.put("secret", "sec");
		row.put("extra_config", extra);
		Map<String, Object> out = TrustLoginConfigSupport.sanitizeConfigRow(row, true);
		assertFalse(out.containsKey("secret"));
		assertEquals("", out.get("extra_config"));
	}

	@Test
	void normalizeStatus_acceptsFourTruthyForms() {
		assertTrue(TrustLoginConfigSupport.normalizeStatus(true));
		assertTrue(TrustLoginConfigSupport.normalizeStatus("true"));
		assertTrue(TrustLoginConfigSupport.normalizeStatus(1));
		assertTrue(TrustLoginConfigSupport.normalizeStatus("1"));
		assertFalse(TrustLoginConfigSupport.normalizeStatus("false"));
		assertFalse(TrustLoginConfigSupport.normalizeStatus(0));
	}

	private static Map<String, Object> row(String type, String status) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("type", type);
		row.put("app_id", "");
		row.put("secret", "");
		row.put("name", type);
		row.put("status", status);
		row.put("extra_config", "");
		return row;
	}

	private static Map<String, Object> defaultRoot() {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("standard", List.of(row("weixin", "false")));
		root.put(
				"touch",
				List.of(
						row("weixin", "false"),
						row("apple", "false"),
						row("google", "false"),
						row("facebook", "false"),
						row("line", "false")));
		return root;
	}
}
