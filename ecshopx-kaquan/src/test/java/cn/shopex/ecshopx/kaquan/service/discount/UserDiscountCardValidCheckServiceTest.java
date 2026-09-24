package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.kaquan.service.discount.dto.CartItemMoneyRow;
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
class UserDiscountCardValidCheckServiceTest {

	private static final long TAGGED_ITEM = 2614L;
	private static final long UNTAGGED_ITEM = 2446L;
	private static final long TAG_ID = 1001L;

	@Mock
	private UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService;

	private UserDiscountCardValidCheckService service;

	@BeforeEach
	void setUp() {
		service = new UserDiscountCardValidCheckService(userDiscountCardMatchedAmountService);
		when(userDiscountCardMatchedAmountService.loadScopes(anyLong(), any()))
				.thenReturn(Map.of(
						TAGGED_ITEM, new CouponCartItemScope(TAGGED_ITEM, TAGGED_ITEM, null, null, Set.of(TAG_ID)),
						UNTAGGED_ITEM, new CouponCartItemScope(UNTAGGED_ITEM, UNTAGGED_ITEM, null, null, Set.of())));
	}

	@Test
	@DisplayName("结算校验：指定标签券活动商品金额不满足门槛则不可用")
	void tagCouponBelowLeastCostSetInvalid() {
		Map<String, Object> card = new HashMap<>();
		card.put("card_type", "cash");
		card.put("least_cost", 2000);
		card.put("use_bound", 3);
		card.put("rel_item_ids", "," + TAG_ID + ",");
		card.put("valid", true);
		Map<String, Object> coupon = new HashMap<>();
		coupon.put("valid", true);
		card.put("coupon", coupon);
		Map<String, Object> lists = new HashMap<>();
		lists.put("list", new ArrayList<>(List.of(card)));

		Map<Long, CartItemMoneyRow> items = new LinkedHashMap<>();
		items.put(TAGGED_ITEM, new CartItemMoneyRow(1L, 1390L));
		items.put(UNTAGGED_ITEM, new CartItemMoneyRow(4L, 12000L));

		service.apply(1L, lists, 0L, items);

		assertThat(card.get("valid")).isEqualTo(false);
		assertThat(coupon.get("valid")).isEqualTo(false);
	}
}
