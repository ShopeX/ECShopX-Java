package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDiscountCardValidityEvaluatorTest {

	private static final long TAGGED_ITEM = 2614L;
	private static final long UNTAGGED_ITEM = 2446L;
	private static final long TAG_ID = 1001L;

	@Mock
	private WxShopsListForUserDiscountService wxShopsListForUserDiscountService;

	@Mock
	private UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService;

	private UserDiscountCardValidityEvaluator evaluator;

	@BeforeEach
	void setUp() {
		evaluator = new UserDiscountCardValidityEvaluator(
				wxShopsListForUserDiscountService, userDiscountCardMatchedAmountService);
		when(wxShopsListForUserDiscountService.listShopsPoi(anyLong(), any())).thenReturn(Map.of("list", List.of()));
		when(userDiscountCardMatchedAmountService.loadScopes(anyLong(), any()))
				.thenReturn(Map.of(
						TAGGED_ITEM, new CouponCartItemScope(TAGGED_ITEM, TAGGED_ITEM, null, null, Set.of(TAG_ID)),
						UNTAGGED_ITEM, new CouponCartItemScope(UNTAGGED_ITEM, UNTAGGED_ITEM, null, null, Set.of())));
	}

	@Test
	@DisplayName("指定标签券活动商品金额不满足门槛时 valid=false 且不可选")
	void tagCouponBelowLeastCostIsInvalid() {
		Map<String, Object> card = tagCashCard(2000);
		List<Map<String, Object>> cards = new ArrayList<>();
		cards.add(card);

		evaluator.evaluate(1L, 45097L, pickerRequest(), cartItems(), cards);

		assertThat(card.get("valid")).isEqualTo(false);
		assertThat(String.valueOf(card.get("invalid_desc"))).contains("订单金额需满");
		@SuppressWarnings("unchecked")
		Map<String, Object> coupon = (Map<String, Object>) card.get("coupon");
		assertThat(coupon.get("valid")).isEqualTo(false);
	}

	@Test
	@DisplayName("指定标签券活动商品金额满足门槛时仍可用")
	void tagCouponMeetingLeastCostStaysValid() {
		Map<String, Object> card = tagCashCard(1000);
		List<Map<String, Object>> cards = new ArrayList<>();
		cards.add(card);

		evaluator.evaluate(1L, 45097L, pickerRequest(), cartItems(), cards);

		assertThat(card.get("valid")).isEqualTo(true);
	}

	private static Map<String, Object> tagCashCard(int leastCostFen) {
		long now = System.currentTimeMillis() / 1000L;
		Map<String, Object> card = new LinkedHashMap<>();
		card.put("status", 1);
		card.put("begin_date", now - 10);
		card.put("end_date", now + 86400);
		card.put("card_type", "cash");
		card.put("least_cost", leastCostFen);
		card.put("reduce_cost", 500);
		card.put("use_bound", 3);
		card.put("rel_item_ids", "," + TAG_ID + ",");
		card.put("rel_shops_ids", "all");
		card.put("title", "标签券");
		card.put("code", "G3T2JDN97Z84");
		card.put("card_id", 99L);
		return card;
	}

	private static Map<Long, Map<String, Object>> cartItems() {
		Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
		Map<String, Object> tagged = new HashMap<>();
		tagged.put("item_id", TAGGED_ITEM);
		tagged.put("total_fee", 1390L);
		items.put(TAGGED_ITEM, tagged);
		Map<String, Object> untagged = new HashMap<>();
		untagged.put("item_id", UNTAGGED_ITEM);
		untagged.put("total_fee", 12000L);
		items.put(UNTAGGED_ITEM, untagged);
		return items;
	}

	private static UserDiscountNewGetCardListRequest pickerRequest() {
		return new UserDiscountNewGetCardListRequest(
				null,
				null,
				null,
				null,
				null,
				null,
				1,
				10,
				"mall",
				"picker",
				null,
				"0",
				"true",
				"cart",
				null,
				null,
				"0",
				null,
				"true");
	}
}
