package cn.shopex.ecshopx.adapay.cron;

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
import cn.shopex.ecshopx.adapay.service.AdapayDrawCashService;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
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
class ScheduleDrawCashQueueHandlerTest {

	private static final String SHORT = "adapy-draw-cash-queue";

	@Mock
	private AdapayDrawCashService adapayDrawCashService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleDrawCashQueueHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleDrawCashQueueHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("handler 成功：无 CronAlertEvent，INFO 含 dispatched")
	void execute_ok_noAlert() {
		when(adapayDrawCashService.scheduleDrawCashQueue()).thenReturn(3);
		handler.execute();
		verify(adapayDrawCashService, times(1)).scheduleDrawCashQueue();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("dispatched=3")
								&& e.getFormattedMessage().contains("costMs="));
	}

	@Test
	@DisplayName("handler 失败：CronAlertEvent、ERROR 含 costMs、重抛")
	void execute_failure_alertAndRethrow() {
		RuntimeException err = new RuntimeException("test");
		when(adapayDrawCashService.scheduleDrawCashQueue()).thenThrow(err);
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("costMs="));
	}
}
