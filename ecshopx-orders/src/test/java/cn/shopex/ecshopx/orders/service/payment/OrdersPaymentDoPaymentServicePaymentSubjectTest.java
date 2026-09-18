package cn.shopex.ecshopx.orders.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.springframework.data.redis.core.StringRedisTemplate;
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
		doNothing().when(wxpayPaymentConfigValidationService).assertConfigComplete(anyLong(), anyLong());

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
		Map<String, Object> client = new LinkedHashMap<>();
		client.put("appId", "wx-test");
		when(ordersExternalPayParamBuildService.buildPayParamsOrInvoke(
						eq("wxpayh5"), eq(141L), eq(0L), any(), any(), any(Trade.class)))
				.thenReturn(client);

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
		ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
		verify(tradeMapper).insert(tradeCaptor.capture());
		assertThat(tradeCaptor.getValue().getDistributorId()).isEqualTo("285");
		assertThat(result.get("appId")).isEqualTo("wx-test");
	}
}
