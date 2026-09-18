package cn.shopex.ecshopx.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.point.MemberPointScheduleItemRow;
import cn.shopex.ecshopx.common.port.point.MemberPointScheduleOrderRow;
import cn.shopex.ecshopx.common.port.point.MemberPointSendScheduleDataPort;
import cn.shopex.ecshopx.common.port.point.PointMemberRuleReadPort;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointMemberServiceTest {

	@Mock
	private MemberPointSendScheduleDataPort scheduleDataPort;

	@Mock
	private PointMemberRuleReadPort pointMemberRuleReadPort;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@Mock
	private PointMemberLogMapper pointMemberLogMapper;

	@InjectMocks
	private PointMemberService pointMemberService;

	private static Map<String, Object> openOrderRule() {
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("isOpenMemberPoint", "true");
		rule.put("gain_time", 0);
		rule.put("gain_point", 1);
		rule.put("gain_limit", 9_999_999);
		rule.put("access", "order");
		rule.put("include_freight", false);
		return rule;
	}

	@Test
	@DisplayName("§3.1 §3.2: 单次入口同步跑完全量批处理（PHP 投递+消费内联）")
	void section1_and_2_inlineFullBatch() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(100L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(100L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);

		pointMemberService.scheduleSendMemberPoint();

		verify(scheduleDataPort, times(1)).countSendPointPendingOrders();
		verify(scheduleDataPort, times(1)).listSendPointPendingOrders(0, 100);
		verify(pointMemberRuleReadPort, times(1)).getPointRule(1L);
		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(eq(2L), eq(1L), anyInt(), eq(100L), any());
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(100L);
	}

	@Test
	@DisplayName("§3.4: totalCount==0 早退")
	void section34_emptyEarlyExit() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(0L);
		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();
		assertThat(r.processed()).isZero();
		verify(pointMemberAddPointService, never())
				.addPointForNormalOrderBonus(anyLong(), anyLong(), anyInt(), anyLong(), any());
		verify(scheduleDataPort, never()).markOrderSendPointDone(anyLong());
	}

	@Test
	@DisplayName("§3.5: totalPage 分页与按页加载规则")
	void section35_paginationAndRuleLoad() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(150L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(101L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		when(scheduleDataPort.listSendPointPendingOrders(100, 100)).thenReturn(List.of());
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(101L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);

		pointMemberService.scheduleSendMemberPoint();

		verify(scheduleDataPort, times(1)).listSendPointPendingOrders(0, 100);
		verify(scheduleDataPort, times(1)).listSendPointPendingOrders(100, 100);
		verify(pointMemberRuleReadPort, atLeastOnce()).getPointRule(1L);
	}

	@Test
	@DisplayName("§3.6 §3.7.3: 存在未完结售后 → point=0 仍尝试入账")
	void section36_and_section373_aftersalesBlocksPoints() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(200L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(openOrderRule());
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(200L))).thenReturn(Set.of(200L));

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isEqualTo(1);
		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(
						eq(2L), eq(1L), eq(0), eq(200L), eq("存在售后状态的订单，无法获取积分"));
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(200L);
	}

	@Test
	@DisplayName("§3.7.2: 积分规则未开启则跳过订单")
	void section372_ruleClosedSkips() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(300L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("isOpenMemberPoint", Boolean.FALSE);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(300L))).thenReturn(Set.of());

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isZero();
		verify(pointMemberAddPointService, never())
				.addPointForNormalOrderBonus(anyLong(), anyLong(), anyInt(), anyLong(), any());
		verify(scheduleDataPort, never()).markOrderSendPointDone(anyLong());
	}

	@Test
	@DisplayName("§3.7.2: 收货间隔不足（end_time 未过 gain_time）则跳过")
	void section372_endTimeTooSoonSkips() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		long tooSoon = Instant.now().getEpochSecond() + 86_400L * 365;
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(301L, 1L, 2L, tooSoon, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("gain_time", 7);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(301L))).thenReturn(Set.of());

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isZero();
		verify(pointMemberAddPointService, never())
				.addPointForNormalOrderBonus(anyLong(), anyLong(), anyInt(), anyLong(), any());
		verify(scheduleDataPort, never()).markOrderSendPointDone(anyLong());
	}

	@Test
	@DisplayName("§3.7.3.3: get_point_type=1 扣减退款 return_point")
	void section3733_legacyGetPointTypeWithRefund() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(400L, 1L, 2L, 1_700_000_000L, 1, 100, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(openOrderRule());
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(400L))).thenReturn(Set.of());
		when(scheduleDataPort.sumReturnPointForSuccessfulRefunds(400L)).thenReturn(30);
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);

		pointMemberService.scheduleSendMemberPoint();

		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(eq(2L), eq(1L), eq(70), eq(400L), any());
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(400L);
	}

	@Test
	@DisplayName("§3.7.3.4: access=items 按商品积分与访问表汇总")
	void section3734_itemsAccess() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(500L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("access", "items");
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(500L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);
		when(scheduleDataPort.listLineItemsForOrder(500L))
				.thenReturn(List.of(new MemberPointScheduleItemRow(10L, 2, 0)));
		when(scheduleDataPort.mapItemPointAccess(eq(1L), eq(List.of(10L)))).thenReturn(Map.of(10L, 5L));

		pointMemberService.scheduleSendMemberPoint();

		verify(scheduleDataPort, times(1)).mapItemPointAccess(eq(1L), eq(List.of(10L)));
		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(eq(2L), eq(1L), eq(10), eq(500L), any());
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(500L);
	}

	@Test
	@DisplayName("§3.7.3.5: include_freight 订单维度含运费")
	void section3735_includeFreightOrderDimension() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(600L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(600L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isEqualTo(1);
		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(eq(2L), eq(1L), eq(100), eq(600L), any());
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(600L);
	}

	@Test
	@DisplayName("§3.7.3.6: 订单维度按行 point_fee 汇总参与基数")
	void section3736_orderDimensionPointFeeOnLines() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(700L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_025L, 50, 200);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(openOrderRule());
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(700L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);
		when(scheduleDataPort.listLineItemsForOrder(700L))
				.thenReturn(
						List.of(
								new MemberPointScheduleItemRow(1L, 1, 30),
								new MemberPointScheduleItemRow(2L, 1, 40)));

		pointMemberService.scheduleSendMemberPoint();

		ArgumentCaptor<Integer> pointCap = ArgumentCaptor.forClass(Integer.class);
		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(eq(2L), eq(1L), pointCap.capture(), eq(700L), any());
		assertThat(pointCap.getValue()).isEqualTo(101);
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(700L);
	}

	@Test
	@DisplayName("§3.7.3.7: gain_limit 月度上限截断与说明")
	void section3737_gainLimitTruncates() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(800L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 2000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		rule.put("gain_limit", 100);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(800L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(90);

		pointMemberService.scheduleSendMemberPoint();

		verify(pointMemberAddPointService, times(1))
				.addPointForNormalOrderBonus(
						eq(2L), eq(1L), eq(10), eq(800L), eq("应增加20积分，本月订单获取积分达到限度"));
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(800L);
	}

	@Test
	@DisplayName("§3.7.3.8: addPoint 失败吞异常且不进入 succ 集合")
	void section3738_addPointFailureNotMarked() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(1L);
		MemberPointScheduleOrderRow row =
				new MemberPointScheduleOrderRow(900L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 10_000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(row));
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(900L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);
		doThrow(new RuntimeException("boom"))
				.when(pointMemberAddPointService)
				.addPointForNormalOrderBonus(anyLong(), anyLong(), anyInt(), anyLong(), any());

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isZero();
		verify(scheduleDataPort, never()).markOrderSendPointDone(anyLong());
	}

	@Test
	@DisplayName("§4 §5: 批量 mark send_point 与 processed 计数一致")
	void section4_and_section5_batchMarkAndResult() {
		when(scheduleDataPort.countSendPointPendingOrders()).thenReturn(2L);
		MemberPointScheduleOrderRow a =
				new MemberPointScheduleOrderRow(1L, 1L, 2L, 1_700_000_000L, 0, 0, 0, 5000L, 0, 0);
		MemberPointScheduleOrderRow b =
				new MemberPointScheduleOrderRow(2L, 1L, 3L, 1_700_000_000L, 0, 0, 0, 5000L, 0, 0);
		when(scheduleDataPort.listSendPointPendingOrders(0, 100)).thenReturn(List.of(a, b));
		Map<String, Object> rule = openOrderRule();
		rule.put("include_freight", true);
		when(pointMemberRuleReadPort.getPointRule(1L)).thenReturn(rule);
		when(scheduleDataPort.orderIdsWithOpenAftersales(List.of(1L, 2L))).thenReturn(Set.of());
		when(pointMemberLogMapper.selectSumIncomeOrderBonusInRange(anyLong(), anyLong(), anyInt(), anyInt()))
				.thenReturn(0);

		ScheduleSendMemberPointResult r = pointMemberService.scheduleSendMemberPoint();

		assertThat(r.processed()).isEqualTo(2);
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(1L);
		verify(scheduleDataPort, times(1)).markOrderSendPointDone(2L);
	}
}
