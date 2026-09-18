package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoreHomePageFrontDetailServiceTest {

	@Test
	void pickResolvedPagesTemplateRow_prefersEnabledFirst() {
		List<Map<String, Object>> list =
				List.of(
						Map.of("template_name", "other", "pages_template_id", 1, "status", 1),
						Map.of("template_name", "yyk", "pages_template_id", 99, "status", 1),
						Map.of("template_name", "yyk", "pages_template_id", 100, "status", 0));
		Map<String, Object> row = StoreHomePageFrontDetailService.pickResolvedPagesTemplateRow(list, "yyk");
		assertNotNull(row);
		assertEquals(99, ((Number) row.get("pages_template_id")).intValue());
	}

	@Test
	void pickResolvedPagesTemplateRow_fallsBackWhenNoneEnabled() {
		List<Map<String, Object>> list = List.of(Map.of("template_name", "yyk", "pages_template_id", 7, "status", 0));
		Map<String, Object> row = StoreHomePageFrontDetailService.pickResolvedPagesTemplateRow(list, "yyk");
		assertNotNull(row);
		assertEquals(7, ((Number) row.get("pages_template_id")).intValue());
	}

	@Test
	void pickResolvedPagesTemplateRow_returnsNullWhenNoMatch() {
		assertNull(StoreHomePageFrontDetailService.pickResolvedPagesTemplateRow(List.of(), "yyk"));
		assertNull(
				StoreHomePageFrontDetailService.pickResolvedPagesTemplateRow(
						List.of(Map.of("template_name", "x", "pages_template_id", 1, "status", 1)), "yyk"));
	}
}
