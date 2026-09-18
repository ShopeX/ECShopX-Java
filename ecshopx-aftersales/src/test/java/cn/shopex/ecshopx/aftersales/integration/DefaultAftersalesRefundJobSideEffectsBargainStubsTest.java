package cn.shopex.ecshopx.aftersales.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.TradeInfoSnapshot;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Aligns {@code loadTrade} / {@code processBargainOrder} with PHP {@code RefundJob} 售前助力分支.
 */
class DefaultAftersalesRefundJobSideEffectsBargainStubsTest {

	private TradeByIdReadPort tradeByIdReadPort;
	private OrderNormalOrderHeaderReadPort orderHeaderReadPort;
	private BargainOrderActivityStatusPort bargainOrderActivityStatusPort;
	private DefaultAftersalesRefundJobSideEffects sideEffects;

	@BeforeEach
	void setUp() {
		tradeByIdReadPort = mock(TradeByIdReadPort.class);
		orderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		bargainOrderActivityStatusPort = mock(BargainOrderActivityStatusPort.class);
		sideEffects =
				new DefaultAftersalesRefundJobSideEffects(
						mock(AftersalesRefundOnlineRefundPort.class),
						mock(TradeRefundFinishEventDispatchPublisher.class),
						mock(AftersalesMapper.class),
						mock(AftersalesDetailMapper.class),
						orderHeaderReadPort,
						mock(AftersalesRefundMapper.class),
						tradeByIdReadPort,
						bargainOrderActivityStatusPort);
	}

	@Test
	@DisplayName("loadTrade：按 trade_id 读 trade_source_type")
	void loadTrade_mapsSourceType() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_id", "T1");
		row.put("trade_source_type", "bargain");
		when(tradeByIdReadPort.getByTradeId("T1", 1L)).thenReturn(Optional.of(row));

		TradeInfoSnapshot snap = sideEffects.loadTrade("T1", 1L);

		assertThat(snap).isNotNull();
		assertThat(snap.tradeId()).isEqualTo("T1");
		assertThat(snap.tradeSourceType()).isEqualTo("bargain");
	}

	@Test
	@DisplayName("processBargainOrder：order_class=bargain 时 state=0 回退助力状态")
	void processBargainOrder_bargainClass_callsPortWithState0() {
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("order_class", "bargain");
		header.put("user_id", 88L);
		header.put("act_id", 501L);
		when(orderHeaderReadPort.getHeader(1L, 9L)).thenReturn(Optional.of(header));

		sideEffects.processBargainOrder(1L, 9L);

		verify(bargainOrderActivityStatusPort).changeOrderActivityStatus(88L, 501L, 0);
	}

	@Test
	@DisplayName("processBargainOrder：非 bargain 订单不调助力回退")
	void processBargainOrder_nonBargain_skips() {
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("order_class", "normal");
		header.put("user_id", 88L);
		header.put("act_id", 501L);
		when(orderHeaderReadPort.getHeader(1L, 9L)).thenReturn(Optional.of(header));

		sideEffects.processBargainOrder(1L, 9L);

		verify(bargainOrderActivityStatusPort, never()).changeOrderActivityStatus(anyLong(), anyLong(), eq(0));
	}
}
