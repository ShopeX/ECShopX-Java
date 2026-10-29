package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
class ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest {

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

	private ShopadminNormalOrderCreateOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator =
				new ShopadminNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						shopadminNormalOrderCreateTransactionalRunner,
						shopadminDeferredOrderCache,
						normalOrderNumericIdService);
		doNothing().when(shopadminNormalOrderCreateTransactionalRunner).preparePricingSnapshot(any());
	}

	@Test
	@DisplayName("POST /api/v1/order/create shopadmin — deferred cache only, no NORMAL_ORDER_ADD on create")
	void create_savesWaitPaySnapshot_doesNotPersistOrPublish() {
		long companyId = 9L;
		long userId = 7L;
		long orderId = 1001L;
		String payType = "wxpaypos";

		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().put("order_type", "normal_shopadmin");
		state.getParams().put("company_id", companyId);
		state.getParams().put("user_id", userId);
		state.getOrderData().put("company_id", companyId);
		state.getOrderData().put("user_id", userId);
		state.getOrderData().put("pay_type", payType);
		state.getOrderData().put("total_fee", 100);

		when(normalOrderNumericIdService.generate(userId)).thenReturn(orderId);

		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> res = orchestrator.create(state, request);

		verify(shopadminNormalOrderCreateTransactionalRunner).preparePricingSnapshot(state);
		verify(shopadminDeferredOrderCache)
				.saveWaitPay(eq(state), eq(orderId), eq(companyId), eq(payType), eq(100), anyInt());
		assertEquals(orderId, res.get("order_id"));
		assertEquals("NOTPAY", res.get("order_status"));
	}
}
