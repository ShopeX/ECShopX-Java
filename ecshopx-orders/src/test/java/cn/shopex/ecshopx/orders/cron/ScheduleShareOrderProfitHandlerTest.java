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
import cn.shopex.ecshopx.orders.service.OrderProfitSharingService;
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
class ScheduleShareOrderProfitHandlerTest {

	@Mock
	private OrderProfitSharingService orderProfitSharingService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleShareOrderProfitHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleShareOrderProfitHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3 编排经 Handler：成功路径（plan §5 handler 表「正常路径」；costMs/processed=3，无 CronAlertEvent）")
	void execute_ok() {
		when(orderProfitSharingService.scheduleShareOrderProfit()).thenReturn(3);
		handler.execute();
		verify(orderProfitSharingService, times(1)).scheduleShareOrderProfit();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("share-order-profit")
						&& e.getFormattedMessage().contains("costMs=")
						&& e.getFormattedMessage().contains("processed=3"));
	}

	@Test
	@DisplayName("§3 编排经 Handler：异常路径（plan §5 handler 表「异常路径」；CronAlertEvent share-order-profit、processed=0）")
	void execute_fails() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(orderProfitSharingService).scheduleShareOrderProfit();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("share-order-profit");
		assertThat(cap.getValue().getCause()).isSameAs(err);
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("share-order-profit")
						&& e.getFormattedMessage().contains("costMs=")
						&& e.getFormattedMessage().contains("processed=0"));
	}
}
