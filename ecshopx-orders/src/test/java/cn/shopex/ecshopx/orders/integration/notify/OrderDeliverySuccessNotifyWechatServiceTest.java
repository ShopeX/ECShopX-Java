package cn.shopex.ecshopx.orders.integration.notify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import cn.shopex.ecshopx.common.port.orders.OrderDeliverySuccessWxaSubscribeSendPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
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
class OrderDeliverySuccessNotifyWechatServiceTest {

	@Mock
	private OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort;

	@Mock
	private OrderDeliverySuccessWxaSubscribeSendPort orderDeliverySuccessWxaSubscribeSendPort;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@InjectMocks
	private OrderDeliverySuccessNotifyWechatService service;

	@Test
	@DisplayName("missing recipient skips send")
	void whenNoRecipient_skipsSend() {
		when(orderTradeMiniProgramRecipientLookupPort.findRecipient(1L, 100L)).thenReturn(Optional.empty());

		Map<String, Object> ctx = baseContext();
		service.sendDeliverySuccessNotice(ctx);

		verifyNoInteractions(orderDeliverySuccessWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("batch delivery uses order title and corp display name")
	void whenBatch_buildsOrderDeliverySuccPayload() {
		when(orderTradeMiniProgramRecipientLookupPort.findRecipient(1L, 100L))
				.thenReturn(Optional.of(new MiniProgramTradeRecipient("wxapp", "openid-1")));
		NormalOrders order = new NormalOrders();
		order.setTitle("商品A等2件");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		Map<String, Object> ctx = baseContext();
		ctx.put("delivery_corp_name", "顺丰速运");
		service.sendDeliverySuccessNotice(ctx);

		verify(orderDeliverySuccessWxaSubscribeSendPort)
				.send(
						argThat(
								m -> "orderDeliverySucc".equals(m.get("scenes_name"))
										&& Long.valueOf(1L).equals(toLong(m.get("company_id")))
										&& "wxapp".equals(m.get("appid"))
										&& "openid-1".equals(m.get("openid"))
										&& m.get("data") instanceof Map<?, ?> data
										&& "100".equals(String.valueOf(data.get("order_id")))
										&& "顺丰速运".equals(data.get("delivery_corp"))
										&& "SF123".equals(data.get("delivery_code"))
										&& "商品A等2件".equals(data.get("item_name"))),
						eq(true));
	}

	@Test
	@DisplayName("sep delivery prefers item_name and logi_name from context")
	void whenSep_prefersItemNameAndLogiName() {
		when(orderTradeMiniProgramRecipientLookupPort.findRecipient(1L, 100L))
				.thenReturn(Optional.of(new MiniProgramTradeRecipient("wxapp", "openid-1")));

		Map<String, Object> ctx = baseContext();
		ctx.put("item_name", "SKU名称");
		ctx.put("logi_name", "中通快递");
		ctx.put("delivery_corp_name", "不会用到");
		service.sendDeliverySuccessNotice(ctx);

		verify(normalOrdersMapper, never()).selectOne(any());
		verify(orderDeliverySuccessWxaSubscribeSendPort)
				.send(
						argThat(
								m -> m.get("data") instanceof Map<?, ?> data
										&& "SKU名称".equals(data.get("item_name"))
										&& "中通快递".equals(data.get("delivery_corp"))),
						eq(true));
	}

	private static Map<String, Object> baseContext() {
		Map<String, Object> ctx = new LinkedHashMap<>();
		ctx.put("company_id", 1L);
		ctx.put("order_id", 100L);
		ctx.put("user_id", 9L);
		ctx.put("delivery_corp", "SF");
		ctx.put("delivery_code", "SF123");
		return ctx;
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
