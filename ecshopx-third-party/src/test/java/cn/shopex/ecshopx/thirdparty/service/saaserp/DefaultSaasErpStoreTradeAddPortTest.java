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

class DefaultSaasErpStoreTradeAddPortTest {

	@Test
	void callStoreTradeAdd_delegatesToMatrixClient() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		when(client.call(eq(38L), eq("store.trade.add"), any())).thenReturn(Map.of("msg_id", "x"));
		DefaultSaasErpStoreTradeAddPort port = new DefaultSaasErpStoreTradeAddPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("tid", "1");
		port.callStoreTradeAdd(38L, body);
		verify(client).call(38L, "store.trade.add", body);
	}

	@Test
	void callStoreTradeAdd_whenEmptyBody_skips() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		DefaultSaasErpStoreTradeAddPort port = new DefaultSaasErpStoreTradeAddPort(client);
		port.callStoreTradeAdd(38L, Map.of());
		verify(client, never()).call(anyLong(), anyString(), any());
	}
}
