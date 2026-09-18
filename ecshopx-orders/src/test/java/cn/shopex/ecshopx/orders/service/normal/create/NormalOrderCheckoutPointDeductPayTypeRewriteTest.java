package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.PointUpvaluationEligibleActivityReadPort;
import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import cn.shopex.ecshopx.point.service.PointMemberPointToMoneyService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormalOrderCheckoutPointDeductPayTypeRewriteTest {

	private PointUpvaluationEligibleActivityReadPort upvaluationPort;
	private NormalOrderPointUpvaluationDeductionService upvaluationDeductionService;
	private NormalOrderPointDeductionApplyService deductionApplyService;
	private PointMemberMoneyToPointService moneyToPointService;
	private PointMemberPointToMoneyService pointToMoneyService;
	private NormalOrderCheckoutPointDeductService service;

	@BeforeEach
	void setUp() {
		upvaluationPort = mock(PointUpvaluationEligibleActivityReadPort.class);
		upvaluationDeductionService = mock(NormalOrderPointUpvaluationDeductionService.class);
		deductionApplyService = mock(NormalOrderPointDeductionApplyService.class);
		moneyToPointService = mock(PointMemberMoneyToPointService.class);
		pointToMoneyService = mock(PointMemberPointToMoneyService.class);
		when(upvaluationPort.getEligibleActivity(anyLong(), anyLong(), any())).thenReturn(Optional.empty());
		when(moneyToPointService.moneyToPoint(anyLong(), anyLong())).thenReturn(100L);
		when(pointToMoneyService.pointToMoney(anyLong(), anyLong())).thenReturn(100);
		when(pointToMoneyService.orderMaxMoneyToPoint(anyLong(), anyLong())).thenReturn(100L);
		service =
				new NormalOrderCheckoutPointDeductService(
						upvaluationPort,
						upvaluationDeductionService,
						deductionApplyService,
						moneyToPointService,
						pointToMoneyService);
	}

	@Test
	@DisplayName("积分抵扣后 total_fee=0：订单与入参 pay_type 改写为 point")
	void applyCheckoutPointDeduct_whenFullyDeducted_rewritesPayTypeToPoint() {
		Map<String, Object> orderData = baseOrder("offline_pay", 16590L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 16590);
		params.put("pay_type", "offline_pay");
		Map<String, Object> rule = openPointRule();

		doAnswer(
						inv -> {
							Map<String, Object> od = inv.getArgument(0);
							od.put("total_fee", 0L);
							od.put("point_fee", 16590);
							od.put("point_use", 16590);
							od.put("real_use_point", 16590);
							od.put("point", "16590");
							return null;
						})
				.when(deductionApplyService)
				.apply(any(), anyInt(), anyLong(), anyLong(), any());

		service.applyCheckoutPointDeduct(orderData, params, 1L, 45097L, rule, 99999L);

		assertThat(orderData.get("pay_type")).isEqualTo("point");
		assertThat(params.get("pay_type")).isEqualTo("point");
		assertThat(orderData.get("total_fee")).isEqualTo(0L);
	}

	@Test
	@DisplayName("积分抵扣后仍有余额：保持原 pay_type")
	void applyCheckoutPointDeduct_whenPartialDeduct_keepsOriginalPayType() {
		Map<String, Object> orderData = baseOrder("wxpay", 20000L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 1000);
		params.put("pay_type", "wxpay");
		Map<String, Object> rule = openPointRule();

		doAnswer(
						inv -> {
							Map<String, Object> od = inv.getArgument(0);
							od.put("total_fee", 19000L);
							od.put("point_fee", 1000);
							od.put("point_use", 1000);
							od.put("real_use_point", 1000);
							od.put("point", "1000");
							return null;
						})
				.when(deductionApplyService)
				.apply(any(), anyInt(), anyLong(), anyLong(), any());

		service.applyCheckoutPointDeduct(orderData, params, 1L, 1L, rule, 99999L);

		assertThat(orderData.get("pay_type")).isEqualTo("wxpay");
		assertThat(params.get("pay_type")).isEqualTo("wxpay");
	}

	@Test
	@DisplayName("0 元订单无积分抵扣：不改写 pay_type")
	void rewriteHelper_whenZeroFeeWithoutPoints_keepsPayType() {
		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("total_fee", 0L);
		orderData.put("point_use", 0);
		orderData.put("pay_type", "wxpay");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("pay_type", "wxpay");

		NormalOrderCheckoutPointDeductService.rewritePayTypeToPointIfFullyPointDeducted(orderData, params);

		assertThat(orderData.get("pay_type")).isEqualTo("wxpay");
		assertThat(params.get("pay_type")).isEqualTo("wxpay");
	}

	@Test
	@DisplayName("pay_type=point 且可全额抵扣：强制 point_use=max_point 并抵扣到 0")
	void applyCheckoutPointDeduct_whenPayTypePointAndFullAmount_forcesMaxPointAndDeducts() {
		Map<String, Object> orderData = baseOrder("point", 14000L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 0);
		params.put("pay_type", "point");
		Map<String, Object> rule = openPointRule();

		when(moneyToPointService.moneyToPoint(anyLong(), anyLong()))
				.thenAnswer(inv -> Math.max(1L, (longVal(inv.getArgument(1)) + 99L) / 100L));
		when(pointToMoneyService.pointToMoney(anyLong(), anyLong()))
				.thenAnswer(inv -> (int) Math.min(Integer.MAX_VALUE, longVal(inv.getArgument(1)) * 100L));

		doAnswer(
						inv -> {
							Map<String, Object> od = inv.getArgument(0);
							int use = inv.getArgument(1);
							od.put("total_fee", 0L);
							od.put("point_fee", use * 100L);
							od.put("point_use", use);
							od.put("real_use_point", use);
							od.put("point", String.valueOf(use));
							return null;
						})
				.when(deductionApplyService)
				.apply(any(), anyInt(), anyLong(), anyLong(), any());

		service.applyCheckoutPointDeduct(orderData, params, 1L, 45097L, rule, 99999L);

		assertThat(orderData.get("point_use")).isEqualTo(140);
		assertThat(params.get("point_use")).isEqualTo(140);
		assertThat(orderData.get("total_fee")).isEqualTo(0L);
		assertThat(orderData.get("pay_type")).isEqualTo("point");
	}

	@Test
	@DisplayName("pay_type=point 但不足以全额：抛错")
	void applyCheckoutPointDeduct_whenPayTypePointWithoutFullAmount_throws() {
		Map<String, Object> orderData = baseOrder("point", 14000L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("pay_type", "point");
		params.put("point_use", 100);
		Map<String, Object> rule = openPointRule();

		// member points convert to less than order fee → full_amount=false
		when(moneyToPointService.moneyToPoint(anyLong(), anyLong())).thenAnswer(inv -> inv.getArgument(1));
		when(pointToMoneyService.pointToMoney(anyLong(), anyLong())).thenReturn(100);

		org.junit.jupiter.api.Assertions.assertThrows(
				cn.shopex.ecshopx.common.exception.ResourceException.class,
				() -> service.applyCheckoutPointDeduct(orderData, params, 1L, 1L, rule, 100L));
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> baseOrder(String payType, long totalFee) {
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("pay_type", payType);
		od.put("total_fee", totalFee);
		od.put("freight_fee", 0L);
		od.put("point_use", 0);
		od.put("items", new ArrayList<>());
		return od;
	}

	private static Map<String, Object> openPointRule() {
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("isOpenMemberPoint", true);
		rule.put("isOpenDeductPoint", true);
		rule.put("deduct_point", "1");
		rule.put("deduct_proportion_limit", 100);
		rule.put("can_deduct_freight", true);
		rule.put("name", "积分");
		return rule;
	}
}
