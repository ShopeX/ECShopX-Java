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
import cn.shopex.ecshopx.orders.service.PointsmallNormalOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleCancelNormalPointsmallOrdersHandlerTest {

	@Mock
	private PointsmallNormalOrderService pointsmallNormalOrderService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleCancelNormalPointsmallOrdersHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleCancelNormalPointsmallOrdersHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("plan§5 handler 正常路径 §3 编排经 4 5 6.4.1 6.4.5 7")
	void execute_ok_logsInfoAndNoAlert() {
		when(pointsmallNormalOrderService.scheduleCancelOrders()).thenReturn(3);
		handler.execute();
		verify(pointsmallNormalOrderService, times(1)).scheduleCancelOrders();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("cancel-pointsmall-orders")
						&& e.getFormattedMessage().contains("cancelled=3"));
	}

	@Test
	@DisplayName("plan§5 handler 异常路径 §3 6")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("test");
		doThrow(err).when(pointsmallNormalOrderService).scheduleCancelOrders();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("cancel-pointsmall-orders");
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("cancel-pointsmall-orders")
						&& e.getFormattedMessage().contains("cost="));
	}
}
