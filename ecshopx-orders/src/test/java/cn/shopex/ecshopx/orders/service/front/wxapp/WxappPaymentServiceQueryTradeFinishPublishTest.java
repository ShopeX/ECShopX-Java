package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import cn.shopex.ecshopx.deposit.service.DepositTradeWxappPaymentService;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.payment.OrderPrescriptionPaymentGuardService;
import cn.shopex.ecshopx.orders.service.payment.OrdersExternalPayParamBuildService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentChannelQueryService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentTradeQueryService;
import cn.shopex.ecshopx.orders.service.payment.SupplierSubOrdersForPaymentLoadService;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Mockito coverage for {@link WxappPaymentService#query(String)} driving
 * {@link OrdersPaymentTradeQueryService#resolve} when the channel reports {@code SUCCESS}, leading to
 * {@link OrdersPaymentDoPaymentService#finalizeTradeSuccessAfterChannelQuery} and a single
 * {@link OrdersTradeFinishDispatchPublisher#publish(Map)} with SendOme-aligned row keys ({@code company_id},
 * {@code order_id}, {@code user_id}).
 */
@ExtendWith(MockitoExtension.class)
class WxappPaymentServiceQueryTradeFinishPublishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private DepositTradeWxappPaymentService depositTradeWxappPaymentService;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;

	@Mock
	private AdminNormalOrderDetailService adminNormalOrderDetailService;

	@Mock
	private OrderPrescriptionPaymentGuardService orderPrescriptionPaymentGuardService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;

	@Mock
	private MembersAssociationsMapper membersAssociationsMapper;

	@Mock
	private WechatUsersMapper wechatUsersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private NormalOrderNumericIdService normalOrderNumericIdService;

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

	@Mock(name = "sharedStringRedisTemplate")
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	@Mock
	private OrdersPaymentChannelQueryService ordersPaymentChannelQueryService;

	@Mock
	private PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	@Captor
	private ArgumentCaptor<Map<String, Object>> tradeRowCaptor;

	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort =
			mock(ObjectProvider.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort =
			mock(ObjectProvider.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;
	private OrdersPaymentTradeQueryService ordersPaymentTradeQueryService;
	private WxappPaymentService wxappPaymentService;

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

		ordersPaymentDoPaymentService =
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

		ordersPaymentTradeQueryService =
				new OrdersPaymentTradeQueryService(
						tradeMapper, ordersPaymentChannelQueryService, ordersPaymentDoPaymentService);

		wxappPaymentService =
				new WxappPaymentService(
						depositTradeWxappPaymentService,
						orderAssociationsMapper,
						orderAssociationEffectiveTypeService,
						adminNormalOrderDetailService,
						orderPrescriptionPaymentGuardService,
						normalOrdersMapper,
						distributionDistributorPeekMapper,
						ordersPaymentDoPaymentService,
						ordersPaymentTradeQueryService,
						membersAssociationsMapper,
						wechatUsersMapper);
	}

	@Test
	@DisplayName(
			"query: channel SUCCESS → finalize → publish once with company_id, order_id, user_id (SendOme row keys)")
	void query_whenChannelReturnsSuccess_finalizesAndPublishesTradeFinishKeys() {
		String tradeId = "wx-trade-query-1";
		long companyId = 701L;
		String orderId = "ORD-Q1";
		long userId = 4201L;
		String tradeSourceType = "normal";

		Trade notPay = tradeNotPay(tradeId, companyId, orderId, userId, tradeSourceType);
		Trade success = tradeSuccessCopy(notPay);

		AtomicInteger selectSeq = new AtomicInteger();
		when(tradeMapper.selectById(tradeId))
				.thenAnswer(
						invocation -> {
							int n = selectSeq.incrementAndGet();
							if (n <= 2) {
								return notPay;
							}
							return success;
						});
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> channelOk = new LinkedHashMap<>();
		channelOk.put("status", "SUCCESS");
		channelOk.put("msg", "支付成功");
		channelOk.put("transaction_id", "wx-txn-900");
		when(ordersPaymentChannelQueryService.query(any(Trade.class), any())).thenReturn(channelOk);

		Map<String, Object> out = wxappPaymentService.query(tradeId);

		assertThat(out.get("status")).isEqualTo("SUCCESS");
		verify(ordersTradeFinishDispatchPublisher, times(1)).publish(tradeRowCaptor.capture());
		Map<String, Object> published = tradeRowCaptor.getValue();
		assertThat(published.get("company_id"))
				.satisfies(
						v -> assertThat(String.valueOf(v)).isEqualTo(String.valueOf(companyId)));
		assertThat(published.get("order_id")).isEqualTo(orderId);
		assertThat(published.get("user_id"))
				.satisfies(v -> assertThat(String.valueOf(v)).isEqualTo(String.valueOf(userId)));
		assertThat(published.get("trade_source_type")).isEqualTo(tradeSourceType);
	}

	@Test
	@DisplayName("query: trade already SUCCESS → early return; no channel query and no publish")
	void query_whenTradeAlreadySuccess_doesNotFinalizeOrPublish() {
		String tradeId = "wx-trade-already-paid";
		Trade done = tradeNotPay(tradeId, 1L, "O", 1L, "normal");
		done.setTradeState("SUCCESS");
		when(tradeMapper.selectById(tradeId)).thenReturn(done);

		Map<String, Object> out = wxappPaymentService.query(tradeId);

		assertThat(out.get("status")).isEqualTo("SUCCESS");
		verify(ordersPaymentChannelQueryService, never()).query(any(), any());
		verify(ordersTradeFinishDispatchPublisher, never()).publish(any());
	}

	private static Trade tradeNotPay(String tradeId, long companyId, String orderId, long userId, String sourceType) {
		Trade t = new Trade();
		t.setTradeId(tradeId);
		t.setCompanyId(String.valueOf(companyId));
		t.setOrderId(orderId);
		t.setUserId(String.valueOf(userId));
		t.setTradeSourceType(sourceType);
		t.setTradeState("NOTPAY");
		t.setPayType("wxpay");
		t.setShopId("0");
		t.setDistributorId("0");
		t.setPayFee(100);
		t.setTotalFee(100);
		t.setTimeStart(String.valueOf((int) (System.currentTimeMillis() / 1000L)));
		return t;
	}

	private static Trade tradeSuccessCopy(Trade base) {
		Trade t = new Trade();
		t.setTradeId(base.getTradeId());
		t.setCompanyId(base.getCompanyId());
		t.setOrderId(base.getOrderId());
		t.setUserId(base.getUserId());
		t.setTradeSourceType(base.getTradeSourceType());
		t.setTradeState("SUCCESS");
		t.setPayType(base.getPayType());
		t.setShopId(base.getShopId());
		t.setDistributorId(base.getDistributorId());
		t.setPayFee(base.getPayFee());
		t.setTotalFee(base.getTotalFee());
		t.setTimeStart(base.getTimeStart());
		t.setTradeNo("0508-1");
		t.setTimeExpire(String.valueOf((int) (System.currentTimeMillis() / 1000L)));
		return t;
	}
}
