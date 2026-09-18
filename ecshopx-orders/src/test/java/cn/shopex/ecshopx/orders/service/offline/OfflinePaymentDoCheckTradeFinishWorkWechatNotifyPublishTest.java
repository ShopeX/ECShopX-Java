package cn.shopex.ecshopx.orders.service.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verify-only: admin offline {@code do_check} approve path produces a trade row for {@code EVENT_TRADE_FINISH} that
 * carries producer gate keys for {@code listener:orders.listeners.TradeFinishWorkWechatNotify} ({@link
 * cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishWorkWechatDispatchListener} / {@link
 * cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWorkWechatNotifyService}). Assertions mirror {@link
 * cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest#assertTradeFinishWorkWechatNotifyGateKeys}.
 *
 * <p>Regression (listener fan-out contract): {@code TradeFinishWorkWechatNotifyEventSyncDispatchFlowTest} in {@code
 * ecshopx-bootstrap}.
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckTradeFinishWorkWechatNotifyPublishTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	@Captor
	private ArgumentCaptor<Map<String, Object>> tradeRowCaptor;

	private OfflinePaymentDoCheckService service;

	@BeforeEach
	void setUp() {
		lenient()
				.doAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							Consumer<TransactionStatus> action = invocation.getArgument(0);
							action.accept(mock(TransactionStatus.class));
							return null;
						})
				.when(transactionTemplate)
				.executeWithoutResult(any());

		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedValueOps);
		lenient().when(sharedStringRedisTemplate.opsForHash()).thenReturn(sharedHashOps);
		lenient().doNothing().when(companysValueOps).set(anyString(), anyString(), any(Duration.class));
		lenient().when(sharedValueOps.increment(anyString())).thenReturn(1L);
		lenient().when(sharedHashOps.get(anyString(), any())).thenReturn(null);
		lenient().doNothing().when(sharedHashOps).put(anyString(), any(), any());
		lenient().when(sharedStringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
		lenient().doNothing().when(orderProcessLogPublishPort).publish(any());

		service = new OfflinePaymentDoCheckService(
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
	@DisplayName(
			"Approve (offline_pay): publish row includes TradeFinishWorkWechatNotify gate keys (company_id, order_id)")
	void doCheck_whenApproveSuccess_offlinePay_ordersTradeFinishPublish_includesTradeFinishWorkWechatNotifyGateKeys() {
		runApproveScenario(
				10L,
				88L,
				42L,
				"trade-ww-offline-1",
				7L,
				"offline_pay",
				"normal");
	}

	@Test
	@DisplayName(
			"Approve (alipay): publish row includes TradeFinishWorkWechatNotify gate keys (company_id, order_id)")
	void doCheck_whenApproveSuccess_alipay_ordersTradeFinishPublish_includesTradeFinishWorkWechatNotifyGateKeys() {
		runApproveScenario(
				11L,
				89L,
				43L,
				"trade-ww-alipay-1",
				8L,
				"alipay",
				"normal");
	}

	@Test
	@DisplayName("Approve (remit): publish row includes TradeFinishWorkWechatNotify gate keys (company_id, order_id)")
	void doCheck_whenApproveSuccess_remit_ordersTradeFinishPublish_includesTradeFinishWorkWechatNotifyGateKeys() {
		runApproveScenario(
				13L,
				90L,
				44L,
				"trade-ww-remit-1",
				9L,
				"remit",
				"normal");
	}

	@Test
	@DisplayName(
			"Approve (wxpay, optional): publish row includes TradeFinishWorkWechatNotify gate keys (company_id, order_id)")
	void doCheck_whenApproveSuccess_wxpay_ordersTradeFinishPublish_includesTradeFinishWorkWechatNotifyGateKeys() {
		long paymentId = 14L;
		long companyId = 91L;
		long orderIdNum = 45L;
		String tradeId = "trade-ww-wx-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(92L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("wxpay");
		notPay.setTradeSourceType("normal");
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		notPay.setMobile("13900139888");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("wxpay");
		success.setTradeSourceType("normal");
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile("13900139888");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 92L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		assertTradeFinishWorkWechatNotifyProducerKeys(
				tradeRowCaptor.getValue(), companyId, String.valueOf(orderIdNum));
	}

	private void runApproveScenario(
			long paymentId,
			long companyId,
			long orderIdNum,
			String tradeId,
			long bankAccountId,
			String payTypeLiteral,
			String tradeSourceType) {
		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(bankAccountId);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType(payTypeLiteral);
		notPay.setTradeSourceType(tradeSourceType);
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		notPay.setMobile("13900139888");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType(payTypeLiteral);
		success.setTradeSourceType(tradeSourceType);
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile("13900139888");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", bankAccountId);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		assertTradeFinishWorkWechatNotifyProducerKeys(
				tradeRowCaptor.getValue(), companyId, String.valueOf(orderIdNum));
	}

	/**
	 * Producer gate keys on the published trade row for {@code listener:orders.listeners.TradeFinishWorkWechatNotify},
	 * aligned with {@link
	 * cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest#assertTradeFinishWorkWechatNotifyGateKeys}.
	 */
	private static void assertTradeFinishWorkWechatNotifyProducerKeys(
			Map<String, Object> published, long expectedCompanyId, String expectedOrderIdLiteral) {
		assertThat(published).isNotNull();
		assertThat(published.get("company_id")).isEqualTo(String.valueOf(expectedCompanyId));
		assertThat(published.get("order_id")).isEqualTo(expectedOrderIdLiteral);
		assertThat(String.valueOf(published.get("order_id")).trim()).isNotEmpty();
	}
}
