package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;

@ExtendWith(MockitoExtension.class)
class NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest {

	static final DispatchOptions PARENT_OPTS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					null,
					null,
					RetryPolicy.platformDefault());

	@Mock
	private DispatchFacade dispatchFacade;

	@Mock
	private NormalOrderAdminCreateUserOrderParamBuilder paramBuilder;

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

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private NormalOrderAdminCreateUserOrderService normalOrderAdminCreateUserOrderService;

	@BeforeEach
	void setUp() {
		NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher =
				payload ->
						dispatchFacade.publishEvent(
								OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD, payload, PARENT_OPTS);

		ShopadminNormalOrderCreateOrchestrator shopadminNormalOrderCreateOrchestrator =
				new ShopadminNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						shopadminNormalOrderCreateTransactionalRunner,
						normalOrderAddDispatchPublisher,
						mock(cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService.class));

		normalOrderAdminCreateUserOrderService =
				new NormalOrderAdminCreateUserOrderService(
						paramBuilder, shopadminNormalOrderCreateOrchestrator);

		doNothing().when(shopadminNormalOrderCreateTransactionalRunner).runInTransaction(any(), any());
	}

	@Test
	@DisplayName("EVENT_NORMAL_ORDER_ADD: admin POST /api/v1/order/create — createUserOrder publishes after transaction")
	void createUserOrder_whenNormalShopadmin_thenPublishEventOnceWithThreeKeyPayload() {
		long companyId = 9L;
		long operatorId = 42L;

		NormalOrderCreateState prebuiltState = new NormalOrderCreateState();
		prebuiltState.getParams().put("order_type", "normal_shopadmin");
		Map<String, Object> insert = prebuiltState.getOrdersInsertResult();
		insert.put("company_id", companyId);
		insert.put("order_id", 1001L);
		insert.put("pay_type", "wxpay");
		prebuiltState.getOrderData().put("pay_type", "wxpay");

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> mergedInput = new LinkedHashMap<>();
		mergedInput.put("order_type", "normal_shopadmin");
		mergedInput.put("pay_type", "wxpay");

		when(paramBuilder.build(eq(companyId), eq(operatorId), anyMap(), any())).thenReturn(prebuiltState);

		normalOrderAdminCreateUserOrderService.createUserOrder(companyId, operatorId, request, mergedInput);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD),
						payloadCaptor.capture(),
						eq(PARENT_OPTS));

		Map<String, Object> captured = payloadCaptor.getValue();
		assertEquals(insert.get("company_id"), captured.get("company_id"));
		assertEquals(insert.get("order_id"), captured.get("order_id"));
		assertEquals(insert.get("pay_type"), captured.get("pay_type"));
	}
}
