package cn.shopex.ecshopx.adapay.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdapayWithdrawRuleLegacySerializeUtilTest {

	@Test
	void serialize_monthDayMatchesLegacyVector() {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("day", "1");
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("type", "month");
		rule.put("filter", filter);
		assertEquals(
				"a:2:{s:4:\"type\";s:5:\"month\";s:6:\"filter\";a:1:{s:3:\"day\";s:1:\"1\";}}",
				AdapayWithdrawRuleLegacySerializeUtil.serialize(rule));
	}

	@Test
	void serialize_emptyMap() {
		assertEquals("a:0:{}", AdapayWithdrawRuleLegacySerializeUtil.serialize(Map.of()));
	}

	@Test
	void deserialize_roundTrip_monthRule() {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("day", "1");
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("type", "month");
		rule.put("filter", filter);
		String ser = AdapayWithdrawRuleLegacySerializeUtil.serialize(rule);
		Map<String, Object> back = AdapayWithdrawRuleLegacySerializeUtil.deserializeWithdrawRule(ser);
		assertEquals("month", back.get("type"));
		@SuppressWarnings("unchecked")
		Map<String, Object> f = (Map<String, Object>) back.get("filter");
		assertEquals("1", f.get("day"));
	}

	@Test
	void deserialize_emptyAndA0() {
		assertEquals(0, AdapayWithdrawRuleLegacySerializeUtil.deserializeWithdrawRule(null).size());
		assertEquals(0, AdapayWithdrawRuleLegacySerializeUtil.deserializeWithdrawRule("  ").size());
		assertEquals(0, AdapayWithdrawRuleLegacySerializeUtil.deserializeWithdrawRule("a:0:{}").size());
	}
}
