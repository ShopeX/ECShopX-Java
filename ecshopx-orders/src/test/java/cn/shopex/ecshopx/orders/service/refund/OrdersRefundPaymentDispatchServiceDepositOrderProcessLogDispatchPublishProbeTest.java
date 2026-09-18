package cn.shopex.ecshopx.orders.service.refund;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/deposit dispatch publishEvent probe")
class OrdersRefundPaymentDispatchServiceDepositOrderProcessLogDispatchPublishProbeTest {

	@Test
	void depositRefund_afterRefundOrderDeposit_invokesPublishEventOnce_withDepositRefundSuccessDetail() {
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		OrdersRefundPaymentDispatchService svc =
				new OrdersRefundPaymentDispatchService(
						Collections.emptyList(),
						pointMemberAddPointService,
						mock(OrdersDepositOrderRefundService.class),
						orderProcessLogPublishPort,
						mock(NormalOrdersMapper.class));

		long companyId = 92002L;
		long orderId = 62002L;
		long refundBn = 52002L;
		long userId = 72002L;
		long shopId = 32002L;

		AftersalesRefund refund = new AftersalesRefund();
		refund.setPayType("DEPOSIT");
		refund.setOrderId(orderId);
		refund.setCompanyId(companyId);
		refund.setRefundBn(refundBn);
		refund.setUserId(userId);
		refund.setShopId(shopId);

		Trade trade = new Trade();
		trade.setTradeId("tr-deposit-probe-1");

		svc.dispatch(companyId, "", refund, trade, 100, 0, false);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						argThat(
								(Map<String, Object> m) ->
										m != null
												&& Objects.equals(m.get("order_id"), orderId)
												&& Objects.equals(((Number) m.get("company_id")).longValue(), companyId)
												&& "system".equals(m.get("operator_type"))
												&& "订单退款".equals(m.get("remarks"))
												&& ("订单号：" + orderId + "，订单退款成功（储值金额渠道)")
														.equals(String.valueOf(m.get("detail")))),
						eq(DispatchOptions.oplQueuedAfterCommit()));
	}
}
