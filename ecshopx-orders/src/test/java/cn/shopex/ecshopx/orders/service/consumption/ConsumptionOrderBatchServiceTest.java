package cn.shopex.ecshopx.orders.service.consumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.stats.MemberAggregateConsumptionFromCronService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.dto.OrderIdRefundSumRow;
import cn.shopex.ecshopx.orders.mapper.ConsumptionOrderDqlMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumptionOrderBatchServiceTest {

	@Mock
	ConsumptionOrderDqlMapper consumptionOrderDqlMapper;
	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	TradeMapper tradeMapper;
	@Mock
	MemberAggregateConsumptionFromCronService memberAggregate;

	@InjectMocks
	ConsumptionOrderBatchService service;

	/** 2.1, 2.5-2, 2.6 */
	@Test
	void runEquivalentToHandle_noCandidates_returns0() {
		when(consumptionOrderDqlMapper.selectHaveAftersalesOrderIds(any(Long.class)))
				.thenReturn(List.of());
		when(normalOrdersMapper.selectConsumptionCandidatePage(any(Long.class), anyList(), eq(100)))
				.thenReturn(List.of());
		int n = service.runEquivalentToHandle();
		assertThat(n).isEqualTo(0);
	}

	/** 2.2, 2.3, 2.4 */
	@Test
	void runEquivalentToHandle_selectHaveAftersales_passedToPage() {
		when(consumptionOrderDqlMapper.selectHaveAftersalesOrderIds(any(Long.class)))
				.thenReturn(List.of(9L, 8L));
		when(normalOrdersMapper.selectConsumptionCandidatePage(any(Long.class), anyList(), eq(100)))
				.thenReturn(List.of());
		service.runEquivalentToHandle();
		verify(normalOrdersMapper, times(1))
				.selectConsumptionCandidatePage(any(Long.class), eq(List.of(9L, 8L)), eq(100));
	}

	/** 2.5-1, 2.5-3, 2.5-4, 2.5-5, 2.5, 2.6, 3.1–3.4, 3.5, 3.6, 3.7, 4.1, 4.1-1, 4.1-2 */
	@Test
	void doNotAftersalesConsumption_appliesTradesAndMarks() {
		List<Long> oids = List.of(1L);
		when(consumptionOrderDqlMapper.selectProcessedAftersalesOrderIds(oids)).thenReturn(List.of());
		Trade t = new Trade();
		t.setOrderId("1");
		t.setUserId("10");
		t.setCompanyId("100");
		t.setPayFee(500);
		t.setPayType("wxpay");
		t.setTradeState("SUCCESS");
		when(tradeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));
		when(normalOrdersMapper.updateIsConsumptionWhenZero(oids)).thenReturn(1);
		int n = service.doNotAftersalesConsumption(oids);
		assertThat(n).isEqualTo(1);
		verify(memberAggregate, times(1)).applyAggregates(any());
		verify(normalOrdersMapper, times(1)).updateIsConsumptionWhenZero(oids);
	}

	/** 3.1, 3.2 */
	@Test
	void doNotAftersalesConsumption_refundSubtracted() {
		List<Long> oids = List.of(1L);
		when(consumptionOrderDqlMapper.selectProcessedAftersalesOrderIds(oids)).thenReturn(List.of(1L));
		OrderIdRefundSumRow sum = new OrderIdRefundSumRow();
		sum.setOrderId(1L);
		sum.setSumRefundedFee(new BigDecimal("100"));
		when(consumptionOrderDqlMapper.selectRefundSumsExcludingPoint(List.of(1L)))
				.thenReturn(List.of(sum));
		Trade t = new Trade();
		t.setOrderId("1");
		t.setUserId("20");
		t.setCompanyId("1");
		t.setPayFee(300);
		t.setPayType("wxpay");
		t.setTradeState("SUCCESS");
		when(tradeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));
		when(normalOrdersMapper.updateIsConsumptionWhenZero(oids)).thenReturn(1);
		service.doNotAftersalesConsumption(oids);
		verify(memberAggregate, times(1)).applyAggregates(any());
	}

	/** 2.5, 2.5-1..2.5-5, 2.6 */
	@Test
	void runEquivalentToHandle_twoPages_sumMarked() {
		List<NormalOrders> p1 = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			NormalOrders o = new NormalOrders();
			o.setOrderId(1000L + i);
			p1.add(o);
		}
		List<NormalOrders> p2 = new ArrayList<>();
		NormalOrders last = new NormalOrders();
		last.setOrderId(3000L);
		p2.add(last);
		when(consumptionOrderDqlMapper.selectHaveAftersalesOrderIds(any(Long.class)))
				.thenReturn(List.of());
		when(tradeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		when(normalOrdersMapper.updateIsConsumptionWhenZero(anyList())).thenReturn(1);
		when(normalOrdersMapper.selectConsumptionCandidatePage(any(Long.class), anyList(), eq(100)))
				.thenReturn(p1, p2, List.of());
		int total = service.runEquivalentToHandle();
		assertThat(total).isEqualTo(101);
		verify(normalOrdersMapper, atLeast(2)).selectConsumptionCandidatePage(any(Long.class), anyList(), eq(100));
	}

	/** 3.5 + 3.6：会员链抛错则本批不置 is_consumption */
	@Test
	void doNotAftersalesConsumption_memberThrows_noConsumptionUpdate() {
		List<Long> oids = List.of(1L);
		when(consumptionOrderDqlMapper.selectProcessedAftersalesOrderIds(oids)).thenReturn(List.of());
		Trade t = new Trade();
		t.setOrderId("1");
		t.setUserId("1");
		t.setCompanyId("1");
		t.setPayFee(10);
		t.setPayType("wxpay");
		t.setTradeState("SUCCESS");
		when(tradeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));
		doThrow(new RuntimeException("boom")).when(memberAggregate).applyAggregates(any());
		Assertions.assertThrows(RuntimeException.class, () -> service.doNotAftersalesConsumption(oids));
		verify(normalOrdersMapper, never()).updateIsConsumptionWhenZero(anyList());
	}
}
