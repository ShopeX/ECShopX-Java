package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.members.dispatch.BatchActionMembersJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.BatchActionMembersJobHandler;
import cn.shopex.ecshopx.members.service.admin.AdminMemberBatchOperatingChunkExecutor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BatchActionMembersJob191DispatchFlowTest {

	@Test
	@DisplayName("dispatch job 191 async enqueues on slow queue and consumer invokes chunk executor")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesChunkExecutor() {
		AdminMemberBatchOperatingChunkExecutor chunkExecutor = mock(AdminMemberBatchOperatingChunkExecutor.class);
		BatchActionMembersJobHandler handler = new BatchActionMembersJobHandler(chunkExecutor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BATCH_ACTION_MEMBERS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 42L;
		Map<String, Object> operatorParams = sampleOperatorParams(companyId);
		String actionType = "send_sms";
		Map<String, Object> chunkFilter = sampleChunkFilter(companyId);

		new BatchActionMembersJobDispatchPublisher(facade)
				.enqueueBatchActionMembersChunk(companyId, operatorParams, actionType, chunkFilter, 1, 50);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.BATCH_ACTION_MEMBERS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());

		Map<String, Object> pl = msg.payload();
		assertEquals(companyId, asLong(pl.get("company_id")));
		assertEquals(actionType, pl.get("action_type"));
		assertEquals(Boolean.TRUE, pl.get("is_queue"));
		assertEquals(1, asInt(pl.get("page")));
		assertEquals(50, asInt(pl.get("page_size")));
		assertNotNull(pl.get("params"));
		assertNotNull(pl.get("filter"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(chunkExecutor)
				.executeChunk(
						eq(companyId),
						argThat(
								m ->
										m != null
												&& Long.valueOf(9L).equals(m.get("distributor_id"))
												&& "staff-1".equals(m.get("sender"))
												&& m.get("__batch_operator_jwt__") instanceof Map),
						eq(actionType),
						argThat(
								f ->
										f != null
												&& companyId == asLong(f.get("company_id"))
												&& f.get("user_id") instanceof List<?> ids
												&& ids.size() == 2
												&& ids.contains(101L)
												&& ids.contains(102L)),
						eq(1),
						eq(50));
	}

	@Test
	void dispatchJob_publishPayloadMatchesBatchActionMembersEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		BatchActionMembersJobDispatchPublisher publisher = new BatchActionMembersJobDispatchPublisher(dispatchFacade);

		long companyId = 7L;
		Map<String, Object> operatorParams = sampleOperatorParams(companyId);
		Map<String, Object> chunkFilter = sampleChunkFilter(companyId);

		publisher.enqueueBatchActionMembersChunk(companyId, operatorParams, "give_coupon", chunkFilter, 1, 50);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.BATCH_ACTION_MEMBERS_JOB),
						Mockito.argThat(
								m ->
										m != null
												&& companyId == asLong(m.get("company_id"))
												&& "give_coupon".equals(m.get("action_type"))
												&& Boolean.TRUE.equals(m.get("is_queue"))
												&& 1 == asInt(m.get("page"))
												&& 50 == asInt(m.get("page_size"))
												&& m.get("params") instanceof Map<?, ?> pm
												&& Long.valueOf(9L).equals(pm.get("distributor_id"))
												&& m.get("filter") instanceof Map<?, ?> fm
												&& companyId == asLong(fm.get("company_id"))),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> sampleOperatorParams(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("distributor_id", 9L);
		jwt.put("operator_type", "staff");
		jwt.put("username", "1");
		jwt.put("mobile", "");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("distributor_id", 9L);
		params.put("sender", "staff-1");
		params.put("__batch_operator_jwt__", jwt);
		params.put("sms_content", "hello");
		return params;
	}

	private static Map<String, Object> sampleChunkFilter(long companyId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", List.of(101L, 102L));
		return filter;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
