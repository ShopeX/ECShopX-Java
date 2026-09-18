package cn.shopex.ecshopx.orders.dispatch.profit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.orders.TradeFinishProfitSalespersonSideEffectsPort;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.service.OrderProfitService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeFinishProfitBusServiceTest {

	@Mock
	private OrderProfitService orderProfitService;

	@Mock
	private TradeFinishProfitSalespersonSideEffectsPort salespersonSideEffectsService;

	@InjectMocks
	private TradeFinishProfitBusService tradeFinishProfitBusService;

	@Test
	void handle_whenNoProfitRow_skipsUpdatesAndSideEffects() {
		when(orderProfitService.findForTradeFinishProfit(501L, 9L, 3L)).thenReturn(Optional.empty());

		Map<String, Object> row = baseRow();
		tradeFinishProfitBusService.handleTradeFinishRow(row);

		verify(orderProfitService, never()).markTradeFinishProfitClosed(anyLong(), anyLong(), anyLong(), anyLong());
		verify(salespersonSideEffectsService, never())
				.applyOnPopularizeSeller(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
	}

	@Test
	void handle_whenNoPopularizeSeller_stillClosesProfit() {
		OrderProfit op = new OrderProfit();
		op.setPopularizeSellerId(0L);
		op.setOrderDistributorId(12L);
		op.setPayFee(100L);
		when(orderProfitService.findForTradeFinishProfit(501L, 9L, 3L)).thenReturn(Optional.of(op));

		Map<String, Object> row = baseRow();
		tradeFinishProfitBusService.handleTradeFinishRow(row);

		verify(salespersonSideEffectsService, never())
				.applyOnPopularizeSeller(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
		verify(orderProfitService).markTradeFinishProfitClosed(eq(501L), eq(9L), eq(3L), anyLong());
	}

	@Test
	void handle_whenPopularizeSeller_invokesSideEffectsAndClose() {
		OrderProfit op = new OrderProfit();
		op.setPopularizeSellerId(77L);
		op.setOrderDistributorId(12L);
		op.setPayFee(100L);
		when(orderProfitService.findForTradeFinishProfit(501L, 9L, 3L)).thenReturn(Optional.of(op));

		Map<String, Object> row = baseRow();
		tradeFinishProfitBusService.handleTradeFinishRow(row);

		verify(salespersonSideEffectsService)
				.applyOnPopularizeSeller(eq(9L), eq(12L), eq(77L), eq(100L), eq(3L), eq(501L));
		verify(orderProfitService).markTradeFinishProfitClosed(eq(501L), eq(9L), eq(3L), anyLong());
	}

	@Test
	void handle_whenMarkClosedThrows_doesNotPropagate() {
		OrderProfit op = new OrderProfit();
		op.setPopularizeSellerId(null);
		when(orderProfitService.findForTradeFinishProfit(501L, 9L, 3L)).thenReturn(Optional.of(op));
		doThrow(new RuntimeException("db down"))
				.when(orderProfitService)
				.markTradeFinishProfitClosed(anyLong(), anyLong(), anyLong(), anyLong());

		Map<String, Object> row = baseRow();
		assertDoesNotThrow(() -> tradeFinishProfitBusService.handleTradeFinishRow(row));
	}

	private static Map<String, Object> baseRow() {
		Map<String, Object> m = new HashMap<>();
		m.put("order_id", 501L);
		m.put("company_id", 9L);
		m.put("user_id", 3L);
		return m;
	}
}
