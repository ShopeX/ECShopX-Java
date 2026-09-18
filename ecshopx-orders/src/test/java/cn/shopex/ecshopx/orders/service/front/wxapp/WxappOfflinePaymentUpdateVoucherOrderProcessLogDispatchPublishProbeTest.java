package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: wxapp offline update voucher publishEvent probe")
class WxappOfflinePaymentUpdateVoucherOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
		TableInfoHelper.initTableInfo(assistant, OfflineBankAccount.class);
	}

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private ObjectMapper objectMapper;
	private WxappOfflinePaymentUpdateVoucherService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		objectMapper = new ObjectMapper();
		service = new WxappOfflinePaymentUpdateVoucherService(
				offlinePaymentMapper,
				normalOrdersMapper,
				offlineBankAccountMapper,
				orderProcessLogPublishPort,
				objectMapper);
	}

	@Test
	void updateOfflineVoucher_success_invokesPublishEventOnce_withOfflineTransferPayload() {
		long companyId = 11L;
		long userId = 22L;
		long orderId = 33_001L;
		long bankAccountId = 5L;
		long paymentId = 77L;

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("id", paymentId);
		merged.put("order_id", orderId);
		merged.put("bank_account_id", bankAccountId);
		merged.put("voucher_pic", List.of("https://img.example/v2.jpg"));
		merged.put("transfer_remark", "probe update note");

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		auth.put("user_id", userId);

		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setSupplierId(null);
		order.setUserId(userId);
		order.setOrderStatus("NOTPAY");
		order.setPayType("offline_pay");
		order.setSalesmanId(0L);
		order.setTotalFee("9900");
		order.setShopId(3L);
		order.setDistributorId(0L);

		OfflinePayment beforeUpdate = new OfflinePayment();
		beforeUpdate.setId(paymentId);
		beforeUpdate.setCheckStatus(2);
		beforeUpdate.setOrderId(orderId);

		OfflinePayment afterUpdate = new OfflinePayment();
		afterUpdate.setId(paymentId);
		afterUpdate.setCheckStatus(0);
		afterUpdate.setOrderId(orderId);

		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(beforeUpdate, afterUpdate);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(bankAccountId);
		acc.setCompanyId(companyId);
		acc.setBankAccountName("acct");
		acc.setBankAccountNo("622200");
		acc.setBankName("ICBC");
		acc.setChinaUmsNo("");
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);

		service.updateOfflineVoucher(httpServletRequest, merged, auth);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(orderId, payload.get("order_id"));
		assertEquals(companyId, payload.get("company_id"));
		assertEquals(0L, payload.get("supplier_id"));
		assertEquals("user", payload.get("operator_type"));
		assertEquals(userId, payload.get("operator_id"));
		assertEquals("线下转账提交", payload.get("remarks"));
		assertEquals("订单号：" + orderId + "，线下转账信息提交", payload.get("detail"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) payload.get("params");
		assertEquals(companyId, ((Number) params.get("company_id")).longValue());
		assertEquals(userId, ((Number) params.get("user_id")).longValue());
		assertEquals(orderId, ((Number) params.get("order_id")).longValue());
		assertEquals(bankAccountId, ((Number) params.get("bank_account_id")).longValue());
		assertEquals(paymentId, ((Number) params.get("id")).longValue());
	}
}
