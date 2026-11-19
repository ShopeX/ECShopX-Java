package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSaasErpStoreTradeRefundAddPortTest {

	@Test
	void callStoreTradeRefundAdd_delegatesToMatrixClient() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		when(client.call(eq(38L), eq("store.trade.refund.add"), any())).thenReturn(Map.of("msg_id", "x"));
		DefaultSaasErpStoreTradeRefundAddPort port = new DefaultSaasErpStoreTradeRefundAddPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("refund_id", "R1");
		port.callStoreTradeRefundAdd(38L, body);
		verify(client).call(38L, "store.trade.refund.add", body);
	}

	@Test
	void callStoreTradeRefundAdd_whenEmptyBody_skips() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		DefaultSaasErpStoreTradeRefundAddPort port = new DefaultSaasErpStoreTradeRefundAddPort(client);
		port.callStoreTradeRefundAdd(38L, Map.of());
		verify(client, never()).call(anyLong(), anyString(), any());
	}
}
