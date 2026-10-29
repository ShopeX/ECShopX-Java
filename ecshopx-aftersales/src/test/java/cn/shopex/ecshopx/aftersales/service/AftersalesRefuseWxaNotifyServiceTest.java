package cn.shopex.ecshopx.aftersales.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefuseWxaSubscribeSendPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesRefuseWxaNotifyServiceTest {

	@Mock
	private OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort;

	@Mock
	private AftersalesRefuseWxaSubscribeSendPort aftersalesRefuseWxaSubscribeSendPort;

	@InjectMocks
	private AftersalesRefuseWxaNotifyService service;

	@Test
	@DisplayName("seller refuse builds aftersalesRefuse payload")
	void sellerRefuseSends() {
		when(orderTradeMiniProgramRecipientLookupPort.findRecipient(1L, 100L))
				.thenReturn(Optional.of(new MiniProgramTradeRecipient("wx", "oid")));

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 1L);
		row.put("order_id", 100L);
		row.put("aftersales_type", "REFUND_GOODS");
		row.put("refuse_reason", "质量问题");
		row.put("refund_fee", 1500);

		service.sendByStatus(3, row);

		verify(aftersalesRefuseWxaSubscribeSendPort)
				.send(
						argThat(
								m -> "aftersalesRefuse".equals(m.get("scenes_name"))
										&& "wx".equals(m.get("appid"))
										&& "oid".equals(m.get("openid"))
										&& m.get("data") instanceof Map<?, ?> data
										&& "15.00".equals(data.get("refund_fee"))
										&& "质量问题".equals(data.get("remarks"))),
						eq(true));
	}

	@Test
	@DisplayName("unknown status skips")
	void unknownStatusSkips() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 1L);
		row.put("order_id", 100L);
		row.put("aftersales_type", "REFUND_GOODS");
		service.sendByStatus(0, row);
		verifyNoInteractions(orderTradeMiniProgramRecipientLookupPort, aftersalesRefuseWxaSubscribeSendPort);
	}
}
