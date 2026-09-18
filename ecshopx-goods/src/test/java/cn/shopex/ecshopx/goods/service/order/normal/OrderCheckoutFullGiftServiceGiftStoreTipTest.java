package cn.shopex.ecshopx.goods.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutFullGiftServiceGiftStoreTipTest {

	@Mock
	private GiftActivityStoreAdjustService giftActivityStoreAdjustService;

	@Mock
	private ItemAvailableStoreResolver itemAvailableStoreResolver;

	@Mock
	private ItemsCategoryPathByItemService itemsCategoryPathByItemService;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	private OrderCheckoutFullGiftService service;

	@BeforeEach
	void setUp() {
		service =
				new OrderCheckoutFullGiftService(
						giftActivityStoreAdjustService,
						itemAvailableStoreResolver,
						itemsCategoryPathByItemService,
						companysRedisTemplate);
		when(companysRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get(anyString(), anyString())).thenReturn(null);
	}

	@Test
	void applyGiftActivities_zeroStore_recordsGiftStoreAdjustmentWithoutSwitch() {
		when(giftActivityStoreAdjustService.adjustSingleGift(anyLong(), anyString(), any(), any()))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> gift = inv.getArgument(2);
							gift.put("gift_num", 0);
							gift.put("store", 0);
							return false;
						});
		when(itemAvailableStoreResolver.resolveAvailable(eq(141L), eq("ziti"), any())).thenReturn(0);

		Map<String, Object> od = baseOrderData();
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);
		p.getParams().put("receipt_type", "logistics");

		service.applyGiftActivitiesFromCheckoutMeta(
				p, Map.of("gift_activity", List.of(activity(gift(7894L, "测试", 1, 657L)))));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> adj = (List<Map<String, Object>>) od.get("gift_store_adjustments");
		assertEquals(1, adj.size());
		assertEquals(0, adj.get(0).get("available_num"));
		assertFalse(Boolean.TRUE.equals(adj.get(0).get("can_switch_receipt")));
		assertTrue(String.valueOf(od.get("gift_store_adjust_tip")).contains("继续结算将不送无货赠品"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) od.get("items");
		assertEquals(1, items.size());
		assertEquals("normal", items.get(0).get("order_item_type"));
	}

	@Test
	void applyGiftActivities_supplierGiftOtherReceiptHasStore_canSwitch() {
		when(giftActivityStoreAdjustService.adjustSingleGift(anyLong(), anyString(), any(), any()))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> gift = inv.getArgument(2);
							gift.put("gift_num", 0);
							gift.put("store", 0);
							return false;
						});
		when(itemAvailableStoreResolver.resolveAvailable(eq(141L), eq("ziti"), any())).thenReturn(2);

		Map<String, Object> od = baseOrderData();
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);
		p.getParams().put("receipt_type", "logistics");

		service.applyGiftActivitiesFromCheckoutMeta(
				p, Map.of("gift_activity", List.of(activity(gift(7894L, "测试", 1, 657L)))));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> adj = (List<Map<String, Object>>) od.get("gift_store_adjustments");
		assertEquals(1, adj.size());
		assertTrue(Boolean.TRUE.equals(adj.get(0).get("can_switch_receipt")));
		assertEquals("ziti", adj.get(0).get("suggested_receipt_type"));
		assertTrue(String.valueOf(od.get("gift_store_adjust_tip")).contains("切换配送方式"));
	}

	private static Map<String, Object> baseOrderData() {
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> main = new LinkedHashMap<>();
		main.put("item_id", 7753L);
		main.put("item_name", "泄压阀");
		main.put("num", 10);
		main.put("order_item_type", "normal");
		main.put("total_fee", 30000);
		main.put("item_fee", 30000);
		main.put("discount_fee", 0);
		items.add(main);
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1271L);
		od.put("order_id", "OID");
		od.put("distributor_id", 0L);
		od.put("shop_id", 0L);
		od.put("mobile", "15601636091");
		od.put("receipt_type", "logistics");
		od.put("items", items);
		od.put("discount_fee", 0);
		od.put("item_fee", 30000L);
		od.put("items_promotion", new ArrayList<>());
		return od;
	}

	private static Map<String, Object> gift(long itemId, String name, int giftNum, long supplierId) {
		Map<String, Object> g = new LinkedHashMap<>();
		g.put("item_id", itemId);
		g.put("item_name", name);
		g.put("gift_num", giftNum);
		g.put("original_gift_num", giftNum);
		g.put("price", 100);
		g.put("store", 0);
		g.put("supplier_id", supplierId);
		g.put("supplier_item_id", 344L);
		return g;
	}

	private static Map<String, Object> activity(Map<String, Object> gift) {
		Map<String, Object> activity = new LinkedHashMap<>();
		activity.put("activity_id", 468L);
		activity.put("activity_item_ids", List.of(7753L));
		activity.put("gifts", List.of(gift));
		activity.put(
				"discount_desc",
				Map.of("type", "full_gift", "id", 468, "info", "测试", "max_limit", Long.MAX_VALUE));
		return activity;
	}
}
