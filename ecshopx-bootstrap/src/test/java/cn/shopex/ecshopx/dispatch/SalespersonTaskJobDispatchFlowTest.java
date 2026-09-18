package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SalespersonDispatchJobNames;
import cn.shopex.ecshopx.salesperson.dispatch.SalespersonTaskJobHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterTasksCompletePort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SalespersonTaskJobDispatchFlowTest {

	@Test
	@DisplayName("Salesperson task job async slow queue with item_id drives marketing center complete")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesMarketingCenterComplete_withItemId() {
		MarketingCenterTasksCompletePort port = mock(MarketingCenterTasksCompletePort.class);
		SalespersonTaskJobHandler handler = new SalespersonTaskJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_TASK_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 11L;
		long subtaskId = 22L;
		String storeBn = "S001";
		String employeeNumber = "E9";
		String unionId = "wx-union-1";
		long itemId = 33L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("subtask_id", subtaskId);
		payload.put("store_bn", storeBn);
		payload.put("employee_number", employeeNumber);
		payload.put("user_id", unionId);
		payload.put("item_id", itemId);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_TASK_JOB,
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
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_TASK_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(companyId, msg.payload().get("company_id"));
		assertEquals(subtaskId, msg.payload().get("subtask_id"));
		assertEquals(storeBn, msg.payload().get("store_bn"));
		assertEquals(employeeNumber, msg.payload().get("employee_number"));
		assertEquals(unionId, msg.payload().get("user_id"));
		assertEquals(itemId, ((Number) msg.payload().get("item_id")).longValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.completeTasks(
						eq(companyId),
						argThat(
								m ->
										m.get("company_id").equals(companyId)
												&& m.get("subtask_id").equals(subtaskId)
												&& m.get("store_bn").equals(storeBn)
												&& m.get("employee_number").equals(employeeNumber)
												&& m.get("user_id").equals(unionId)
												&& m.containsKey("item_id")
												&& ((Number) m.get("item_id")).longValue() == itemId));
	}

	@Test
	@DisplayName("Salesperson task job async slow queue without item_id drives marketing center complete")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesMarketingCenterComplete_withoutItemId() {
		MarketingCenterTasksCompletePort port = mock(MarketingCenterTasksCompletePort.class);
		SalespersonTaskJobHandler handler = new SalespersonTaskJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_TASK_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 11L;
		long subtaskId = 22L;
		String storeBn = "S001";
		String employeeNumber = "E9";
		String unionId = "wx-union-1";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("subtask_id", subtaskId);
		payload.put("store_bn", storeBn);
		payload.put("employee_number", employeeNumber);
		payload.put("user_id", unionId);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_TASK_JOB,
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
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_TASK_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertFalse(msg.payload().containsKey("item_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.completeTasks(
						eq(companyId),
						argThat(
								m ->
										m.get("company_id").equals(companyId)
												&& m.get("subtask_id").equals(subtaskId)
												&& m.get("store_bn").equals(storeBn)
												&& m.get("employee_number").equals(employeeNumber)
												&& m.get("user_id").equals(unionId)
												&& !m.containsKey("item_id")));
	}
}
