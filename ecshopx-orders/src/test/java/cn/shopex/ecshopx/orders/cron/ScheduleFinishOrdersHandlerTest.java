package cn.shopex.ecshopx.orders.cron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.orders.service.NormalOrderCronService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleFinishOrdersHandlerTest {

	@Mock
	private NormalOrderCronService normalOrderCronService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleFinishOrdersHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleFinishOrdersHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	/** plan §5 analysis §3 **2** */
	@Test
	void execute_ok_noAlert() {
		when(normalOrderCronService.scheduleFinishOrders()).thenReturn(1);
		handler.execute();
		verify(normalOrderCronService, times(1)).scheduleFinishOrders();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("finish-orders")
						&& e.getFormattedMessage().contains("scheduleFinishOrdersReturn=1"));
	}

	/** plan §5 analysis §3 **2** */
	@Test
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("test");
		doThrow(err).when(normalOrderCronService).scheduleFinishOrders();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("finish-orders");
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("finish-orders")
						&& e.getFormattedMessage().contains("failed")
						&& e.getFormattedMessage().contains("cost="));
	}
}
