package cn.shopex.ecshopx.orders.service.normal.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateCouponConsumePort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseEmptyCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartDeleteDataService;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import org.springframework.data.redis.core.StringRedisTemplate;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
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
import cn.shopex.ecshopx.orders.service.normal.create.OrderCreatePostCommitPortImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: shopadmin normal order createUserOrder publishEvent probe")
class OrderCreatePostCommitPortImplNormalOrderCreateUserOrderOrderProcessLogDispatchPublishProbeTest {

	@Mock
	private OrderCreateCouponConsumePort orderCreateCouponConsumePort;

	@Mock
	private OrderCreateEmployeePurchaseEmptyCartPort orderCreateEmployeePurchaseEmptyCartPort;

	@Mock
	private OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@Mock
	private OperatorCartDeleteDataService operatorCartDeleteDataService;

	@Mock
	private CartMapper cartMapper;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher;

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private NormalOrderSaveOrderRelZitiService normalOrderSaveOrderRelZitiService;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;

	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private OrderCreatePostCommitPortImpl port;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		port =
				new OrderCreatePostCommitPortImpl(
						orderCreateCouponConsumePort,
						orderCreateEmployeePurchaseEmptyCartPort,
						orderDirectedCrowdDiscountPort,
						pointMemberAddPointService,
						operatorCartDeleteDataService,
						cartMapper,
						stringRedisTemplate,
						sendPayOrdersRemindJobDispatchPublisher,
						memberAccountService,
						orderProcessLogPublishPort,
						normalOrderSaveOrderRelZitiService);
	}

	@Test
	void runAfterOrderInsert_shopadminCreateUserOrder_shapedParams_invokesPublishEventOnce_withOrderDataUserIdAsOperatorIdAndFullParamsMap() {
		long companyId = 12L;
		long staffOperatorId = 99L;
		long memberUserId = 5001L;

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> pr = p.getParams();
		pr.put("company_id", companyId);
		pr.put("operator_id", staffOperatorId);
		pr.put("order_source", "shop_offline");
		pr.put("source_from", "dianwu");
		pr.put("order_type", "normal_shopadmin");

		Map<String, Object> od = p.getOrderData();
		od.put("user_id", memberUserId);
		od.put("pay_type", "wxpay");

		Map<String, Object> res = p.getOrdersInsertResult();
		res.put("order_id", 70001L);
		res.put("company_id", companyId);

		port.runAfterOrderInsert(p);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("70001", payload.get("order_id"));
		assertEquals("12", payload.get("company_id"));
		assertEquals("user", payload.get("operator_type"));
		assertEquals(Boolean.TRUE, payload.get("is_show"));
		assertEquals(memberUserId, payload.get("operator_id"));
		assertEquals("订单创建", payload.get("remarks"));
		assertEquals("订单号：70001，订单创建", payload.get("detail"));
		assertEquals(new LinkedHashMap<>(pr), payload.get("params"));
		verify(orderCreateCouponConsumePort, times(1)).consumeIfNeeded(any());
	}

	@Test
	void runAfterOrderInsert_shopadminCreateUserOrder_excardOrderClass_usesExcardRemarksAndDetail() {
		long companyId = 3L;

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().put("company_id", companyId);
		p.getParams().put("operator_id", 1L);
		p.getOrderData().put("user_id", 77L);
		p.getOrderData().put("order_class", "excard");
		p.getOrderData().put("pay_type", "wxpay");

		Map<String, Object> res = p.getOrdersInsertResult();
		res.put("order_id", "EX-ORD-1");
		res.put("company_id", companyId);

		port.runAfterOrderInsert(p);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("EX-ORD-1", payload.get("order_id"));
		assertEquals("3", payload.get("company_id"));
		assertEquals("订单核销", payload.get("remarks"));
		assertEquals("订单号：EX-ORD-1，订单核销成功", payload.get("detail"));
		assertEquals(77L, payload.get("operator_id"));
	}
}
