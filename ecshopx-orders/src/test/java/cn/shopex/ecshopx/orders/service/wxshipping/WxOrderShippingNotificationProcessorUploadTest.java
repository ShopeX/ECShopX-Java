package cn.shopex.ecshopx.orders.service.wxshipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.wechat.WxaOrderShippingPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.superadmin.domain.Logistics;
import cn.shopex.ecshopx.superadmin.mapper.LogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxOrderShippingNotificationProcessorUploadTest {

	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock
	private LogisticsMapper logisticsMapper;
	@Mock
	private WxaOrderShippingPort wxaOrderShippingPort;
	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> uploadParamsCaptor;

	private WxOrderShippingNotificationProcessor processor;

	@BeforeEach
	void setUp() {
		processor =
				new WxOrderShippingNotificationProcessor(
						tradeMapper,
						normalOrdersMapper,
						normalOrdersItemsMapper,
						logisticsMapper,
						wxaOrderShippingPort,
						wxOrderShippingDispatchPublisher);
	}

	@Test
	void handle_logisticsSuccess_uploadsWithExpectedParams() {
		stubTrade("tr-1", "wx_txn_1", "wxa_app_1", "openid_1");
		stubOrder(1L, 2L, "13800138000");
		stubBatchItems(1L, 2L, item("商品A", 2), item("商品B", 1));
		stubLogisticsByCorpCode("SF", "SF");

		Map<String, Object> payload = basePayload("logistics", "batch");
		payload.put("trade_id", "tr-1");
		payload.put("delivery_corp", "SF");
		payload.put("delivery_code", "TRACK001");
		payload.put("is_all_delivered", true);

		when(wxaOrderShippingPort.getOrder("wxa_app_1", "wx_txn_1"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_app_1"), any()))
				.thenReturn(Map.of("errcode", 0));

		processor.handle(payload);

		verify(wxaOrderShippingPort).uploadShippingInfo(eq("wxa_app_1"), uploadParamsCaptor.capture());
		Map<String, Object> params = uploadParamsCaptor.getValue();
		assertThat(params.get("logistics_type")).isEqualTo(1);
		assertThat(params.get("delivery_mode")).isEqualTo("UNIFIED_DELIVERY");
		assertThat(params.get("is_all_delivered")).isEqualTo(true);

		@SuppressWarnings("unchecked")
		Map<String, Object> orderKey = (Map<String, Object>) params.get("order_key");
		assertThat(orderKey.get("order_number_type")).isEqualTo(2);
		assertThat(orderKey.get("transaction_id")).isEqualTo("wx_txn_1");

		@SuppressWarnings("unchecked")
		Map<String, Object> payer = (Map<String, Object>) params.get("payer");
		assertThat(payer.get("openid")).isEqualTo("openid_1");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> shippingList = (List<Map<String, Object>>) params.get("shipping_list");
		assertThat(shippingList).hasSize(1);
		Map<String, Object> shipping = shippingList.get(0);
		assertThat(shipping.get("item_desc")).isEqualTo("商品A*2,商品B*1");
		assertThat(shipping.get("tracking_no")).isEqualTo("TRACK001");
		assertThat(shipping.get("express_company")).isEqualTo("SF");
		@SuppressWarnings("unchecked")
		Map<String, Object> contact = (Map<String, Object>) shipping.get("contact");
		assertThat(contact.get("receiver_contact"))
				.isEqualTo(DataMasking.mask("mobile", "13800138000"));
		assertThat(params.get("upload_time")).isNotNull();
	}

	@Test
	void handle_logisticsSep_usesSplitDeliveryMode() {
		stubTrade("tr-sep", "wx_txn_sep", "wxa_sep", "openid_sep");
		stubOrder(10L, 20L, "13900139000");
		stubLogisticsByCorpCode("SF", "SF");

		Map<String, Object> payload = basePayload("logistics", "sep");
		payload.put("trade_id", "tr-sep");
		payload.put("company_id", 10L);
		payload.put("order_id", 20L);
		payload.put("delivery_corp", "SF");
		payload.put("delivery_code", "TRACK_SEP");
		payload.put(
				"delivery_items",
				List.of(Map.of("item_name", "分单商品", "num", 3)));

		when(wxaOrderShippingPort.getOrder("wxa_sep", "wx_txn_sep"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_sep"), any()))
				.thenReturn(Map.of("errcode", 0));

		processor.handle(payload);

		verify(wxaOrderShippingPort).uploadShippingInfo(eq("wxa_sep"), uploadParamsCaptor.capture());
		Map<String, Object> params = uploadParamsCaptor.getValue();
		assertThat(params.get("delivery_mode")).isEqualTo("SPLIT_DELIVERY");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> shippingList = (List<Map<String, Object>>) params.get("shipping_list");
		assertThat(shippingList.get(0).get("item_desc")).isEqualTo("分单商品*3");
	}

	@Test
	void handle_dada_usesLogisticsType2WithoutTrackingFields() {
		stubTrade("tr-dada", "wx_txn_dada", "wxa_dada", "openid_dada");
		stubOrder(1L, 2L, "13800138000");
		stubBatchItems(1L, 2L, item("达达商品", 1));

		Map<String, Object> payload = basePayload("dada", "batch");
		payload.put("trade_id", "tr-dada");
		payload.put("delivery_code", "dada");

		when(wxaOrderShippingPort.getOrder("wxa_dada", "wx_txn_dada"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_dada"), any()))
				.thenReturn(Map.of("errcode", 0));

		processor.handle(payload);

		verify(wxaOrderShippingPort).uploadShippingInfo(eq("wxa_dada"), uploadParamsCaptor.capture());
		Map<String, Object> params = uploadParamsCaptor.getValue();
		assertThat(params.get("logistics_type")).isEqualTo(2);
		@SuppressWarnings("unchecked")
		Map<String, Object> shipping = ((List<Map<String, Object>>) params.get("shipping_list")).get(0);
		assertThat(shipping).doesNotContainKey("tracking_no");
		assertThat(shipping).doesNotContainKey("express_company");
		assertThat(shipping).doesNotContainKey("contact");
	}

	@Test
	void handle_ziti_usesLogisticsType4() {
		stubTrade("tr-ziti", "wx_txn_ziti", "wxa_ziti", "openid_ziti");
		stubOrder(1L, 2L, "13800138000");
		stubBatchItems(1L, 2L, item("自提商品", 2));

		Map<String, Object> payload = basePayload("ziti", "batch");
		payload.put("trade_id", "tr-ziti");

		when(wxaOrderShippingPort.getOrder("wxa_ziti", "wx_txn_ziti"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_ziti"), any()))
				.thenReturn(Map.of("errcode", 0));

		processor.handle(payload);

		verify(wxaOrderShippingPort).uploadShippingInfo(eq("wxa_ziti"), uploadParamsCaptor.capture());
		assertThat(uploadParamsCaptor.getValue().get("logistics_type")).isEqualTo(4);
	}

	@Test
	void handle_getOrderRetryableErrcode_republishesWithoutUpload() {
		stubTrade("tr-retry", "wx_txn_retry", "wxa_retry", "openid_retry");

		Map<String, Object> payload = basePayload("logistics", "batch");
		payload.put("trade_id", "tr-retry");

		when(wxaOrderShippingPort.getOrder("wxa_retry", "wx_txn_retry"))
				.thenReturn(Map.of("errcode", -1));

		processor.handle(payload);

		verify(wxOrderShippingDispatchPublisher).publish(eq(payload), eq(Duration.ofSeconds(3)));
		verify(wxaOrderShippingPort, never()).uploadShippingInfo(any(), any());
	}

	@Test
	void handle_uploadRetryableErrcode_republishesWithDelay() {
		stubTrade("tr-up", "wx_txn_up", "wxa_up", "openid_up");
		stubOrder(1L, 2L, "13800138000");
		stubBatchItems(1L, 2L, item("商品", 1));

		Map<String, Object> payload = basePayload("dada", "batch");
		payload.put("trade_id", "tr-up");

		when(wxaOrderShippingPort.getOrder("wxa_up", "wx_txn_up"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_up"), any()))
				.thenReturn(Map.of("errcode", 10060019));

		processor.handle(payload);

		verify(wxOrderShippingDispatchPublisher).publish(eq(payload), eq(Duration.ofSeconds(3)));
	}

	@Test
	void handle_emptyTransactionId_skipsPortCalls() {
		Trade trade = new Trade();
		trade.setTransactionId("");
		when(tradeMapper.selectById("tr-empty")).thenReturn(trade);

		Map<String, Object> payload = basePayload("logistics", "batch");
		payload.put("trade_id", "tr-empty");

		processor.handle(payload);

		verify(wxaOrderShippingPort, never()).getOrder(any(), any());
		verify(wxaOrderShippingPort, never()).uploadShippingInfo(any(), any());
		verify(wxOrderShippingDispatchPublisher, never()).publish(any(), any());
	}

	@Test
	void handle_orderStateNotReady_skipsUpload() {
		stubTrade("tr-state", "wx_txn_state", "wxa_state", "openid_state");

		Map<String, Object> payload = basePayload("logistics", "batch");
		payload.put("trade_id", "tr-state");

		when(wxaOrderShippingPort.getOrder("wxa_state", "wx_txn_state"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 2)));

		processor.handle(payload);

		verify(wxaOrderShippingPort, never()).uploadShippingInfo(any(), any());
		verify(wxOrderShippingDispatchPublisher, never()).publish(any(), any());
	}

	@Test
	void handle_syntheticErrcodeMinus999_doesNotRepublish() {
		stubTrade("tr-999", "wx_txn_999", "wxa_999", "openid_999");
		stubOrder(1L, 2L, "13800138000");
		stubBatchItems(1L, 2L, item("商品", 1));

		Map<String, Object> payload = basePayload("dada", "batch");
		payload.put("trade_id", "tr-999");

		when(wxaOrderShippingPort.getOrder("wxa_999", "wx_txn_999"))
				.thenReturn(Map.of("errcode", 0, "order", Map.of("order_state", 1)));
		when(wxaOrderShippingPort.uploadShippingInfo(eq("wxa_999"), any()))
				.thenReturn(Map.of("errcode", -999, "errmsg", "invalid wechat response"));

		processor.handle(payload);

		verify(wxOrderShippingDispatchPublisher, never()).publish(any(), any());
	}

	private Map<String, Object> basePayload(String receiptType, String deliveryType) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);
		payload.put("receipt_type", receiptType);
		payload.put("delivery_type", deliveryType);
		payload.put("is_all_delivered", false);
		return payload;
	}

	private void stubTrade(String tradeId, String transactionId, String wxaAppId, String openId) {
		Trade trade = new Trade();
		trade.setTradeId(tradeId);
		trade.setTransactionId(transactionId);
		trade.setWxaAppid(wxaAppId);
		trade.setOpenId(openId);
		when(tradeMapper.selectById(tradeId)).thenReturn(trade);
	}

	private void stubOrder(long companyId, long orderId, String receiverMobile) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setReceiverMobile(receiverMobile);
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
	}

	private void stubBatchItems(long companyId, long orderId, NormalOrdersItems... items) {
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(items));
	}

	private static NormalOrdersItems item(String name, int num) {
		NormalOrdersItems row = new NormalOrdersItems();
		row.setItemName(name);
		row.setNum(num);
		return row;
	}

	private void stubLogisticsByCorpCode(String lookup, String corpCode) {
		Logistics logistics = new Logistics();
		logistics.setCorpCode(corpCode);
		when(logisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(logistics);
	}
}
