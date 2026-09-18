package cn.shopex.ecshopx.espier.cron;

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
import cn.shopex.ecshopx.espier.service.UploadFileService;
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
class ScheduleDeleteErrorFileHandlerTest {

	private static final String SHORT = "delete-error-file";

	@Mock
	private UploadFileService uploadFileService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleDeleteErrorFileHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleDeleteErrorFileHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3: 1,2,3,4,5,5.1,5.2,5.2.1,5.2.2,5.2.3,6 — handler 正常路径")
	void execute_ok_logsInfoAndNoAlert() {
		when(uploadFileService.scheduleDeleteErrorFile()).thenReturn(3);
		handler.execute();
		verify(uploadFileService, times(1)).scheduleDeleteErrorFile();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains(SHORT)
						&& e.getFormattedMessage().contains("costMs=")
						&& e.getFormattedMessage().contains("processed=3"));
	}

	@Test
	@DisplayName("§3: 1,2,3 — handler 异常路径（含 costMs 与 CronAlertEvent）")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("test");
		doThrow(err).when(uploadFileService).scheduleDeleteErrorFile();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains(SHORT)
						&& e.getFormattedMessage().contains("costMs="));
	}
}
