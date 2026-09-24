package cn.shopex.ecshopx.kaquan.service.order.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.kaquan.service.discount.AdminUserCardListFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountCardMatchedAmountService;
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
class NormalOrderCheckoutCouponFacadeTagLeastCostTest {

	private static final long TAGGED_ITEM = 2614L;
	private static final long UNTAGGED_ITEM = 2446L;
	private static final long TAG_ID = 1001L;

	@Mock
	private AdminUserCardListFacadeService adminUserCardListFacadeService;

	@Mock
	private UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService;

	private NormalOrderCheckoutCouponFacade facade;

	@BeforeEach
	void setUp() {
		facade = new NormalOrderCheckoutCouponFacade(
				adminUserCardListFacadeService, userDiscountCardMatchedAmountService);
		lenient().when(userDiscountCardMatchedAmountService.loadScopes(anyLong(), any()))
				.thenReturn(Map.of(
						TAGGED_ITEM, new CouponCartItemScope(TAGGED_ITEM, TAGGED_ITEM, null, null, Set.of(TAG_ID)),
						UNTAGGED_ITEM, new CouponCartItemScope(UNTAGGED_ITEM, UNTAGGED_ITEM, null, null, Set.of())));
	}

	@Test
	@DisplayName("显式选择门槛不满足的指定标签券时不扣减")
	void explicitTagCouponBelowLeastCostIsNotApplied() {
		Map<String, Object> card = tagCashCard(true);
		when(adminUserCardListFacadeService.buildList(
						anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(),
						anyBoolean(), anyInt(), anyInt(), any()))
				.thenReturn(Map.of("list", new ArrayList<>(List.of(card))));

		NormalOrderCreateParams p = checkoutParams();
		long totalBefore = (long) p.getOrderData().get("total_fee");
		facade.applyOptimalCouponAndSilentDeduction(p);

		assertThat(p.getOrderData().get("total_fee")).isEqualTo(totalBefore);
		assertThat(p.getOrderData().get("coupon_discount")).isNull();
	}

	@Test
	@DisplayName("卡券已被校验为不可用时即使带券码也不使用")
	void invalidCardCodeIsIgnored() {
		Map<String, Object> card = tagCashCard(false);
		when(adminUserCardListFacadeService.buildList(
						anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(),
						anyBoolean(), anyInt(), anyInt(), any()))
				.thenReturn(Map.of("list", new ArrayList<>(List.of(card))));

		NormalOrderCreateParams p = checkoutParams();
		long totalBefore = (long) p.getOrderData().get("total_fee");
		facade.applyOptimalCouponAndSilentDeduction(p);

		assertThat(p.getOrderData().get("total_fee")).isEqualTo(totalBefore);
		assertThat(p.getOrderData().get("coupon_discount")).isNull();
	}

	private static Map<String, Object> tagCashCard(boolean valid) {
		Map<String, Object> card = new HashMap<>();
		card.put("card_type", "cash");
		card.put("least_cost", 2000);
		card.put("reduce_cost", 500);
		card.put("use_bound", 3);
		card.put("rel_item_ids", "," + TAG_ID + ",");
		card.put("valid", valid);
		card.put("code", "G3T2JDN97Z84");
		card.put("card_id", 99L);
		card.put("title", "标签券");
		Map<String, Object> coupon = new HashMap<>();
		coupon.put("valid", valid);
		coupon.put("code", "G3T2JDN97Z84");
		card.put("coupon", coupon);
		return card;
	}

	private static NormalOrderCreateParams checkoutParams() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().put("not_use_coupon", 0);
		p.getParams().put("user_id", 45097L);
		p.getParams().put("company_id", 1L);
		p.getParams().put("is_online_order", true);
		p.getParams().put("coupon_discount", "G3T2JDN97Z84");
		Map<String, Object> od = p.getOrderData();
		od.put("company_id", 1L);
		od.put("distributor_id", 0L);
		od.put("total_fee", 13390L);
		od.put("freight_fee", 0);
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> tagged = new LinkedHashMap<>();
		tagged.put("item_id", TAGGED_ITEM);
		tagged.put("num", 1);
		tagged.put("total_fee", 1390);
		items.add(tagged);
		Map<String, Object> untagged = new LinkedHashMap<>();
		untagged.put("item_id", UNTAGGED_ITEM);
		untagged.put("num", 4);
		untagged.put("total_fee", 12000);
		items.add(untagged);
		od.put("items", items);
		return p;
	}
}
