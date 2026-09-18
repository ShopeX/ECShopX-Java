package cn.shopex.ecshopx.goods.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.GiftSettingRedisService;
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GiftActivityStoreAdjustServiceTest {

	@Mock
	private GiftSettingRedisService giftSettingRedisService;

	@Mock
	private ItemAvailableStoreResolver itemAvailableStoreResolver;

	private GiftActivityStoreAdjustService service;

	@BeforeEach
	void setUp() {
		service = new GiftActivityStoreAdjustService(giftSettingRedisService, itemAvailableStoreResolver);
	}

	@Test
	void checkGiftStoreFalse_doesNotAdjust() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", false));
		List<Map<String, Object>> activities = List.of(activity(gift(10, 100)));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "logistics", activities);

		assertEquals(10, ((List<?>) out.get(0).get("gifts")).size() > 0
				? ((Map<?, ?>) ((List<?>) out.get(0).get("gifts")).get(0)).get("gift_num")
				: null);
		verify(itemAvailableStoreResolver, never()).resolveAvailable(anyLong(), anyString(), any());
	}

	@Test
	void checkGiftStoreTrue_clampsGiftNumAndUpdatesStore() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(3);
		Map<String, Object> gift = gift(10, 100);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(gift));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "logistics", activities);

		@SuppressWarnings("unchecked")
		Map<String, Object> adjusted = ((List<Map<String, Object>>) out.get(0).get("gifts")).get(0);
		assertEquals(3, adjusted.get("gift_num"));
		assertEquals(3, adjusted.get("store"));
	}

	@Test
	void checkGiftStoreTrue_keepsZeroGiftRowWithUnavailableTip() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(0);
		List<Map<String, Object>> activities = new ArrayList<>();
		Map<String, Object> g = gift(5, 100);
		g.put("item_name", "测试赠品");
		activities.add(activity(g));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "logistics", activities);

		assertEquals(1, out.size());
		assertEquals(1L, out.get(0).get("activity_id"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts = (List<Map<String, Object>>) out.get(0).get("gifts");
		assertEquals(1, gifts.size());
		assertEquals(0, gifts.get(0).get("gift_num"));
		assertEquals(5, gifts.get(0).get("original_gift_num"));
		assertEquals("赠品「测试赠品」暂时无货", out.get(0).get("gift_unavailable_tip"));
	}

	@Test
	void adjustSingleGift_clampsWhenEnabled() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("ziti"), any())).thenReturn(2);
		Map<String, Object> gift = gift(5, 50);

		service.adjustSingleGift(1L, "ziti", gift);

		assertEquals(2, gift.get("gift_num"));
		assertEquals(2, gift.get("store"));
	}

	@Test
	void checkGiftStoreTrue_keepsGiftWhenSupplierAvailableDespiteZeroLocalStore() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(5);
		Map<String, Object> gift = gift(3, 0);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(gift));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "logistics", activities);

		assertEquals(1, out.size());
		@SuppressWarnings("unchecked")
		Map<String, Object> adjusted = ((List<Map<String, Object>>) out.get(0).get("gifts")).get(0);
		assertEquals(3, adjusted.get("gift_num"));
		assertEquals(5, adjusted.get("store"));
	}

	@Test
	void checkGiftStoreTrue_blankReceiptType_usesCartDisplayAvailableAndKeepsZeroGiftRow() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailableForCartDisplay(eq(1L), any())).thenReturn(0);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(gift(1, 0)));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, null, activities);

		assertEquals(1, out.size());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts = (List<Map<String, Object>>) out.get(0).get("gifts");
		assertEquals(1, gifts.size());
		assertEquals(0, gifts.get(0).get("gift_num"));
		assertEquals("赠品暂时无货", out.get(0).get("gift_unavailable_tip"));
		verify(itemAvailableStoreResolver).resolveAvailableForCartDisplay(eq(1L), any());
		verify(itemAvailableStoreResolver, never()).resolveAvailable(anyLong(), anyString(), any());
	}

	@Test
	void checkGiftStoreTrue_blankReceiptType_clampsByCartDisplayAvailable() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailableForCartDisplay(eq(1L), any())).thenReturn(2);
		Map<String, Object> gift = gift(5, 0);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(gift));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "", activities);

		@SuppressWarnings("unchecked")
		Map<String, Object> adjusted = ((List<Map<String, Object>>) out.get(0).get("gifts")).get(0);
		assertEquals(2, adjusted.get("gift_num"));
		assertEquals(2, adjusted.get("store"));
	}

	@Test
	void checkGiftStoreTrue_mainProductReservedConsumesAllStore_keepsZeroGiftWithTip() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(1);
		Map<String, Object> g = gift(1, 1);
		g.put("item_name", "测试");
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(g));
		List<Map<String, Object>> cartLines = List.of(checkedMainLine(100L, 1));

		List<Map<String, Object>> out =
				service.adjustGiftActivities(1L, "logistics", activities, cartLines);

		assertEquals(1, out.size());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts = (List<Map<String, Object>>) out.get(0).get("gifts");
		assertEquals(1, gifts.size());
		assertEquals(0, gifts.get(0).get("gift_num"));
		assertEquals("赠品「测试」暂时无货", out.get(0).get("gift_unavailable_tip"));
	}

	@Test
	void checkGiftStoreTrue_mainProductReservedLeavesRemainder_clampsGiftNum() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(2);
		Map<String, Object> g = gift(2, 2);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activity(g));
		List<Map<String, Object>> cartLines = List.of(checkedMainLine(100L, 1));

		List<Map<String, Object>> out =
				service.adjustGiftActivities(1L, "logistics", activities, cartLines);

		@SuppressWarnings("unchecked")
		Map<String, Object> adjusted = ((List<Map<String, Object>>) out.get(0).get("gifts")).get(0);
		assertEquals(1, adjusted.get("gift_num"));
		assertEquals(1, adjusted.get("store"));
	}

	@Test
	void checkGiftStoreTrue_twoGiftActivitiesSameSku_doNotOversell() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(2);
		Map<String, Object> g1 = gift(2, 2);
		Map<String, Object> g2 = gift(2, 2);
		List<Map<String, Object>> activities = new ArrayList<>();
		activities.add(activityWithId(11L, g1));
		activities.add(activityWithId(12L, g2));

		List<Map<String, Object>> out = service.adjustGiftActivities(1L, "logistics", activities);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts1 = (List<Map<String, Object>>) out.get(0).get("gifts");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts2 = (List<Map<String, Object>>) out.get(1).get("gifts");
		assertEquals(1, gifts1.size());
		assertEquals(2, gifts1.get(0).get("gift_num"));
		assertEquals(1, gifts2.size());
		assertEquals(0, gifts2.get(0).get("gift_num"));
		assertEquals("赠品暂时无货", out.get(1).get("gift_unavailable_tip"));
	}

	@Test
	void adjustSingleGift_withReservedMap_clampsAndUpdatesReserved() {
		when(giftSettingRedisService.getGiftSetting(1L)).thenReturn(Map.of("check_gift_store", true));
		when(itemAvailableStoreResolver.resolveAvailable(eq(1L), eq("logistics"), any())).thenReturn(3);
		Map<String, Object> gift = gift(2, 3);
		Map<Long, Integer> reserved = new HashMap<>();
		reserved.put(100L, 1);

		assertTrue(service.adjustSingleGift(1L, "logistics", gift, reserved));
		assertEquals(2, gift.get("gift_num"));
		assertEquals(2, gift.get("store"));
		assertEquals(3, reserved.get(100L));
	}

	private static Map<String, Object> checkedMainLine(long itemId, int num) {
		Map<String, Object> line = new HashMap<>();
		line.put("item_id", itemId);
		line.put("num", num);
		line.put("is_checked", true);
		return line;
	}

	private static Map<String, Object> gift(int giftNum, int store) {
		Map<String, Object> g = new HashMap<>();
		g.put("item_id", 100L);
		g.put("gift_num", giftNum);
		g.put("store", store);
		g.put("supplier_id", 9L);
		g.put("supplier_item_id", 88L);
		return g;
	}

	private static Map<String, Object> activity(Map<String, Object> gift) {
		return activityWithId(1L, gift);
	}

	private static Map<String, Object> activityWithId(long activityId, Map<String, Object> gift) {
		Map<String, Object> a = new HashMap<>();
		a.put("activity_id", activityId);
		List<Map<String, Object>> gifts = new ArrayList<>();
		gifts.add(gift);
		a.put("gifts", gifts);
		return a;
	}
}
