package cn.shopex.ecshopx.selfservice.cron;

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
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityService;
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
class ScheduleSendWxRemindMsgHandlerTest {

	private static final String SHORT = "send-wx-remind-msg";

	@Mock
	private RegistrationActivityService registrationActivityService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleSendWxRemindMsgHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleSendWxRemindMsgHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("正常路径：不发布 CronAlertEvent；INFO 含 dispatched=")
	void execute_ok() {
		when(registrationActivityService.scheduleSendWxRemindMsg()).thenReturn(3);
		handler.execute();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("dispatched=3")
								&& e.getFormattedMessage().contains(SHORT));
	}

	@Test
	@DisplayName("异常：CronAlertEvent 一次、重抛、ERROR 日志")
	void execute_error_alert() {
		RuntimeException err = new RuntimeException("t");
		doThrow(err).when(registrationActivityService).scheduleSendWxRemindMsg();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT));
	}
}
