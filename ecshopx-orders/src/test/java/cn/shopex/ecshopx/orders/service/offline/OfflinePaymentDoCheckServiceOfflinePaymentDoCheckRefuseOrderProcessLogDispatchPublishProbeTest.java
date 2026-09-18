package cn.shopex.ecshopx.orders.service.offline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin offline do_check refuse publishEvent probe")
class OfflinePaymentDoCheckServiceOfflinePaymentDoCheckRefuseOrderProcessLogDispatchPublishProbeTest {

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
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ValueOperations<String, String> companysValueOps;

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

		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		lenient().doNothing().when(companysValueOps).set(anyString(), anyString(), any(Duration.class));

		service =
				new OfflinePaymentDoCheckService(
						companysRedisTemplate,
						sharedStringRedisTemplate,
						offlinePaymentMapper,
						offlineBankAccountMapper,
						normalOrdersMapper,
						tradeMapper,
						orderProcessLogPublishPort,
						ordersTradeFinishDispatchPublisher,
						objectMapper,
						transactionTemplate);
	}

	@Test
	void doCheck_refuse_checkStatus2_invokesPublishEvent_forRefusePayloadOnce() {
		long paymentId = 10L;
		long companyId = 88L;
		long orderIdNum = 42L;
		long operatorId = 1L;
		String operatorName = "op";
		String refuseRemark = "审核拒绝说明";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);

		OfflinePayment afterRefuse = new OfflinePayment();
		afterRefuse.setId(paymentId);
		afterRefuse.setCompanyId(companyId);
		afterRefuse.setOrderId(orderIdNum);
		afterRefuse.setCheckStatus(2);
		afterRefuse.setRemark(refuseRemark);
		afterRefuse.setOperatorName(operatorName);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending, afterRefuse);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(7L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 2);
		body.put("remark", refuseRemark);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, operatorId, "admin", operatorName, body);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> captured = payloadCaptor.getValue();
		assertEquals("线下转账审核拒绝", captured.get("remarks"));
		assertEquals("审核拒绝", captured.get("detail"));
		assertEquals(Boolean.TRUE, captured.get("is_show"));
		assertEquals(orderIdNum, toLong(captured.get("order_id")));
		assertEquals(companyId, toLong(captured.get("company_id")));
		assertEquals("admin", captured.get("operator_type"));
		assertEquals(operatorId, toLong(captured.get("operator_id")));

		String remarks = String.valueOf(captured.get("remarks"));
		String detail = String.valueOf(captured.get("detail"));
		assertFalse(remarks.contains("交易支付成功"));
		assertFalse(detail.contains("NOTPAY→SUCCESS"));

		Object paramsObj = captured.get("params");
		assertInstanceOf(Map.class, paramsObj);
		@SuppressWarnings("unchecked")
		Map<String, Object> pm = (Map<String, Object>) paramsObj;
		assertEquals(2, ((Number) pm.get("check_status")).intValue());
		assertEquals(paymentId, toLong(pm.get("offline_payment_id")));
		assertEquals(orderIdNum, toLong(pm.get("order_id")));
		assertEquals(refuseRemark, pm.get("remark"));
		assertEquals(operatorName, pm.get("operator_name"));

		verify(ordersTradeFinishDispatchPublisher, times(0)).publish(any());
	}

	private static long toLong(Object o) {
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
