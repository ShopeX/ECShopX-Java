package cn.shopex.ecshopx.adapay.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.domain.AdapayPaymentReverse;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymentReverseMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymemtConfirmMapper;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import com.huifu.adapay.model.PaymentReverse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: adapay payment reverse publishEvent probe")
class AdapayAftersalesRefundPayExecutorPaymentReverseOrderProcessLogDispatchPublishProbeTest {

	@Test
	void paymentReverse_success_invokesPublishEventOnce_withReverseSuccessDetail() {
		AdapayPaymemtConfirmMapper confirmMapper = mock(AdapayPaymemtConfirmMapper.class);
		when(confirmMapper.selectOne(any())).thenReturn(null);
		AdapayPaymentSettingRedisReader settingReader = mock(AdapayPaymentSettingRedisReader.class);
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("app_id", "app-x");
		cfg.put("live_api_key", "live");
		cfg.put("test_api_key", "test");
		cfg.put("rsa_private_key", "rsa");
		when(settingReader.getPaymentSetting(701L)).thenReturn(cfg);
		AdapayPaymentReverseMapper reverseMapper = mock(AdapayPaymentReverseMapper.class);
		when(reverseMapper.insert(any(AdapayPaymentReverse.class))).thenReturn(1);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		AdapayPaymentReverseApplicationService reverseApplicationService =
				new AdapayPaymentReverseApplicationService(
						confirmMapper, settingReader, reverseMapper, orderProcessLogPublishPort, false);

		AdapayAftersalesRefundPayExecutor executor =
				new AdapayAftersalesRefundPayExecutor(
						confirmMapper, settingReader, reverseApplicationService, false);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", "succeeded");
		data.put("id", "pr-success-1");
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", data);

		AftersalesRefundPaymentContext ctx =
				new AftersalesRefundPaymentContext(
						701L,
						"",
						42001L,
						90001L,
						1L,
						0L,
						0L,
						0L,
						0L,
						"adapay",
						"wx_lite",
						"tr-1",
						200,
						100,
						"",
						"pay_txn_1",
						false,
						"",
						"",
						"",
						0);

		try (MockedStatic<PaymentReverse> pr = mockStatic(PaymentReverse.class)) {
			pr.when(() -> PaymentReverse.create(anyMap(), anyString())).thenReturn(root);
			Map<String, Object> payRes = executor.execute(ctx);
			assertEquals("SUCCESS", String.valueOf(payRes.get("status")));
			assertEquals(Boolean.TRUE, payRes.get("order_process_log_via_payment_reverse"));
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(90001L, payload.get("order_id"));
		assertEquals(701L, payload.get("company_id"));
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals(
				"订单号：90001，订单支付撤销成功（adapay支付渠道）",
				String.valueOf(payload.get("detail")));
		verify(reverseMapper, times(1)).insert(any(AdapayPaymentReverse.class));
	}

	@Test
	void paymentReverse_failed_invokesPublishEventOnce_withReverseFailDetail() {
		AdapayPaymemtConfirmMapper confirmMapper = mock(AdapayPaymemtConfirmMapper.class);
		when(confirmMapper.selectOne(any())).thenReturn(null);
		AdapayPaymentSettingRedisReader settingReader = mock(AdapayPaymentSettingRedisReader.class);
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("app_id", "app-y");
		cfg.put("live_api_key", "live");
		cfg.put("test_api_key", "test");
		cfg.put("rsa_private_key", "rsa");
		when(settingReader.getPaymentSetting(702L)).thenReturn(cfg);
		AdapayPaymentReverseMapper reverseMapper = mock(AdapayPaymentReverseMapper.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		AdapayPaymentReverseApplicationService reverseApplicationService =
				new AdapayPaymentReverseApplicationService(
						confirmMapper, settingReader, reverseMapper, orderProcessLogPublishPort, false);

		AdapayAftersalesRefundPayExecutor executor =
				new AdapayAftersalesRefundPayExecutor(
						confirmMapper, settingReader, reverseApplicationService, false);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", "failed");
		data.put("error_msg", "channel declined");
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", data);

		AftersalesRefundPaymentContext ctx =
				new AftersalesRefundPaymentContext(
						702L,
						"",
						42002L,
						90002L,
						2L,
						0L,
						0L,
						0L,
						0L,
						"AdaPay",
						"wx_lite",
						"tr-2",
						100,
						50,
						"",
						"pay_txn_2",
						false,
						"",
						"",
						"",
						0);

		try (MockedStatic<PaymentReverse> pr = mockStatic(PaymentReverse.class)) {
			pr.when(() -> PaymentReverse.create(anyMap(), anyString())).thenReturn(root);
			Map<String, Object> payRes = executor.execute(ctx);
			assertEquals("FAIL", String.valueOf(payRes.get("status")));
			assertEquals(Boolean.TRUE, payRes.get("order_process_log_via_payment_reverse"));
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(
				"订单号：90002，订单支付撤销（adapay支付渠道），失败原因：channel declined",
				String.valueOf(payload.get("detail")));
		verify(reverseMapper, times(0)).insert(any(AdapayPaymentReverse.class));
	}
}
