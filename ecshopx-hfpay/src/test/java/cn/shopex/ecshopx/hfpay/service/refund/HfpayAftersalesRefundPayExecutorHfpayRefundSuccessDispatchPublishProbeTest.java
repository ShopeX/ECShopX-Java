package cn.shopex.ecshopx.hfpay.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.hfpay.HfpayRefundSuccessEventPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("event:294: Hfpay refund success publishEvent after gateway success (RefundJob executor path)")
class HfpayAftersalesRefundPayExecutorHfpayRefundSuccessDispatchPublishProbeTest {

	@Test
	void gatewaySuccess_invokesPublishPortOnce_withOrderIdAndRefundBn() {
		HfpayRefundSuccessEventPublishPort publishPort = mock(HfpayRefundSuccessEventPublishPort.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "M1");
		when(paymentSettingService.loadForCompany(701L)).thenReturn(setting);
		Map<String, Object> gateway = new LinkedHashMap<>();
		gateway.put("resp_code", "C00002");
		gateway.put("order_id", "gw-1");
		when(acouJsonPostClient.reb001(eq(setting), any())).thenReturn(gateway);

		HfpayAftersalesRefundPayExecutor executor =
				new HfpayAftersalesRefundPayExecutor(publishPort, acouJsonPostClient, paymentSettingService);
		AftersalesRefundPaymentContext ctx =
				new AftersalesRefundPaymentContext(
						701L,
						"",
						52001L,
						62001L,
						1L,
						0L,
						0L,
						0L,
						0L,
						"hfpay",
						"",
						"trade-org-1",
						500,
						100,
						"",
						"",
						false,
						"hfoid-1",
						"20240101",
						"20240102",
						1);

		Map<String, Object> out = executor.execute(ctx);
		assertEquals("SUCCESS", String.valueOf(out.get("status")));
		verify(publishPort, times(1)).publishSyncOnGatewayRefundSuccess(eq("62001"), eq(52001L));
	}
}
