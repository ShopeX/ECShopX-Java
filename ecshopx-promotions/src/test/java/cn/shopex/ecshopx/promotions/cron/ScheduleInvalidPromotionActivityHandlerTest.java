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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.service.PromotionActivityService;
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
class ScheduleInvalidPromotionActivityHandlerTest {

	@Mock
	private PromotionActivityService promotionActivityService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleInvalidPromotionActivityHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleInvalidPromotionActivityHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("plan §5 handler: 成功路径；无 CronAlertEvent")
	void execute_ok_noAlert() {
		when(promotionActivityService.activityInvalid()).thenReturn(3);
		handler.execute();
		verify(promotionActivityService, times(1)).activityInvalid();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("invalid-promotion-activity")
								&& e.getFormattedMessage().contains("done")
								&& e.getFormattedMessage().contains("updated=3"));
	}

	@Test
	@DisplayName("plan §5 handler: 异常路径 + CronAlertEvent + 重抛 (RuntimeException)")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("test error");
		doThrow(err).when(promotionActivityService).activityInvalid();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("invalid-promotion-activity");
		assertThat(cap.getValue().getCause()).isSameAs(err);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("invalid-promotion-activity")
								&& e.getFormattedMessage().contains("cost=")
								&& e.getFormattedMessage().contains("ms"));
	}

	@Test
	@DisplayName("plan §5 handler: 异常路径 ResourceException 同样告警并重抛")
	void execute_resourceException_publishesAlertAndRethrows() {
		ResourceException err = new ResourceException("未查询到更新数据");
		doThrow(err).when(promotionActivityService).activityInvalid();
		assertThrows(ResourceException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("invalid-promotion-activity");
		assertThat(cap.getValue().getCause()).isSameAs(err);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("invalid-promotion-activity")
								&& e.getFormattedMessage().contains("cost=")
								&& e.getFormattedMessage().contains("ms"));
	}
}
