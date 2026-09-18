package cn.shopex.ecshopx.payment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AlipayNotifyFacadeDepositSmsDispatchTest {

	@Mock
	private AlipayNotifyPaymentContextLoader loader;

	@Mock
	private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;

	@Mock
	private AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService;

	@Mock
	private AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;

	@Mock
	private AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private AlipayNotifyFacade facade;

	@BeforeEach
	void setUp() {
		facade = new AlipayNotifyFacade(
				loader,
				alipayPaymentConfigValidationService,
				alipayAsyncNotifyVerificationService,
				alipayNotifyOrderSidePort,
				alipayNotifyDepositSidePort,
				companysRedisTemplate,
				objectMapper);
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString()))
				.thenReturn("{\"app_id\":\"test-app\",\"ali_public_key\":\"pk\",\"private_key\":\"sk\"}");
	}

	@Test
	void depositRecharge_tradeSuccess_invokesRechargeCallbackOnPort() throws Exception {
		HttpServletRequest mockRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
		String depositId = "deposit-out-1";
		String tradeNo = "alipay-trade-1";

		Map<String, String> encodedParams = new LinkedHashMap<>();
		encodedParams.put("trade_status", "TRADE_SUCCESS");
		encodedParams.put("out_trade_no", depositId);
		encodedParams.put("trade_no", tradeNo);

		Map<String, String> returnData = new LinkedHashMap<>();
		returnData.put("pay_type", "alipay");
		returnData.put("attach", "depositRecharge");

		AlipayNotifyPaymentContext ctx = new AlipayNotifyPaymentContext(
				encodedParams, returnData, 42L, depositId, 7L);

		when(loader.load(mockRequest)).thenReturn(Optional.of(ctx));
		doNothing()
				.when(alipayPaymentConfigValidationService)
				.assertAsyncNotifyConfigComplete(anyLong(), anyLong());
		doNothing()
				.when(alipayAsyncNotifyVerificationService)
				.verifySignedNotify(any(Map.class), any());

		facade.handle(mockRequest);

		verify(alipayNotifyDepositSidePort)
				.rechargeCallback(
						eq(depositId),
						eq("SUCCESS"),
						argThat(
								m ->
										"alipay".equals(m.get("pay_type"))
												&& tradeNo.equals(m.get("transaction_id"))));
	}

	@Test
	void nonDepositBranch_doesNotInvokeDepositPortRechargeCallback() throws Exception {
		HttpServletRequest mockRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
		String orderTradeId = "order-out-1";

		Map<String, String> encodedParams = new LinkedHashMap<>();
		encodedParams.put("trade_status", "TRADE_SUCCESS");
		encodedParams.put("out_trade_no", orderTradeId);
		encodedParams.put("trade_no", "tn-1");

		Map<String, String> returnData = new LinkedHashMap<>();
		returnData.put("attach", "orderPay");

		AlipayNotifyPaymentContext ctx = new AlipayNotifyPaymentContext(
				encodedParams, returnData, 42L, orderTradeId, 0L);

		when(loader.load(mockRequest)).thenReturn(Optional.of(ctx));
		doNothing()
				.when(alipayPaymentConfigValidationService)
				.assertAsyncNotifyConfigComplete(anyLong(), anyLong());
		doNothing()
				.when(alipayAsyncNotifyVerificationService)
				.verifySignedNotify(any(Map.class), any());

		facade.handle(mockRequest);

		verifyNoInteractions(alipayNotifyDepositSidePort);
		verify(alipayNotifyOrderSidePort)
				.applyTradePaymentAfterAlipay(eq(orderTradeId), eq("SUCCESS"), any());
	}
}
