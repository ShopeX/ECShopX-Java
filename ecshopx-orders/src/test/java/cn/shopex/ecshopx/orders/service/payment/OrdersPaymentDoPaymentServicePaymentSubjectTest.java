package cn.shopex.ecshopx.orders.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrdersPaymentDoPaymentServicePaymentSubjectTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock private TradeMapper tradeMapper;
	@Mock private NormalOrderNumericIdService normalOrderNumericIdService;
	@Mock private DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	@Mock private SensitiveFieldEncryptor sensitiveFieldEncryptor;
	@Mock private CompanyDefaultCurrencyService companyDefaultCurrencyService;
	@Mock private TransactionTemplate transactionTemplate;
	@Mock private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;
	@Mock private PointMemberAddPointService pointMemberAddPointService;
	@Mock private DepositTradeConsumeService depositTradeConsumeService;
	@Mock private WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;
	@Mock private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	@Mock private SupplierSubOrdersForPaymentLoadService supplierSubOrdersForPaymentLoadService;
	@Mock private OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;
	@Mock private MembercardBsPayPaymentSdkService membercardBsPayPaymentSdkService;
	@Mock private MembercardAdapayPaymentSdkService membercardAdapayPaymentSdkService;
	@Mock(name = "sharedStringRedisTemplate") private StringRedisTemplate sharedStringRedisTemplate;
	@Mock(name = "companysRedisTemplate") private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> sharedValueOps;
	@Mock private HashOperations<String, Object, Object> sharedHashOps;
	@Mock private PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort = mock(ObjectProvider.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort =
			mock(ObjectProvider.class);

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
		lenient().when(sensitiveFieldEncryptor.encrypt(any())).thenReturn("enc-phone");
		CurrencyExchangeRate noFx = new CurrencyExchangeRate();
		noFx.setRate(null);
		lenient().when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(noFx);
		lenient().doNothing().when(wxpayPaymentConfigValidationService).assertConfigComplete(anyLong(), anyLong());
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedValueOps);
		lenient().when(sharedStringRedisTemplate.opsForHash()).thenReturn(sharedHashOps);
		lenient().when(sharedValueOps.increment(any())).thenReturn(1L);
		lenient().when(sharedHashOps.get(any(), any())).thenReturn(null);
		lenient().doNothing().when(sharedHashOps).put(any(), any(), any());
		lenient().when(sharedStringRedisTemplate.expire(any(), any(java.time.Duration.class))).thenReturn(true);

		service =
				new OrdersPaymentDoPaymentService(
						tradeMapper,
						normalOrderNumericIdService,
						distributionDistributorPeekMapper,
						sensitiveFieldEncryptor,
						companyDefaultCurrencyService,
						transactionTemplate,
						ordersTradeFinishDispatchPublisher,
						new ObjectMapper(),
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
	@DisplayName("payment_subject=平台时，支付配置校验应使用 distributor_id=0")
	void doPayment_whenPlatformPaymentSubject_usesPlatformSetting() {
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(141L, 285L)).thenReturn(0L);
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.update(any(), any())).thenReturn(1);
		when(tradeMapper.insert(any(Trade.class))).thenReturn(1);

		AtomicInteger txDepth = new AtomicInteger();
		AtomicBoolean channelCalledOutsideTx = new AtomicBoolean(false);
		lenient()
				.doAnswer(
						invocation -> {
							txDepth.incrementAndGet();
							try {
								TransactionCallback<?> callback = invocation.getArgument(0);
								return callback.doInTransaction(mock(TransactionStatus.class));
							} finally {
								txDepth.decrementAndGet();
							}
						})
				.when(transactionTemplate)
				.execute(any());

		Map<String, Object> client = new LinkedHashMap<>();
		client.put("appId", "wx-test");
		when(ordersExternalPayParamBuildService.buildPayParamsOrInvoke(
						eq("wxpayh5"), eq(141L), eq(0L), any(), any(), any(Trade.class)))
				.thenAnswer(
						inv -> {
							channelCalledOutsideTx.set(txDepth.get() == 0);
							return client;
						});

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", 141L);
		data.put("order_id", "5306709000061271");
		data.put("order_id_numeric", 5306709000061271L);
		data.put("pay_type", "wxpayh5");
		data.put("distributor_id", 285L);
		data.put("pay_fee", 400);
		data.put("trade_source_type", "normal");
		data.put("user_id", 1271L);
		data.put("open_id", "openid");
		data.put("body", "测试");

		Map<String, Object> result = service.doPayment(null, data, false);

		verify(wxpayPaymentConfigValidationService).assertConfigComplete(141L, 0L);
		verify(ordersExternalPayParamBuildService)
				.buildPayParamsOrInvoke(eq("wxpayh5"), eq(141L), eq(0L), any(), any(), any(Trade.class));
		verify(transactionTemplate, times(2)).execute(any());
		assertThat(channelCalledOutsideTx.get()).isTrue();
		ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
		verify(tradeMapper).insert(tradeCaptor.capture());
		assertThat(tradeCaptor.getValue().getDistributorId()).isEqualTo("285");
		assertThat(result.get("appId")).isEqualTo("wx-test");
	}

	@Test
	@DisplayName("本地 point 支付：单次事务，不调用外部渠道")
	void doPayment_whenPointPay_usesSingleTransactionAndNeverCallsExternalChannel() {
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(101L, 0L)).thenReturn(0L);
		when(tradeMapper.selectOne(any())).thenReturn(null);
		when(tradeMapper.insert(any(Trade.class)))
				.thenAnswer(
						invocation -> {
							Trade row = invocation.getArgument(0, Trade.class);
							when(tradeMapper.selectById(row.getTradeId()))
									.thenAnswer(
											sel -> {
												Trade loaded = new Trade();
												loaded.setTradeId(row.getTradeId());
												loaded.setOrderId(row.getOrderId());
												loaded.setCompanyId(row.getCompanyId());
												loaded.setShopId(row.getShopId());
												loaded.setDistributorId(row.getDistributorId());
												loaded.setUserId(row.getUserId());
												loaded.setPayFee(row.getPayFee());
												loaded.setPayType(row.getPayType());
												loaded.setTradeSourceType(row.getTradeSourceType());
												loaded.setTradeState("NOTPAY");
												loaded.setTimeStart(row.getTimeStart());
												return loaded;
											})
									.thenAnswer(
											sel -> {
												Trade loaded = new Trade();
												loaded.setTradeId(row.getTradeId());
												loaded.setOrderId(row.getOrderId());
												loaded.setCompanyId(row.getCompanyId());
												loaded.setShopId(row.getShopId());
												loaded.setDistributorId(row.getDistributorId());
												loaded.setUserId(row.getUserId());
												loaded.setPayFee(row.getPayFee());
												loaded.setPayType(row.getPayType());
												loaded.setTradeSourceType(row.getTradeSourceType());
												loaded.setTradeState("SUCCESS");
												loaded.setTradeNo("stub-no");
												loaded.setTimeStart(row.getTimeStart());
												return loaded;
											});
							return 1;
						});
		when(tradeMapper.update(any(), any())).thenReturn(1);
		doNothing()
				.when(pointMemberAddPointService)
				.addPointForManualAdjustment(
						anyLong(),
						anyLong(),
						org.mockito.ArgumentMatchers.anyInt(),
						org.mockito.ArgumentMatchers.anyBoolean(),
						org.mockito.ArgumentMatchers.anyString());

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", 101L);
		data.put("order_id", "O-POINT");
		data.put("order_id_numeric", 1L);
		data.put("pay_type", "point");
		data.put("distributor_id", 0L);
		data.put("pay_fee", 50);
		data.put("point_amount", 50);
		data.put("user_id", 7L);
		data.put("trade_source_type", "normal");

		Map<String, Object> result = service.doPayment(Map.of(), data, false);

		verify(transactionTemplate, times(1)).execute(any());
		verifyNoInteractions(
				ordersExternalPayParamBuildService,
				membercardBsPayPaymentSdkService,
				membercardAdapayPaymentSdkService);
		assertThat(result.get("pay_status")).isEqualTo(true);
	}
}
