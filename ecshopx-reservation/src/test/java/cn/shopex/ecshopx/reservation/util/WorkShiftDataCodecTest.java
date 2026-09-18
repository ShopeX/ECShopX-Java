package cn.shopex.ecshopx.reservation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkShiftDataCodecTest {

	/**
	 * In legacy {@code s:N:"..."} segments, {@code N} is the UTF-8 <strong>byte</strong> length of the string body
	 * after unescaping, not the Unicode character count (e.g. one hiragana is often 3 bytes in UTF-8).
	 */
	@Test
	void serialize_usesUtf8ByteLengthInSTypePrefix() {
		LinkedHashMap<String, Map<String, String>> data = new LinkedHashMap<>();
		LinkedHashMap<String, String> inner = new LinkedHashMap<>();
		inner.put("typeId", "あ");
		data.put("monday", inner);
		String ser = WorkShiftDataCodec.serializeWorkShiftData(data);
		assertTrue(ser.contains("s:3:"), "expected s:3: for あ (3 UTF-8 bytes), got: " + ser);
		ObjectMapper om = new ObjectMapper();
		Map<String, Object> root = WorkShiftDataCodec.parseWorkShiftDataRoot(ser, om);
		@SuppressWarnings("unchecked")
		Map<String, Object> mon = (Map<String, Object>) root.get("monday");
		assertEquals("あ", mon.get("typeId").toString());
	}

	@Test
	void roundTrip_matchesExpectedWireFormForAscii() {
		LinkedHashMap<String, Map<String, String>> data = new LinkedHashMap<>();
		data.put("monday", Map.of("typeId", "1"));
		String ser = WorkShiftDataCodec.serializeWorkShiftData(data);
		assertEquals("a:1:{s:6:\"monday\";a:1:{s:6:\"typeId\";s:1:\"1\";}}", ser);
		ObjectMapper om = new ObjectMapper();
		Map<String, Object> root = WorkShiftDataCodec.parseWorkShiftDataRoot(ser, om);
		@SuppressWarnings("unchecked")
		Map<String, Object> mon = (Map<String, Object>) root.get("monday");
		assertEquals("1", mon.get("typeId").toString());
	}

	@Test
	void jsonFallback_whenTrimmedObject_thenLegacyArray() {
		ObjectMapper om = new ObjectMapper();
		String json = "{\"monday\":{\"typeId\":\"7\"}}";
		Map<String, Object> root = WorkShiftDataCodec.parseWorkShiftDataRoot(json, om);
		@SuppressWarnings("unchecked")
		Map<String, Object> mon = (Map<String, Object>) root.get("monday");
		assertEquals("7", mon.get("typeId").toString());

		String legacy = "a:1:{s:6:\"monday\";a:1:{s:6:\"typeId\";s:1:\"2\";}}";
		Map<String, Object> root2 = WorkShiftDataCodec.parseWorkShiftDataRoot(legacy, om);
		@SuppressWarnings("unchecked")
		Map<String, Object> mon2 = (Map<String, Object>) root2.get("monday");
		assertEquals("2", mon2.get("typeId").toString());
	}
}
