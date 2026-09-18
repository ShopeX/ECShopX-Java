package cn.shopex.ecshopx.goods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItemsCategoryFrontDisplayResolverTest {

	private final ItemsCategoryFrontDisplayResolver resolver = new ItemsCategoryFrontDisplayResolver();

	@Test
	@DisplayName("resolve + shouldFallback 矩阵（对齐 TC-U1）")
	void resolveAndFallbackMatrix() {
		assertTrue(resolver.resolveFrontCategoryListIsMainCategory("platform", 0L, "false"));
		assertFalse(resolver.shouldFallbackFrontCategoryList("platform", 0L, true));

		assertTrue(resolver.resolveFrontCategoryListIsMainCategory("platform", 0L, "true"));
		assertFalse(resolver.shouldFallbackFrontCategoryList("platform", 0L, true));

		assertFalse(resolver.resolveFrontCategoryListIsMainCategory("platform", 5L, "false"));
		assertTrue(resolver.shouldFallbackFrontCategoryList("platform", 5L, false));

		assertFalse(resolver.resolveFrontCategoryListIsMainCategory("standard", 0L, "false"));
		assertTrue(resolver.shouldFallbackFrontCategoryList("standard", 0L, false));

		assertFalse(resolver.resolveFrontCategoryListIsMainCategory("b2c", 0L, "false"));
		assertTrue(resolver.shouldFallbackFrontCategoryList("b2c", 0L, false));

		assertFalse(resolver.resolveFrontCategoryListIsMainCategory("b2c", 3L, "false"));
		assertTrue(resolver.shouldFallbackFrontCategoryList("b2c", 3L, false));
	}

	@Test
	@DisplayName("platform 商城页忽略请求 is_main_category=false")
	void platformDistributorZeroIgnoresRequestIsMain() {
		assertTrue(resolver.resolveFrontCategoryListIsMainCategory("platform", 0L, "0"));
		assertTrue(resolver.resolveFrontCategoryListIsMainCategory("platform", 0L, "false"));
		assertFalse(resolver.shouldFallbackFrontCategoryList("platform", 0L, true));
	}

	@Test
	@DisplayName("distributor_id 解析：standard 强制 0，其余用请求值")
	void resolveCategoryDistributorIdForFront() {
		assertEquals(0L, resolver.resolveCategoryDistributorIdForFront("standard", 9L));
		assertEquals(5L, resolver.resolveCategoryDistributorIdForFront("platform", 5L));
		assertEquals(0L, resolver.resolveCategoryDistributorIdForFront("platform", 0L));
		assertEquals(3L, resolver.resolveCategoryDistributorIdForFront("b2c", 3L));
	}

	@Test
	@DisplayName("自定义页绑定：仅 platform 要求管理分类")
	void customizePageBindMainCategoryMatrix() {
		assertTrue(resolver.isCustomizePageBindMainCategory("platform"));
		assertFalse(resolver.isCustomizePageBindMainCategory("standard"));
		assertFalse(resolver.isCustomizePageBindMainCategory("b2c"));
		assertFalse(resolver.isCustomizePageBindMainCategory("in_purchase"));
		assertEquals("只能绑定一级管理分类", resolver.getCustomizePageBindCategoryErrorMessage("platform"));
		assertEquals("只能绑定一级销售分类", resolver.getCustomizePageBindCategoryErrorMessage("standard"));
	}

	@Test
	@DisplayName("validateCustomizePageBindCategory 矩阵")
	void validateCustomizePageBindCategory() {
		assertNull(resolver.validateCustomizePageBindCategory(1, true, "platform"));
		assertEquals("只能绑定一级管理分类", resolver.validateCustomizePageBindCategory(1, false, "platform"));
		assertEquals("只能绑定一级管理分类", resolver.validateCustomizePageBindCategory(2, true, "platform"));

		assertNull(resolver.validateCustomizePageBindCategory(1, false, "standard"));
		assertEquals("只能绑定一级销售分类", resolver.validateCustomizePageBindCategory(1, true, "standard"));

		assertNull(resolver.validateCustomizePageBindCategory(1, false, "b2c"));
		assertEquals("只能绑定一级销售分类", resolver.validateCustomizePageBindCategory(1, true, "b2c"));
	}

	@Test
	@DisplayName("injectFrontCategoryListIsMainCategoryFlag 递归写入 JSON boolean")
	void injectIsMainCategoryFlagOnTree() {
		Map<String, Object> child = new LinkedHashMap<>();
		child.put("category_id", 2L);
		child.put("children", new ArrayList<>());
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("category_id", 1L);
		root.put("children", new ArrayList<>(List.of(child)));
		List<Map<String, Object>> tree = new ArrayList<>(List.of(root));

		resolver.injectFrontCategoryListIsMainCategoryFlag(tree, true);

		assertEquals(Boolean.TRUE, tree.get(0).get("is_main_category"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get(0).get("children");
		assertEquals(Boolean.TRUE, children.get(0).get("is_main_category"));
	}
}
