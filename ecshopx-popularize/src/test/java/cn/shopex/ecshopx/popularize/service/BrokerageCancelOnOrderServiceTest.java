package cn.shopex.ecshopx.popularize.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrokerageCancelOnOrderServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;

	@Mock
	private BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	private BrokerageCancelOnOrderService service;

	@BeforeEach
	void setUp() {
		service =
				new BrokerageCancelOnOrderService(
						brokerageMapper,
						popularizeBrokerageStatisticsIncrementService,
						brokeragePlanCloseTimeService);
	}

	@Test
	@DisplayName("无未结算佣金：不插负佣，仍更新 plan_close_time")
	void whenNoOpenBrokerage_thenOnlyUpdatePlanCloseTime() {
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

		service.brokerageByCancelOrder(38L, 5433647000113890L);

		verify(brokerageMapper, never()).insert(any(Brokerage.class));
		verify(popularizeBrokerageStatisticsIncrementService, never()).add(any(), any(Long.class), any(Long.class));
		verify(brokeragePlanCloseTimeService).updatePlanCloseTime(38L, 5433647000113890L);
	}

	@Test
	@DisplayName("有未结算正佣：插负佣冲销，原行不改 is_close，冲减统计并更新 plan_close_time")
	void whenOpenMoneyBrokerage_thenInsertNegativeAndAdjustStats() {
		Brokerage open = new Brokerage();
		open.setId(101L);
		open.setBrokerageType("first_level");
		open.setOrderId("5433647000113890");
		open.setUserId(261L);
		open.setBuyUserId(999L);
		open.setOrderType("normal");
		open.setSource("order");
		open.setCompanyId(38L);
		open.setPrice(1_300_000);
		open.setCommissionType("money");
		open.setRebate(13_000);
		open.setRebatePoint("money");
		open.setDetail("{}");
		open.setIsClose(false);
		open.setPlanCloseTime(Integer.MAX_VALUE);
		open.setCreated(1_700_000_000);
		open.setUpdated(1_700_000_000);

		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(open));

		AtomicReference<Brokerage> inserted = new AtomicReference<>();
		when(brokerageMapper.insert(any(Brokerage.class)))
				.thenAnswer(
						inv -> {
							Brokerage row = inv.getArgument(0);
							inserted.set(row);
							return 1;
						});

		service.brokerageByCancelOrder(38L, 5433647000113890L);

		Brokerage neg = inserted.get();
		assertNull(neg.getId());
		assertEquals("5433647000113890", neg.getOrderId());
		assertEquals(261L, neg.getUserId());
		assertEquals(-13_000, neg.getRebate());
		assertEquals(-1_300_000, neg.getPrice());
		assertFalse(Boolean.TRUE.equals(neg.getIsClose()));
		assertTrue(neg.getDetail() != null && neg.getDetail().contains("取消订单生成"));

		ArgumentCaptor<Map<String, Long>> deltas = ArgumentCaptor.captor();
		verify(popularizeBrokerageStatisticsIncrementService)
				.add(deltas.capture(), eq(261L), eq(38L));
		assertEquals(-1_300_000L, deltas.getValue().get("item_total_price"));
		assertEquals(-13_000L, deltas.getValue().get("rebate_total"));
		assertEquals(-13_000L, deltas.getValue().get("no_close_rebate"));

		verify(brokerageMapper, never()).update(any(), any());
		verify(brokeragePlanCloseTimeService).updatePlanCloseTime(38L, 5433647000113890L);
	}
}
