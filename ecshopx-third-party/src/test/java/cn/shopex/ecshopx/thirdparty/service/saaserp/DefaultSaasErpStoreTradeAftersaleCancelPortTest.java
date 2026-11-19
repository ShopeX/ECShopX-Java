package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DefaultSaasErpStoreTradeAftersaleCancelPortTest {

	@Test
	void onlyRefund_usesRefundAddMethod() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		when(client.call(eq(38L), eq("store.trade.refund.add"), any())).thenReturn(Map.of());
		DefaultSaasErpStoreTradeAftersaleCancelPort port =
				new DefaultSaasErpStoreTradeAftersaleCancelPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_type", "ONLY_REFUND");
		port.callStoreTradeAftersaleCancel(38L, body);
		verify(client).call(38L, "store.trade.refund.add", body);
	}

	@Test
	void refundGoods_usesStatusUpdateWithStatus5() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		DefaultSaasErpStoreTradeAftersaleCancelPort port =
				new DefaultSaasErpStoreTradeAftersaleCancelPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_type", "REFUND_GOODS");
		port.callStoreTradeAftersaleCancel(38L, body);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(client).call(eq(38L), eq("store.trade.aftersale.status.update"), captor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(5, captor.getValue().get("status"));
	}
}
