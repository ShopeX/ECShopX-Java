package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpAftersaleStatusPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateAftersalesStatusSaasErpActTest {

	private static final long COMPANY_ID = 10L;

	@Mock
	private SaasErpAftersaleStatusPort aftersaleStatusPort;

	private UpdateAftersalesStatusSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateAftersalesStatusSaasErpAct(aftersaleStatusPort);
	}

	@Test
	void actIsGetReturnStatus() {
		assertEquals("get_return_status", handler.act());
	}

	@Test
	void missingAftersaleIdFailsWithoutPortCall() {
		Map<String, String> params = validParams(null, "ord-1", "4");
		params.remove("aftersale_id");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("售后单号缺少！", result.msg());
		assertNull(result.info());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void blankAftersaleIdFailsWithoutPortCall() {
		SaasErpInboundResult result = handler.handle(validParams("  ", "ord-1", "4"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("售后单号缺少！", result.msg());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void missingOrderIdFailsWithoutPortCall() {
		Map<String, String> params = validParams("as-1", null, "4");
		params.remove("order_id");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单号缺少！", result.msg());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void blankOrderIdFailsWithoutPortCall() {
		SaasErpInboundResult result = handler.handle(validParams("as-1", "  ", "4"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单号缺少！", result.msg());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void missingStatusFailsWithoutPortCall() {
		Map<String, String> params = validParams("as-1", "ord-1", null);
		params.remove("status");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("售后状态缺少！", result.msg());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void blankStatusFailsWithoutPortCall() {
		SaasErpInboundResult result = handler.handle(validParams("as-1", "ord-1", "  "), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("售后状态缺少！", result.msg());
		verify(aftersaleStatusPort, never()).update(anyMap());
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void missingAftersalesRowFailsWithPortMessage() {
		Map<String, String> params = validParams("as-1", "ord-1", "4");
		when(aftersaleStatusPort.update(params))
				.thenThrow(new IllegalStateException("未获取到售后单信息"));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("未获取到售后单信息", result.msg());
		assertNull(result.info());
		verify(aftersaleStatusPort).update(params);
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void portRuntimeExceptionReturnsOkWithEmptyList() {
		Map<String, String> params = validParams("as-1", "ord-1", "4");
		when(aftersaleStatusPort.update(params)).thenThrow(new RuntimeException("boom"));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertInstanceOf(List.class, result.info());
		assertEquals(List.of(), result.info());
		verify(aftersaleStatusPort).update(params);
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void otherIllegalStateExceptionReturnsOkWithEmptyList() {
		Map<String, String> params = validParams("as-1", "ord-1", "4");
		when(aftersaleStatusPort.update(params)).thenThrow(new IllegalStateException("other"));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertEquals(List.of(), result.info());
		verify(aftersaleStatusPort).update(params);
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	@Test
	void portReturnListPassedThroughAsInfo() {
		Map<String, String> params = validParams("as-1", "ord-1", "4");
		List<Object> expected = List.of(Map.of("k", "v"));
		when(aftersaleStatusPort.update(params)).thenReturn(expected);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertEquals(expected, result.info());
		verify(aftersaleStatusPort).update(params);
		verifyNoMoreInteractions(aftersaleStatusPort);
	}

	private static Map<String, String> validParams(String aftersaleId, String orderId, String status) {
		Map<String, String> params = new LinkedHashMap<>();
		if (aftersaleId != null) {
			params.put("aftersale_id", aftersaleId);
		}
		if (orderId != null) {
			params.put("order_id", orderId);
		}
		if (status != null) {
			params.put("status", status);
		}
		return params;
	}
}
