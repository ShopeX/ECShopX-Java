package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSaasErpStoreTradeRefundCancelPortTest {

	@Test
	void callStoreTradeRefundCancel_usesRefundAddMethod() {
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		when(client.call(eq(38L), eq("store.trade.refund.add"), any())).thenReturn(Map.of("msg_id", "x"));
		DefaultSaasErpStoreTradeRefundCancelPort port = new DefaultSaasErpStoreTradeRefundCancelPort(client);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("refund_id", "R1");
		port.callStoreTradeRefundCancel(38L, body);
		verify(client).call(38L, "store.trade.refund.add", body);
	}
}
