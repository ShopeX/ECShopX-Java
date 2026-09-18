package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.companys.dispatch.EmployeeJobDispatchPublisher;
import cn.shopex.ecshopx.companys.dispatch.EmployeeJobHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterBasicsUserProcessPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmployeeJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch job async enqueues on slow queue and consumer invokes marketing center port with normalized payload")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesMarketingCenterPort() {
		MarketingCenterBasicsUserProcessPort port = mock(MarketingCenterBasicsUserProcessPort.class);
		EmployeeJobHandler handler = new EmployeeJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("login_name", "staff1");
		payload.put("mobile", "13800138000");
		payload.put("user_name", "showname");
		payload.put("password", "hashed");
		payload.put("synctype", "add");

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.EMPLOYEE_JOB,
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
		assertEquals(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(100L, asLong(got.get("company_id")));
		assertEquals("staff1", got.get("login_name"));
		assertEquals("13800138000", got.get("mobile"));
		assertEquals("showname", got.get("user_name"));
		assertEquals("hashed", got.get("password"));
		assertEquals("add", got.get("synctype"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(port).processUser(eq(100L), captor.capture());
		Map<String, Object> sent = captor.getValue();
		assertEquals("100", sent.get("company_id"));
		assertEquals("staff1", sent.get("login_name"));
		assertEquals("13800138000", sent.get("mobile"));
		assertEquals("showname", sent.get("user_name"));
		assertEquals("hashed", sent.get("password"));
		assertEquals("add", sent.get("synctype"));
	}

	@Test
	@DisplayName("dispatch job async with synctype=update enqueues and port receives update")
	void dispatchJob_async_updateSynctype_enqueuesAndPortReceivesUpdate() {
		MarketingCenterBasicsUserProcessPort port = mock(MarketingCenterBasicsUserProcessPort.class);
		EmployeeJobHandler handler = new EmployeeJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 200L);
		payload.put("login_name", "staff2");
		payload.put("mobile", "13800138001");
		payload.put("user_name", "showname2");
		payload.put("password", "hashed2");
		payload.put("synctype", "update");

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.EMPLOYEE_JOB,
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
		assertEquals(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(200L, asLong(got.get("company_id")));
		assertEquals("staff2", got.get("login_name"));
		assertEquals("13800138001", got.get("mobile"));
		assertEquals("showname2", got.get("user_name"));
		assertEquals("hashed2", got.get("password"));
		assertEquals("update", got.get("synctype"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(port).processUser(eq(200L), captor.capture());
		Map<String, Object> sent = captor.getValue();
		assertEquals("200", sent.get("company_id"));
		assertEquals("staff2", sent.get("login_name"));
		assertEquals("13800138001", sent.get("mobile"));
		assertEquals("showname2", sent.get("user_name"));
		assertEquals("hashed2", sent.get("password"));
		assertEquals("update", sent.get("synctype"));
	}

	@Test
	@DisplayName("dispatch job async with synctype=del enqueues three-key payload and consumer invokes marketing center port")
	void dispatchJob_async_delSynctype_enqueuesAndPortReceivesDel() {
		MarketingCenterBasicsUserProcessPort port = mock(MarketingCenterBasicsUserProcessPort.class);
		EmployeeJobHandler handler = new EmployeeJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 300L);
		payload.put("mobile", "13800138002");
		payload.put("synctype", "del");

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.EMPLOYEE_JOB,
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
		assertEquals(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(3, got.size());
		assertEquals(300L, asLong(got.get("company_id")));
		assertEquals("13800138002", got.get("mobile"));
		assertEquals("del", got.get("synctype"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(port).processUser(eq(300L), captor.capture());
		Map<String, Object> sent = captor.getValue();
		assertEquals("300", sent.get("company_id"));
		assertEquals("13800138002", sent.get("mobile"));
		assertEquals("del", sent.get("synctype"));
	}

	@Test
	void enqueueAfterCommit_publishPayloadAndOptionsMatchEmployeeJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		EmployeeJobDispatchPublisher publisher = new EmployeeJobDispatchPublisher(dispatchFacade);

		Map<String, Object> sample = new LinkedHashMap<>();
		sample.put("company_id", 7L);
		sample.put("login_name", "u");
		sample.put("mobile", "");
		sample.put("user_name", "n");
		sample.put("password", "p");
		sample.put("synctype", "add");

		publisher.enqueueAfterCommit(sample);

		verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.EMPLOYEE_JOB),
						argThat(
								m ->
										m != null
												&& m.size() == 6
												&& Long.valueOf(7L).equals(asLong(m.get("company_id")))
												&& "u".equals(m.get("login_name"))
												&& "".equals(m.get("mobile"))
												&& "n".equals(m.get("user_name"))
												&& "p".equals(m.get("password"))
												&& "add".equals(m.get("synctype"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void enqueueAfterCommit_updatePayloadAndOptionsMatchEmployeeJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		EmployeeJobDispatchPublisher publisher = new EmployeeJobDispatchPublisher(dispatchFacade);

		Map<String, Object> sample = new LinkedHashMap<>();
		sample.put("company_id", 9L);
		sample.put("login_name", "u2");
		sample.put("mobile", "13900000001");
		sample.put("user_name", "n2");
		sample.put("password", "p2");
		sample.put("synctype", "update");

		publisher.enqueueAfterCommit(sample);

		verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.EMPLOYEE_JOB),
						argThat(
								m ->
										m != null
												&& m.size() == 6
												&& Long.valueOf(9L).equals(asLong(m.get("company_id")))
												&& "u2".equals(m.get("login_name"))
												&& "13900000001".equals(m.get("mobile"))
												&& "n2".equals(m.get("user_name"))
												&& "p2".equals(m.get("password"))
												&& "update".equals(m.get("synctype"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void enqueueAfterCommit_delPayloadAndOptionsMatchEmployeeJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		EmployeeJobDispatchPublisher publisher = new EmployeeJobDispatchPublisher(dispatchFacade);

		Map<String, Object> sample = new LinkedHashMap<>();
		sample.put("company_id", 11L);
		sample.put("mobile", "13900000099");
		sample.put("synctype", "del");

		publisher.enqueueAfterCommit(sample);

		verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.EMPLOYEE_JOB),
						argThat(
								m ->
										m != null
												&& m.size() == 3
												&& Long.valueOf(11L).equals(asLong(m.get("company_id")))
												&& "13900000099".equals(m.get("mobile"))
												&& "del".equals(m.get("synctype"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_normalizesNullAndNumericFieldsBeforePortCall() {
		MarketingCenterBasicsUserProcessPort port = mock(MarketingCenterBasicsUserProcessPort.class);
		EmployeeJobHandler handler = new EmployeeJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 55);
		payload.put("login_name", null);
		payload.put("mobile", "13900000000");
		payload.put("user_name", "x");
		payload.put("password", "pw");
		payload.put("synctype", "add");
		payload.put("extra_list", List.of());

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CompanysBundleDispatchJobNames.EMPLOYEE_JOB,
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

		verify(port)
				.processUser(
						eq(55L),
						argThat(
								m ->
										"55".equals(m.get("company_id"))
												&& "".equals(m.get("login_name"))
												&& "13900000000".equals(m.get("mobile"))
												&& "x".equals(m.get("user_name"))
												&& "pw".equals(m.get("password"))
												&& "add".equals(m.get("synctype"))
												&& "".equals(m.get("extra_list"))));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
