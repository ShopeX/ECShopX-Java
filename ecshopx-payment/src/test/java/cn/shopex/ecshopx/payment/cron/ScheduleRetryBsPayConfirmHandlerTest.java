package cn.shopex.ecshopx.payment.cron;

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
import cn.shopex.ecshopx.payment.service.BsPayService;
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
class ScheduleRetryBsPayConfirmHandlerTest {

	@Mock
	private BsPayService bsPayService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleRetryBsPayConfirmHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleRetryBsPayConfirmHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("成功：不发布 CronAlertEvent，打 done 日志含 processed 与 costMs")
	void execute_ok() {
		when(bsPayService.scheduleRetryBsPayConfirm()).thenReturn(2);
		handler.execute();
		verify(bsPayService, times(1)).scheduleRetryBsPayConfirm();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("bspay-confirm-retry")
								&& e.getFormattedMessage().contains("done")
								&& e.getFormattedMessage().contains("processed=2")
								&& e.getFormattedMessage().contains("costMs="));
	}

	@Test
	@DisplayName("异常：CronAlertEvent 一次且重抛，ERROR 含 costMs")
	void execute_fails() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(bsPayService).scheduleRetryBsPayConfirm();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("bspay-confirm-retry");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("bspay-confirm-retry")
								&& e.getFormattedMessage().contains("failed")
								&& e.getFormattedMessage().contains("costMs="));
	}
}
