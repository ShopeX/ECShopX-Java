package cn.shopex.ecshopx.popularize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCompletedItemQtyReadPort;
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
class BrokerageAftersalesClawbackServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;

	@Mock
	private BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	@Mock
	private AftersalesCompletedItemQtyReadPort aftersalesCompletedItemQtyReadPort;

	private BrokerageAftersalesClawbackService service;

	@BeforeEach
	void setUp() {
		service =
				new BrokerageAftersalesClawbackService(
						brokerageMapper,
						popularizeBrokerageStatisticsIncrementService,
						brokeragePlanCloseTimeService,
						aftersalesCompletedItemQtyReadPort);
	}

	@Test
	@DisplayName("部分退：按件数比例插入负佣，原行 is_close 不变")
	void partialRefund_insertsProportionalNegative() {
		Brokerage open = sampleOpenRow();
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(open));
		when(aftersalesCompletedItemQtyReadPort.sumCompletedNum(38L, 9001L)).thenReturn(0);
		when(brokeragePlanCloseTimeService.computePlanCloseTimeEpochSec(38L)).thenReturn(2_000_000_000);

		AtomicReference<Brokerage> inserted = new AtomicReference<>();
		when(brokerageMapper.insert(any(Brokerage.class)))
				.thenAnswer(
						inv -> {
							inserted.set(inv.getArgument(0));
							return 1;
						});

		service.brokerageByAftersales(38L, 9001L, 11L, 1);

		Brokerage neg = inserted.get();
		assertThat(neg.getRebate()).isEqualTo(-50);
		assertThat(neg.getPrice()).isEqualTo(-5000);
		assertThat(neg.getIsClose()).isFalse();
		assertThat(neg.getDetail()).contains("申请售后生成");

		ArgumentCaptor<Map<String, Long>> deltas = ArgumentCaptor.captor();
		verify(popularizeBrokerageStatisticsIncrementService).add(deltas.capture(), eq(200L), eq(38L));
		assertThat(deltas.getValue().get("rebate_total")).isEqualTo(-50L);
		assertThat(deltas.getValue().get("no_close_rebate")).isEqualTo(-50L);
		verify(brokerageMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("无 rebate_detail：跳过冲销")
	void missingRebateDetail_skips() {
		Brokerage open = sampleOpenRow();
		open.setDetail("{}");
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(open));
		when(aftersalesCompletedItemQtyReadPort.sumCompletedNum(38L, 9001L)).thenReturn(0);

		service.brokerageByAftersales(38L, 9001L, 11L, 1);

		verify(brokerageMapper, never()).insert(any(Brokerage.class));
		verify(popularizeBrokerageStatisticsIncrementService, never()).add(any(), any(Long.class), any(Long.class));
	}

	@Test
	@DisplayName("left_num < 0：抛申请售后单数据异常")
	void leftNumNegative_throws() {
		Brokerage open = sampleOpenRow();
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(open));
		when(aftersalesCompletedItemQtyReadPort.sumCompletedNum(38L, 9001L)).thenReturn(2);

		assertThatThrownBy(() -> service.brokerageByAftersales(38L, 9001L, 11L, 1))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("申请售后单数据异常");
	}

	@Test
	@DisplayName("无开放正佣：直接返回")
	void noOpenRows_noop() {
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
		service.brokerageByAftersales(38L, 9001L, 11L, 1);
		verify(brokerageMapper, never()).insert(any(Brokerage.class));
	}

	private static Brokerage sampleOpenRow() {
		Brokerage open = new Brokerage();
		open.setId(1L);
		open.setBrokerageType("first_level");
		open.setOrderId("9001");
		open.setUserId(200L);
		open.setBuyUserId(100L);
		open.setOrderType("normal");
		open.setSource("order");
		open.setCompanyId(38L);
		open.setPrice(10000);
		open.setCommissionType("money");
		open.setRebate(100);
		open.setRebatePoint("money");
		open.setDetail(
				"{\"rebate_detail\":{\"11\":{\"item_id\":11,\"item_num\":2,\"total_fee\":10000,\"rebate\":100}}}");
		open.setIsClose(false);
		return open;
	}
}
