/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.wxshipping;

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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxOrderShippingNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(WxOrderShippingNotificationProcessor.class);

	private static final Duration RETRY_DELAY = Duration.ofSeconds(3);
	private static final Set<Integer> GET_ORDER_RETRYABLE = Set.of(-1, 10060012);
	private static final Set<Integer> UPLOAD_RETRYABLE = Set.of(-1, 10060012, 10060019);

	private final TradeMapper tradeMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final LogisticsMapper logisticsMapper;
	private final WxaOrderShippingPort wxaOrderShippingPort;
	private final WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	public WxOrderShippingNotificationProcessor(
			TradeMapper tradeMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			LogisticsMapper logisticsMapper,
			WxaOrderShippingPort wxaOrderShippingPort,
			WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher) {
		this.tradeMapper = tradeMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.logisticsMapper = logisticsMapper;
		this.wxaOrderShippingPort = wxaOrderShippingPort;
		this.wxOrderShippingDispatchPublisher = wxOrderShippingDispatchPublisher;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Long companyId = longVal(payload.get("company_id"));
		Long orderId = longVal(payload.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}
		String tradeId = stringVal(payload.get("trade_id"));
		if (!StringUtils.hasText(tradeId)) {
			log.debug(
					"wx order shipping skipped: missing trade_id companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		Trade trade = tradeMapper.selectById(tradeId);
		String transactionId = trade == null || trade.getTransactionId() == null
				? ""
				: trade.getTransactionId().trim();
		if (!StringUtils.hasText(transactionId)) {
			log.debug(
					"wx order shipping skipped: empty transaction_id tradeId={} orderId={}",
					tradeId,
					orderId);
			return;
		}
		String wxaAppId = resolveWxaAppId(trade, payload);
		if (!StringUtils.hasText(wxaAppId)) {
			log.debug(
					"wx order shipping skipped: missing wxa_appid tradeId={} orderId={}",
					tradeId,
					orderId);
			return;
		}
		String receiptType = receiptTypeOf(payload);
		if (!isSupportedReceiptType(receiptType)) {
			log.debug(
					"wx order shipping skipped: receipt_type={} companyId={} orderId={}",
					receiptType,
					companyId,
					orderId);
			return;
		}
		Integer logisticsType = logisticsTypeOf(receiptType);
		if (logisticsType == null) {
			log.info("wechat_order_shipping_receipt_type:{}", receiptType);
			return;
		}
		String deliveryType = deliveryTypeOf(payload);

		Map<String, Object> getOrderResult;
		try {
			getOrderResult = wxaOrderShippingPort.getOrder(wxaAppId, transactionId);
		} catch (RuntimeException e) {
			log.error(
					"wx order shipping get_order failed companyId={} orderId={} transactionId={}",
					companyId,
					orderId,
					transactionId,
					e);
			return;
		}
		Integer getOrderErrcode = errcodeOf(getOrderResult);
		if (getOrderErrcode == null) {
			log.info("wechat_order_shipping_errcode:missing");
			return;
		}
		if (getOrderErrcode != 0) {
			if (GET_ORDER_RETRYABLE.contains(getOrderErrcode)) {
				wxOrderShippingDispatchPublisher.publish(payload, RETRY_DELAY);
			}
			log.info("wechat_order_shipping_errcode:{}", getOrderErrcode);
			return;
		}
		Integer orderState = orderStateOf(getOrderResult);
		if (orderState == null || orderState != 1) {
			log.info("wechat_order_shipping_order_state:{}", orderState);
			return;
		}

		NormalOrders order = loadOrder(companyId, orderId);
		String itemDesc = buildItemDesc(deliveryType, companyId, orderId, payload);
		Map<String, Object> shipping = new LinkedHashMap<>();
		shipping.put("item_desc", itemDesc);
		if ("logistics".equals(receiptType)) {
			fillLogisticsShipping(shipping, payload, order);
		}

		Map<String, Object> params = new LinkedHashMap<>();
		Map<String, Object> orderKey = new LinkedHashMap<>();
		orderKey.put("order_number_type", 2);
		orderKey.put("transaction_id", transactionId);
		params.put("order_key", orderKey);
		params.put("logistics_type", logisticsType);
		params.put(
				"delivery_mode",
				"sep".equals(deliveryType) ? "SPLIT_DELIVERY" : "UNIFIED_DELIVERY");
		params.put("is_all_delivered", booleanFrom(payload.get("is_all_delivered")));
		params.put("shipping_list", List.of(shipping));
		params.put(
				"upload_time",
				OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		Map<String, Object> payer = new LinkedHashMap<>();
		payer.put("openid", trade != null ? stringVal(trade.getOpenId()) : "");
		params.put("payer", payer);

		Map<String, Object> uploadResult;
		try {
			uploadResult = wxaOrderShippingPort.uploadShippingInfo(wxaAppId, params);
		} catch (RuntimeException e) {
			log.error(
					"wx order shipping upload failed companyId={} orderId={} transactionId={}",
					companyId,
					orderId,
					transactionId,
					e);
			return;
		}
		Integer uploadErrcode = errcodeOf(uploadResult);
		if (uploadErrcode == null) {
			log.info("wechat_order_shipping_result_errcode:missing");
			return;
		}
		if (uploadErrcode != 0) {
			if (UPLOAD_RETRYABLE.contains(uploadErrcode)) {
				wxOrderShippingDispatchPublisher.publish(payload, RETRY_DELAY);
			}
			log.info("wechat_order_shipping_result_errcode:{}", uploadErrcode);
		}
	}

	private void fillLogisticsShipping(
			Map<String, Object> shipping, Map<String, Object> payload, NormalOrders order) {
		String deliveryCorp = stringVal(payload.get("delivery_corp"));
		Logistics corp = resolveLogistics(deliveryCorp);
		shipping.put("tracking_no", stringVal(payload.get("delivery_code")));
		shipping.put("express_company", corp != null ? stringVal(corp.getCorpCode()) : "OTHER");
		if (!StringUtils.hasText((String) shipping.get("express_company"))) {
			shipping.put("express_company", "OTHER");
		}
		String receiverMobile = order == null ? "" : stringVal(order.getReceiverMobile());
		Map<String, Object> contact = new LinkedHashMap<>();
		contact.put("receiver_contact", DataMasking.mask("mobile", receiverMobile));
		shipping.put("contact", contact);
	}

	private Logistics resolveLogistics(String deliveryCorp) {
		if (!StringUtils.hasText(deliveryCorp)) {
			return null;
		}
		String corp = deliveryCorp.trim();
		Logistics row =
				logisticsMapper.selectOne(
						new LambdaQueryWrapper<Logistics>()
								.eq(Logistics::getCorpCode, corp)
								.last("LIMIT 1"));
		if (row != null) {
			return row;
		}
		return logisticsMapper.selectOne(
				new LambdaQueryWrapper<Logistics>()
						.eq(Logistics::getKuaidiCode, corp)
						.last("LIMIT 1"));
	}

	private NormalOrders loadOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private String buildItemDesc(
			String deliveryType, long companyId, long orderId, Map<String, Object> payload) {
		List<ItemDescRow> rows = new ArrayList<>();
		if ("batch".equals(deliveryType)) {
			List<NormalOrdersItems> items =
					normalOrdersItemsMapper.selectList(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getCompanyId, companyId)
									.eq(NormalOrdersItems::getOrderId, orderId));
			for (NormalOrdersItems item : items) {
				rows.add(new ItemDescRow(stringVal(item.getItemName()), item.getNum()));
			}
		} else {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> deliveryItems =
					(List<Map<String, Object>>) payload.get("delivery_items");
			if (deliveryItems != null) {
				for (Map<String, Object> item : deliveryItems) {
					String name = itemNameOf(item);
					Integer num = intVal(item.get("num"));
					rows.add(new ItemDescRow(name, num == null ? 0 : num));
				}
			}
		}
		StringBuilder itemDesc = new StringBuilder();
		for (ItemDescRow row : rows) {
			String piece = row.name + "*" + row.num + ",";
			int nextLen =
					itemDesc.toString().codePointCount(0, itemDesc.length())
							+ piece.codePointCount(0, piece.length());
			if (nextLen > 120) {
				break;
			}
			itemDesc.append(piece);
		}
		return trimTrailingCommas(itemDesc.toString());
	}

	private static String itemNameOf(Map<String, Object> item) {
		Object direct = item.get("item_name");
		if (direct != null && StringUtils.hasText(String.valueOf(direct).trim())) {
			return String.valueOf(direct).trim();
		}
		Object camel = item.get("itemName");
		return camel == null ? "" : String.valueOf(camel).trim();
	}

	private static String trimTrailingCommas(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		int end = value.length();
		while (end > 0 && value.charAt(end - 1) == ',') {
			end--;
		}
		int start = 0;
		while (start < end && value.charAt(start) == ',') {
			start++;
		}
		return value.substring(start, end);
	}

	private static Integer orderStateOf(Map<String, Object> getOrderResult) {
		if (getOrderResult == null) {
			return null;
		}
		Object orderObj = getOrderResult.get("order");
		if (!(orderObj instanceof Map<?, ?> order)) {
			return null;
		}
		return intVal(order.get("order_state"));
	}

	private static Integer errcodeOf(Map<String, Object> result) {
		if (result == null) {
			return null;
		}
		return intVal(result.get("errcode"));
	}

	private static String resolveWxaAppId(Trade trade, Map<String, Object> payload) {
		if (trade != null && StringUtils.hasText(trade.getWxaAppid())) {
			return trade.getWxaAppid().trim();
		}
		return stringVal(payload.get("wxa_appid"));
	}

	private static Integer logisticsTypeOf(String receiptType) {
		return switch (receiptType) {
			case "logistics" -> 1;
			case "dada" -> 2;
			case "ziti" -> 4;
			default -> null;
		};
	}

	private static boolean booleanFrom(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		return Boolean.TRUE.equals(raw);
	}

	private static boolean isSupportedReceiptType(String receiptType) {
		if (!StringUtils.hasText(receiptType)) {
			return false;
		}
		String r = receiptType.trim().toLowerCase(Locale.ROOT);
		return "logistics".equals(r) || "dada".equals(r) || "ziti".equals(r);
	}

	private static String receiptTypeOf(Map<String, Object> p) {
		Object direct = p.get("receipt_type");
		if (direct != null && StringUtils.hasText(String.valueOf(direct).trim())) {
			return String.valueOf(direct).trim().toLowerCase(Locale.ROOT);
		}
		Object legacy = p.get("shipping_type");
		return legacy == null ? "" : String.valueOf(legacy).trim().toLowerCase(Locale.ROOT);
	}

	private static String deliveryTypeOf(Map<String, Object> p) {
		Object direct = p.get("delivery_type");
		if (direct != null && StringUtils.hasText(String.valueOf(direct).trim())) {
			return String.valueOf(direct).trim().toLowerCase(Locale.ROOT);
		}
		Object legacy = p.get("package_type");
		return legacy == null ? "" : String.valueOf(legacy).trim().toLowerCase(Locale.ROOT);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static Long longVal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer intVal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record ItemDescRow(String name, int num) {}
}
