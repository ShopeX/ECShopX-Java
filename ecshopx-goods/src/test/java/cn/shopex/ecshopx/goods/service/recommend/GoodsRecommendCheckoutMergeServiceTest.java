package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class GoodsRecommendCheckoutMergeServiceTest {

	@Mock
	private GoodsRecommendRuleMainItemMapper mainItemMapper;

	@Mock
	private GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private GoodsRecommendSalabilityResolver salabilityResolver;

	@Mock
	private MessageSource messageSource;

	private GoodsRecommendCheckoutMergeService service;

	@BeforeEach
	void setUp() {
		service =
				new GoodsRecommendCheckoutMergeService(
						mainItemMapper,
						recommendItemMapper,
						itemsMapper,
						salabilityResolver,
						new GoodsRecommendGoodsIdResolver(itemsMapper),
						messageSource);
	}

	@Test
	void apply_missingOrEmptyRecommend_isNoOp() {
		Map<String, Object> params = freightFeeSnapshot();
		service.apply(1L, params);
		assertFalse(params.containsKey(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS));

		params.put("recommend_item_id", List.of());
		service.apply(1L, params);
		assertFalse(params.containsKey("recommend_item_id"));
		assertFalse(params.containsKey(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		assertEquals(1, items.size());
	}

	@Test
	void apply_multipleRecommendItems_mergesAll() {
		stubSellableMerge(item(5L, 5L), item(8L, 8L));

		Map<String, Object> params = freightFeeSnapshot();
		params.put(
				"recommend_item_id",
				List.of(Map.of("item_id", 5, "num", 1), Map.of("item_id", 8, "num", 2)));
		service.apply(1L, params);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		assertEquals(3, items.size());
		assertEquals(5L, ((Number) items.get(1).get("item_id")).longValue());
		assertEquals(1, items.get(1).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(1)));
		assertEquals(8L, ((Number) items.get(2).get("item_id")).longValue());
		assertEquals(2, items.get(2).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(2)));
		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(0)));
		assertTrue(
				Boolean.TRUE.equals(
						params.get(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS)));
		assertFalse(params.containsKey("recommend_item_id"));
	}

	@Test
	void apply_keepsRequestedRecommendSku_notRemappedCheckoutSku() {
		Items recommend6742 = item(6742L, 6737L);
		Items main = item(6763L, 6763L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend6742);
		when(itemsMapper.selectList(any())).thenReturn(List.of(main));
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(6737L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(mainItemMapper.selectCount(any())).thenReturn(1L);
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);

		Map<String, Object> params = freightFeeSnapshot();
		params.put("items", List.of(line(6763L, 1)));
		params.put("recommend_item_id", List.of(Map.of("item_id", 6742, "num", 1)));
		service.apply(1L, params);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		assertEquals(2, items.size());
		assertEquals(6742L, ((Number) items.get(1).get("item_id")).longValue());
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(1)));
		verify(salabilityResolver, never()).resolveCheckoutSkuItemId(any(), anyLong(), any(), any(), any());
	}

	@Test
	void apply_cartWithoutItems_keepsItemsEmptyAndStoresRecommendRows() {
		Items recommend5 = item(5L, 5L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
		Map<String, Object> params = new HashMap<>();
		params.put("order_type", "normal");
		params.put("cart_type", "cart");
		params.put("recommend_item_id", List.of(Map.of("item_id", 5, "num", 1)));
		service.apply(1L, params);
		assertFalse(params.containsKey("items"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> recRows =
				(List<Map<String, Object>>)
						params.get(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_REQUEST_ITEMS);
		assertEquals(1, recRows.size());
		assertEquals(5L, ((Number) recRows.get(0).get("item_id")).longValue());
		assertEquals(1, recRows.get(0).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(recRows.get(0)));
	}

	@Test
	void apply_fastbuyWithoutItems_keepsItemsEmptyAndStoresRecommendRows() {
		Items recommend5 = item(5L, 5L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
		Map<String, Object> params = new HashMap<>();
		params.put("order_type", "normal");
		params.put("cart_type", "fastbuy");
		params.put("recommend_item_id", "[{\"item_id\":5,\"num\":1}]");
		service.apply(1L, params);
		assertFalse(params.containsKey("items"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> recRows =
				(List<Map<String, Object>>)
						params.get(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_REQUEST_ITEMS);
		assertEquals(1, recRows.size());
		assertEquals(5L, ((Number) recRows.get(0).get("item_id")).longValue());
		assertEquals(1, recRows.get(0).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(recRows.get(0)));
	}

	@Test
	void apply_cartWithoutItems_sameItemIdAccumulatesRecommendRows() {
		Items recommend5 = item(5L, 5L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
		Map<String, Object> params = new HashMap<>();
		params.put("order_type", "normal");
		params.put("cart_type", "cart");
		params.put(
				"recommend_item_id",
				List.of(Map.of("item_id", 5, "num", 1), Map.of("item_id", 5, "num", 1)));
		service.apply(1L, params);
		assertFalse(params.containsKey("items"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> recRows =
				(List<Map<String, Object>>)
						params.get(GoodsRecommendCheckoutMergeService.CHECKOUT_RECOMMEND_REQUEST_ITEMS);
		assertEquals(1, recRows.size());
		assertEquals(5L, ((Number) recRows.get(0).get("item_id")).longValue());
		assertEquals(2, recRows.get(0).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(recRows.get(0)));
	}

	@Test
	void apply_sameItemId_accumulatesThenMerges() {
		Items recommend5 = item(5L, 5L);
		Items main = item(2L, 2L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		when(itemsMapper.selectList(any())).thenReturn(List.of(main));
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(mainItemMapper.selectCount(any())).thenReturn(1L);
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
		Map<String, Object> params = freightFeeSnapshot();
		params.put(
				"recommend_item_id",
				List.of(Map.of("item_id", 5, "num", 1), Map.of("item_id", 5, "num", 3)));
		service.apply(1L, params);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		assertEquals(2, items.size());
		assertEquals(2L, ((Number) items.get(0).get("item_id")).longValue());
		assertEquals(1, items.get(0).get("num"));
		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(0)));
		assertEquals(5L, ((Number) items.get(1).get("item_id")).longValue());
		assertEquals(4, items.get(1).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(1)));
	}

	@Test
	void apply_sameItemIdAsSettlement_keepsSeparateRecommendLine() {
		Items recommend5 = item(5L, 5L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		when(itemsMapper.selectList(any())).thenReturn(List.of(recommend5));
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(mainItemMapper.selectCount(any())).thenReturn(1L);
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
		Map<String, Object> params = new HashMap<>();
		params.put("order_type", "normal");
		params.put("cart_type", "offline");
		params.put("items", List.of(line(5L, 2)));
		params.put("recommend_item_id", List.of(Map.of("item_id", 5, "num", 1)));
		service.apply(1L, params);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		assertEquals(2, items.size());
		assertEquals(5L, ((Number) items.get(0).get("item_id")).longValue());
		assertEquals(2, items.get(0).get("num"));
		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(0)));
		assertEquals(5L, ((Number) items.get(1).get("item_id")).longValue());
		assertEquals(1, items.get(1).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(1)));
	}

	@Test
	void apply_notSellable_throws() {
		Items recommend5 = item(5L, 5L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5);
		when(itemsMapper.selectList(any())).thenReturn(List.of());
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(false);
		when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("forbidden");

		Map<String, Object> params = freightFeeSnapshot();
		params.put("recommend_item_id", List.of(Map.of("item_id", 5, "num", 1)));
		assertThrows(BadRequestException.class, () -> service.apply(1L, params));
	}

	@Test
	void apply_bareId_throws() {
		when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("invalid");
		Map<String, Object> params = freightFeeSnapshot();
		params.put("recommend_item_id", List.of(5));
		assertThrows(BadRequestException.class, () -> service.apply(1L, params));
	}

	private void stubSellableMerge(Items recommend5, Items recommend8) {
		Items main = item(2L, 2L);
		when(itemsMapper.selectOne(any())).thenReturn(recommend5, recommend8);
		when(itemsMapper.selectList(any())).thenReturn(List.of(main));
		GoodsRecommendRuleRecommendItem recRow = new GoodsRecommendRuleRecommendItem();
		recRow.setRuleId(1L);
		recRow.setGoodsId(5L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recRow));
		when(mainItemMapper.selectCount(any())).thenReturn(1L);
		when(salabilityResolver.resolveProductModel(1L)).thenReturn("platform");
		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isSellable(any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(true);
	}

	private static Map<String, Object> freightFeeSnapshot() {
		Map<String, Object> input = new HashMap<>();
		input.put("order_type", "normal");
		input.put("cart_type", "offline");
		input.put("items", List.of(line(2L, 1)));
		return input;
	}

	private static Map<String, Object> line(long itemId, int num) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", itemId);
		line.put("num", num);
		line.put("activity_type", "normal");
		return line;
	}

	private static Items item(long itemId, long goodsId) {
		Items item = new Items();
		item.setItemId(itemId);
		item.setGoodsId(goodsId);
		item.setCompanyId(1L);
		return item;
	}
}
