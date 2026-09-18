package cn.shopex.ecshopx.orders.service.offline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.refund.OfflinePayTradeFinishCanceledNormalOrderRefundService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order process log: offline do_check — cancelled order after trade success publish probe")
class OfflinePaymentDoCheckTradeFinishCanceledRefundOplPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
		TableInfoHelper.initTableInfo(assistant, OfflineBankAccount.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock(name = "sharedStringRedisTemplate")
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private SupplierOrderMapper supplierOrderMapper;

	@Mock
	private OrderSuccessTradeReadPort orderSuccessTradeReadPort;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	@Mock
	private ObjectMapper objectMapper;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private TransactionTemplate transactionTemplate;
	private OfflinePaymentDoCheckService service;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		transactionTemplate = new TransactionTemplate(syncFiringTxManager());

		OfflinePayTradeFinishCanceledNormalOrderRefundService refundService =
				new OfflinePayTradeFinishCanceledNormalOrderRefundService(
						normalOrdersMapper,
						supplierOrderMapper,
						orderSuccessTradeReadPort,
						aftersalesRefundService,
						orderProcessLogPublishPort);
		OrdersTradeFinishDispatchPublisher inlineTradeFinish = refundService::executeIfApplicable;

		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedValueOps);
		lenient().when(sharedStringRedisTemplate.opsForHash()).thenReturn(sharedHashOps);
		lenient().doNothing().when(companysValueOps).set(anyString(), anyString(), any(Duration.class));
		lenient().when(sharedValueOps.increment(anyString())).thenReturn(1L);
		lenient().when(sharedHashOps.get(anyString(), any())).thenReturn(null);
		lenient().doNothing().when(sharedHashOps).put(anyString(), any(), any());
		lenient().when(sharedStringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
		lenient().doNothing().when(aftersalesRefundService).createRefund(any());

		service =
				new OfflinePaymentDoCheckService(
						companysRedisTemplate,
						sharedStringRedisTemplate,
						offlinePaymentMapper,
						offlineBankAccountMapper,
						normalOrdersMapper,
						tradeMapper,
						orderProcessLogPublishPort,
						inlineTradeFinish,
						objectMapper,
						transactionTemplate);
	}

	@Test
	@DisplayName("Approve path publishes refund order process log entities once")
	void approve_whenCanceledNormalOrder_publishesRefundOplOnce() {
		long paymentId = 20L;
		long companyId = 88L;
		long orderIdNum = 42L;
		String tradeId = "trade-confirm-canceled-opl-probe-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(7L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		NormalOrders canceled = new NormalOrders();
		canceled.setOrderId(orderIdNum);
		canceled.setCompanyId(companyId);
		canceled.setOrderStatus("CANCEL");
		canceled.setOrderClass("normal");
		canceled.setUserId(9001L);
		canceled.setTotalFee("10000");
		canceled.setFreightFee(0);
		canceled.setFreightType("cash");
		canceled.setPayType("offline_pay");
		canceled.setShopId(501L);
		canceled.setDistributorId(0L);
		canceled.setPoint(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(canceled);

		when(supplierOrderMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> tradeMap = new LinkedHashMap<>();
		tradeMap.put("trade_id", tradeId);
		tradeMap.put("pay_type", "offline_pay");
		tradeMap.put("fee_type", "CNY");
		tradeMap.put("cur_fee_type", "CNY");
		tradeMap.put("cur_fee_rate", 1.0);
		tradeMap.put("cur_fee_symbol", "￥");
		tradeMap.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderIdNum)).thenReturn(Optional.of(tradeMap));

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(dispatchFacade, times(3))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		List<Map<String, Object>> payloads = payloadCaptor.getAllValues();
		long refundCount =
				payloads.stream().filter(p -> "订单退款".equals(String.valueOf(p.get("remarks")))).count();
		assertEquals(1L, refundCount);
		Map<String, Object> refundPayload =
				payloads.stream().filter(p -> "订单退款".equals(String.valueOf(p.get("remarks")))).findFirst().orElseThrow();
		assertEquals("订单号：" + orderIdNum + "，系统自动同意退款", refundPayload.get("detail"));
		assertEquals("system", refundPayload.get("operator_type"));
		assertEquals(0L, ((Number) refundPayload.get("operator_id")).longValue());
		assertEquals(Boolean.FALSE, refundPayload.get("is_show"));
		Object paramsObj = refundPayload.get("params");
		assertTrue(paramsObj instanceof Map<?, ?>);
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) paramsObj;
		assertEquals(orderIdNum, toLong(params.get("order_id")));
		assertEquals(companyId, toLong(params.get("company_id")));
		assertEquals(9001L, toLong(params.get("user_id")));
		verify(aftersalesRefundService, times(1)).createRefund(any());
	}

	@Test
	@DisplayName("Approve path does not publish refund log when order remains payed")
	void approve_whenPayedNormalOrder_skipsRefundOpl() {
		long paymentId = 21L;
		long companyId = 88L;
		long orderIdNum = 43L;
		String tradeId = "trade-confirm-payeed-opl-probe-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(7L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		NormalOrders payed = new NormalOrders();
		payed.setOrderId(orderIdNum);
		payed.setCompanyId(companyId);
		payed.setOrderStatus("PAYED");
		payed.setOrderClass("normal");
		payed.setUserId(9001L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(payed);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(dispatchFacade, times(2))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						any(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		verify(aftersalesRefundService, times(0)).createRefund(any());
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	private static PlatformTransactionManager syncFiringTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							s.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		};
	}
}
