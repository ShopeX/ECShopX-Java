package cn.shopex.ecshopx.datacube.cron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.datacube.service.GoodsDataService;
import cn.shopex.ecshopx.datacube.service.goodsdata.ScheduleGoodsDailyInitResult;
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
class ScheduleEmployeePurchaseGoodsDailyStatisticsHandlerTest {

	private static final String SHORT = "employee-purchase-goods-daily-statistics";

	@Mock
	private GoodsDataService goodsDataService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleEmployeePurchaseGoodsDailyStatisticsHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger =
				(Logger) LoggerFactory.getLogger(ScheduleEmployeePurchaseGoodsDailyStatisticsHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§5 handler 用例, S-EPG 对照: execute 成功, processed, costMs, 无 CronAlertEvent")
	void execute_ok_noAlert() {
		when(goodsDataService.scheduleInitEmployeePurchaseStatistic())
				.thenReturn(new ScheduleGoodsDailyInitResult(2));
		handler.execute();
		verify(goodsDataService, times(1)).scheduleInitEmployeePurchaseStatistic();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("processed=")
								&& e.getFormattedMessage().contains("costMs"));
	}

	@Test
	@DisplayName("§5 handler 用例, S-EPG 对照: execute 失败, CronAlertEvent, error 与 costMs, 重抛")
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(goodsDataService).scheduleInitEmployeePurchaseStatistic();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		verifyNoMoreInteractions(eventPublisher);
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo(SHORT);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains(SHORT)
								&& e.getFormattedMessage().contains("costMs"));
	}
}
