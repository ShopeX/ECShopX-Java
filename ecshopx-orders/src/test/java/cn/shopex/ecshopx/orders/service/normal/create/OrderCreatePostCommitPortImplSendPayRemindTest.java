package cn.shopex.ecshopx.orders.service.normal.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateCouponConsumePort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseEmptyCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartDeleteDataService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.shopex.ecshopx.orders.service.normal.create.OrderCreatePostCommitPortImpl;

@ExtendWith(MockitoExtension.class)
class OrderCreatePostCommitPortImplSendPayRemindTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private NormalOrderSaveOrderRelZitiService normalOrderSaveOrderRelZitiService;

	@Captor
	private ArgumentCaptor<Map<String, Object>> orderDataCaptor;

	private OrderCreatePostCommitPortImpl port;

	@BeforeEach
	void setUp() {
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
	void runAfterOrderInsert_publishesPayOrdersRemind_viaDispatchPublisher() {
		long companyId = 700L;
		long userId = 800L;
		long operatorId = 900L;

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> pr = p.getParams();
		pr.put("company_id", companyId);
		pr.put("operator_id", operatorId);
		pr.put("auto_cancel_time", "2026-05-09 00:00:00");

		Map<String, Object> od = p.getOrderData();
		od.put("user_id", userId);
		od.put("pay_type", "wxpay");
		od.put("wxa_appid", "wx-from-order");

		Map<String, Object> res = p.getOrdersInsertResult();
		res.put("order_id", 42L);
		res.put("company_id", companyId);
		res.put("pay_type", "wxpay");
		res.put("total_fee", 888);
		res.put("title", "shopadmin unit order");
		res.put("fee_symbol", "￥");
		res.put("fee_rate", 1.0);
		res.put("create_time", "2026-05-08 10:00:00");
		res.put("auto_cancel_time", "2026-05-09 12:00:00");

		Map<String, Object> member = new LinkedHashMap<>();
		member.put("wxapp_appid", "wx-from-member");
		member.put("open_id", "openid-member");
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(member);

		port.runAfterOrderInsert(p);

		verify(orderCreateCouponConsumePort, times(1)).consumeIfNeeded(any());
		verify(sendPayOrdersRemindJobDispatchPublisher, times(1)).publish(orderDataCaptor.capture());
		Map<String, Object> published = orderDataCaptor.getValue();
		assertEquals(42L, published.get("order_id"));
		assertEquals(companyId, published.get("company_id"));
		assertEquals(userId, published.get("user_id"));
		assertEquals("wx-from-order", published.get("wxa_appid"));
		assertEquals("openid-member", published.get("open_id"));
		assertEquals(888, published.get("total_fee"));
		assertEquals("shopadmin unit order", published.get("title"));
		assertEquals("￥", published.get("fee_symbol"));
		assertEquals(1.0, published.get("fee_rate"));
		assertEquals("2026-05-08 10:00:00", published.get("create_time"));
		assertEquals("2026-05-09 00:00:00", published.get("auto_cancel_time"));
	}

	@Test
	void runAfterOrderInsert_prefersMemberWxAppIdWhenOrderDataLacksIt() {
		long companyId = 701L;
		long userId = 801L;

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().put("company_id", companyId);
		p.getParams().put("operator_id", 1L);
		p.getOrderData().put("user_id", userId);
		p.getOrderData().put("pay_type", "wxpay");

		Map<String, Object> res = p.getOrdersInsertResult();
		res.put("order_id", 43L);
		res.put("company_id", companyId);
		res.put("pay_type", "wxpay");
		res.put("total_fee", 1);
		res.put("title", "t");

		Map<String, Object> member = new LinkedHashMap<>();
		member.put("wxapp_appid", "wx-only-member");
		member.put("open_id", "oid");
		when(memberAccountService.getMemberInfo(eq(userId), eq(companyId))).thenReturn(member);

		port.runAfterOrderInsert(p);

		verify(sendPayOrdersRemindJobDispatchPublisher, times(1)).publish(orderDataCaptor.capture());
		assertEquals("wx-only-member", orderDataCaptor.getValue().get("wxa_appid"));
		assertEquals("oid", orderDataCaptor.getValue().get("open_id"));
	}
}
