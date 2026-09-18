package cn.shopex.ecshopx.orders.service.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.common.service.brokerage.NormalOrderFinishBrokerageCoordinator;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_NORMAL_ORDER_CONFIRM_RECEIPT: Wxapp confirmReceipt chain — orderFinishBrokerage publishEvent probe")
class NormalOrderBrokerageOnFinishNormalOrderConfirmReceiptDispatchPublishProbeTest {

	@Mock
	private NormalOrderFinishBrokerageCoordinator normalOrderFinishBrokerageCoordinator;

	@Mock
	private BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	@Mock
	private DispatchFacade dispatchFacade;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private NormalOrderBrokerageOnFinishService underTest;

	@BeforeEach
	void setUp() {
		underTest = new NormalOrderBrokerageOnFinishService(
				normalOrderFinishBrokerageCoordinator, brokeragePlanCloseTimeService, dispatchFacade);
	}

	@Test
	void orderFinishBrokerage_invokesPublishEventOnce_withCompanyAndOrderPayload() {
		long companyId = 100L;
		long orderId = 200L;
		NormalOrders order = new NormalOrders();
		order.setUserId(1L);
		order.setOrderClass("normal");
		order.setTotalFee("100");
		order.setCommissionFee(0);

		underTest.orderFinishBrokerage(companyId, orderId, order);

		InOrder inOrder = inOrder(dispatchFacade, normalOrderFinishBrokerageCoordinator, brokeragePlanCloseTimeService);
		inOrder
				.verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT),
						payloadCaptor.capture(),
						ArgumentMatchers.argThat(new NormalOrderConfirmReceiptParentPublishOptionsMatcher()));
		inOrder
				.verify(normalOrderFinishBrokerageCoordinator, times(1))
				.onNormalOrderFinishBrokerage(
						eq(companyId),
						eq(orderId),
						eq(new NormalOrderBrokerageFinishInput(1L, "normal", "100", 0)));
		inOrder.verify(brokeragePlanCloseTimeService, times(1)).updatePlanCloseTime(companyId, orderId);

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload).containsOnlyKeys("company_id", "order_id");
		assertThat(payload.get("company_id")).isEqualTo(companyId);
		assertThat(payload.get("order_id")).isEqualTo(orderId);
	}

	/** Parent {@link DispatchOptions} for EVENT_NORMAL_ORDER_CONFIRM_RECEIPT publish (plan §1.3). */
	private static final class NormalOrderConfirmReceiptParentPublishOptionsMatcher
			implements ArgumentMatcher<DispatchOptions> {
		@Override
		public boolean matches(DispatchOptions o) {
			return o != null
					&& o.mode() == DispatchMode.ASYNC
					&& o.driverOverride() == DispatchDriverType.REDIS
					&& o.queue() == null
					&& o.delay() == null
					&& RetryPolicy.platformDefault().equals(o.retryPolicy());
		}
	}
}
