package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.promotions.port.PointUpvaluationEligibleActivityReadPort;
import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import cn.shopex.ecshopx.point.service.PointMemberPointToMoneyService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Aligns checkout max_point caps with PHP {@code PointMemberRuleService::orderMaxPoint}.
 */
class NormalOrderCheckoutPointDeductOrderMaxPointTest {

	private static final long COMPANY_ID = 38L;

	private PointMemberRuleReadService ruleReadService;
	private NormalOrderPointDeductionApplyService deductionApplyService;
	private NormalOrderCheckoutPointDeductService service;

	@BeforeEach
	void setUp() {
		ruleReadService = mock(PointMemberRuleReadService.class);
		PointMemberMoneyToPointService moneyToPointService =
				new PointMemberMoneyToPointService(ruleReadService);
		PointMemberPointToMoneyService pointToMoneyService =
				new PointMemberPointToMoneyService(ruleReadService, moneyToPointService);
		deductionApplyService = mock(NormalOrderPointDeductionApplyService.class);
		PointUpvaluationEligibleActivityReadPort upvaluationPort =
				mock(PointUpvaluationEligibleActivityReadPort.class);
		when(upvaluationPort.getEligibleActivity(anyLong(), anyLong(), any())).thenReturn(Optional.empty());
		doThrow(new ResourceException("preview skip"))
				.when(deductionApplyService)
				.apply(any(), anyInt(), anyLong(), anyLong(), any());

		service =
				new NormalOrderCheckoutPointDeductService(
						upvaluationPort,
						mock(NormalOrderPointUpvaluationDeductionService.class),
						deductionApplyService,
						moneyToPointService,
						pointToMoneyService);
	}

	@Test
	@DisplayName("30% 上限 + 可抵运费：max_point/max_point_ziti 与 PHP orderMaxPoint 一致")
	void applyCheckoutPointDeduct_capsMatchPhpOrderMaxPoint() {
		mockRule(openRule(30, "1", true));

		Map<String, Object> orderData = orderWithFreight(207100L, 100L, 207000L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 0);

		service.applyCheckoutPointDeduct(orderData, params, COMPANY_ID, 1170L, openRule(30, "1", true), 78482L);

		assertThat(orderData.get("max_point")).isEqualTo(622);
		assertThat(orderData.get("max_point_ziti")).isEqualTo(621);
		assertThat(orderData.get("limit_point")).isEqualTo(622);
	}

	@Test
	@DisplayName("分段 ceil 曾导致 max_point 超出 moneyOutLimit：整单一次换算后应为 150")
	void applyCheckoutPointDeduct_neverExceedsMoneyOutLimitWithFreightDeduct() {
		mockRule(openRule(50, "10", true));

		Map<String, Object> orderData = orderWithFreight(2999L, 10L, 2989L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 0);

		service.applyCheckoutPointDeduct(orderData, params, COMPANY_ID, 1L, openRule(50, "10", true), 10000L);

		assertThat(orderData.get("max_point")).isEqualTo(150);
		assertThat(orderData.get("limit_point")).isEqualTo(150);
	}

	@Test
	@DisplayName("不可抵运费：max_point 与 max_point_ziti 相同")
	void applyCheckoutPointDeduct_whenFreightNotDeductible_zitiEqualsMaxPoint() {
		mockRule(openRule(30, "1", false));

		Map<String, Object> orderData = orderWithFreight(207100L, 100L, 207000L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("point_use", 0);

		service.applyCheckoutPointDeduct(orderData, params, COMPANY_ID, 1L, openRule(30, "1", false), 78482L);

		assertThat(orderData.get("max_point")).isEqualTo(621);
		assertThat(orderData.get("max_point_ziti")).isEqualTo(621);
	}

	private void mockRule(Map<String, Object> rule) {
		when(ruleReadService.getPointRule(anyLong())).thenReturn(rule);
	}

	private static Map<String, Object> openRule(int proportionLimit, String deductPoint, boolean canDeductFreight) {
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("isOpenMemberPoint", "true");
		rule.put("isOpenDeductPoint", "true");
		rule.put("deduct_proportion_limit", proportionLimit);
		rule.put("deduct_point", deductPoint);
		rule.put("can_deduct_freight", canDeductFreight ? "1" : "0");
		rule.put("name", "积分");
		return rule;
	}

	private static Map<String, Object> orderWithFreight(long totalFeeFen, long freightFen, long itemTotalFeeFen) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("price", itemTotalFeeFen);
		item.put("num", 1L);
		item.put("total_fee", itemTotalFeeFen);

		List<Map<String, Object>> items = new ArrayList<>();
		items.add(item);

		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("total_fee", totalFeeFen);
		orderData.put("freight_fee", freightFen);
		orderData.put("point_use", 0);
		orderData.put("items", items);
		return orderData;
	}
}
