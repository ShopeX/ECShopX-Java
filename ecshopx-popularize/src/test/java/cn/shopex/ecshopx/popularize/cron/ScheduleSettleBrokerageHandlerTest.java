package cn.shopex.ecshopx.popularize.cron;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.popularize.service.BrokerageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleSettleBrokerageHandlerTest {

	@Mock
	private BrokerageService brokerageService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleSettleBrokerageHandler handler;

	@Test
	void execute_ok() {
		handler.execute();
		verify(brokerageService).scheduleSettleRebate();
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void execute_exception_publishesEventAndRethrows() {
		RuntimeException ex = new RuntimeException("cron fail");
		doThrow(ex).when(brokerageService).scheduleSettleRebate();
		assertThrows(RuntimeException.class, () -> handler.execute());
		verify(eventPublisher)
				.publishEvent(
						argThat(
								ev -> {
									if (!(ev instanceof CronAlertEvent e)) {
										return false;
									}
									return "settle-brokerage".equals(e.getHandlerShortName()) && e.getCause() == ex;
								}));
	}
}
