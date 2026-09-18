package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

class OpenapiDeveloperUpdateEmployeeJob196FlowTest {

	@Test
	@DisplayName("enqueueAfterCommit with job:196 uses slow queue, async mode, and synctype add envelope")
	void openapiStaffEnqueue_usesJob196NameSlowQueueAndSynctypeAdd() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		EmployeeJobDispatchPublisher publisher = new EmployeeJobDispatchPublisher(dispatchFacade);

		Map<String, Object> sample = new LinkedHashMap<>();
		sample.put("company_id", 42L);
		sample.put("login_name", "ln");
		sample.put("mobile", "13900000000");
		sample.put("user_name", "un");
		sample.put("password", "pw");
		sample.put("synctype", "add");

		publisher.enqueueAfterCommit(CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196, sample);

		verify(dispatchFacade)
				.dispatchJob(
						eq(CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196),
						argThat(
								m ->
										m != null
												&& m.size() == 6
												&& Long.valueOf(42L).equals(asLong(m.get("company_id")))
												&& "ln".equals(m.get("login_name"))
												&& "13900000000".equals(m.get("mobile"))
												&& "un".equals(m.get("user_name"))
												&& "pw".equals(m.get("password"))
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
	@DisplayName("job:196 dispatch and consume twice invokes marketing port per staff row")
	void openapiStaffEnqueue_handlerConsumesAndCallsMarketingPort() {
		MarketingCenterBasicsUserProcessPort port = mock(MarketingCenterBasicsUserProcessPort.class);
		EmployeeJobHandler handler = new EmployeeJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196, handler);
		registry.registerJob(CompanysBundleDispatchJobNames.EMPLOYEE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload1 = new LinkedHashMap<>();
		payload1.put("company_id", 501L);
		payload1.put("login_name", "a");
		payload1.put("mobile", "13800138001");
		payload1.put("user_name", "ua");
		payload1.put("password", "pa");
		payload1.put("synctype", "add");

		Map<String, Object> payload2 = new LinkedHashMap<>();
		payload2.put("company_id", 501L);
		payload2.put("login_name", "b");
		payload2.put("mobile", "13800138002");
		payload2.put("user_name", "ub");
		payload2.put("password", "pb");
		payload2.put("synctype", "add");

		facade.dispatchJob(
				CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196,
				payload1,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
		facade.dispatchJob(
				CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196,
				payload2,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		for (DispatchMessage msg : captured) {
			assertEquals(DispatchMessageType.JOB, msg.messageType());
			assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
			assertEquals(DispatchDriverType.REDIS, msg.driverType());
			assertEquals("slow", msg.queue());
			assertNull(msg.delay());
			assertEquals(CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196, msg.messageName());
			assertNull(msg.listenerName());
		}

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(port, times(2)).processUser(eq(501L), captor.capture());
		List<Map<String, Object>> all = captor.getAllValues();
		assertEquals(2, all.size());
		assertEquals("a", all.get(0).get("login_name"));
		assertEquals("b", all.get(1).get("login_name"));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
