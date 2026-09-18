package cn.shopex.ecshopx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradeQueryParsedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AlipayH5SyncReturnServiceTest {

	private static final String OUT_TRADE_NO = "OUT20240506120000";

	@Mock
	private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;

	@Mock
	private AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService;

	@Mock
	private AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService;

	@Mock
	private AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;

	@Mock
	private AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private AlipayH5SyncReturnService alipayH5SyncReturnService;

	@BeforeEach
	void createService() {
		alipayH5SyncReturnService =
				new AlipayH5SyncReturnService(
						alipayPaymentConfigValidationService,
						alipayAsyncNotifyVerificationService,
						alipayOpenapiTradeQueryService,
						alipayNotifyOrderSidePort,
						alipayNotifyDepositSidePort,
						companysRedisTemplate,
						objectMapper);
	}

	@Test
	void alipayResult_whenTradeSuccessAndNotDeposit_invokesApplyTradePaymentAfterAlipayWithSuccess() {
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID)).thenReturn(1L);
		Map<String, String[]> pm = new HashMap<>();
		pm.put("out_trade_no", new String[] {OUT_TRADE_NO});
		pm.put("charset", new String[] {"UTF-8"});
		when(request.getParameterMap()).thenReturn(pm);

		when(alipayNotifyOrderSidePort.resolveDistributorIdForAlipayRedis(1L, OUT_TRADE_NO)).thenReturn(0L);
		doNothing()
				.when(alipayPaymentConfigValidationService)
				.assertAsyncNotifyConfigComplete(anyLong(), anyLong());
		doNothing().when(alipayAsyncNotifyVerificationService).verifySignedNotify(any(), any());

		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString()))
				.thenReturn(
						"{\"app_id\":\"test_app\",\"ali_public_key\":\"MIIB\",\"private_key\":\"MIIE\"}");

		AlipayTradeQueryParsedResponse parsed =
				new AlipayTradeQueryParsedResponse("10000", "TRADE_SUCCESS", "2024050622001", "", null);
		when(alipayOpenapiTradeQueryService.queryTrade(1L, 0L, OUT_TRADE_NO)).thenReturn(parsed);

		String status = alipayH5SyncReturnService.alipayResult(request);

		assertThat(status).isEqualTo("SUCCESS");
		verify(alipayNotifyOrderSidePort)
				.applyTradePaymentAfterAlipay(
						eq(OUT_TRADE_NO),
						eq("SUCCESS"),
						argThat(
								m ->
										"alipay".equals(m.get("pay_type"))
												&& "2024050622001".equals(m.get("transaction_id"))));
		verify(alipayNotifyDepositSidePort, never()).rechargeCallback(anyString(), anyString(), any());
	}
}
