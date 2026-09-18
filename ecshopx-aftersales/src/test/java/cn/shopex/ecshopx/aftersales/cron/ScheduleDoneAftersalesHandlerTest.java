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

/**
 * Cron shell only: {@link ScheduleDoneAftersalesHandler#execute} delegates to
 * {@link AftersalesService#scheduleAutoDoneAftersales()} and publishes {@link CronAlertEvent} on failure.
 *
 * <p>Per migration plan for the CSV {@code AftersalesService.php:2308} / cron path, detailed Bus assertions for
 * {@code EVENT_TRADE_AFTERSALES_CANCEL} fan-out (including PHP {@code TradeAftersaleCancelSendOme} →
 * {@link cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAftersaleCancelSendOmeDispatchListener} and the
 * marketing-center child) are not duplicated here. The handler only routes XXL {@code done-aftersales} into the same
 * {@code AftersalesService} close path that shares {@code afterCommit} with other close entry points; publisher order
 * and async Send-OME consume paths are covered by {@code AftersalesServiceScheduleDoneAftersalesTest},
 * {@code TradeAftersalesCancelSyncEventDispatchFlowTest}, and {@code TradeAftersalesCancelSendOmeEventDispatchFlowTest}
 * in {@code ecshopx-bootstrap}.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleDoneAftersalesHandlerTest {

	@Mock
	private AftersalesService aftersalesService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleDoneAftersalesHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleDoneAftersalesHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("正常路径：不发布 CronAlertEvent，日志含 costMs 与 processed")
	void execute_ok() {
		when(aftersalesService.scheduleAutoDoneAftersales()).thenReturn(0);
		handler.execute();
		verify(aftersalesService, times(1)).scheduleAutoDoneAftersales();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("done-aftersales")
								&& e.getFormattedMessage().contains("costMs=")
								&& e.getFormattedMessage().contains("processed=0"));
	}

	@Test
	@DisplayName("异常：发布 CronAlertEvent 且重抛")
	void execute_fails() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(aftersalesService).scheduleAutoDoneAftersales();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("done-aftersales");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("done-aftersales"));
	}
}
