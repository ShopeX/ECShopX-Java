package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
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
import cn.shopex.ecshopx.thirdparty.dispatch.OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderAddPushMarketingCenterOnNormalOrderAddProcessor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;

@ExtendWith(MockitoExtension.class)
class ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest {

	private static final String LISTENER_NAME = "listener:orders.supplier_order_split_on_normal_order_add";

	@Mock
	private OrderCreateNeedParamsPort orderCreateNeedParamsPort;

	@Mock
	private OrderCheckoutCartPort orderCheckoutCartPort;

	@Mock
	private OrderCreateItemCheckPort orderCreateItemCheckPort;

	@Mock
	private OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;

	@Mock
	private OrderCreateFormatDataPort orderCreateFormatDataPort;

	@Mock
	private ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner;

	private DispatchFacade dispatchFacade;

	private DispatchCore dispatchCore;

	private ShopadminNormalOrderCreateOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		dispatchCore = mock(DispatchCore.class);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				payload -> {});
		dispatchFacade = new DispatchFacade(dispatchCore, new DispatchFanOutPlanner(registry));
		NormalOrderAddDispatchPublisher publisher =
				payload ->
						dispatchFacade.publishEvent(
								OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));
		orchestrator =
				new ShopadminNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						shopadminNormalOrderCreateTransactionalRunner,
						publisher,
						mock(cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService.class));
	}

	@Test
	@DisplayName("EVENT_NORMAL_ORDER_ADD: POST /api/v1/order/create shopadmin path — publishEvent probe after transaction")
	void create_afterTransactionalRunner_success_invokesPublishOnceWithThreeKeyPayload() {
		long companyId = 9L;
		long orderId = 1001L;
		String payType = "wxpay";

		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().put("order_type", "normal_shopadmin");
		state.getOrderData().put("pay_type", payType);
		Map<String, Object> insert = state.getOrdersInsertResult();
		insert.put("company_id", companyId);
		insert.put("order_id", orderId);
		insert.put("pay_type", payType);

		Map<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("pay_type", payType);

		HttpServletRequest request = mock(HttpServletRequest.class);
		orchestrator.create(state, request);

		List<DispatchMessage> published = dispatchFacade.publishedMessages();
		assertEquals(1, published.size());
		assertEquals(LISTENER_NAME, published.get(0).listenerName());
		assertEquals(expectedPayload, published.get(0).payload());
		assertEquals(DispatchMessageType.EVENT, published.get(0).messageType());
		assertEquals(DispatchMode.ASYNC, published.get(0).dispatchMode());
		assertEquals(DispatchDriverType.REDIS, published.get(0).driverType());
		assertEquals("default", published.get(0).queue());
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD, published.get(0).messageName());
		assertNotNull(published.get(0).traceId());
		verify(dispatchCore, times(1)).dispatch(any(DispatchMessage.class));
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_ADD: POST /api/v1/order/create shopadmin path — marketing listener consumed via DispatchConsumerRuntime")
	void create_afterTransactionalRunner_thenDispatchConsumerRuntimeInvokesMarketingProcessorHandle() {
		OrderAddPushMarketingCenterOnNormalOrderAddProcessor mockProcessor =
				mock(OrderAddPushMarketingCenterOnNormalOrderAddProcessor.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				new OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener(mockProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		NormalOrderAddDispatchPublisher publisher =
				payload ->
						facade.publishEvent(
								OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));

		ShopadminNormalOrderCreateOrchestrator marketingPathOrchestrator =
				new ShopadminNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						shopadminNormalOrderCreateTransactionalRunner,
						publisher,
						mock(cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService.class));

		long companyId = 9L;
		long orderId = 1001L;
		String payType = "wxpay";

		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().put("order_type", "normal_shopadmin");
		state.getOrderData().put("pay_type", payType);
		Map<String, Object> insert = state.getOrdersInsertResult();
		insert.put("company_id", companyId);
		insert.put("order_id", orderId);
		insert.put("pay_type", payType);

		Map<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("pay_type", payType);

		HttpServletRequest request = mock(HttpServletRequest.class);
		marketingPathOrchestrator.create(state, request);

		assertEquals(1, captured.size());
		DispatchMessage fanOut = captured.get(0);
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				fanOut.listenerName());
		assertEquals(expectedPayload, fanOut.payload());
		assertEquals(DispatchMessageType.EVENT, fanOut.messageType());
		assertEquals(DispatchMode.ASYNC, fanOut.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, fanOut.driverType());
		assertEquals("default", fanOut.queue());

		new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder())
				.consume(fanOut, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> handleCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockProcessor, times(1)).handle(handleCaptor.capture());
		assertEquals(expectedPayload, handleCaptor.getValue());
	}
}
