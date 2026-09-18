package cn.shopex.ecshopx.orders.service.alipay;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishAfterCommitDispatchPublisher;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener;
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
class AlipayH5SyncReturnServiceOrderProcessLogPublishProbeTest {

	private static final String LISTENER_NAME =
			"listener:orders.listeners.TradeFinishPaymentSuccessOrderProcessLog";

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
	void success_alipayResult_nonDeposit_invokesOrderProcessLogPublishPortOnce_withAnalysisSection3Payload()
			throws Exception {
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener orderProcessLogListener =
				new OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener(orderProcessLogPublishPort);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				orderProcessLogListener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade dispatchFacade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher =
				new OrdersTradeFinishAfterCommitDispatchPublisher(
						dispatchFacade, OrdersDispatchEventNames.EVENT_TRADE_FINISH);

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
		notPay.setDiscountFee(5);
		notPay.setPayFee(100);

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
		success.setDiscountFee(5);

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

		h5.alipayResult(request);

		verify(orderProcessLogPublishPort, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.get("order_id") instanceof String
												&& Objects.equals("501", m.get("order_id"))
												&& m.get("company_id") instanceof String
												&& Objects.equals("9", m.get("company_id"))
												&& Objects.equals("system", String.valueOf(m.get("operator_type")))
												&& Boolean.TRUE.equals(m.get("is_show"))
												&& Objects.equals("订单支付", m.get("remarks"))
												&& Objects.equals("订单号：501，订单支付成功", m.get("detail"))
												&& !m.containsKey("operator_id")
												&& !m.containsKey("params")));
	}
}
