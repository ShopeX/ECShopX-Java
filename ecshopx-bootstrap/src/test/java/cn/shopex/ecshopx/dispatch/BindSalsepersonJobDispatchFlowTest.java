package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobHandler;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.bind.MemberSalespersonBindMarketingCoordinator;
import cn.shopex.ecshopx.members.service.h5.bind.ShoppingGuideForH5BindLookup;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmUpdateMemberInfoByMobilePort;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BindSalsepersonJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch job 185 async enqueues on slow queue and consumer invokes handler")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesHandler() {
		MarketingCenterOpenApiSignedFormClient marketing = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		WorkWechatRelMapper workWechatRelMapper = Mockito.mock(WorkWechatRelMapper.class);
		ShoppingGuideForH5BindLookup lookup = Mockito.mock(ShoppingGuideForH5BindLookup.class);

		when(marketing.postReturningFullRootMap(anyLong(), eq("salesperson.bind.member"), any()))
				.thenReturn(Map.of("code", 200));

		DmCrmSettingReadPort dmCrm = mock(DmCrmSettingReadPort.class);
		when(dmCrm.isPointIntegrationOpen(anyLong())).thenReturn(false);
		DmCrmUpdateMemberInfoByMobilePort dmUpdate = mock(DmCrmUpdateMemberInfoByMobilePort.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);

		MemberSalespersonBindMarketingCoordinator coordinator = new MemberSalespersonBindMarketingCoordinator(
				marketing, workWechatRelMapper, lookup, dmCrm, dmUpdate, memberAccountService);
		BindSalsepersonJobHandler handler = new BindSalsepersonJobHandler(coordinator);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = samplePayload();
		facade.dispatchJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB,
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
		assertEquals(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());
		Map<String, Object> pl = msg.payload();
		assertEquals(6, pl.size());
		assertEquals(100L, asLong(pl.get("company_id")));
		assertEquals("union-z", pl.get("unionid"));
		assertEquals("work-u", pl.get("work_userid"));
		assertEquals(1, asInt(pl.get("customer_type")));
		assertEquals("13800000000", pl.get("mobile"));
		assertEquals(200L, asLong(pl.get("user_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(marketing).postReturningFullRootMap(eq(100L), eq("salesperson.bind.member"), any());
	}

	@Test
	@DisplayName("when bind returns 200 and Damo CRM is on, relShop then updateMemberInfoByMobile run")
	void dispatchJob_whenBind200AndCrmEnabled_syncsRelShopAndDmCrm() {
		MarketingCenterOpenApiSignedFormClient marketing = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		WorkWechatRelMapper workWechatRelMapper = Mockito.mock(WorkWechatRelMapper.class);
		ShoppingGuideForH5BindLookup lookup = Mockito.mock(ShoppingGuideForH5BindLookup.class);
		DmCrmSettingReadPort dmCrm = mock(DmCrmSettingReadPort.class);
		when(dmCrm.isPointIntegrationOpen(100L)).thenReturn(true);
		DmCrmUpdateMemberInfoByMobilePort dmUpdate = mock(DmCrmUpdateMemberInfoByMobilePort.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);
		when(marketing.postReturningFullRootMap(anyLong(), eq("salesperson.bind.member"), any()))
				.thenReturn(Map.of("code", 200));
		when(marketing.postReturningFullRootMap(anyLong(), eq("basics.salesperson.relShop"), any()))
				.thenReturn(Map.of("code", 200));
		when(memberAccountService.getMemberInfo(200L, 100L))
				.thenReturn(
						new LinkedHashMap<>(
								Map.of("mobile", "13800000000", "username", "u1", "sex", 1, "birthday", "", "email", "")));

		MemberSalespersonBindMarketingCoordinator coordinator = new MemberSalespersonBindMarketingCoordinator(
				marketing, workWechatRelMapper, lookup, dmCrm, dmUpdate, memberAccountService);
		BindSalsepersonJobHandler handler = new BindSalsepersonJobHandler(coordinator);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB, handler);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB,
				samplePayload(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(marketing).postReturningFullRootMap(eq(100L), eq("salesperson.bind.member"), any());
		verify(marketing).postReturningFullRootMap(eq(100L), eq("basics.salesperson.relShop"), any());
		verify(dmUpdate).updateMemberInfoByMobile(eq(100L), any());
	}

	@Test
	void dispatchJob_whenMarketingNon200_attemptsRollbackLookup() {
		MarketingCenterOpenApiSignedFormClient marketing = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		WorkWechatRelMapper workWechatRelMapper = Mockito.mock(WorkWechatRelMapper.class);
		ShoppingGuideForH5BindLookup lookup = Mockito.mock(ShoppingGuideForH5BindLookup.class);

		when(marketing.postReturningFullRootMap(anyLong(), anyString(), any())).thenReturn(Map.of("code", 500));
		when(lookup.getShoppingGuideDetailForH5Bind(100L, "work-u"))
				.thenReturn(Map.of("salesperson_id", 77L));

		DmCrmSettingReadPort dmCrm = mock(DmCrmSettingReadPort.class);
		DmCrmUpdateMemberInfoByMobilePort dmUpdate = mock(DmCrmUpdateMemberInfoByMobilePort.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);

		MemberSalespersonBindMarketingCoordinator coordinator = new MemberSalespersonBindMarketingCoordinator(
				marketing, workWechatRelMapper, lookup, dmCrm, dmUpdate, memberAccountService);
		BindSalsepersonJobHandler handler = new BindSalsepersonJobHandler(coordinator);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB, handler);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB,
				samplePayload(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(marketing).postReturningFullRootMap(eq(100L), eq("salesperson.bind.member"), any());
		verify(lookup).getShoppingGuideDetailForH5Bind(100L, "work-u");
	}

	@Test
	void dispatchJob_publishPayloadMatchesBindSalsepersonEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		BindSalsepersonJobDispatchPublisher publisher = new BindSalsepersonJobDispatchPublisher(dispatchFacade);

		publisher.enqueueBindSalseperson(100L, "union-z", "work-z", 1, "13900000000", 200L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB),
						Mockito.argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& "union-z".equals(m.get("unionid"))
												&& "work-z".equals(m.get("work_userid"))
												&& 1 == asInt(m.get("customer_type"))
												&& "13900000000".equals(m.get("mobile"))
												&& 200L == asLong(m.get("user_id"))),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	@Test
	@DisplayName("dispatch job 186 async enqueues on slow queue and consumer invokes handler with user_id 0")
	void dispatchJob_job186_async_enqueuesAndConsumerInvokesHandler() {
		MarketingCenterOpenApiSignedFormClient marketing = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		WorkWechatRelMapper workWechatRelMapper = Mockito.mock(WorkWechatRelMapper.class);
		ShoppingGuideForH5BindLookup lookup = Mockito.mock(ShoppingGuideForH5BindLookup.class);

		when(marketing.postReturningFullRootMap(anyLong(), eq("salesperson.bind.member"), any()))
				.thenReturn(Map.of("code", 200));

		DmCrmSettingReadPort dmCrm = mock(DmCrmSettingReadPort.class);
		when(dmCrm.isPointIntegrationOpen(anyLong())).thenReturn(false);
		DmCrmUpdateMemberInfoByMobilePort dmUpdate = mock(DmCrmUpdateMemberInfoByMobilePort.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);

		MemberSalespersonBindMarketingCoordinator coordinator = new MemberSalespersonBindMarketingCoordinator(
				marketing, workWechatRelMapper, lookup, dmCrm, dmUpdate, memberAccountService);
		BindSalsepersonJobHandler handler = new BindSalsepersonJobHandler(coordinator);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_DM_MEMBER_REGISTER, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		BindSalsepersonJobDispatchPublisher publisher = new BindSalsepersonJobDispatchPublisher(facade);
		publisher.enqueueBindSalsepersonAfterDmMemberCreate(100L, "union-z", "work-u", 1, "13800000000");

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_DM_MEMBER_REGISTER, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());
		Map<String, Object> pl = msg.payload();
		assertEquals(6, pl.size());
		assertEquals(100L, asLong(pl.get("company_id")));
		assertEquals("union-z", pl.get("unionid"));
		assertEquals("work-u", pl.get("work_userid"));
		assertEquals(1, asInt(pl.get("customer_type")));
		assertEquals("13800000000", pl.get("mobile"));
		assertEquals(0L, asLong(pl.get("user_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(marketing).postReturningFullRootMap(eq(100L), eq("salesperson.bind.member"), any());
	}

	@Test
	@DisplayName("dispatch job 194 async enqueues on slow queue and consumer invokes handler")
	void dispatchJob_job194_async_enqueuesOnSlowQueue_andConsumerInvokesHandler() {
		MarketingCenterOpenApiSignedFormClient marketing = Mockito.mock(MarketingCenterOpenApiSignedFormClient.class);
		WorkWechatRelMapper workWechatRelMapper = Mockito.mock(WorkWechatRelMapper.class);
		ShoppingGuideForH5BindLookup lookup = Mockito.mock(ShoppingGuideForH5BindLookup.class);

		when(marketing.postReturningFullRootMap(anyLong(), eq("salesperson.bind.member"), any()))
				.thenReturn(Map.of("code", 200));

		DmCrmSettingReadPort dmCrm = mock(DmCrmSettingReadPort.class);
		when(dmCrm.isPointIntegrationOpen(anyLong())).thenReturn(false);
		DmCrmUpdateMemberInfoByMobilePort dmUpdate = mock(DmCrmUpdateMemberInfoByMobilePort.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);

		MemberSalespersonBindMarketingCoordinator coordinator = new MemberSalespersonBindMarketingCoordinator(
				marketing, workWechatRelMapper, lookup, dmCrm, dmUpdate, memberAccountService);
		BindSalsepersonJobHandler handler = new BindSalsepersonJobHandler(coordinator);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		BindSalsepersonJobDispatchPublisher publisher = new BindSalsepersonJobDispatchPublisher(facade);
		publisher.enqueueBindSalsepersonAfterWxappBindSalesperson(100L, "union-z", "work-u", 2, "13800000000", 301L);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());
		Map<String, Object> pl = msg.payload();
		assertEquals(6, pl.size());
		assertEquals(100L, asLong(pl.get("company_id")));
		assertEquals("union-z", pl.get("unionid"));
		assertEquals("work-u", pl.get("work_userid"));
		assertEquals(2, asInt(pl.get("customer_type")));
		assertEquals("13800000000", pl.get("mobile"));
		assertEquals(301L, asLong(pl.get("user_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(marketing).postReturningFullRootMap(eq(100L), eq("salesperson.bind.member"), any());
	}

	@Test
	void dispatchJob_publishPayloadMatchesBindSalsepersonEnvelope_job194() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		BindSalsepersonJobDispatchPublisher publisher = new BindSalsepersonJobDispatchPublisher(dispatchFacade);

		publisher.enqueueBindSalsepersonAfterWxappBindSalesperson(100L, "union-z", "work-z", 2, "13900000000", 200L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON),
						Mockito.argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& "union-z".equals(m.get("unionid"))
												&& "work-z".equals(m.get("work_userid"))
												&& 2 == asInt(m.get("customer_type"))
												&& "13900000000".equals(m.get("mobile"))
												&& 200L == asLong(m.get("user_id"))),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> samplePayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("unionid", "union-z");
		payload.put("work_userid", "work-u");
		payload.put("customer_type", 1);
		payload.put("mobile", "13800000000");
		payload.put("user_id", 200L);
		return payload;
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
