package cn.shopex.ecshopx.kaquan.service.order.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
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

/**
 * POS 购物车券后预览（ECX-11082）：按下单同口径计算指定券码优惠并分摊到传入行。
 * 仅预览，不写订单数据；券不可用/门槛不满足时 deduct_fen=0 且不改行。
 */
@ExtendWith(MockitoExtension.class)
class NormalOrderCheckoutCouponFacadePreviewTest {

	private static final long COMPANY_ID = 38L;
	private static final long OPERATOR_ID = 21L;
	private static final long USER_ID = 3850L;
	private static final long DISTRIBUTOR_ID = 129L;
	private static final long ITEM_A = 7666L;
	private static final long ITEM_B = 8116L;

	@Mock private AdminUserCardListFacadeService adminUserCardListFacadeService;
	@Mock private UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService;

	private NormalOrderCheckoutCouponFacade facade;

	@BeforeEach
	void setUp() {
		facade = new NormalOrderCheckoutCouponFacade(
				adminUserCardListFacadeService, userDiscountCardMatchedAmountService);
	}

	@Test
	@DisplayName("全场代金券按 reduce_cost 预览并分摊到行")
	void previewCashCouponDeductsAndSplitsLines() {
		Map<String, Object> card = cashCard("POS-CASH-001", 1000, 500, true);
		stubCardList(card);

		List<Map<String, Object>> lines = cartLines();
		Map<String, Object> out = facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, USER_ID, DISTRIBUTOR_ID, "POS-CASH-001", lines);

		assertThat(((Number) out.get("deduct_fen")).longValue()).isEqualTo(500L);

		Map<String, Object> lineA = lines.get(0);
		Map<String, Object> lineB = lines.get(1);
		// 行 A 占 19900/20100，Math.round 占比分摊；行 B 为末行兜底
		assertThat(((Number) lineA.get("total_fee")).longValue()).isEqualTo(19405L);
		assertThat(((Number) lineA.get("discount_fee")).longValue()).isEqualTo(495L);
		assertThat(String.valueOf(lineA.get("coupon_discount"))).isEqualTo("495");
		assertThat(lineA.get("discount_info")).asList().hasSize(1);
		// 行 B 为末行兜底
		assertThat(((Number) lineB.get("total_fee")).longValue()).isEqualTo(195L);
		assertThat(((Number) lineB.get("discount_fee")).longValue()).isEqualTo(5L);
	}

	@Test
	@DisplayName("全场折扣券按 discount 比例与 most_cost 限额预览并分摊到行")
	void previewDiscountCouponDeductsByRate() {
		Map<String, Object> card = discountCard("POS-DISC-001", 10, 0, true);
		stubCardList(card);

		List<Map<String, Object>> lines = cartLines();
		Map<String, Object> out = facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, USER_ID, DISTRIBUTOR_ID, "POS-DISC-001", lines);

		// basis=20100（无 most_cost 限额），disc=10 → DOWN(20100*0.10)=2010
		assertThat(((Number) out.get("deduct_fen")).longValue()).isEqualTo(2010L);
		assertThat(String.valueOf(lines.get(0).get("coupon_discount"))).isNotBlank();
		assertThat(Boolean.TRUE.equals(lines.get(0).get("coupon_valid"))).isTrue();
		long sumLineCoupon = lines.stream()
				.mapToLong(l -> Long.parseLong(String.valueOf(l.get("coupon_discount"))))
				.sum();
		assertThat(sumLineCoupon).isEqualTo(2010L);
	}

	@Test
	@DisplayName("门槛不满足时 deduct_fen=0 且不改动行")
	void previewSkipsBelowLeastCost() {
		Map<String, Object> card = cashCard("POS-CASH-001", 99999, 500, true);
		stubCardList(card);

		List<Map<String, Object>> lines = cartLines();
		Map<String, Object> out = facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, USER_ID, DISTRIBUTOR_ID, "POS-CASH-001", lines);

		assertThat(((Number) out.get("deduct_fen")).longValue()).isEqualTo(0L);
		// 门槛不满足时不改行：行 total_fee 保持原字符串口径
		assertThat(String.valueOf(lines.get(0).get("total_fee"))).isEqualTo("19900");
		assertThat(lines.get(0).get("coupon_discount")).isNull();
	}

	@Test
	@DisplayName("券不可用/券码为空/无会员时 deduct_fen=0")
	void previewReturnsZeroForInvalidInputs() {
		Map<String, Object> invalidCard = cashCard("POS-CASH-001", 1000, 500, false);
		stubCardList(invalidCard);
		List<Map<String, Object>> lines = cartLines();

		assertThat(((Number) facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, USER_ID, DISTRIBUTOR_ID, "POS-CASH-001", lines)
				.get("deduct_fen")).longValue()).isEqualTo(0L);

		// 券码为空 / 无会员：提前返回，不查券列表
		assertThat(((Number) facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, USER_ID, DISTRIBUTOR_ID, " ", lines)
				.get("deduct_fen")).longValue()).isEqualTo(0L);
		assertThat(((Number) facade.previewCouponDeduction(
				COMPANY_ID, OPERATOR_ID, 0L, DISTRIBUTOR_ID, "POS-CASH-001", lines)
				.get("deduct_fen")).longValue()).isEqualTo(0L);
	}

	private void stubCardList(Map<String, Object> card) {
		when(adminUserCardListFacadeService.buildList(
						anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(),
						anyBoolean(), anyInt(), anyInt(), any()))
				.thenReturn(Map.of("list", new ArrayList<>(List.of(card))));
	}

	private static List<Map<String, Object>> cartLines() {
		Map<String, Object> lineA = new LinkedHashMap<>();
		lineA.put("item_id", ITEM_A);
		lineA.put("num", 1);
		lineA.put("total_fee", "19900");
		Map<String, Object> lineB = new LinkedHashMap<>();
		lineB.put("item_id", ITEM_B);
		lineB.put("num", 1);
		lineB.put("total_fee", 200);
		List<Map<String, Object>> lines = new ArrayList<>();
		lines.add(lineA);
		lines.add(lineB);
		return lines;
	}

	private static Map<String, Object> cashCard(String code, int leastCost, int reduceCost, boolean valid) {
		Map<String, Object> card = new HashMap<>();
		card.put("card_type", "cash");
		card.put("least_cost", leastCost);
		card.put("reduce_cost", reduceCost);
		card.put("use_bound", 0);
		card.put("valid", valid);
		card.put("code", code);
		card.put("card_id", 99L);
		card.put("title", "全场代金券");
		Map<String, Object> coupon = new HashMap<>();
		coupon.put("valid", valid);
		coupon.put("code", code);
		card.put("coupon", coupon);
		return card;
	}

	private static Map<String, Object> discountCard(String code, int discount, int mostCost, boolean valid) {		Map<String, Object> card = new HashMap<>();
		card.put("card_type", "discount");
		card.put("discount", discount);
		card.put("most_cost", mostCost);
		card.put("least_cost", 0);
		card.put("use_bound", 0);
		card.put("valid", valid);
		card.put("code", code);
		card.put("card_id", 100L);
		card.put("title", "全场折扣券");
		Map<String, Object> coupon = new HashMap<>();
		coupon.put("valid", valid);
		coupon.put("code", code);
		card.put("coupon", coupon);
		return card;
	}
}
