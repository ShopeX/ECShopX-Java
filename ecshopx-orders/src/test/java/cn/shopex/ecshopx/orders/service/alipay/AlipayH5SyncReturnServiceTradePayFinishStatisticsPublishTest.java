package cn.shopex.ecshopx.orders.service.alipay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersTradePaymentCallbackService;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.service.AlipayAsyncNotifyVerificationService;
import cn.shopex.ecshopx.payment.service.AlipayH5SyncReturnService;
import cn.shopex.ecshopx.payment.service.AlipayOpenapiTradeQueryService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradeQueryParsedResponse;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AlipayH5SyncReturnServiceTradePayFinishStatisticsPublishTest {

	private static final String OUT_TRADE_NO = "T1";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
	}

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Mock
	private HashOperations<String, Object, Object> hashOps;

	@Mock
	private JushuitanTradeFinishDispatchPublisher jushuitanPublisher;

	@Mock
	private WdtErpTradeFinishDispatchPublisher wdtPublisher;

	@Mock
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher;

	@Mock
	private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;

	@Mock
	private AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService;

	@Mock
	private AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService;

	@Mock
	private AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Test
	void alipayResult_whenTradeSuccessAndNotDeposit_publishesOrdersTradeFinishRowWithTradePayFinishStatisticsKeys()
			throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		when(sharedStringRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(anyString())).thenReturn(1L);
		when(hashOps.get(anyString(), any())).thenReturn(null);

		when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		when(companysValueOps.get(anyString()))
				.thenReturn(
						"{\"app_id\":\"test_app\",\"ali_public_key\":\"MIIB\",\"private_key\":\"MIIE\"}");

		Trade notPay = new Trade();
		notPay.setTradeId(OUT_TRADE_NO);
		notPay.setTradeState("NOTPAY");
		notPay.setOrderId("501");
		notPay.setCompanyId("9");
		notPay.setDistributorId("12");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");
		notPay.setMerchantId(88L);

		Trade success = new Trade();
		success.setTradeId(OUT_TRADE_NO);
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("12");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setMerchantId(88L);
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);

		AtomicInteger selects = new AtomicInteger();
		when(tradeMapper.selectById(eq(OUT_TRADE_NO)))
				.thenAnswer(
						inv -> {
							int n = selects.incrementAndGet();
							if (n == 1) {
								return null;
							}
							if (n <= 3) {
								return notPay;
							}
							return success;
						});
		when(tradeMapper.updateById(any(Trade.class))).thenReturn(1);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		OrdersTradePaymentCallbackService callbackService =
				new OrdersTradePaymentCallbackService(
						tradeMapper,
						objectMapper,
						sharedStringRedisTemplate,
						jushuitanPublisher,
						ordersTradeFinishPublisher,
						wdtPublisher,
						org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class));

		AlipayNotifyOrderSidePortImpl orderSidePort =
				new AlipayNotifyOrderSidePortImpl(
						tradeMapper, distributionDistributorPeekMapper, callbackService);

		AlipayH5SyncReturnService h5 =
				new AlipayH5SyncReturnService(
						alipayPaymentConfigValidationService,
						alipayAsyncNotifyVerificationService,
						alipayOpenapiTradeQueryService,
						orderSidePort,
						alipayNotifyDepositSidePort,
						companysRedisTemplate,
						objectMapper);

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		when(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID)).thenReturn(1L);
		Map<String, String[]> pm = new HashMap<>();
		pm.put("out_trade_no", new String[] {OUT_TRADE_NO});
		pm.put("charset", new String[] {"UTF-8"});
		when(request.getParameterMap()).thenReturn(pm);

		doNothing()
				.when(alipayPaymentConfigValidationService)
				.assertAsyncNotifyConfigComplete(anyLong(), anyLong());
		doNothing().when(alipayAsyncNotifyVerificationService).verifySignedNotify(any(), any());

		AlipayTradeQueryParsedResponse parsed =
				new AlipayTradeQueryParsedResponse("10000", "TRADE_SUCCESS", "tx-1", "", null);
		when(alipayOpenapiTradeQueryService.queryTrade(eq(1L), eq(0L), eq(OUT_TRADE_NO)))
				.thenReturn(parsed);

		String status = h5.alipayResult(request);

		assertThat(status).isEqualTo("SUCCESS");

		// EVENT_TRADE_FINISH row must surface order_id, company_id, user_id (sync trade-finish profit consume).
		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& m.containsKey("trade_source_type")
												&& m.containsKey("trade_state")
												&& (m.containsKey("total_fee") || m.containsKey("pay_fee"))
												&& m.containsKey("user_id")
												&& m.containsKey("distributor_id")
												&& m.containsKey("merchant_id")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))
												&& Objects.equals(
														"SUCCESS", String.valueOf(m.get("trade_state")))
												&& Objects.equals("3", String.valueOf(m.get("user_id")))));
	}
}
