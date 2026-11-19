package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSaasErpStoreTradeAftersaleAddPortTest {

	@Test
	void callStoreTradeAftersaleAdd_delegatesToMatrixClient() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		when(client.call(eq(38L), eq("store.trade.aftersale.add"), any())).thenReturn(Map.of("msg_id", "x"));
		DefaultSaasErpStoreTradeAftersaleAddPort port = new DefaultSaasErpStoreTradeAftersaleAddPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("aftersale_id", "A1");
		port.callStoreTradeAftersaleAdd(38L, body);
		verify(client).call(38L, "store.trade.aftersale.add", body);
	}

	@Test
	void callOmeAftersaleStatusUpdate_delegatesToMatrixClient() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		DefaultSaasErpStoreTradeAftersaleAddPort port = new DefaultSaasErpStoreTradeAftersaleAddPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("status", 3);
		port.callOmeAftersaleStatusUpdate(38L, body);
		verify(client).call(38L, "ome.aftersale.status_update", body);
	}
}
