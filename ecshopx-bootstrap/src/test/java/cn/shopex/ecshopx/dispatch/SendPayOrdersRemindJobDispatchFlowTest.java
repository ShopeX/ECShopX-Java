package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.common.port.orders.SendPayOrdersRemindWxaTemplatePort;
import cn.shopex.ecshopx.orders.dispatch.SendPayOrdersRemindJobHandler;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SendPayOrdersRemindJobDispatchFlowTest {

	@Test
	@DisplayName("dispatchJob: async delayed job uses default queue, 300s delay, consumer invokes WXA template port")
	void dispatchJob_asyncDelayed_enqueuesDefaultQueueWith300sDelay_andConsumerInvokesPort() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		OrderAssociations row = new OrderAssociations();
		row.setOrderId(42L);
		row.setCompanyId(9L);
		row.setOrderStatus("NOTPAY");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(row);

		SendPayOrdersRemindWxaTemplatePort wxaPort = mock(SendPayOrdersRemindWxaTemplatePort.class);
		SendPayOrdersRemindJobHandler handler = new SendPayOrdersRemindJobHandler(orderAssociationsMapper, wxaPort);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.SEND_PAY_ORDERS_REMIND_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("order_id", 42L);
		orderData.put("company_id", 9L);
		orderData.put("probe", "v1");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("orderData", orderData);

		facade.dispatchJob(
				OrdersDispatchJobNames.SEND_PAY_ORDERS_REMIND_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						Duration.ofSeconds(300),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertEquals(Duration.ofSeconds(300), msg.delay());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(OrdersDispatchJobNames.SEND_PAY_ORDERS_REMIND_JOB, msg.messageName());
		@SuppressWarnings("unchecked")
		Map<String, Object> wrapped = (Map<String, Object>) msg.payload().get("orderData");
		assertEquals(42L, wrapped.get("order_id"));
		assertEquals(9L, wrapped.get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(wxaPort).sendPayOrdersRemind(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertEquals(42L, passed.get("order_id"));
		assertEquals(9L, passed.get("company_id"));
		assertEquals("v1", passed.get("probe"));
	}
}
