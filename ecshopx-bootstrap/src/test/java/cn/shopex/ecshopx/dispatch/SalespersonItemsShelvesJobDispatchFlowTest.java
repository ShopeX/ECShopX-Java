package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SalespersonDispatchJobNames;
import cn.shopex.ecshopx.config.SalespersonItemsShelvesJobDispatchPublisherImpl;
import cn.shopex.ecshopx.promotions.service.MarketingActivityPostCommitJobsService;
import cn.shopex.ecshopx.salesperson.dispatch.SalespersonItemsShelvesJobHandler;
import cn.shopex.ecshopx.salesperson.service.SalespersonItemsShelvesJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SalespersonItemsShelvesJobDispatchFlowTest {

	@Test
	@DisplayName(
			"SalespersonItemsShelves: dispatch async default queue and consumer invokes job service")
	void dispatchJob_asyncDefaultQueue_enqueuesAndConsumerInvokesJobService() {
		SalespersonItemsShelvesJobService jobService = Mockito.mock(SalespersonItemsShelvesJobService.class);
		SalespersonItemsShelvesJobHandler handler = new SalespersonItemsShelvesJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 9L;
		long activityId = 42L;
		String activityType = "package";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", companyId);
		payload.put("activityId", activityId);
		payload.put("activityType", activityType);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertNull(msg.listenerName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB139_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB140_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB141_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB142_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB144_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB145_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB146_ALIAS, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB147_ALIAS, msg.messageName());
		assertEquals(companyId, msg.payload().get("companyId"));
		assertEquals(activityId, msg.payload().get("activityId"));
		assertEquals(activityType, msg.payload().get("activityType"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(companyId), eq(activityId), eq(activityType));
	}

	@Test
	@DisplayName(
			"SalespersonItemsShelves: marketing activity type payload flows to job service")
	void dispatchJob_asyncDefaultQueue_withFullMinusActivityType_enqueuesAndConsumerInvokesJobService() {
		SalespersonItemsShelvesJobService jobService = Mockito.mock(SalespersonItemsShelvesJobService.class);
		SalespersonItemsShelvesJobHandler handler = new SalespersonItemsShelvesJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 7L;
		long activityId = 701L;
		String activityType = "full_minus";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", companyId);
		payload.put("activityId", activityId);
		payload.put("activityType", activityType);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB141_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB142_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB144_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB145_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB146_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB147_ALIAS, msg.messageName());
		assertEquals("full_minus", msg.payload().get("activityType"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(companyId), eq(activityId), eq("full_minus"));
	}

	@Test
	@DisplayName(
			"SalespersonItemsShelves: limited_time_sale activity type payload flows to job service")
	void dispatchJob_asyncDefaultQueue_withLimitedTimeSaleActivityType_enqueuesAndConsumerInvokesJobService() {
		SalespersonItemsShelvesJobService jobService = Mockito.mock(SalespersonItemsShelvesJobService.class);
		SalespersonItemsShelvesJobHandler handler = new SalespersonItemsShelvesJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 7L;
		long activityId = 702L;
		String activityType = "limited_time_sale";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", companyId);
		payload.put("activityId", activityId);
		payload.put("activityType", activityType);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB146_ALIAS, msg.messageName());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB147_ALIAS, msg.messageName());
		assertEquals("limited_time_sale", msg.payload().get("activityType"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(companyId), eq(activityId), eq("limited_time_sale"));
	}

	@Test
	@DisplayName(
			"Marketing post-commit: seckill end uses publisher impl, default queue, job:149 alias, consumer runs handler")
	void marketingPostCommit_enqueueSeckillEnd_usesPublisherImpl_dispatchesDefaultQueueAndConsumerRunsHandler() {
		SalespersonItemsShelvesJobService jobService = Mockito.mock(SalespersonItemsShelvesJobService.class);
		SalespersonItemsShelvesJobHandler handler = new SalespersonItemsShelvesJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		SalespersonItemsShelvesJobDispatchPublisherImpl publisher =
				new SalespersonItemsShelvesJobDispatchPublisherImpl(facade);
		MarketingActivityPostCommitJobsService svc = new MarketingActivityPostCommitJobsService(publisher);

		long companyId = 9L;
		long seckillId = 42L;
		svc.enqueueSeckillEndSalespersonItemsShelves(companyId, seckillId, "normal");

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, msg.messageName());
		assertEquals(
				SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB149_ALIAS, msg.messageName());
		assertEquals(companyId, msg.payload().get("companyId"));
		assertEquals(seckillId, msg.payload().get("activityId"));
		assertEquals("seckill", msg.payload().get("activityType"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(companyId), eq(seckillId), eq("seckill"));
	}

	@Test
	@DisplayName(
			"Marketing post-commit: group create uses publisher impl, default queue, consumer runs handler with group type")
	void marketingPostCommit_enqueueGroup_usesPublisherImpl_dispatchesDefaultQueueAndConsumerRunsHandler() {
		SalespersonItemsShelvesJobService jobService = Mockito.mock(SalespersonItemsShelvesJobService.class);
		SalespersonItemsShelvesJobHandler handler = new SalespersonItemsShelvesJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		SalespersonItemsShelvesJobDispatchPublisherImpl publisher =
				new SalespersonItemsShelvesJobDispatchPublisherImpl(facade);
		MarketingActivityPostCommitJobsService svc = new MarketingActivityPostCommitJobsService(publisher);

		long companyId = 9L;
		long groupsActivityId = 42L;
		svc.enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_ITEMS_SHELVES_JOB, msg.messageName());
		assertEquals(companyId, msg.payload().get("companyId"));
		assertEquals(groupsActivityId, msg.payload().get("activityId"));
		assertEquals("group", msg.payload().get("activityType"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(companyId), eq(groupsActivityId), eq("group"));
	}
}
