package cn.shopex.ecshopx.companys.cron;

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
import cn.shopex.ecshopx.companys.service.OperatorLogsService;
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
class ScheduleDeleteOperatorLogsHandlerTest {

	@Mock
	private OperatorLogsService operatorLogsService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleDeleteOperatorLogsHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleDeleteOperatorLogsHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("成功：不发布事件，INFO 含 costMs 与 processed")
	void executeOk() {
		when(operatorLogsService.scheduleDeleteOperatorLogs()).thenReturn(3);
		handler.execute();
		verify(operatorLogsService, times(1)).scheduleDeleteOperatorLogs();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("delete-operator-logs")
								&& e.getFormattedMessage().contains("costMs=")
								&& e.getFormattedMessage().contains("processed=3"));
	}

	@Test
	@DisplayName("异常：CronAlertEvent 且重抛")
	void executeFail() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(operatorLogsService).scheduleDeleteOperatorLogs();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("delete-operator-logs");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("delete-operator-logs"));
	}
}
