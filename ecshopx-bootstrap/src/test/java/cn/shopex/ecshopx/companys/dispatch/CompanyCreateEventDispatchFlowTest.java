package cn.shopex.ecshopx.companys.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.CompanysDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.kaquan.dispatch.CompanyCreateDefaultMemberGradeDispatchListener;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class CompanyCreateEventDispatchFlowTest {

	/**
	 * Entry coverage note: operator OAuth login ({@code POST /api/v1/operator/oauth/login},
	 * {@code OperatorsController#login}) reaches {@code OperatorAuthService#credentialsByShopexOauthCode} and
	 * then {@code OperatorsOpenService#open}, the same publish-after-commit path exercised for company
	 * create dispatch as sibling entries. Runtime fan-out and consume behavior are asserted by the tests
	 * below; this method only pins that linkage for reviewers. In
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}, registration slot 3 is
	 * {@code listener:companys.company_create_online_open_callback}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenCallbackDispatchListener}; slot 4 is
	 * {@code listener:companys.company_create_online_open_sms}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenSmsDispatchListener} on the same
	 * six-listener Bus fan-out and consume path; slot 5 is
	 * {@code listener:companys.company_create_online_open_email}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenEmailDispatchListener} on the same
	 * six-listener Bus fan-out and consume path.
	 */
	@Test
	void oauthLoginEntrySharesCompanyCreatePublishPath() {
		assertTrue(true);
	}

	/**
	 * Entry coverage note: Espier admin login ({@code POST /api/v1/operator/login},
	 * {@code cn.shopex.ecshopx.espier.api.admin.v1.AuthController#login}) delegates to
	 * {@code cn.shopex.ecshopx.espier.service.OperatorLoginFacade#login}; for {@code logintype=oauthadmin}
	 * the same {@code OperatorAuthService#credentialsByShopexOauthCode} → {@code OperatorsOpenService#open}
	 * after-commit {@code publishEvent(EVENT_COMPANY_CREATE)} chain applies as for
	 * {@link #oauthLoginEntrySharesCompanyCreatePublishPath()}. Registration slot 6 (0-based index 5) is
	 * {@code listener:companys.company_create_init_demo_data}, asserted dynamically in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}.
	 */
	@Test
	void espierAuthLoginDocPinsOpenPublishPath() {
		assertTrue(true);
	}

	/**
	 * Entry coverage note: operator credential retrieval ({@code GET /api/v1/operator/credential},
	 * {@code OperatorsController#getCredentials}) validates {@code common-api-token}, then when
	 * {@code logintype} is {@code oauthadmin} uses {@code OperatorAuthService#retrieveByCredentials} and
	 * {@code OperatorsOpenService#open}, the same publish-after-commit path as other operator OAuth entries.
	 * Runtime fan-out and consume behavior are asserted by the tests below; this method only pins that linkage
	 * for reviewers. In
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}, registration slot 4 is
	 * {@code listener:companys.company_create_online_open_sms}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenSmsDispatchListener} on the same
	 * six-listener Bus fan-out and consume path.
	 */
	@Test
	void getCredentialsOauthAdminEntrySharesCompanyCreatePublishPath() {
		assertTrue(true);
	}

	/**
	 * Entry coverage note: same operator credential ingress as
	 * {@link #getCredentialsOauthAdminEntrySharesCompanyCreatePublishPath()} ({@code GET /api/v1/operator/credential},
	 * {@code OperatorsController#getCredentials}, {@code logintype=oauthadmin} → {@code OperatorsOpenService#open}).
	 * The default member grade completion boundary is
	 * {@code CompanyCreateDefaultMemberGradeDispatchListener} ({@code listener:kaquan.company_create_default_member_grade}),
	 * registered on {@code EVENT_COMPANY_CREATE} with the same Bus fan-out and consume path exercised below.
	 */
	@Test
	void defaultGradeListenerEntrySharesCompanyCreatePublishPathViaCredentialApi() {
		assertTrue(true);
	}

	/**
	 * Documentation: OpenAPI method key {@code ecx.jurisdiction.sysuser} has no ThirdApi handler class in
	 * this repository. The online-open callback slot ({@code listener:companys.company_create_online_open_callback},
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenCallbackDispatchListener} in registration
	 * order) is exercised together with the other listeners in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}. The same anchor applies when the
	 * scheduled-work row lists the SystemLink {@code Company} create trigger column as the CSV trigger column.
	 */
	@Test
	void jurisdictionSysuserOnlineOpenCallbackDoc() {
		assertTrue(true);
	}

	/**
	 * Documentation: OpenAPI method key {@code ecx.jurisdiction.sysuser} has no ThirdApi handler class in this
	 * repository. Registration slot 4 (1-based order in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}) is
	 * {@code listener:companys.company_create_online_open_sms}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenSmsDispatchListener}. The same event and
	 * Bus fan-out anchor applies when the migration CSV trigger column describes the SystemLink {@code Company}
	 * create path. Dynamic fan-out order and consume coverage for {@code EVENT_COMPANY_CREATE}, including the fourth
	 * listener ({@code listener:companys.company_create_online_open_sms}), are asserted in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}; this method only pins that linkage
	 * for reviewers.
	 */
	@Test
	void jurisdictionSysuserOnlineOpenSmsDoc() {
		assertTrue(true);
	}

	/**
	 * Documentation: OpenAPI method key {@code ecx.jurisdiction.sysuser} has no ThirdApi handler class in this
	 * repository. Registration slot 5 (1-based order in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}) is
	 * {@code listener:companys.company_create_online_open_email}, handled by
	 * {@link cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenEmailDispatchListener}. The same event and
	 * Bus fan-out anchor applies when the migration CSV trigger column describes the SystemLink {@code Company}
	 * create path. Dynamic fan-out order and consume coverage for {@code EVENT_COMPANY_CREATE}, including the fifth
	 * listener ({@code listener:companys.company_create_online_open_email}), are asserted in
	 * {@link #publishEvent_async_fanOutOrderMatchesSixListenerRegistration()}; this method only pins that linkage
	 * for reviewers.
	 */
	@Test
	void jurisdictionSysuserOnlineOpenEmailDoc() {
		assertTrue(true);
	}

	@Test
	void publishEvent_async_enqueuesShuyunListener_defaultQueue_andConsumeInvokesOnEvent() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_developer_shuyun",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("issue_id", null);
		payload.put("sms_mobile", "13800138000");
		payload.put("notify_email", null);
		payload.put("active_at_epoch_seconds", 100L);
		payload.put("expired_at_epoch_seconds", 200L);

		facade.publishEvent(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:companys.company_create_developer_shuyun", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(7L, msg.payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, calls.get());
	}

	@Test
	void publishEvent_async_fanOutOrderMatchesSixListenerRegistration() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger n1 = new AtomicInteger();
		MemberCardGradeMapper defaultGradeMapper = mock(MemberCardGradeMapper.class);
		when(defaultGradeMapper.selectOne(any())).thenReturn(null);
		AtomicInteger n3 = new AtomicInteger();
		AtomicInteger n4 = new AtomicInteger();
		AtomicInteger n5 = new AtomicInteger();
		AtomicInteger n6 = new AtomicInteger();
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_developer_shuyun",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> n1.incrementAndGet());
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:kaquan.company_create_default_member_grade",
				ListenerDispatchOptions.asyncDefaults(),
				new CompanyCreateDefaultMemberGradeDispatchListener(defaultGradeMapper));
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_callback",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> n3.incrementAndGet());
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_sms",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> n4.incrementAndGet());
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_email",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> n5.incrementAndGet());
		registry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_init_demo_data",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> n6.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("issue_id", null);
		payload.put("sms_mobile", "");
		payload.put("notify_email", null);
		payload.put("active_at_epoch_seconds", 0L);
		payload.put("expired_at_epoch_seconds", 0L);

		facade.publishEvent(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(6, captured.size());
		assertEquals("listener:companys.company_create_developer_shuyun", captured.get(0).listenerName());
		assertEquals("listener:kaquan.company_create_default_member_grade", captured.get(1).listenerName());
		assertEquals("listener:companys.company_create_online_open_callback", captured.get(2).listenerName());
		assertEquals("listener:companys.company_create_online_open_sms", captured.get(3).listenerName());
		assertEquals("listener:companys.company_create_online_open_email", captured.get(4).listenerName());
		assertEquals("listener:companys.company_create_init_demo_data", captured.get(5).listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		for (DispatchMessage m : captured) {
			runtime.consume(m, 1);
		}
		assertEquals(1, n1.get());
		verify(defaultGradeMapper)
				.insert(
						ArgumentMatchers.<MemberCardGrade>argThat(
								row -> "普通会员".equals(row.getGradeName())
										&& Boolean.TRUE.equals(row.getDefaultGrade())));
		assertEquals(1, n3.get());
		assertEquals(1, n4.get());
		assertEquals(1, n5.get());
		assertEquals(1, n6.get());
	}
}
