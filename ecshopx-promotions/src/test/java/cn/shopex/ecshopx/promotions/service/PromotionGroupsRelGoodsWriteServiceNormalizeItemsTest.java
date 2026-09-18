package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;

class PromotionGroupsRelGoodsWriteServiceNormalizeItemsTest {

	private static final long COMPANY_ID = 1L;

	private MarketingActivityCatalogAccess catalogAccess;
	private PromotionGroupsActivityGroupParamValidationService validationService;
	private PromotionGroupsRelGoodsWriteService writeService;

	@BeforeEach
	void setUp() {
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(anyString(), nullable(Object[].class), any(Locale.class)))
				.thenAnswer(invocation -> invocation.getArgument(0, String.class));

		catalogAccess = mock(MarketingActivityCatalogAccess.class);
		when(catalogAccess.anyGiftItem(eq(COMPANY_ID), anyList())).thenReturn(false);
		when(catalogAccess.listItemIdsByGoodsId(eq(COMPANY_ID), eq(100L))).thenReturn(List.of(100L));

		PromotionGroupsActivityMapper activityMapper = mock(PromotionGroupsActivityMapper.class);
		validationService =
				new PromotionGroupsActivityGroupParamValidationService(
						messageSource, catalogAccess, activityMapper, mock(org.springframework.jdbc.core.JdbcTemplate.class));

		writeService =
				new PromotionGroupsRelGoodsWriteService(
						mock(PromotionGroupsRelGoodsMapper.class),
						mock(PromotionGroupsRelGoodsReadService.class),
						catalogAccess,
						validationService,
						messageSource,
						mock(StringRedisTemplate.class));
	}

	@Test
	void normalize_fallsBackToLegacyFields_whenItemsMissing() {
		Map<String, Object> params = new HashMap<>();
		params.put("goods_id", 100L);
		params.put("act_price", "9.90");
		params.put("store", 10);

		Map<String, Object> skuRow = skuRow(100L, 2000L);
		when(catalogAccess.loadSkuItemsList(eq(COMPANY_ID), eq(List.of(100L))))
				.thenReturn(Map.of("list", List.of(skuRow)));

		List<PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem> items =
				writeService.normalizeAndValidateItems(params, COMPANY_ID, Locale.CHINA);

		assertEquals(1, items.size());
		assertEquals(100L, items.get(0).itemId());
		assertEquals(990L, items.get(0).activityPriceFen());
		assertEquals(10L, items.get(0).activityStore());
	}

	@Test
	void normalize_rejectsEmptyItemsArray() {
		Map<String, Object> params = new HashMap<>();
		params.put("goods_id", 100L);
		params.put("items", List.of());

		assertThrows(
				BadRequestException.class,
				() -> writeService.normalizeAndValidateItems(params, COMPANY_ID, Locale.CHINA));
	}

	@Test
	void normalize_acceptsSiblingSkus_whenGoodsIdIsNonDefaultSku() {
		Map<String, Object> params = new HashMap<>();
		params.put("goods_id", 7226L);
		params.put(
				"items",
				List.of(
						Map.of("item_id", 7226L, "act_price", "9.90", "store", 1),
						Map.of("item_id", 7225L, "act_price", "10.00", "store", 2),
						Map.of("item_id", 7221L, "act_price", "13.00", "store", 6)));

		Map<String, Object> anchor = skuRow(7226L, 12000L);
		anchor.put("goods_id", 7221L);
		anchor.put("default_item_id", 7221L);
		when(catalogAccess.loadSkuItemsList(eq(COMPANY_ID), eq(List.of(7226L))))
				.thenReturn(Map.of("list", List.of(anchor)));
		when(catalogAccess.listItemIdsByGoodsId(eq(COMPANY_ID), eq(7221L)))
				.thenReturn(List.of(7221L, 7222L, 7223L, 7224L, 7225L, 7226L));

		Map<String, Object> sku7226 = skuRow(7226L, 12000L);
		sku7226.put("goods_id", 7221L);
		Map<String, Object> sku7225 = skuRow(7225L, 12000L);
		sku7225.put("goods_id", 7221L);
		Map<String, Object> sku7221 = skuRow(7221L, 12000L);
		sku7221.put("goods_id", 7221L);
		when(catalogAccess.loadSkuItemsList(eq(COMPANY_ID), eq(List.of(7226L, 7225L, 7221L))))
				.thenReturn(Map.of("list", List.of(sku7226, sku7225, sku7221)));

		List<PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem> items =
				writeService.normalizeAndValidateItems(params, COMPANY_ID, Locale.CHINA);

		assertEquals(3, items.size());
		assertEquals(7226L, items.get(0).itemId());
		assertEquals(7225L, items.get(1).itemId());
		assertEquals(7221L, items.get(2).itemId());
	}

	private static Map<String, Object> skuRow(long itemId, long priceFen) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", itemId);
		row.put("company_id", COMPANY_ID);
		row.put("price", priceFen);
		row.put("goods_id", 100L);
		row.put("item_name", "Test SKU");
		row.put("pics", "http://x/p.jpg");
		row.put("item_spec_desc", "Red / L");
		return row;
	}
}
