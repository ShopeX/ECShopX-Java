package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInvoiceCallbackPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@ExtendWith(MockitoExtension.class)
class InvoiceCallbackSaasErpActTest {

	private static final long COMPANY_ID = 10L;

	@Mock
	private SaasErpInvoiceCallbackPort invoiceCallbackPort;

	private InvoiceCallbackSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new InvoiceCallbackSaasErpAct(invoiceCallbackPort);
	}

	@Test
	void actIsB2cInvoiceSend() {
		assertEquals("b2c.invoice.send", handler.act());
	}

	@Test
	void conditionalOnInvoiceCallbackPort() {
		assertNotNull(InvoiceCallbackSaasErpAct.class.getAnnotation(Service.class));
		ConditionalOnBean conditional =
				InvoiceCallbackSaasErpAct.class.getAnnotation(ConditionalOnBean.class);
		assertNotNull(conditional);
		assertEquals(Set.of(SaasErpInvoiceCallbackPort.class), Set.of(conditional.value()));
	}

	@Test
	void missingInvoiceMemoFailsBeforePortCall() {
		Map<String, String> params = validParamsExcept("invoice_memo");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("发票状态缺少！", result.msg());
		assertNull(result.info());
		verify(invoiceCallbackPort, never()).handle(params);
		verifyNoMoreInteractions(invoiceCallbackPort);
	}

	@Test
	void invoiceMemoTwoWithoutPdfInfoFailsBeforePortCall() {
		Map<String, String> params = baseValidParams();
		params.put("invoice_memo", "2");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少发票文件！", result.msg());
		assertNull(result.info());
		verify(invoiceCallbackPort, never()).handle(params);
		verifyNoMoreInteractions(invoiceCallbackPort);
	}

	@Test
	void portSuccessWithDataReturnsOk() {
		Map<String, String> params = baseValidParams();
		params.put("invoice_memo", "1");
		Map<String, Object> data = Map.of("invoice_id", 99L);
		when(invoiceCallbackPort.handle(params))
				.thenReturn(Map.of("success", true, "message", "更新成功", "data", data));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("更新成功", result.msg());
		assertEquals(data, result.info());
		verify(invoiceCallbackPort).handle(params);
		verifyNoMoreInteractions(invoiceCallbackPort);
	}

	@Test
	void portFailureWithoutDataUsesEmptyListInfo() {
		Map<String, String> params = baseValidParams();
		params.put("invoice_memo", "1");
		when(invoiceCallbackPort.handle(params))
				.thenReturn(Map.of("success", false, "message", "未找到发票"));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("未找到发票", result.msg());
		assertEquals(List.of(), result.info());
		verify(invoiceCallbackPort).handle(params);
		verifyNoMoreInteractions(invoiceCallbackPort);
	}

	private static Map<String, String> baseValidParams() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("blue_invoice_code", "BLUE-CODE");
		params.put("blue_invoice_no", "BLUE-NO");
		params.put("invoice_no", "INV-1");
		params.put("order_bn", "ORD-1");
		return params;
	}

	private static Map<String, String> validParamsExcept(String omitKey) {
		Map<String, String> params = baseValidParams();
		params.put("invoice_memo", "1");
		params.remove(omitKey);
		return params;
	}
}
