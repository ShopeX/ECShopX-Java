package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaasErpMatrixSignSupportTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void assembleSortsKeysAndSkipsNull() {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("b", "2");
		params.put("a", "1");
		params.put("c", null);
		assertEquals("a1b2", SaasErpMatrixSignSupport.assemble(params));
	}

	@Test
	void matrixSignMatchesPhpDoubleMd5Pattern() {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("method", "store.trade.add");
		params.put("tid", "123");
		params.put("orders", Map.of("oid", "1"));
		String sign = SaasErpMatrixSignSupport.matrixSign(params, "token", objectMapper);
		assertEquals(32, sign.length());
		assertTrue(sign.matches("[0-9A-F]{32}"));
		// deterministic for same inputs
		assertEquals(sign, SaasErpMatrixSignSupport.matrixSign(params, "token", objectMapper));
	}
}
