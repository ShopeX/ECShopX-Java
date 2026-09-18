package cn.shopex.ecshopx.goods.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
class OrderCheckoutFullGiftServiceItemOrderTest {

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
	}

	@Test
	void applyGiftActivities_insertsGiftImmediatelyAfterLinkedMainItem() {
		when(companysRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get(anyString(), anyString())).thenReturn(null);
		when(giftActivityStoreAdjustService.adjustSingleGift(anyLong(), anyString(), any(), any()))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> gift = inv.getArgument(2);
							return intVal(gift.get("gift_num")) > 0;
						});

		List<Map<String, Object>> items = new ArrayList<>();
		items.add(mainItem(7753L, "泄压阀 电池包防爆阀", 10));
		items.add(mainItem(7894L, "测试", 1));

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
		od.put("item_fee", 30100L);
		od.put("items_promotion", new ArrayList<>());

		Map<String, Object> gift = new LinkedHashMap<>();
		gift.put("item_id", 7894L);
		gift.put("item_name", "测试");
		gift.put("gift_num", 1);
		gift.put("price", 100);
		gift.put("store", 1);
		gift.put("supplier_id", 657);

		Map<String, Object> activity = new LinkedHashMap<>();
		activity.put("activity_id", 468L);
		activity.put("activity_item_ids", List.of(7753L));
		activity.put("gifts", List.of(gift));
		activity.put(
				"discount_desc",
				Map.of("type", "full_gift", "id", 468, "info", "测试", "max_limit", Long.MAX_VALUE));

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);
		p.getParams().put("receipt_type", "logistics");

		service.applyGiftActivitiesFromCheckoutMeta(p, Map.of("gift_activity", List.of(activity)));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> out = (List<Map<String, Object>>) od.get("items");
		assertEquals(3, out.size());
		assertEquals(7753L, out.get(0).get("item_id"));
		assertEquals("normal", out.get(0).get("order_item_type"));
		assertEquals(7894L, out.get(1).get("item_id"));
		assertEquals("gift", out.get(1).get("order_item_type"));
		assertEquals(7894L, out.get(2).get("item_id"));
		assertEquals("normal", out.get(2).get("order_item_type"));
	}

	private static Map<String, Object> mainItem(long itemId, String name, int num) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", itemId);
		m.put("item_name", name);
		m.put("num", num);
		m.put("order_item_type", "normal");
		m.put("total_fee", num * 100);
		m.put("item_fee", num * 100);
		m.put("discount_fee", 0);
		return m;
	}

	private static int intVal(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (Exception e) {
			return 0;
		}
	}
}
