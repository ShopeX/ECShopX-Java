package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.companys.dispatch.RecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonActiveArticleRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonCommissionRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonGiveCouponsRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonPopularizeRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.service.CompanysStatisticsService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordStatisticsJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch job 173 async enqueues on slow queue and consumer runs company statistics block")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		facade.dispatchJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("handler tolerates null payload map and still runs company block")
	void consume_withNullPayload_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("handler tolerates non-empty payload and still runs company block only")
	void consume_withExtraPayloadKeys_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 175 async enqueues on slow queue and consumer invokes company statistics block")
	void dispatchJob_job175_async_enqueuesOnSlowQueue_andConsumerInvokesCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 175 handler tolerates null payload map and still runs company block")
	void consume_job175_withNullPayload_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 175 handler tolerates non-empty payload and still runs company block only")
	void consume_job175_withExtraPayloadKeys_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 177 async enqueues on slow queue and consumer invokes company statistics block")
	void dispatchJob_job177_async_enqueuesOnSlowQueue_andConsumerInvokesCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 177 handler tolerates null payload map and still runs company block")
	void consume_job177_withNullPayload_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 177 handler tolerates non-empty payload and still runs company block only")
	void consume_job177_withExtraPayloadKeys_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 174 async enqueues on slow queue and consumer invokes shopping guide block")
	void dispatchJob_job174_async_enqueuesOnSlowQueue_andConsumerInvokesShoppingGuideBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonRecordStatisticsJobHandler handler = new SalespersonRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		facade.dispatchJob(
				CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runShoppingGuideBlockForYesterday();
	}

	@Test
	@DisplayName("job 174 handler tolerates null payload and still runs shopping guide block")
	void consume_job174_withNullPayload_stillRunsShoppingGuideBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonRecordStatisticsJobHandler handler = new SalespersonRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runShoppingGuideBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 176 async enqueues on slow queue and consumer invokes active article statistics block")
	void dispatchJob_job176_async_enqueuesOnSlowQueue_andConsumerInvokesActiveArticleBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonActiveArticleRecordStatisticsJobHandler handler =
				new SalespersonActiveArticleRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		facade.dispatchJob(
				CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runActiveArticleBlockForYesterday();
	}

	@Test
	@DisplayName("job 176 handler tolerates null payload map and still runs active article block")
	void consume_job176_withNullPayload_stillRunsActiveArticleBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonActiveArticleRecordStatisticsJobHandler handler =
				new SalespersonActiveArticleRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runActiveArticleBlockForYesterday();
	}

	@Test
	@DisplayName("job 176 handler tolerates non-empty payload and still runs active article block only")
	void consume_job176_withExtraPayloadKeys_stillRunsActiveArticleBlockOnly() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonActiveArticleRecordStatisticsJobHandler handler =
				new SalespersonActiveArticleRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runActiveArticleBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 178 async enqueues on slow queue and consumer invokes commission statistics block")
	void dispatchJob_job178_async_enqueuesOnSlowQueue_andConsumerInvokesCommissionBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonCommissionRecordStatisticsJobHandler handler =
				new SalespersonCommissionRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		facade.dispatchJob(
				CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCommissionBlockForYesterday();
	}

	@Test
	@DisplayName("job 178 handler tolerates null payload map and still runs commission block")
	void consume_job178_withNullPayload_stillRunsCommissionBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonCommissionRecordStatisticsJobHandler handler =
				new SalespersonCommissionRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCommissionBlockForYesterday();
	}

	@Test
	@DisplayName("job 178 handler tolerates non-empty payload and still runs commission block only")
	void consume_job178_withExtraPayloadKeys_stillRunsCommissionBlockOnly() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonCommissionRecordStatisticsJobHandler handler =
				new SalespersonCommissionRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCommissionBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 179 async enqueues on slow and consumer runs company block")
	void dispatchJob179_async_enqueuesOnSlowQueue_andConsumerInvokesCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_POPULARIZE_SCHEDULE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_POPULARIZE_SCHEDULE,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_POPULARIZE_SCHEDULE, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 180 async and consumer runs popularize block")
	void dispatchJob180_async_andConsumerInvokesPopularizeBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonPopularizeRecordStatisticsJobHandler handler =
				new SalespersonPopularizeRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_POPULARIZE_RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.SALESPERSON_POPULARIZE_RECORD_STATISTICS_JOB,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.SALESPERSON_POPULARIZE_RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runPopularizeBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 181 async enqueues on slow queue and consumer runs company statistics block")
	void dispatchJob_job181_async_enqueuesOnSlowQueue_andConsumerInvokesCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("dispatch job 182 async and consumer runs give-coupons block")
	void dispatchJob_job182_async_andConsumerInvokesGiveCouponsBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonGiveCouponsRecordStatisticsJobHandler handler =
				new SalespersonGiveCouponsRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(
				CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(0, msg.payload().size());
		assertEquals(RetryPolicy.platformDefault(), msg.retryPolicy());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runGiveCouponsBlockForYesterday();
	}

	@Test
	@DisplayName("job 181 handler tolerates null payload map and still runs company block")
	void consume_job181_withNullPayload_stillRunsCompanyBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 182 handler tolerates null payload map and still runs give-coupons block")
	void consume_job182_withNullPayload_stillRunsGiveCouponsBlock() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonGiveCouponsRecordStatisticsJobHandler handler =
				new SalespersonGiveCouponsRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB,
						null,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runGiveCouponsBlockForYesterday();
	}

	@Test
	@DisplayName("job 181 handler tolerates non-empty payload and still runs company block only")
	void consume_job181_withExtraPayloadKeys_stillRunsCompanyBlockOnly() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		RecordStatisticsJobHandler handler = new RecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runCompanyRecordStatisticsBlockForYesterday();
	}

	@Test
	@DisplayName("job 182 handler tolerates non-empty payload and still runs give-coupons block only")
	void consume_job182_withExtraPayloadKeys_stillRunsGiveCouponsBlockOnly() {
		CompanysStatisticsService statisticsService = mock(CompanysStatisticsService.class);
		SalespersonGiveCouponsRecordStatisticsJobHandler handler =
				new SalespersonGiveCouponsRecordStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("unexpected", 1);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						java.util.UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(statisticsService).runGiveCouponsBlockForYesterday();
	}
}
