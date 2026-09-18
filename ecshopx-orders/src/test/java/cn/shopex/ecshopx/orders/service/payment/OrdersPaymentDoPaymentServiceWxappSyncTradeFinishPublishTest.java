package cn.shopex.ecshopx.orders.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.order.port.VipGradeMembercardTradePaidPort;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.deposit.service.DepositTradeConsumeService;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.WxpayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.membercard.MembercardAdapayPaymentSdkService;
import cn.shopex.ecshopx.payment.service.membercard.MembercardBsPayPaymentSdkService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Covers the wxapp (WeChat mini-program) synchronous payment SUCCESS path: when {@link OrdersPaymentDoPaymentService}
 * finishes a successful payment in-process, it publishes the {@code trade_finish} payload through
 * {@link OrdersTradeFinishDispatchPublisher} exactly once on the calling thread.
 *
 * <p>Each captured row must carry {@code company_id}, {@code order_id}, and {@code pay_type} (brokerage producer gate
 * keys for {@code listener:orders.listeners.TradeFinishCountBrokerage} via {@link
 * cn.shopex.ecshopx.orders.service.brokerage.TradeFinishCountBrokerageBusService}), plus the same {@code company_id} /
 * {@code order_id} pair used by {@code listener:orders.listeners.UpdateItemSalesListener} ({@link
 * cn.shopex.ecshopx.orders.service.sales.UpdateItemSalesBusService}) and SMS notify. When {@code
 * trade_source_type=normal_groups}, the published row must carry that value for {@code
 * listener:orders.listeners.UpdateGroupsActivityOrder} gating ({@code isNormalGroupsTrade}).
 *
 * <p>The same publish capture must expose {@code user_id}, {@code company_id}, {@code shop_id}, and {@code
 * distributor_id} for {@code listener:orders.listeners.TradeFinishLinkMember} ({@link
 * cn.shopex.ecshopx.orders.service.memberlink.TradeFinishLinkMemberBusService}), matching the snake_case row shape
 * asserted in bootstrap {@code TradeFinishLinkMemberEventSyncDispatchFlowTest} (numeric fields as String or Number).
 *
 * <p>{@code company_id}, {@code order_id}, {@code distributor_id}, {@code pay_fee}, and {@code discount_fee} gate
 * {@code listener:orders.listeners.PrinterOrder} ({@link cn.shopex.ecshopx.orders.dispatch.printer.PrinterOrderBusService})
 * on the same row.
 *
 * <p>{@code company_id}, {@code order_id}, {@code user_id}, {@code trade_source_type}, and {@code trade_state=SUCCESS}
 * align with {@code listener:orders.listeners.TradeFinishFapiao} ({@link
 * cn.shopex.ecshopx.orders.service.invoice.TradeFinishFapiaoBusService}) entry gating on the same row.
 *
 * <p>For {@code pay_type=wxpay}, the same publish row is also gated by {@code
 * listener:orders.listeners.TradeFinishCustomDeclareOrder} ({@link
 * cn.shopex.ecshopx.orders.service.customs.TradeFinishCustomDeclareOrderBusService}), which additionally requires
 * {@code pay_type} and a non-empty {@code trade_id}.
 *
 * <p>{@code listener:orders.listeners.TradeFinishWorkWechatNotify} is registered in bootstrap with {@code @Order(96)}
 * and delegates to {@link cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWorkWechatNotifyService}, which
 * treats {@code company_id} and non-blank {@code order_id} as the hard gate before downstream work (same payload
 * contract as {@code TradeFinishWorkWechatNotifyEventSyncDispatchFlowTest}).
 */
@ExtendWith(MockitoExtension.class)
class OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private NormalOrderNumericIdService normalOrderNumericIdService;

	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private CompanyDefaultCurrencyService companyDefaultCurrencyService;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@Mock
	private DepositTradeConsumeService depositTradeConsumeService;

	@Mock
	private WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;

	@Mock
	private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;

	@Mock
	private SupplierSubOrdersForPaymentLoadService supplierSubOrdersForPaymentLoadService;

	@Mock
	private OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;

	@Mock
	private MembercardBsPayPaymentSdkService membercardBsPayPaymentSdkService;

	@Mock
	private MembercardAdapayPaymentSdkService membercardAdapayPaymentSdkService;

	@Mock
	private PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	@Mock(name = "sharedStringRedisTemplate")
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	@Captor
	private ArgumentCaptor<Map<String, Object>> tradeRowCaptor;

	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort =
			mock(ObjectProvider.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort =
			mock(ObjectProvider.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OrdersPaymentDoPaymentService service;

	@BeforeEach
	void setUp() {
		lenient().when(hfpayPayPort.getIfAvailable()).thenReturn(null);
		lenient().when(vipGradeMembercardTradePaidPort.getIfAvailable()).thenReturn(null);

		lenient()
				.doAnswer(
						invocation -> {
							TransactionCallback<?> callback = invocation.getArgument(0);
							return callback.doInTransaction(mock(TransactionStatus.class));
						})
				.when(transactionTemplate)
				.execute(any());

		lenient().when(normalOrderNumericIdService.generate(anyLong())).thenReturn(90001L);

		lenient().when(sensitiveFieldEncryptor.encrypt(anyString())).thenReturn("enc-phone");

		CurrencyExchangeRate noFx = new CurrencyExchangeRate();
		noFx.setRate(null);
		lenient().when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(noFx);

		lenient().doNothing().when(wxpayPaymentConfigValidationService).assertConfigComplete(anyLong(), anyLong());
		lenient().doNothing().when(alipayPaymentConfigValidationService).assertConfigComplete(anyLong(), anyLong());
		lenient().doNothing().when(ordersTradeFinishDispatchPublisher).publish(any());
		lenient()
				.when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(invocation -> invocation.getArgument(1, Long.class));

		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedValueOps);
		lenient().when(sharedStringRedisTemplate.opsForHash()).thenReturn(sharedHashOps);
		lenient().when(sharedValueOps.increment(anyString())).thenReturn(1L);
		lenient().when(sharedHashOps.get(anyString(), any())).thenReturn(null);
		lenient().doNothing().when(sharedHashOps).put(anyString(), any(), any());
		lenient().when(sharedStringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

		service =
				new OrdersPaymentDoPaymentService(
						tradeMapper,
						normalOrderNumericIdService,
						distributionDistributorPeekMapper,
						sensitiveFieldEncryptor,
						companyDefaultCurrencyService,
						transactionTemplate,
						ordersTradeFinishDispatchPublisher,
						objectMapper,
						pointMemberAddPointService,
						depositTradeConsumeService,
						wxpayPaymentConfigValidationService,
						alipayPaymentConfigValidationService,
						supplierSubOrdersForPaymentLoadService,
						ordersExternalPayParamBuildService,
						membercardBsPayPaymentSdkService,
						membercardAdapayPaymentSdkService,
						sharedStringRedisTemplate,
						companysRedisTemplate,
						hfpayPayPort,
						vipGradeMembercardTradePaidPort,
						paymentSubjectDistributorIdPort);
	}

	@Test
	@DisplayName(
			"Wxapp wxpay zero-fee SUCCESS: publish once; row carries UpdateItemSales gate keys company_id + order_id")
	void doPayment_whenZeroPayFeeWxpay_successPath_publishesTradeFinishOnce() {
		stubInsertAndLocalPayFinalize();

		long companyId = 101L;
		String orderLiteral = "O1";
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "wxpay", 0);

		service.doPayment(null, data, false);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishBrokerageProducerKeys(published, companyId, orderLiteral, "localPay");
		assertTradeFinishLinkMemberProducerKeys(published, 1L, companyId, 0L, 0L);
		assertTradeFinishPrinterOrderProducerKeys(published, companyId, orderLiteral, 0L, 0, 0);
		assertTradeFinishFapiaoProducerKeys(published, companyId, orderLiteral, 1L, "normal");
		assertTradeFinishWorkWechatNotifyGateKeys(published, companyId, orderLiteral);
		assertThat(published.get("trade_source_type")).isEqualTo("normal");
		assertThat(published.get("trade_state")).isEqualTo("SUCCESS");
		assertThat(published.get("pay_fee")).isEqualTo(0);
		assertThat(published.get("time_start")).isNotNull();
		assertThat(published.get("mobile")).isEqualTo("enc-phone");
	}

	@Test
	@DisplayName("零元但有积分抵扣：交易 pay_type 落 point，而非 localPay")
	void doPayment_whenZeroPayFeeWithPointAmount_usesPointPayType() {
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId()))
									.thenReturn(cloneTradeNotPay(row))
									.thenReturn(cloneTradeSuccess(row));
							return 1;
						});
		when(tradeMapper.update(any(), any())).thenReturn(1);
		lenient()
				.doNothing()
				.when(pointMemberAddPointService)
				.addPointForManualAdjustment(anyLong(), anyLong(), anyInt(), anyBoolean(), anyString());

		long companyId = 101L;
		String orderLiteral = "O-POINT-0";
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "offline_pay", 0);
		data.put("point_amount", 16590);
		data.put("user_id", 7L);

		service.doPayment(Map.of(), data, false);

		verify(pointMemberAddPointService, times(1))
				.addPointForManualAdjustment(7L, companyId, 16590, false, "订单" + orderLiteral + "支付扣减积分");
		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertThat(published.get("pay_type")).isEqualTo("point");
	}

	@Test
	@DisplayName(
			"Wxpay pay_fee>0 SUCCESS: publish captures company_id + order_id for UpdateItemSales and notify payload shape")
	void doPayment_whenWxpayPositiveFeeAndSyncImmediatePay_success_publishesRowWithPayShapeKeys() {
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId()))
									.thenReturn(cloneTradeNotPay(row))
									.thenReturn(cloneTradeSuccess(row));
							return 1;
						});
		when(tradeMapper.update(any(), any())).thenReturn(1);
		when(
						ordersExternalPayParamBuildService.buildPayParamsOrInvoke(
								anyString(), anyLong(), anyLong(), any(), any(), any(Trade.class)))
				.thenAnswer(
						inv -> {
							Map<String, Object> client = new LinkedHashMap<>();
							client.put("pay_status", Boolean.TRUE);
							return client;
						});

		long companyId = 103L;
		String orderLiteral = "O3";
		long shopId = 55L;
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "wxpay", 199);
		data.put("shop_id", shopId);

		service.doPayment(Map.of(), data, false);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishBrokerageProducerKeys(published, companyId, orderLiteral, "wxpay");
		assertTradeFinishLinkMemberProducerKeys(published, 1L, companyId, shopId, 0L);
		assertTradeFinishPrinterOrderProducerKeys(published, companyId, orderLiteral, 0L, 199, 0);
		assertTradeFinishFapiaoProducerKeys(published, companyId, orderLiteral, 1L, "normal");
		assertTradeFinishCustomDeclareOrderWxpayProducerKeys(published, companyId, orderLiteral, 1L, "normal");
		assertTradeFinishWorkWechatNotifyGateKeys(published, companyId, orderLiteral);
		assertThat(published.get("trade_source_type")).isEqualTo("normal");
		assertThat(published.get("pay_fee")).isEqualTo(199);
		assertThat(published.get("shop_id")).isEqualTo(String.valueOf(shopId));
		assertThat(published.get("time_start")).isNotNull();
	}

	@Test
	@DisplayName(
			"Wxapp wxpay zero-fee SUCCESS with trade_source_type=normal_groups: publish once; payload carries normal_groups for UpdateGroupsActivityOrder")
	void doPayment_whenZeroPayFeeWxpay_normalGroups_successPath_publishesTradeFinishWithGroupsSourceType() {
		stubInsertAndLocalPayFinalize();

		long companyId = 201L;
		String orderLiteral = "OG1";
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "wxpay", 0);
		data.put("trade_source_type", "normal_groups");

		service.doPayment(null, data, false);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishBrokerageProducerKeys(published, companyId, orderLiteral, "localPay");
		assertTradeFinishLinkMemberProducerKeys(published, 1L, companyId, 0L, 0L);
		assertTradeFinishFapiaoProducerKeys(published, companyId, orderLiteral, 1L, "normal_groups");
		assertTradeFinishWorkWechatNotifyGateKeys(published, companyId, orderLiteral);
		assertThat(published.get("trade_source_type")).isEqualTo("normal_groups");
		assertThat(published.get("trade_state")).isEqualTo("SUCCESS");
	}

	@Test
	@DisplayName(
			"Point pay SUCCESS: publish once; company_id + order_id aligned (UpdateItemSales does not skip on pay_type=point)")
	void doPayment_whenPointPay_publishesTradeFinishOnce() {
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId()))
									.thenReturn(cloneTradeNotPay(row))
									.thenReturn(cloneTradeSuccess(row));
							return 1;
						});

		when(tradeMapper.update(any(), any())).thenReturn(1);
		lenient().doNothing().when(pointMemberAddPointService).addPointForManualAdjustment(
				anyLong(), anyLong(), anyInt(), anyBoolean(), anyString());

		long companyId = 102L;
		String orderLiteral = "O2";
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "point", 50);
		data.put("user_id", 7L);
		data.put("point_amount", 50);

		service.doPayment(Map.of(), data, false);

		verify(pointMemberAddPointService, times(1))
				.addPointForManualAdjustment(7L, companyId, 50, false, "订单" + orderLiteral + "支付扣减积分");
		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishBrokerageProducerKeys(published, companyId, orderLiteral, "point");
		assertTradeFinishLinkMemberProducerKeys(published, 7L, companyId, 0L, 0L);
		assertTradeFinishFapiaoProducerKeys(published, companyId, orderLiteral, 7L, "normal");
		assertTradeFinishWorkWechatNotifyGateKeys(published, companyId, orderLiteral);
		assertThat(published.get("trade_source_type")).isEqualTo("normal");
	}

	@Test
	@DisplayName(
			"Wxpay pay_fee>0 SUCCESS with distributor_id: publish row includes LinkMember keys mirroring trade→map")
	void doPayment_whenWxpayPositiveFeeWithDistributor_success_publishesRowWithLinkMemberKeys() {
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId()))
									.thenReturn(cloneTradeNotPay(row))
									.thenReturn(cloneTradeSuccess(row));
							return 1;
						});
		when(tradeMapper.update(any(), any())).thenReturn(1);
		when(
						ordersExternalPayParamBuildService.buildPayParamsOrInvoke(
								anyString(), anyLong(), anyLong(), any(), any(), any(Trade.class)))
				.thenAnswer(
						inv -> {
							Map<String, Object> client = new LinkedHashMap<>();
							client.put("pay_status", Boolean.TRUE);
							return client;
						});

		long companyId = 104L;
		String orderLiteral = "O4";
		long distributorId = 12L;
		Map<String, Object> data = minimalPaymentData(companyId, orderLiteral, "wxpay", 50);
		data.put("distributor_id", distributorId);
		data.put("shop_id", 0L);

		service.doPayment(Map.of(), data, false);

		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertTradeFinishBrokerageProducerKeys(published, companyId, orderLiteral, "wxpay");
		assertTradeFinishLinkMemberProducerKeys(published, 1L, companyId, 0L, distributorId);
		assertTradeFinishPrinterOrderProducerKeys(published, companyId, orderLiteral, distributorId, 50, 0);
		assertTradeFinishFapiaoProducerKeys(published, companyId, orderLiteral, 1L, "normal");
		assertTradeFinishCustomDeclareOrderWxpayProducerKeys(published, companyId, orderLiteral, 1L, "normal");
		assertTradeFinishWorkWechatNotifyGateKeys(published, companyId, orderLiteral);
	}

	/**
	 * Producer keys expected on every Wxapp sync SUCCESS publish capture — same snake_case fields {@link
	 * cn.shopex.ecshopx.orders.service.brokerage.TradeFinishCountBrokerageBusService} gates on after {@code
	 * listener:orders.listeners.TradeFinishCountBrokerage} receives the row.
	 */
	private static void assertTradeFinishBrokerageProducerKeys(
			Map<String, Object> published, long companyId, String orderId, String payType) {
		assertThat(published.get("company_id")).isEqualTo(String.valueOf(companyId));
		assertThat(published.get("order_id")).isEqualTo(orderId);
		assertThat(published.get("pay_type")).isEqualTo(payType);
	}

	/**
	 * Keys consumed from the same {@code EVENT_TRADE_FINISH} row as {@code TradeFinishLinkMember}; values align with
	 * {@link Trade} row-to-map shape; numeric fields may be {@link String} or {@link Number} (use local {@link #longish}).
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

	/**
	 * Minimal producer gate keys on the trade row for {@code TradeFinishFapiao} (same field presence and equality style
	 * as {@link cn.shopex.ecshopx.orders.service.payment.OrdersTradePaymentCallbackServiceTradePayFinishStatisticsPublishTest#successStatus_ordersTradeFinishPublish_includesTradeFinishFapiaoGateKeys}).
	 */
	private static void assertTradeFinishFapiaoProducerKeys(
			Map<String, Object> published,
			long expectedCompanyId,
			String expectedOrderId,
			long expectedUserId,
			String expectedTradeSourceType) {
		assertThat(published).isNotNull();
		assertThat(published).containsKeys("company_id", "order_id", "user_id", "trade_source_type", "trade_state");
		assertThat(String.valueOf(published.get("trade_state"))).isEqualTo("SUCCESS");
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo(expectedOrderId);
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo(String.valueOf(expectedCompanyId));
		assertThat(longish(published.get("user_id"))).isEqualTo(expectedUserId);
		assertThat(String.valueOf(published.get("trade_source_type"))).isEqualTo(expectedTradeSourceType);
	}

	/**
	 * Minimal producer gate keys for {@code TradeFinishCustomDeclareOrder} wxpay path ({@link
	 * cn.shopex.ecshopx.orders.dispatch.TradeFinishCustomDeclareOrderDispatchListener} and {@link
	 * cn.shopex.ecshopx.orders.service.customs.TradeFinishCustomDeclareOrderBusService}), including non-empty {@code
	 * trade_id} for row handling.
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

	/**
	 * Gate keys for {@code listener:orders.listeners.TradeFinishWorkWechatNotify} / {@link
	 * cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishWorkWechatDispatchListener}: non-blank {@code company_id} and
	 * {@code order_id} after string conversion (aligned with {@link
	 * cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWorkWechatNotifyService#dispatchTradeFinishWorkWechatDeliveryWaitJobs}).
	 */
	private static void assertTradeFinishWorkWechatNotifyGateKeys(
			Map<String, Object> published, long companyId, String orderIdLiteral) {
		assertThat(published).isNotNull();
		assertThat(published.get("company_id")).isEqualTo(String.valueOf(companyId));
		assertThat(published.get("order_id")).isEqualTo(orderIdLiteral);
		assertThat(String.valueOf(published.get("order_id")).trim()).isNotEmpty();
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

	private static Integer intish(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private void stubInsertAndLocalPayFinalize() {
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId())).thenReturn(row);
							when(tradeMapper.update(any(), any()))
									.thenAnswer(
											u -> {
												row.setTradeState("SUCCESS");
												row.setPayType("localPay");
												row.setTradeNo("stub-no");
												return 1;
											});
							return 1;
						});
	}

	private static Map<String, Object> minimalPaymentData(
			long companyId, String orderId, String payType, int payFee) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderId);
		data.put("order_id_numeric", 2001L);
		data.put("distributor_id", 0L);
		data.put("pay_type", payType);
		data.put("pay_fee", payFee);
		data.put("user_id", 1L);
		data.put("shop_id", 0L);
		data.put("body", "test body");
		return data;
	}

	private static Trade cloneTradeNotPay(Trade source) {
		Trade t = shallowCopyTrade(source);
		t.setTradeState("NOTPAY");
		return t;
	}

	private static Trade cloneTradeSuccess(Trade source) {
		Trade t = shallowCopyTrade(source);
		t.setTradeState("SUCCESS");
		t.setTradeNo("stub-no");
		return t;
	}

	private static Trade shallowCopyTrade(Trade source) {
		Trade t = new Trade();
		t.setTradeId(source.getTradeId());
		t.setOrderId(source.getOrderId());
		t.setCompanyId(source.getCompanyId());
		t.setShopId(source.getShopId());
		t.setDistributorId(source.getDistributorId());
		t.setDealerId(source.getDealerId());
		t.setTradeSourceType(source.getTradeSourceType());
		t.setUserId(source.getUserId());
		t.setPayFee(source.getPayFee());
		t.setTotalFee(source.getTotalFee());
		t.setPayType(source.getPayType());
		t.setPayChannel(source.getPayChannel());
		t.setTimeStart(source.getTimeStart());
		return t;
	}
}
