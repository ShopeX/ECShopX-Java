package cn.shopex.ecshopx.thirdparty.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaasErpInboundParameterMergeTest {

	@Test
	void queryOverridesBodyAndKeepsEmptyString() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("act", "get_orders_info");
		body.put("order_id", "from-body");
		Map<String, String> query = new LinkedHashMap<>();
		query.put("order_id", "from-query");
		query.put("ac", "");
		Map<String, Object> merged = SaasErpInboundParameterMerge.merge(body, query);
		assertEquals("from-query", merged.get("order_id"));
		assertEquals("get_orders_info", merged.get("act"));
		assertEquals("", merged.get("ac"));
	}
}
