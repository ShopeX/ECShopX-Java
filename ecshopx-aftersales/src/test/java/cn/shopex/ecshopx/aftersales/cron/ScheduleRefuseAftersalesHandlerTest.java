package cn.shopex.ecshopx.aftersales.cron;

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
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
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
class ScheduleRefuseAftersalesHandlerTest {

	@Mock
	private AftersalesService aftersalesService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleRefuseAftersalesHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleRefuseAftersalesHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("handler 正常路径：execute 成功，无 CronAlertEvent")
	void execute_ok_noAlert() {
		when(aftersalesService.scheduleAutoRefuse()).thenReturn(true);
		handler.execute();
		verify(aftersalesService, times(1)).scheduleAutoRefuse();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("refuse-aftersales")
						&& e.getFormattedMessage().contains("result=true"));
	}

	@Test
	@DisplayName("handler 异常路径：ERROR 日志含 refuse-aftersales，发布 CronAlertEvent 并重抛")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("t");
		doThrow(err).when(aftersalesService).scheduleAutoRefuse();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("refuse-aftersales");
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("refuse-aftersales"));
	}
}
