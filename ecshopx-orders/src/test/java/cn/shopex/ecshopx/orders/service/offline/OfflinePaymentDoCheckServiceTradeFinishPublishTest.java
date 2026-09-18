package cn.shopex.ecshopx.orders.service.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import cn.shopex.ecshopx.orders.service.statistics.TradePayFinishStatisticsBusService;
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
 * Verify-only: admin offline {@code do_check} approve path publishes a snake_case trade row to
 * {@link cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher} (production: offline-qualified bean →
 * {@code EVENT_TRADE_FINISH_CSV292} plane). Downstream {@code listener:orders.listeners.TradeFinishCountBrokerage} /
 * {@link cn.shopex.ecshopx.orders.service.brokerage.TradeFinishCountBrokerageBusService} gate on
 * {@code company_id}, {@code order_id}, {@code pay_type}, {@code trade_state}, and {@code pay_fee}.
 * {@code TradeFinishLinkMember} consumes {@code user_id}, {@code company_id}, {@code shop_id},
 * {@code distributor_id} on the same row. When the trade maps to group-buy / activity shapes,
 * {@code trade_source_type} (e.g. {@code normal_groups}) is also present on the same row.
 * {@code listener:orders.listeners.TradePayFinishStatistics} / {@link TradePayFinishStatisticsBusService}
 * gates on {@code company_id}, {@code order_id}, {@code trade_source_type}, {@code trade_state}, amount keys
 * ({@code total_fee} / {@code pay_fee}), and {@code user_id} / {@code distributor_id} / optional {@code merchant_id}.
 * {@code listener:orders.listeners.TradeFinishProfit} / {@link cn.shopex.ecshopx.orders.dispatch.profit.TradeFinishProfitBusService}
 * gates on {@code order_id} / {@code company_id} / {@code user_id} (optional camelCase aliases per consumer).
 * {@code listener:systemlink.trade_finish_send_ome} / {@link cn.shopex.ecshopx.systemlink.dispatch.TradeFinishSendOmeDispatchListener}
 * gates {@code company_id} / {@code order_id}; {@link cn.shopex.ecshopx.systemlink.service.ome.TradeFinishSendOmeBusService}
 * consumes {@code trade_source_type} and {@code user_id} (group branch) on the same row.
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckServiceTradeFinishPublishTest {

	private static final TradePayFinishStatisticsBusService TRADE_PAY_FINISH_STATS_GATE =
			new TradePayFinishStatisticsBusService(mock(StringRedisTemplate.class));

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
			"Approve: publish EVENT_TRADE_FINISH row with TradeFinishCountBrokerage gate keys — company_id, order_id, pay_type, trade_state, pay_fee (offline_pay / SUCCESS)")
	void doCheck_whenConfirmSuccess_triggersOrdersTradeFinishDispatchPublisherPublish() {
		long paymentId = 10L;
		long companyId = 88L;
		long orderIdNum = 42L;
		String tradeId = "trade-confirm-1";

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

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		notPay.setTradeSourceType("normal");
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		String buyerMobile = "13900139888";
		notPay.setMobile(buyerMobile);
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		success.setTradeSourceType("normal");
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile(buyerMobile);
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertThat(published)
				.as("TradeFinishCountBrokerageBusService gates on these producer keys (plan §5.A)")
				.containsKeys("company_id", "order_id", "pay_type", "trade_state", "pay_fee");
		assertThat(published.get("company_id")).isEqualTo(String.valueOf(companyId));
		assertThat(published.get("order_id")).isEqualTo(String.valueOf(orderIdNum));
		assertThat(published.get("pay_type")).isEqualTo("offline_pay");
		assertThat(published.get("trade_state")).isEqualTo("SUCCESS");
		assertThat(published.get("pay_fee")).isEqualTo(10_000);
		assertThat(published.get("trade_source_type")).isEqualTo("normal");
		assertThat(published.get("mobile")).isEqualTo(buyerMobile);
		assertThat(published.get("time_start")).isEqualTo("1704067200");
		assertTradeFinishLinkMemberProducerKeys(published, 9001L, companyId, 501L, 0L);
		assertTradePayFinishStatisticsProducerKeys(
				published,
				companyId,
				orderIdNum,
				"normal",
				"SUCCESS",
				"9001",
				"0",
				0L,
				null,
				10_000L);
		assertTradeFinishProfitProducerKeys(published, orderIdNum, companyId, 9001L);
		assertTradeFinishSendOmeProducerKeys(published, companyId, orderIdNum, 9001L);
		assertTradeFinishPrinterOrderProducerKeys(
				published, companyId, String.valueOf(orderIdNum), 0L, 10_000, 0);
	}

	@Test
	@DisplayName(
			"Approve (normal_groups + offline_pay + positive pay_fee): TradeFinishCountBrokerage row includes plan keys + trade_source_type=normal_groups")
	void doCheck_whenConfirmSuccess_normalGroups_offlinePay_positivePayFee_publishesGroupsGateRow() {
		long paymentId = 12L;
		long companyId = 77L;
		long orderIdNum = 55L;
		String tradeId = "trade-confirm-groups-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(71L);
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
		notPay.setPayType("offline_pay");
		notPay.setTradeSourceType("normal_groups");
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		String buyerMobile = "13800138000";
		notPay.setMobile(buyerMobile);
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		success.setTradeSourceType("normal_groups");
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile(buyerMobile);
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 71L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertThat(published)
				.as("TradeFinishCountBrokerage gate keys + optional trade_source_type for offline group-buy mapping")
				.containsKeys(
						"company_id", "order_id", "pay_type", "trade_state", "pay_fee", "trade_source_type");
		assertThat(published.get("trade_source_type")).isEqualTo("normal_groups");
		assertThat(published.get("company_id")).isEqualTo(String.valueOf(companyId));
		assertThat(published.get("order_id")).isEqualTo(String.valueOf(orderIdNum));
		assertThat(published.get("pay_type")).isEqualTo("offline_pay");
		assertThat(published.get("trade_state")).isEqualTo("SUCCESS");
		assertThat(published.get("pay_fee")).isEqualTo(10_000);
		assertThat(published.get("time_start")).isEqualTo("1704067200");
		assertThat(published.get("mobile")).isEqualTo(buyerMobile);
		assertTradeFinishLinkMemberProducerKeys(published, 9001L, companyId, 501L, 0L);
		assertTradePayFinishStatisticsProducerKeys(
				published,
				companyId,
				orderIdNum,
				"normal_groups",
				"SUCCESS",
				"9001",
				"0",
				0L,
				null,
				10_000L);
		assertTradeFinishProfitProducerKeys(published, orderIdNum, companyId, 9001L);
		assertTradeFinishSendOmeProducerKeys(published, companyId, orderIdNum, 9001L);
		assertTradeFinishPrinterOrderProducerKeys(
				published, companyId, String.valueOf(orderIdNum), 0L, 10_000, 0);
	}

	@Test
	@DisplayName(
			"Approve: publish row satisfies TradePayFinishStatisticsBusService keys with non-zero distributor_id and merchant_id")
	void doCheck_whenConfirmSuccess_withDistributorAndMerchant_setsTradePayFinishStatisticsOptionalDimensions() {
		long paymentId = 13L;
		long companyId = 66L;
		long orderIdNum = 901L;
		String tradeId = "trade-confirm-merchant-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(17L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("12");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		notPay.setTradeSourceType("normal");
		notPay.setPayFee(20_000);
		notPay.setUserId("8002");
		notPay.setShopId("502");
		notPay.setMerchantId(305L);
		notPay.setTimeStart("1704067200");
		String buyerMobile = "13700137000";
		notPay.setMobile(buyerMobile);
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		success.setTradeSourceType("normal");
		success.setPayFee(20_000);
		success.setUserId("8002");
		success.setShopId("502");
		success.setDistributorId("12");
		success.setMerchantId(305L);
		success.setTimeStart("1704067200");
		success.setMobile(buyerMobile);
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 17L);
		body.put("pay_fee", "200.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishLinkMemberProducerKeys(published, 8002L, companyId, 502L, 12L);
		assertTradePayFinishStatisticsProducerKeys(
				published, companyId, orderIdNum, "normal", "SUCCESS", "8002", "12", 305L, null, 20_000L);
		assertTradeFinishProfitProducerKeys(published, orderIdNum, companyId, 8002L);
		assertTradeFinishSendOmeProducerKeys(published, companyId, orderIdNum, 8002L);
		assertTradeFinishPrinterOrderProducerKeys(
				published, companyId, String.valueOf(orderIdNum), 12L, 20_000, 0);
	}

	@Test
	@DisplayName("Reject: no OrdersTradeFinishDispatchPublisher.publish — TradeFinishCountBrokerage path not triggered")
	void doCheck_whenRefuseCheck_doesNotInvokeOrdersTradeFinishDispatchPublisher() {
		long paymentId = 11L;
		long companyId = 99L;
		long orderIdNum = 43L;

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(8L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 2);
		body.put("bank_account_id", 8L);
		body.put("pay_fee", "50.00");
		body.put("remark", "bad voucher");

		service.doCheck(companyId, 2L, "admin", "op", body);

		verifyNoInteractions(ordersTradeFinishDispatchPublisher);
	}

	/**
	 * Producer keys for {@link cn.shopex.ecshopx.orders.dispatch.profit.TradeFinishProfitBusService#handleTradeFinishRow}:
	 * {@code order_id} / {@code company_id} / {@code user_id} with the same {@code first(snake, camel)} resolution as the
	 * bus service.
	 */
	private static void assertTradeFinishProfitProducerKeys(
			Map<String, Object> publishRow, long expectedOrderId, long expectedCompanyId, long expectedUserId) {
		assertThat(publishRow).isNotNull();
		assertThat(longish(firstTradeFinishProfitKey(publishRow, "order_id", "orderId")))
				.isEqualTo(expectedOrderId);
		assertThat(longish(firstTradeFinishProfitKey(publishRow, "company_id", "companyId")))
				.isEqualTo(expectedCompanyId);
		assertThat(longish(firstTradeFinishProfitKey(publishRow, "user_id", "userId")))
				.isEqualTo(expectedUserId);
	}

	/**
	 * Keys on the published trade row for {@link cn.shopex.ecshopx.systemlink.dispatch.TradeFinishSendOmeDispatchListener}
	 * ({@code company_id}, {@code order_id}) and {@link cn.shopex.ecshopx.systemlink.service.ome.TradeFinishSendOmeBusService}
	 * ({@code user_id}, {@code trade_source_type}).
	 */
	private static void assertTradeFinishSendOmeProducerKeys(
			Map<String, Object> publishRow, long companyId, long orderId, long userId) {
		assertThat(publishRow).isNotNull();
		assertThat(publishRow)
				.as("TradeFinishSendOme mandatory producer keys")
				.containsKeys("company_id", "order_id", "user_id", "trade_source_type");
		assertThat(longish(firstTradeFinishSendOmeKey(publishRow, "company_id", "companyId")))
				.isEqualTo(companyId);
		assertThat(longish(firstTradeFinishSendOmeKey(publishRow, "order_id", "orderId")))
				.isEqualTo(orderId);
		assertThat(longish(firstTradeFinishSendOmeKey(publishRow, "user_id", "userId")))
				.isEqualTo(userId);
		Object sourceType = publishRow.get("trade_source_type");
		assertThat(sourceType).as("trade_source_type").isNotNull();
		assertThat(String.valueOf(sourceType).trim()).as("trade_source_type").isNotEmpty();
	}

	/**
	 * Keys {@link cn.shopex.ecshopx.orders.dispatch.printer.PrinterOrderBusService} gates on the published trade row
	 * (fen amounts; values may be {@link String} or {@link Number}).
	 */
	private static void assertTradeFinishPrinterOrderProducerKeys(
			Map<String, Object> published,
			long expectedCompanyId,
			String expectedOrderId,
			long expectedDistributorId,
			int expectedPayFeeFen,
			int expectedDiscountFeeFen) {
		assertThat(published).isNotNull();
		assertThat(longish(published.get("company_id"))).isEqualTo(expectedCompanyId);
		assertThat(published.get("order_id")).isEqualTo(expectedOrderId);
		assertThat(longish(published.get("distributor_id"))).isEqualTo(expectedDistributorId);
		assertThat(intish(published.get("pay_fee"))).isEqualTo(expectedPayFeeFen);
		assertThat(intish(published.get("discount_fee"))).isEqualTo(expectedDiscountFeeFen);
	}

	private static Integer intish(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static Object firstTradeFinishSendOmeKey(Map<String, Object> m, String snake, String camel) {
		Object x = m.get(snake);
		if (x != null) {
			return x;
		}
		return m.get(camel);
	}

	private static Object firstTradeFinishProfitKey(Map<String, Object> m, String snake, String camel) {
		Object x = m.get(snake);
		if (x != null) {
			return x;
		}
		return m.get(camel);
	}

	/**
	 * LinkMember producer keys on the trade row map; numeric values may be {@link String} or {@link Number} (see
	 * {@link #longish}).
	 */
	private static void assertTradeFinishLinkMemberProducerKeys(
			Map<String, Object> published,
			long expectedUserId,
			long expectedCompanyId,
			long expectedShopId,
			long expectedDistributorId) {
		assertThat(published).isNotNull();
		assertThat(longish(published.get("user_id"))).isEqualTo(expectedUserId);
		assertThat(longish(published.get("company_id"))).isEqualTo(expectedCompanyId);
		assertThat(longish(published.get("shop_id"))).isEqualTo(expectedShopId);
		assertThat(longish(published.get("distributor_id"))).isEqualTo(expectedDistributorId);
	}

	/**
	 * Producer keys on the published trade row for {@link TradePayFinishStatisticsBusService#recordPayFinishStatistics};
	 * fee resolution mirrors {@code payAmountFen} ({@code total_fee} overrides {@code pay_fee} when positive).
	 */
	private static void assertTradePayFinishStatisticsProducerKeys(
			Map<String, Object> publishRow,
			long expectedCompanyId,
			long expectedOrderId,
			String expectedTradeSourceType,
			String expectedTradeState,
			String expectedUserId,
			String expectedDistributorId,
			long expectedMerchantId,
			Long expectedTotalFeeFen,
			Long expectedPayFeeFen) {
		assertThat(publishRow).isNotNull();
		assertThat(expectedTotalFeeFen == null && expectedPayFeeFen == null)
				.as("at least one of expectedTotalFeeFen / expectedPayFeeFen must be non-null")
				.isFalse();

		Map<String, Object> expectedFeeProbe = new LinkedHashMap<>();
		if (expectedTotalFeeFen != null) {
			expectedFeeProbe.put("total_fee", expectedTotalFeeFen);
		}
		if (expectedPayFeeFen != null) {
			expectedFeeProbe.put("pay_fee", expectedPayFeeFen);
		}
		assertThat(expectedFeeProbe).as("fee probe must include at least one column").isNotEmpty();
		long expectedAmountFen = payAmountFenForAssert(expectedFeeProbe);
		assertThat(expectedAmountFen).isGreaterThanOrEqualTo(0L);

		assertThat(TRADE_PAY_FINISH_STATS_GATE.isEligibleTradeSourceType(publishRow.get("trade_source_type")))
				.as("trade_source_type must pass TradePayFinishStatisticsBusService eligibility")
				.isTrue();
		assertThat(statsStringVal(publishRow.get("trade_source_type"))).isEqualTo(expectedTradeSourceType);
		assertThat(longish(publishRow.get("company_id"))).isEqualTo(expectedCompanyId);
		assertThat(longish(publishRow.get("order_id"))).isEqualTo(expectedOrderId);
		assertThat(statsStringVal(publishRow.get("trade_state"))).isEqualTo(expectedTradeState);
		assertThat(statsStringVal(publishRow.get("user_id"))).isEqualTo(expectedUserId);
		assertThat(statsStringVal(publishRow.get("distributor_id"))).isEqualTo(expectedDistributorId);

		long merchantLoose = parseLongLoose(publishRow.get("merchant_id"));
		if (expectedMerchantId > 0L) {
			assertThat(merchantLoose).isEqualTo(expectedMerchantId);
		} else {
			assertThat(merchantLoose).as("merchant_id omitted or zero means merchant aggregate skipped").isLessThanOrEqualTo(0L);
		}

		long rowAmountFen = payAmountFenForAssert(publishRow);
		assertThat(rowAmountFen).isEqualTo(expectedAmountFen);
		assertThat(publishRow.containsKey("total_fee") || publishRow.containsKey("pay_fee"))
				.as("row must expose total_fee and/or pay_fee for payAmountFen")
				.isTrue();
	}

	private static String statsStringVal(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private static long parseLongLoose(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long payAmountFenForAssert(Map<String, Object> data) {
		Object total = data.get("total_fee");
		if (total != null) {
			long v = longFromAmount(total);
			if (v > 0L) {
				return v;
			}
		}
		Object payFee = data.get("pay_fee");
		return payFee == null ? 0L : longFromAmount(payFee);
	}

	private static long longFromAmount(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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
