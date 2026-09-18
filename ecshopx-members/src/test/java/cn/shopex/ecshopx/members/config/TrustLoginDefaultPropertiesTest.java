package cn.shopex.ecshopx.members.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrustLoginDefaultPropertiesTest {

	@Test
	void snapshotAsResponseMap_usesBuiltinWhenYamlUnbound() {
		TrustLoginDefaultProperties props = new TrustLoginDefaultProperties();
		assertTouchHasOverseas(props.snapshotAsResponseMap());
	}

	@Test
	void snapshotAsResponseMap_fallsBackWhenConfiguredListsEmpty() {
		TrustLoginDefaultProperties props = new TrustLoginDefaultProperties();
		props.setStandard(new ArrayList<>());
		props.setTouch(new ArrayList<>());
		assertTouchHasOverseas(props.snapshotAsResponseMap());
	}

	@SuppressWarnings("unchecked")
	private static void assertTouchHasOverseas(Map<String, Object> root) {
		List<Map<String, Object>> standard = (List<Map<String, Object>>) root.get("standard");
		List<Map<String, Object>> touch = (List<Map<String, Object>>) root.get("touch");
		assertEquals(1, standard.size());
		assertEquals("weixin", standard.get(0).get("type"));
		assertEquals(5, touch.size());
		assertTrue(touch.stream().anyMatch(r -> "weixin".equals(r.get("type"))));
		assertTrue(touch.stream().anyMatch(r -> "apple".equals(r.get("type"))));
		assertTrue(touch.stream().anyMatch(r -> "google".equals(r.get("type"))));
		assertTrue(touch.stream().anyMatch(r -> "facebook".equals(r.get("type"))));
		assertTrue(touch.stream().anyMatch(r -> "line".equals(r.get("type"))));
	}
}
