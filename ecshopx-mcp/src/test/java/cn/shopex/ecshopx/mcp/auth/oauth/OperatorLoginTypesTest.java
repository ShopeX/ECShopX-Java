package cn.shopex.ecshopx.mcp.auth.oauth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperatorLoginTypesTest {

	@Test
	void normalizeDefaultsAndAcceptsKnownTypes() {
		assertEquals("admin", OperatorLoginTypes.normalize(null));
		assertEquals("admin", OperatorLoginTypes.normalize(""));
		assertEquals("staff", OperatorLoginTypes.normalize("Staff"));
		assertEquals("supplier", OperatorLoginTypes.normalize("supplier"));
		assertEquals("distributor", OperatorLoginTypes.normalize("distributor"));
	}

	@Test
	void normalizeRejectsUnknown() {
		assertThrows(IllegalArgumentException.class, () -> OperatorLoginTypes.normalize("member"));
	}

	@Test
	void labelsMatchOperatorType() {
		assertEquals("超级管理员", OperatorLoginTypes.labels().get("admin"));
		assertEquals("平台管理员", OperatorLoginTypes.labels().get("staff"));
		assertEquals("商户", OperatorLoginTypes.labels().get("merchant"));
		assertEquals("店员", OperatorLoginTypes.labels().get("distributor"));
		assertEquals("供应商", OperatorLoginTypes.labels().get("supplier"));
		assertEquals(5, OperatorLoginTypes.allowed().size());
		assertEquals(
				List.of("admin", "staff", "merchant", "distributor", "supplier"),
				new ArrayList<>(OperatorLoginTypes.labels().keySet()));
	}

	@Test
	void normalizeRejectsDealer() {
		assertThrows(IllegalArgumentException.class, () -> OperatorLoginTypes.normalize("dealer"));
	}
}
