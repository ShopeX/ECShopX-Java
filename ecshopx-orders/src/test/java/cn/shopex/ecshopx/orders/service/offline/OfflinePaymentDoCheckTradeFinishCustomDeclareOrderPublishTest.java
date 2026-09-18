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
import java.util.Locale;
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
 * satisfies {@code listener:orders.listeners.TradeFinishCustomDeclareOrder} gate keys on the published map (see {@link
 * cn.shopex.ecshopx.orders.dispatch.TradeFinishCustomDeclareOrderDispatchListener} / {@link
 * cn.shopex.ecshopx.orders.service.customs.TradeFinishCustomDeclareOrderBusService}).
 *
 * <p>Non-wxpay paths align with bootstrap {@code publishEvent_whenPayTypeAlipay_skipsBusService}: listener short-circuits
 * before customs HTTP.
 *
 * <p>Regression (listener fan-out contract): {@code TradeFinishCustomDeclareOrderEventSyncDispatchFlowTest} in {@code
 * ecshopx-bootstrap}.
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckTradeFinishCustomDeclareOrderPublishTest {

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
			"Approve (offline_pay): publish row includes TradeFinishCustomDeclareOrder non-wxpay producer keys incl. trade_id")
	void doCheck_whenApproveSuccess_offlinePay_ordersTradeFinishPublish_includesTradeFinishCustomDeclareOrderNonWxGateKeys() {
		runApproveScenario(
				10L,
				88L,
				42L,
				"trade-custom-decl-offline-1",
				7L,
				"offline_pay",
				"normal");
	}

	@Test
	@DisplayName(
			"Approve (alipay): publish row includes TradeFinishCustomDeclareOrder non-wxpay producer keys incl. trade_id")
	void doCheck_whenApproveSuccess_alipay_ordersTradeFinishPublish_includesTradeFinishCustomDeclareOrderNonWxGateKeys() {
		runApproveScenario(
				11L,
				89L,
				43L,
				"trade-custom-decl-alipay-1",
				8L,
				"alipay",
				"normal");
	}

	@Test
	@DisplayName(
			"Approve (remit): publish row includes TradeFinishCustomDeclareOrder non-wxpay producer keys incl. trade_id")
	void doCheck_whenApproveSuccess_remit_ordersTradeFinishPublish_includesTradeFinishCustomDeclareOrderNonWxGateKeys() {
		runApproveScenario(
				13L,
				90L,
				44L,
				"trade-custom-decl-remit-1",
				9L,
				"remit",
				"normal");
	}

	@Test
	@DisplayName(
			"Approve (wxpay, optional): publish row includes TradeFinishCustomDeclareOrder wxpay producer keys incl. trade_id")
	void doCheck_whenApproveSuccess_wxpay_ordersTradeFinishPublish_includesTradeFinishCustomDeclareOrderWxpayProducerKeys() {
		long paymentId = 14L;
		long companyId = 91L;
		long orderIdNum = 45L;
		String tradeId = "trade-custom-decl-wx-1";

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
		assertTradeFinishCustomDeclareOrderWxpayProducerKeys(
				tradeRowCaptor.getValue(), companyId, String.valueOf(orderIdNum), 9001L, "normal");
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
		assertTradeFinishCustomDeclareOrderNonWxpayProducerKeys(
				tradeRowCaptor.getValue(), companyId, String.valueOf(orderIdNum), 9001L, tradeSourceType, payTypeLiteral);
	}

	/**
	 * Minimal producer gate keys on the trade row for {@code TradeFinishCustomDeclareOrder} when {@code pay_type} is not
	 * {@code wxpay} (listener skips customs HTTP); mirrors wxpay key set with non-wxpay literal.
	 */
	private static void assertTradeFinishCustomDeclareOrderNonWxpayProducerKeys(
			Map<String, Object> published,
			long expectedCompanyId,
			String expectedOrderId,
			long expectedUserId,
			String expectedTradeSourceType,
			String expectedPayTypeLiteral) {
		assertThat(published).isNotNull();
		assertThat(published)
				.containsKeys(
						"company_id",
						"order_id",
						"user_id",
						"trade_source_type",
						"trade_state",
						"pay_type",
						"trade_id");
		assertThat(String.valueOf(published.get("trade_state"))).isEqualTo("SUCCESS");
		assertThat(String.valueOf(published.get("pay_type"))).isEqualTo(expectedPayTypeLiteral);
		assertThat(String.valueOf(published.get("pay_type")).trim().toLowerCase(Locale.ROOT))
				.isNotEqualTo("wxpay");
		assertThat(String.valueOf(published.get("trade_id")).trim()).isNotEmpty();
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo(expectedOrderId);
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo(String.valueOf(expectedCompanyId));
		assertThat(longish(published.get("user_id"))).isEqualTo(expectedUserId);
		assertThat(String.valueOf(published.get("trade_source_type"))).isEqualTo(expectedTradeSourceType);
	}

	/**
	 * Minimal producer gate keys for {@code TradeFinishCustomDeclareOrder} wxpay path ({@link
	 * cn.shopex.ecshopx.orders.dispatch.TradeFinishCustomDeclareOrderDispatchListener} and {@link
	 * cn.shopex.ecshopx.orders.service.customs.TradeFinishCustomDeclareOrderBusService}), including non-empty {@code
	 * trade_id} for row handling. Copied aligned with {@link
	 * cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest}.
	 */
	private static void assertTradeFinishCustomDeclareOrderWxpayProducerKeys(
			Map<String, Object> published,
			long expectedCompanyId,
			String expectedOrderId,
			long expectedUserId,
			String expectedTradeSourceType) {
		assertThat(published).isNotNull();
		assertThat(published)
				.containsKeys(
						"company_id",
						"order_id",
						"user_id",
						"trade_source_type",
						"trade_state",
						"pay_type",
						"trade_id");
		assertThat(String.valueOf(published.get("trade_state"))).isEqualTo("SUCCESS");
		assertThat(String.valueOf(published.get("pay_type")).trim().toLowerCase(Locale.ROOT)).isEqualTo("wxpay");
		assertThat(String.valueOf(published.get("trade_id")).trim()).isNotEmpty();
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo(expectedOrderId);
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo(String.valueOf(expectedCompanyId));
		assertThat(longish(published.get("user_id"))).isEqualTo(expectedUserId);
		assertThat(String.valueOf(published.get("trade_source_type"))).isEqualTo(expectedTradeSourceType);
	}

	private static Long longish(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
