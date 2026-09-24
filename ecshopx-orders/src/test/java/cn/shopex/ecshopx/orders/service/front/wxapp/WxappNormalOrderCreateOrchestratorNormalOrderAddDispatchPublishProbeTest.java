package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.goods.GoodsRecommendCheckoutMergePort;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutEmployeePurchaseCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseFormatPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsNormalCheckoutPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest {

	private static final String LISTENER_NAME = "listener:orders.supplier_order_split_on_normal_order_add";

	@Mock
	private OrderCreateNeedParamsPort orderCreateNeedParamsPort;

	@Mock
	private OrderCheckoutCartPort orderCheckoutCartPort;

	@Mock
	private OrderCheckoutEmployeePurchaseCartPort orderCheckoutEmployeePurchaseCartPort;

	@Mock
	private OrderCreateEmployeePurchaseFormatPort orderCreateEmployeePurchaseFormatPort;

	@Mock
	private OrderCreateItemCheckPort orderCreateItemCheckPort;

	@Mock
	private OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;

	@Mock
	private OrderCreateFormatDataPort orderCreateFormatDataPort;

	@Mock
	private OrderCreateGroupsNormalCheckoutPort orderCreateGroupsNormalCheckoutPort;

	@Mock
	private WxappNormalOrderTempInfoEnrichmentService wxappNormalOrderTempInfoEnrichmentService;

	@Mock
	private WxappNormalOrderCreateTransactionalRunner wxappNormalOrderCreateTransactionalRunner;

	@Mock
	private GoodsRecommendCheckoutMergePort goodsRecommendCheckoutMergePort;

	private DispatchFacade dispatchFacade;

	private DispatchCore dispatchCore;

	private WxappNormalOrderCreateOrchestrator orchestrator;

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
				new WxappNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCheckoutEmployeePurchaseCartPort,
						orderCreateEmployeePurchaseFormatPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						orderCreateGroupsNormalCheckoutPort,
						wxappNormalOrderTempInfoEnrichmentService,
						wxappNormalOrderCreateTransactionalRunner,
						publisher,
						mock(cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService.class),
						goodsRecommendCheckoutMergePort);
	}

	@Test
	@DisplayName("EVENT_NORMAL_ORDER_ADD: wxapp normal create — publishEvent probe after transaction")
	void create_afterTransaction_success_invokesPublishOnceWithThreeKeyPayload() {
		long companyId = 9L;
		long orderId = 1001L;
		String payType = "wxpay";

		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().put("order_type", "normal");
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
		orchestrator.create(state, request, "normal");

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
	@DisplayName("order_new 在填购物车前 merge recommend_item_id")
	void create_appliesRecommendMergeBeforeFillCart() {
		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().put("company_id", 9L);
		state.getParams().put("order_type", "normal");
		state.getOrderData().put("pay_type", "wxpay");
		state.getOrdersInsertResult().put("company_id", 9L);
		state.getOrdersInsertResult().put("order_id", 1001L);
		state.getOrdersInsertResult().put("pay_type", "wxpay");

		HttpServletRequest request = mock(HttpServletRequest.class);
		orchestrator.create(state, request, "normal");

		var order = inOrder(goodsRecommendCheckoutMergePort, orderCheckoutCartPort);
		order.verify(goodsRecommendCheckoutMergePort).apply(eq(9L), eq(state.getParams()));
		order.verify(orderCheckoutCartPort).fillItemsFromMemberDistributorCart(eq(state), eq(request));
	}
}
