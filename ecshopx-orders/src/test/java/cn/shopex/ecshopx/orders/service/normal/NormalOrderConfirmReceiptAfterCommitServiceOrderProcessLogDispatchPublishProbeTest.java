package cn.shopex.ecshopx.orders.service.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.supplier.service.SupplierOrderConfirmReceiptSyncService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: confirmReceipt after-commit — publishEvent probe (admin + user order complete)")
class NormalOrderConfirmReceiptAfterCommitServiceOrderProcessLogDispatchPublishProbeTest {

	@Mock
	private SupplierOrderConfirmReceiptSyncService supplierOrderConfirmReceiptSyncService;

	@Mock
	private OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@Mock
	private NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;

	@Mock
	private OrderProfitPlanCloseTimeWriteService orderProfitPlanCloseTimeWriteService;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private NormalOrderConfirmReceiptAfterCommitService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		service =
				new NormalOrderConfirmReceiptAfterCommitService(
						supplierOrderConfirmReceiptSyncService,
						orderProcessLogPublishPort,
						ordersRelChinaumspayDivisionWriteService,
						pointMemberAddPointService,
						normalOrderBrokerageOnFinishService,
						orderProfitPlanCloseTimeWriteService);
	}

	@Test
	void run_adminOperator_invokesPublishEventOnce_withConfirmReceiptOrderCompleteShapedPayload() {
		long companyId = 100L;
		long orderIdNum = 200L;
		NormalOrders fresh = new NormalOrders();
		fresh.setPayType("wxpay");
		fresh.setDistributorId(0L);
		fresh.setUserId(1L);

		service.run(companyId, orderIdNum, fresh, 1_710_000_000, 86_400L, "admin", 42L, null);

		verify(supplierOrderConfirmReceiptSyncService, times(1)).confirmReceipt(companyId, orderIdNum);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(orderIdNum);
		assertThat(payload.get("company_id")).isEqualTo(companyId);
		assertThat(payload.get("operator_type")).isEqualTo("admin");
		assertThat(payload.get("operator_id")).isEqualTo(42L);
		assertThat(payload.get("remarks")).isEqualTo("订单完成");
		assertThat(payload.get("detail")).isEqualTo("订单号：" + orderIdNum + "，订单完成");
		assertThat(payload.get("params")).isEqualTo(Map.of());
	}

	@Test
	void run_userOperator_invokesPublishEventOnce_withWxappUserConfirmReceiptShapedPayload() {
		long companyId = 100L;
		long orderIdNum = 200L;
		NormalOrders fresh = new NormalOrders();
		fresh.setPayType("wxpay");
		fresh.setDistributorId(0L);
		fresh.setUserId(1L);

		service.run(companyId, orderIdNum, fresh, 1_710_000_000, 86_400L, "user", 99L, null);

		verify(supplierOrderConfirmReceiptSyncService, times(1)).confirmReceipt(companyId, orderIdNum);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(orderIdNum);
		assertThat(payload.get("company_id")).isEqualTo(companyId);
		assertThat(payload.get("operator_type")).isEqualTo("user");
		assertThat(payload.get("operator_id")).isEqualTo(99L);
		assertThat(payload.get("remarks")).isEqualTo("订单完成");
		assertThat(payload.get("detail")).isEqualTo("订单单号：" + orderIdNum + "，订单完成");
		assertThat(payload.get("params")).isEqualTo(Map.of());
	}
}
