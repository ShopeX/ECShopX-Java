package cn.shopex.ecshopx.reservation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationNumLimitCodecTest {

	@Test
	void serializeEmptyMap() {
		assertEquals("a:0:{}", ReservationNumLimitCodec.serialize(Map.of()));
	}

	@Test
	void serializeLimitDaysMatchesExpectedShape() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limit_type", "limit_days");
		m.put("limit_days", 5);
		String expected = "a:2:{s:10:\"limit_type\";s:10:\"limit_days\";s:10:\"limit_days\";i:5;}";
		assertEquals(expected, ReservationNumLimitCodec.serialize(m));
	}
}
