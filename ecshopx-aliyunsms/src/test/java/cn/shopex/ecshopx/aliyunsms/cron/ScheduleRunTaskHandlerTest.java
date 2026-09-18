package cn.shopex.ecshopx.aliyunsms.cron;

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
import cn.shopex.ecshopx.aliyunsms.service.TaskService;
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
class ScheduleRunTaskHandlerTest {

	private static final String SHORT = "aliyunsms-run-task";

	@Mock
	private TaskService taskService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleRunTaskHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleRunTaskHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("analysis §3 §4 / plan §5.1：整轮终态（handler 不展开 1–3-5 内层）；成功路径 execute→scheduleRunTask 返回 int 且无异常；不发布 CronAlert；INFO 含 aliyunsms-run-task、cost=…ms、taskRowsIsSendSet")
	void execute_ok() {
		when(taskService.scheduleRunTask()).thenReturn(2);
		handler.execute();
		verify(taskService, times(1)).scheduleRunTask();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("cost=")
								&& e.getFormattedMessage().contains("ms")
								&& e.getFormattedMessage().contains("taskRowsIsSendSet=2"));
	}

	@Test
	@DisplayName("handler 整轮外抛（非 per-task §3-5）：scheduleRunTask 抛错 → CronAlertEvent 一次、ERROR 含 aliyunsms-run-task 与 cost=…ms、堆栈、重抛")
	void execute_fails() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(taskService).scheduleRunTask();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("failed")
								&& e.getFormattedMessage().contains("cost=")
								&& e.getFormattedMessage().contains("ms"));
	}
}
