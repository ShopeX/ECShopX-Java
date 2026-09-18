package cn.shopex.ecshopx.orders.cron;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.service.OrderProfitSharingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleShareOrderProfitHandlerHfpayProfitSharingEvent295ChainProbeTest {

	@Test
	void execute_delegatesToOrderProfitSharingService() {
		OrderProfitSharingService orderProfitSharingService = mock(OrderProfitSharingService.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		when(orderProfitSharingService.scheduleShareOrderProfit()).thenReturn(0);
		ScheduleShareOrderProfitHandler handler =
				new ScheduleShareOrderProfitHandler(orderProfitSharingService, eventPublisher);

		handler.execute();

		verify(orderProfitSharingService).scheduleShareOrderProfit();
		verifyNoInteractions(eventPublisher);
	}
}
