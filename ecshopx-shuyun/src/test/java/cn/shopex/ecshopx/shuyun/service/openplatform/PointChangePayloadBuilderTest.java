package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PointChangePayloadBuilderTest {

	@Test
	void resolveSourceMapping() {
		assertEquals("REFUND", PointChangePayloadBuilder.resolveSource(9, true));
		assertEquals("MARKET", PointChangePayloadBuilder.resolveSource(1, true));
		assertEquals("TRADE", PointChangePayloadBuilder.resolveSource(7, true));
		assertEquals("CONSUME", PointChangePayloadBuilder.resolveSource(5, false));
	}

	@Test
	void buildPayloadBasics() {
		Map<String, Object> body =
				PointChangePayloadBuilder.build(88L, 1L, 100, true, 7, "下单赠送", "OID1", "12-off", Map.of());
		assertEquals("OFFLINE", body.get("platCode"));
		assertEquals("88", body.get("id"));
		assertEquals("12-off", body.get("shopId"));
		assertEquals(100, body.get("changePoint"));
		assertEquals("TRADE", body.get("source"));
		assertEquals("system", body.get("operator"));
		assertEquals("下单赠送", body.get("desc"));
	}

	@Test
	void deductWithOrderUsesMemberOperator() {
		Map<String, Object> body =
				PointChangePayloadBuilder.build(88L, 1L, 50, false, 5, "抵扣", "OID1", "12-off", Map.of());
		assertEquals(-50, body.get("changePoint"));
		assertEquals("88", body.get("operator"));
	}
}
