package cn.shopex.ecshopx.companys.cron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleRecordActiveArticleStatisticsHandlerTest {

	@Mock
	private DispatchFacade dispatchFacade;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleRecordActiveArticleStatisticsHandler handler;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger handlerLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleRecordActiveArticleStatisticsHandler.class);
		handlerLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		handlerLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	private static boolean matchesActiveArticleCronDispatchOptions(DispatchOptions opts) {
		return opts != null
				&& opts.mode() == DispatchMode.ASYNC
				&& opts.driverOverride() == DispatchDriverType.REDIS
				&& "slow".equals(opts.queue())
				&& opts.delay() == null
				&& RetryPolicy.platformDefault().equals(opts.retryPolicy());
	}

	@Test
	@DisplayName("success: dispatch job 175 then 176, never sync active block, INFO jobDispatches=2")
	void executeOk_dispatchesJob175Then176_neverSyncActiveBlock_logsTwoDispatches() {
		handler.execute();
		InOrder inOrder = inOrder(dispatchFacade);
		inOrder
				.verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE),
						argThat(m -> m != null && m.isEmpty()),
						argThat(ScheduleRecordActiveArticleStatisticsHandlerTest::matchesActiveArticleCronDispatchOptions));
		inOrder
				.verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB),
						argThat(m -> m != null && m.isEmpty()),
						argThat(ScheduleRecordActiveArticleStatisticsHandlerTest::matchesActiveArticleCronDispatchOptions));
		verify(dispatchFacade, times(2)).dispatchJob(any(), any(), any());
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("record-active-article-statistics")
								&& e.getFormattedMessage().contains("costMs=")
								&& e.getFormattedMessage().contains("jobDispatches=2"));
	}

	@Test
	@DisplayName("failure on first dispatchJob: CronAlertEvent and rethrow without second dispatch or sync")
	void executeFail_onFirstDispatchJob_cronAlertWithoutSyncActiveBlock() {
		RuntimeException err = new RuntimeException("x");
		doThrow(err).when(dispatchFacade).dispatchJob(any(), any(), any());
		assertThrows(RuntimeException.class, () -> handler.execute());
		verify(dispatchFacade, times(1)).dispatchJob(any(), any(), any());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("record-active-article-statistics");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("record-active-article-statistics")
								&& e.getFormattedMessage().contains("costMs="));
	}

	@Test
	@DisplayName("failure on second dispatchJob (176): CronAlertEvent after 175 dispatched, never sync active block")
	void executeFail_onSecondDispatchJob_afterJob175_cronAlertWithoutSyncActiveBlock() {
		RuntimeException err = new RuntimeException("second");
		doNothing()
				.doThrow(err)
				.when(dispatchFacade)
				.dispatchJob(any(), any(), any());
		assertThrows(RuntimeException.class, () -> handler.execute());
		verify(dispatchFacade, times(2)).dispatchJob(any(), any(), any());
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher, times(1)).publishEvent(cap.capture());
		assertThat(cap.getValue().getHandlerShortName()).isEqualTo("record-active-article-statistics");
	}
}
