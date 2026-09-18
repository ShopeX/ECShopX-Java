package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.members.dispatch.PushMemberTagRelationJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.PushMemberTagRelationJobHandler;
import cn.shopex.ecshopx.members.service.reltag.MemberTagRelationMarketingSyncService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Job 188 dispatch flow — admin bulk dual-collection member/tag reltag path")
class PushMemberTagRelationJob188DispatchFlowTest {

	private static final DispatchOptions PUSH_MEMBER_TAG_RELATION_JOB_OPTIONS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					"marketing",
					null,
					RetryPolicy.platformDefault());

	@Test
	@DisplayName("dispatch job 188 async enqueues on marketing queue and consumer invokes handler")
	void dispatchJob188_async_enqueuesOnMarketingQueue_andConsumerInvokesHandler() {
		MarketingCenterOpenApiSignedFormClient marketingClient = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		when(marketingClient.postReturningFullRootMap(anyLong(), eq("members.tag.relation.sync"), any()))
				.thenReturn(Map.of("code", 200));

		MemberTagRelationMarketingSyncService marketingSyncService =
				new MemberTagRelationMarketingSyncService(marketingClient);
		PushMemberTagRelationJobHandler handler = new PushMemberTagRelationJobHandler(marketingSyncService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.PUSH_MEMBER_TAG_RELATION_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = samplePayloadTwoRelations();
		facade.dispatchJob(
				MembersBundleDispatchJobNames.PUSH_MEMBER_TAG_RELATION_JOB,
				payload,
				PUSH_MEMBER_TAG_RELATION_JOB_OPTIONS);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("marketing", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.PUSH_MEMBER_TAG_RELATION_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(marketingClient).postReturningFullRootMap(eq(77L), eq("members.tag.relation.sync"), any());
	}

	@Test
	@DisplayName("job 188 publisher forwards add payload with async marketing dispatch options")
	void dispatchJob188_publishPayloadMatchesExpectedEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		PushMemberTagRelationJobDispatchPublisher publisher = new PushMemberTagRelationJobDispatchPublisher(dispatchFacade);

		List<Map<String, Object>> relations = sampleRelationsTwo();
		publisher.enqueuePushMemberTagRelation(77L, "add", relations);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.PUSH_MEMBER_TAG_RELATION_JOB),
						Mockito.argThat(
								m ->
										m != null
												&& 77L == asLong(m.get("company_id"))
												&& "add".equals(m.get("action"))
												&& m.get("relations") instanceof List<?> rel
												&& rel.size() == 2),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "marketing".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> samplePayloadTwoRelations() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 77L);
		payload.put("action", "add");
		payload.put("relations", sampleRelationsTwo());
		return payload;
	}

	private static List<Map<String, Object>> sampleRelationsTwo() {
		List<Map<String, Object>> relations = new ArrayList<>();
		Map<String, Object> r1 = new LinkedHashMap<>();
		r1.put("user_id", "10");
		r1.put("mobile", "13900000001");
		r1.put("tag_id", "20");
		r1.put("tag_name", "t1");
		r1.put("tag_type", "self");
		r1.put("wechat_tag_id", null);
		relations.add(r1);
		Map<String, Object> r2 = new LinkedHashMap<>();
		r2.put("user_id", "11");
		r2.put("mobile", "");
		r2.put("tag_id", "21");
		r2.put("tag_name", "t2");
		r2.put("tag_type", "wechat");
		r2.put("wechat_tag_id", "wx-1");
		relations.add(r2);
		return relations;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
