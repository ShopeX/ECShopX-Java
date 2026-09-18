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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleGoodsDailyStatisticsHandlerTest {

	@Mock
	private GoodsDataService goodsDataService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleGoodsDailyStatisticsHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleGoodsDailyStatisticsHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	void execute_ok_noAlert() {
		when(goodsDataService.scheduleInitStatistic()).thenReturn(new ScheduleGoodsDailyInitResult(2));
		handler.execute();
		verify(goodsDataService, times(1)).scheduleInitStatistic();
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("goods-daily-statistics")
								&& e.getFormattedMessage().contains("processed=")
								&& e.getFormattedMessage().contains("costMs"));
	}

	@Test
	void execute_failure_publishesAlertAndRethrows() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(goodsDataService).scheduleInitStatistic();
		assertThrows(RuntimeException.class, () -> handler.execute());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		verifyNoMoreInteractions(eventPublisher);
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("goods-daily-statistics");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("goods-daily-statistics")
								&& e.getFormattedMessage().contains("costMs"));
	}
}
