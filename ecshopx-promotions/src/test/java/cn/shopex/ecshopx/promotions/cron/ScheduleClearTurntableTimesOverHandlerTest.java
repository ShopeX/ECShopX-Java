package cn.shopex.ecshopx.promotions.cron;

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
import cn.shopex.ecshopx.promotions.service.TurntableScheduleService;
import cn.shopex.ecshopx.promotions.service.TurntableScheduleService.ScheduleClearResult;
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
class ScheduleClearTurntableTimesOverHandlerTest {

	private static final String SHORT = "clear-turntable-times-over";

	@Mock
	private TurntableScheduleService turntableScheduleService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleClearTurntableTimesOverHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleClearTurntableTimesOverHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("plan §5: 成功；无 CronAlertEvent；INFO 含 redisDeleted")
	void execute_ok_noAlert() {
		when(turntableScheduleService.scheduleClearTurntableTimesOver()).thenReturn(new ScheduleClearResult(0, 0));
		handler.execute();
		verify(turntableScheduleService, times(1)).scheduleClearTurntableTimesOver();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("redisDeleted=0")
								&& e.getFormattedMessage().contains("cost="));
	}

	@Test
	@DisplayName("plan §5: 失败路径 CronAlertEvent + 重抛")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("cron test");
		doThrow(err).when(turntableScheduleService).scheduleClearTurntableTimesOver();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(cap.getValue().getCause()).isSameAs(err);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT));
	}
}
