package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest {

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

	@Mock
	private ShopadminDeferredOrderCache shopadminDeferredOrderCache;

	@Mock
	private NormalOrderNumericIdService normalOrderNumericIdService;

	private NormalOrderAdminCreateUserOrderService normalOrderAdminCreateUserOrderService;

	@BeforeEach
	void setUp() {
		ShopadminNormalOrderCreateOrchestrator shopadminNormalOrderCreateOrchestrator =
				new ShopadminNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						shopadminNormalOrderCreateTransactionalRunner,
						shopadminDeferredOrderCache,
						normalOrderNumericIdService);

		normalOrderAdminCreateUserOrderService =
				new NormalOrderAdminCreateUserOrderService(
						paramBuilder, shopadminNormalOrderCreateOrchestrator);

		doNothing().when(shopadminNormalOrderCreateTransactionalRunner).preparePricingSnapshot(any());
	}

	@Test
	@DisplayName("admin POST /api/v1/order/create — createUserOrder writes deferred cache, no dispatch on create")
	void createUserOrder_whenNormalShopadmin_thenSaveWaitPayOnly() {
		long companyId = 9L;
		long operatorId = 42L;
		long userId = 7L;
		long orderId = 1001L;

		NormalOrderCreateState prebuiltState = new NormalOrderCreateState();
		prebuiltState.getParams().put("order_type", "normal_shopadmin");
		prebuiltState.getParams().put("user_id", userId);
		prebuiltState.getOrderData().put("company_id", companyId);
		prebuiltState.getOrderData().put("user_id", userId);
		prebuiltState.getOrderData().put("pay_type", "wxpay");
		prebuiltState.getOrderData().put("total_fee", 200);

		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> mergedInput = new LinkedHashMap<>();
		mergedInput.put("order_type", "normal_shopadmin");
		mergedInput.put("pay_type", "wxpay");

		when(paramBuilder.build(eq(companyId), eq(operatorId), anyMap(), any())).thenReturn(prebuiltState);
		when(normalOrderNumericIdService.generate(userId)).thenReturn(orderId);

		Map<String, Object> res =
				normalOrderAdminCreateUserOrderService.createUserOrder(
						companyId, operatorId, request, mergedInput);

		verify(shopadminDeferredOrderCache)
				.saveWaitPay(eq(prebuiltState), eq(orderId), eq(companyId), eq("wxpay"), eq(200), anyInt());
		assertEquals(orderId, res.get("order_id"));
	}
}
