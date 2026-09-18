package cn.shopex.ecshopx.common.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenapiSignSupportTest {

	@Test
	void genSign_matchesPhpAssembleOrderAndMd5Wrap() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("app_key", "demo_key");
		params.put("timestamp", "1710000000");
		params.put("version", "2.0");
		params.put("order_id", 1001L);
		params.put("enabled", true);
		params.put("ignored", null);

		String token = "secret_token";
		String sign = OpenapiSignSupport.genSign(params, token);
		assertEquals(32, sign.length());
		assertTrue(sign.equals(sign.toUpperCase()));
		assertTrue(OpenapiSignSupport.signMatches(params, token, sign));
	}

	@Test
	void assemble_sortsKeysLexicographically() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("version", "2.0");
		params.put("app_key", "k");
		params.put("timestamp", "1");
		assertEquals("app_keyktimestamp1version2.0", OpenapiSignSupport.assemble(params));
	}
}
