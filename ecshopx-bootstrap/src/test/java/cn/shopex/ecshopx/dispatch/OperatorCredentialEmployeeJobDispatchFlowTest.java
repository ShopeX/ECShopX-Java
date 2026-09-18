package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.companys.dispatch.EmployeeJobHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterBasicsUserProcessPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Envelope for {@code EmployeeJob} when the operator credential / Shuyun staff auto-create path persists a new staff
 * row: same {@link DispatchMessage} shape as the employee create API path (queue {@code slow}, six payload keys,
 * {@code synctype=add}).
 */
class OperatorCredentialEmployeeJobDispatchFlowTest {

	@Test
	@DisplayName("credential path staff-add payload dispatches async on slow with EMPLOYEE_JOB name")
	void credentialPath_staffAddPayload_dispatchJob_matchesPhpEnvelope() {
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
		payload.put("company_id", 88240602404045L);
		payload.put("login_name", "13100000000");
		payload.put("mobile", "13100000000");
		payload.put("user_name", "测试用普通账号");
		payload.put("password", "456789");
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
		assertEquals(88240602404045L, asLong(got.get("company_id")));
		assertEquals("13100000000", got.get("login_name"));
		assertEquals("13100000000", got.get("mobile"));
		assertEquals("测试用普通账号", got.get("user_name"));
		assertEquals("456789", got.get("password"));
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
		verify(port).processUser(eq(88240602404045L), captor.capture());
		Map<String, Object> sent = captor.getValue();
		assertEquals("88240602404045", sent.get("company_id"));
		assertEquals("13100000000", sent.get("login_name"));
		assertEquals("13100000000", sent.get("mobile"));
		assertEquals("测试用普通账号", sent.get("user_name"));
		assertEquals("456789", sent.get("password"));
		assertEquals("add", sent.get("synctype"));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
