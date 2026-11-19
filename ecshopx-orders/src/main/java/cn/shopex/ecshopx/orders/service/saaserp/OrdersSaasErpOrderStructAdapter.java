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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderStructPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrdersSaasErpOrderStructAdapter implements SaasErpOrderStructPort {

	static final String DADA_SHIPPING_TYPE = "STORE_TONGCHEN_EXPRESS";

	static final Set<String> ALLOW_PARAMS =
			Set.of(
					"rights_level",
					"lastmodify",
					"payment_lists",
					"promotion_details",
					"total_weight",
					"buyer_name",
					"currency_rate",
					"app_id",
					"shipping_type",
					"receiver_address",
					"has_invoice",
					"receiver_district",
					"from_type",
					"callback_type",
					"protect_fee",
					"receiver_phone",
					"to_node_id",
					"order_source",
					"logistics_no",
					"pay_cost",
					"buyer_uname",
					"timestamp",
					"_id",
					"tid",
					"receiver_mobile",
					"goods_discount_fee",
					"orders_number",
					"invoice_fee",
					"discount_fee",
					"pay_status",
					"buyer_obtain_point_fee",
					"payment_type",
					"v",
					"real_time",
					"shipping_fee",
					"refresh_time",
					"is_cod",
					"msg_id",
					"currency",
					"node_type",
					"pay_time",
					"payment_tid",
					"orders",
					"receiver_city",
					"channel_ver",
					"orders_discount_fee",
					"format",
					"buyer_memo",
					"from_node_id",
					"shipping_tid",
					"method",
					"channel",
					"status",
					"total_trade_fee",
					"buyer_state",
					"receiver_zip",
					"callback_type_id",
					"to_type",
					"node_id",
					"total_goods_fee",
					"date",
					"buyer_mobile",
					"task",
					"created",
					"ship_status",
					"payed_fee",
					"is_protect",
					"receiver_state",
					"receiver_name",
					"consign_time",
					"step_trade_status",
					"trade_memo",
					"invoice_desc   ",
					"invoice_title",
					"trade_type",
					"buyer_email",
					"cod_status",
					"step_paid_fee",
					"modified",
					"end_time",
					"service_order_objects",
					"buyer_phone",
					"service_orders",
					"o2o_info");

	private static final Set<String> OMS_PUSH_SKIP_PAY_TYPES = Set.of("localPay");

	private static final Map<String, String> PAYMENT_LABELS = paymentLabels();

	private static final DateTimeFormatter PHP_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private final OrdersSaasErpOrderBundleReader orderReader;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OrdersSaasErpOrderStructAdapter(
			OrdersSaasErpOrderBundleReader orderReader,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.orderReader = orderReader;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@Override
	public Optional<Map<String, Object>> getOrderStruct(long companyId, String orderId, String sourceType) {
		Optional<Map<String, Object>> loaded = orderReader.read(companyId, orderId, sourceType);
		if (loaded.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> orderInfo = asStringKeyMap(loaded.get().get("orderInfo"));
		if (orderInfo.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> tradeInfo = asStringKeyMap(loaded.get().get("tradeInfo"));
		Map<String, Object> distributor = asStringKeyMap(loaded.get().get("distributor"));

		Map<String, Object> setting = readSendOmsSetting(companyId);
		if ("ziti".equals(text(orderInfo.get("receipt_type"))) && !phpTruthy(setting.get("ziti_send_oms"))) {
			return Optional.empty();
		}
		if ("NOTPAY".equals(text(orderInfo.get("order_status")))) {
			return Optional.empty();
		}
		Object paymentTidValue = nullCoalesce(tradeInfo, "payType", orderInfo.get("pay_type"));
		String effectivePayType = paymentTidValue == null ? "" : String.valueOf(paymentTidValue);
		if (shouldSkipOmsPushForPayType(effectivePayType)) {
			return Optional.empty();
		}

		Map<String, Object> memberInfo = formatMemberInfo(orderInfo);
		if (memberInfo == null) {
			return Optional.empty();
		}

		String status = "";
		String payStatus = "";
		switch (text(orderInfo.get("order_status"))) {
			case "DONE" -> {
				status = "TRADE_FINISHED";
				payStatus = "PAY_FINISH";
			}
			case "PAYED" -> {
				status = "TRADE_ACTIVE";
				payStatus = "PAY_FINISH";
			}
			case "CANCEL" -> {
				status = "TRADE_CLOSED";
				payStatus = "REFUND_ALL";
			}
			default -> {
				// unmatched statuses stay empty, matching the PHP switch
			}
		}

		String shipStatus = "";
		switch (text(orderInfo.get("delivery_status"))) {
			case "DONE" -> shipStatus = "SHIP_FINISH";
			case "PENDING" -> shipStatus = "SHIP_NO";
			case "PARTAIL" -> shipStatus = "SHIP_PART";
			default -> {
				// unmatched delivery statuses stay empty
			}
		}
		String receiptType = text(orderInfo.get("receipt_type"));
		if ("ziti".equals(receiptType) && "DONE".equals(text(orderInfo.get("ziti_status")))) {
			shipStatus = "SHIP_FINISH";
		}
		if ("dada".equals(receiptType) && "DONE".equals(text(orderInfo.get("order_status")))) {
			shipStatus = "SHIP_FINISH";
		}

		String paymentDisplayLabel = resolveOmsPaymentDisplayLabel(effectivePayType);

		Object orders = formatOrders(orderInfo, status, distributor);
		Object paymentLists = formatPaymentLists(tradeInfo);
		Map<String, Object> taxInfo = taxInfo(orderInfo);

		Map<String, Object> struct = new LinkedHashMap<>();
		struct.put("tid", orderInfo.get("order_id"));
		struct.put("lastmodify", phpDate(orderInfo.get("update_time")));
		struct.put("promotion_details", writeJson(pmtDetail(orderInfo)));
		struct.put("total_weight", "0.000");
		struct.put("created", phpDate(orderInfo.get("create_time")));
		struct.put("payment_lists", writeJson(paymentLists));
		struct.put("status", status);
		struct.put("pay_status", payStatus);
		struct.put("ship_status", shipStatus);
		struct.put("payed_fee", bcdiv100(tradeInfo.get("payFee")));
		struct.put("total_goods_fee", bcdiv100(orderInfo.get("item_fee")));
		struct.put("total_trade_fee", bcdiv100(orderInfo.get("total_fee")));
		struct.put("currency", "CNY");
		struct.put("currency_rate", orderInfo.get("fee_rate"));
		struct.put("buyer_obtain_point_fee", "0.000");
		struct.put("is_protect", "false");
		struct.put("protect_fee", "0.000");
		struct.put("discount_fee", 0);
		struct.put("is_cod", "false");
		struct.put("payment_tid", paymentTidValue);
		struct.put("payment_type", paymentDisplayLabel);
		struct.put("orders_number", 1);
		struct.put("buyer_uname", memberInfo.get("uname"));
		struct.put("buyer_name", buyerName(memberInfo));
		struct.put("buyer_mobile", memberInfo.get("mobile"));
		struct.put("buyer_phone", memberInfo.get("mobile"));
		struct.put("buyer_email", "");
		struct.put("buyer_state", "");
		struct.put("receiver_name", orderInfo.get("receiver_name"));
		struct.put("receiver_phone", "");
		struct.put("receiver_mobile", orderInfo.get("receiver_mobile"));
		struct.put("receiver_state", orderInfo.get("receiver_state"));
		struct.put("receiver_city", orderInfo.get("receiver_city"));
		struct.put("receiver_district", orderInfo.get("receiver_district"));
		struct.put("receiver_address", orderInfo.get("receiver_address"));
		struct.put("receiver_zip", orderInfo.get("receiver_zip"));
		struct.put(
				"orders_discount_fee",
				bcdiv100(toCents(orderInfo.get("discount_fee")).add(toCents(orderInfo.get("point_fee")))));
		struct.put("orders", writeJson(orders));
		struct.put("goods_discount_fee", "0.00");
		struct.put("shipping_tid", "0");
		struct.put("shipping_type", orderInfo.get("delivery_corp"));
		struct.put("shipping_fee", bcdiv100(orderInfo.get("freight_fee")));
		struct.put("has_invoice", taxInfo.get("is_tax"));
		struct.put("invoice_title", taxInfo.get("title"));
		struct.put("invoice_fee", taxInfo.get("cost_tax"));
		struct.put("pay_cost", "0.00");
		struct.put("buyer_memo", orderInfo.get("remark"));
		struct.put("trade_memo", packDesc(orderInfo));

		if (!distributor.isEmpty()) {
			Map<String, Object> o2o = new LinkedHashMap<>();
			o2o.put("o2o_store_bn", distributor.get("shop_code"));
			o2o.put("o2o_store_name", distributor.get("name"));
			struct.put("o2o_info", writeJson(o2o));
		}

		if ("ziti".equals(receiptType)) {
			struct.put("receiver_name", "");
			struct.put("receiver_mobile", "");
			struct.put("receiver_state", "");
			struct.put("receiver_city", "");
			struct.put("receiver_district", "");
			struct.put("receiver_address", "");
			struct.put("receiver_zip", "");
			struct.put("delivery_corp", "");
			struct.put("shipping_type", "STORE_SELF_FETCH");
		}
		if ("dada".equals(receiptType)) {
			struct.put("shipping_type", DADA_SHIPPING_TYPE);
		}
		if ("dhpoint".equals(text(tradeInfo.get("payType")))) {
			String tradeFee =
					bcdiv100(toCents(orderInfo.get("item_fee")).add(toCents(orderInfo.get("freight_fee"))));
			struct.put("total_trade_fee", tradeFee);
			struct.put("payed_fee", tradeFee);
		}

		struct.keySet().removeIf(key -> !ALLOW_PARAMS.contains(key));
		fixPayed(struct);
		return Optional.of(struct);
	}

	private Map<String, Object> readSendOmsSetting(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get("sendOmsSetting:" + companyId);
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}
		try {
			Object decoded = objectMapper.readValue(raw, Object.class);
			return asStringKeyMap(decoded);
		} catch (JsonProcessingException ex) {
			return Map.of();
		}
	}

	private Map<String, Object> formatMemberInfo(Map<String, Object> orderInfo) {
		if (phpInt(orderInfo.get("user_id")) == 0L) {
			Map<String, Object> anonymous = new LinkedHashMap<>();
			anonymous.put("uname", "");
			anonymous.put("tel", "");
			anonymous.put("name", "匿名用户");
			anonymous.put("mobile", "");
			return anonymous;
		}
		long userId = phpInt(orderInfo.get("user_id"));
		long memberCompanyId = phpInt(orderInfo.get("company_id"));
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getUserId, userId)
								.eq(Members::getCompanyId, memberCompanyId)
								.last("LIMIT 1"));
		if (member == null) {
			return null;
		}
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getUserId, userId)
								.eq(MembersInfo::getCompanyId, memberCompanyId)
								.last("LIMIT 1"));
		Map<String, Object> memberInfo = new LinkedHashMap<>();
		memberInfo.put("uname", member.getMobile());
		memberInfo.put("tel", "");
		memberInfo.put("mobile", member.getMobile());
		if (info != null && info.getUsername() != null) {
			memberInfo.put("name", info.getUsername());
		}
		return memberInfo;
	}

	private static String buyerName(Map<String, Object> memberInfo) {
		if (memberInfo.containsKey("name") && memberInfo.get("name") != null) {
			return String.valueOf(memberInfo.get("name")).trim();
		}
		Object uname = memberInfo.get("uname");
		return uname == null ? "" : String.valueOf(uname);
	}

	private Object formatOrders(Map<String, Object> orderInfo, String status, Map<String, Object> distributor) {
		List<Map<String, Object>> items = asMapList(orderInfo.get("items"));
		if (items.isEmpty()) {
			return List.of();
		}
		Object storeCode = distributor.containsKey("shop_code") ? distributor.get("shop_code") : null;
		List<Map<String, Object>> lines = new ArrayList<>();
		for (Map<String, Object> value : items) {
			boolean gift = "gift".equals(text(value.get("order_item_type")));
			String sharedDiscount = bcdiv100(toCents(value.get("discount_fee")).add(toCents(value.get("point_fee"))));
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("iid", value.get("item_id"));
			item.put("bn", value.get("item_bn"));
			item.put("price", bcdiv100(value.get("price")));
			item.put("name", removeEmoji(value.get("item_name")));
			item.put("num", value.get("num"));
			item.put("total_item_fee", bcdiv100(value.get("item_fee")));
			item.put("sendnum", "0");
			item.put("item_type", gift ? "gift" : "product");
			item.put("sale_price", bcdiv100(value.get("item_fee")));
			item.put("discount_fee", "0.00");
			item.put("part_mjz_discount", sharedDiscount);
			item.put("score", value.get("item_point"));
			item.put("item_status", "normal");
			item.put("divide_order_fee", bcdiv100(value.get("total_fee")));

			Map<String, Object> itemWrap = new LinkedHashMap<>();
			itemWrap.put("item", List.of(item));

			Map<String, Object> line = new LinkedHashMap<>();
			line.put("iid", value.get("item_id"));
			line.put("title", removeEmoji(value.get("item_name")));
			line.put("bn", value.get("item_bn"));
			line.put("orders_bn", value.get("item_bn"));
			line.put("items_num", value.get("num"));
			line.put("total_order_fee", bcdiv100(value.get("total_fee")));
			line.put("oid", value.get("order_id"));
			line.put("status", status);
			line.put("sale_price", bcdiv100(value.get("item_fee")));
			line.put("discount_fee", sharedDiscount);
			line.put("type", gift ? "gift" : "goods");
			line.put("store_code", storeCode);
			line.put("order_items", itemWrap);
			line.put("part_mjz_discount", sharedDiscount);
			line.put("divide_order_fee", bcdiv100(value.get("total_fee")));
			lines.add(line);
		}
		Map<String, Object> orders = new LinkedHashMap<>();
		orders.put("order", lines);
		return orders;
	}

	private Object formatPaymentLists(Map<String, Object> tradeInfo) {
		if (tradeInfo.isEmpty()) {
			return List.of();
		}
		String payType = text(tradeInfo.get("payType"));
		String payLabel = resolveOmsPaymentDisplayLabel(payType);
		boolean point = "point".equals(payType);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("tid", tradeInfo.get("orderId"));
		row.put("payment_id", tradeInfo.get("tradeId"));
		row.put("seller_bank", payLabel);
		row.put("seller_account", tradeInfo.get("openId"));
		row.put("buyer_account", tradeInfo.get("mchId"));
		row.put("currency", "CNY");
		row.put("paycost", "0.000");
		row.put("pay_type", "online");
		row.put("payment_name", payLabel);
		row.put("payment_code", tradeInfo.get("payType"));
		row.put("t_begin", phpDate(tradeInfo.get("timeStart")));
		row.put("t_end", phpDate(tradeInfo.get("timeExpire")));
		row.put("status", "SUCC");
		row.put("memo", "");
		row.put("outer_no", tradeInfo.get("transactionId"));
		row.put("pay_fee", bcdiv100(point ? 0 : tradeInfo.get("payFee")));
		row.put(
				"currency_fee",
				phpTruthy(tradeInfo.get("curPayFee"))
						? bcdiv100(point ? 0 : tradeInfo.get("curPayFee"))
						: "");
		Map<String, Object> lists = new LinkedHashMap<>();
		lists.put("payment_list", List.of(row));
		return lists;
	}

	private List<Map<String, Object>> pmtDetail(Map<String, Object> orderInfo) {
		Object raw = orderInfo.get("discount_info");
		if (!phpTruthy(raw)) {
			return List.of();
		}
		List<Map<String, Object>> details = new ArrayList<>();
		for (Object val : asObjectList(raw)) {
			if (!phpTruthy(val) || !(val instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> pmt = new LinkedHashMap<>();
			pmt.put("pmt_amount", bcdiv100(row.get("discount_fee")));
			pmt.put("pmt_id", orderInfo.get("order_id"));
			pmt.put("pmt_describe", row.get("rule"));
			details.add(pmt);
		}
		return details;
	}

	private Map<String, Object> taxInfo(Map<String, Object> orderInfo) {
		Map<String, Object> invoice = invoiceMap(orderInfo.get("invoice"));
		boolean hasContent = invoice.containsKey("content") && invoice.get("content") != null;
		Map<String, Object> tax = new LinkedHashMap<>();
		tax.put("title", hasContent ? String.valueOf(invoice.get("content")).trim() : "");
		tax.put("is_tax", hasContent ? 1 : 0);
		tax.put("cost_tax", "0.00");
		return tax;
	}

	private Object packDesc(Map<String, Object> orderInfo) {
		Object packRaw = orderInfo.get("pack");
		if (!phpTruthy(packRaw)) {
			return "";
		}
		Map<String, Object> pack = decodeObject(packRaw);
		if (pack == null) {
			return "";
		}
		return pack.containsKey("packDes") ? pack.get("packDes") : 0;
	}

	private static void fixPayed(Map<String, Object> struct) {
		if (phpFloat(struct.get("payed_fee")) > phpFloat(struct.get("total_trade_fee"))) {
			struct.put("payed_fee", struct.get("total_trade_fee"));
		}
	}

	private static boolean shouldSkipOmsPushForPayType(String payType) {
		return payType != null && !payType.isEmpty() && OMS_PUSH_SKIP_PAY_TYPES.contains(payType);
	}

	private static String resolveOmsPaymentDisplayLabel(String payType) {
		if (payType == null || payType.isEmpty()) {
			return "";
		}
		return PAYMENT_LABELS.getOrDefault(payType, payType);
	}

	/** PHP {@code $trade[$key] ?? $fallback}. Empty string is kept; only null or a missing key falls through. */
	private static Object nullCoalesce(Map<String, Object> map, String key, Object fallback) {
		if (!map.containsKey(key) || map.get(key) == null) {
			return fallback;
		}
		return map.get(key);
	}

	private Map<String, Object> invoiceMap(Object invoice) {
		if (invoice instanceof Map<?, ?>) {
			return asStringKeyMap(invoice);
		}
		Map<String, Object> decoded = decodeObject(invoice);
		return decoded == null ? Map.of() : decoded;
	}

	private List<Map<String, Object>> asMapList(Object raw) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object row : asObjectList(raw)) {
			if (row instanceof Map<?, ?> map) {
				out.add(asStringKeyMap(map));
			}
		}
		return out;
	}

	private List<Object> asObjectList(Object raw) {
		Object value = raw;
		if (value instanceof String text) {
			if (text.isBlank()) {
				return List.of();
			}
			try {
				value = objectMapper.readValue(text, Object.class);
			} catch (JsonProcessingException ex) {
				return List.of();
			}
		}
		if (value instanceof Collection<?> collection) {
			return new ArrayList<>(collection);
		}
		if (value instanceof Map<?, ?> map) {
			return new ArrayList<>(map.values());
		}
		return List.of();
	}

	private Map<String, Object> decodeObject(Object raw) {
		if (raw instanceof Map<?, ?> map) {
			return asStringKeyMap(map);
		}
		if (!(raw instanceof String text) || text.isBlank()) {
			return null;
		}
		try {
			Object decoded = objectMapper.readValue(text, Object.class);
			return decoded instanceof Map<?, ?> map ? asStringKeyMap(map) : null;
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to encode SaaS ERP order JSON", ex);
		}
	}

	private static Map<String, Object> asStringKeyMap(Object value) {
		if (!(value instanceof Map<?, ?> map)) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		map.forEach((key, item) -> out.put(String.valueOf(key), item));
		return out;
	}

	private static String bcdiv100(Object cents) {
		return toCents(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static BigDecimal toCents(Object value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			if (number instanceof Double || number instanceof Float) {
				return new BigDecimal(number.toString());
			}
			return BigDecimal.valueOf(number.longValue());
		}
		String text = String.valueOf(value).trim();
		if (text.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(text);
	}

	private static boolean phpTruthy(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof Number number) {
			return number.doubleValue() != 0.0d;
		}
		if (value instanceof CharSequence text) {
			String s = text.toString();
			return !s.isEmpty() && !"0".equals(s);
		}
		if (value instanceof Collection<?> collection) {
			return !collection.isEmpty();
		}
		if (value instanceof Map<?, ?> map) {
			return !map.isEmpty();
		}
		return true;
	}

	private static long phpInt(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		String text = String.valueOf(value).trim();
		if (text.isEmpty()) {
			return 0L;
		}
		int end = 0;
		if (text.charAt(0) == '+' || text.charAt(0) == '-') {
			end = 1;
		}
		int digits = end;
		while (end < text.length() && Character.isDigit(text.charAt(end))) {
			end++;
		}
		if (end == digits) {
			return 0L;
		}
		try {
			return Long.parseLong(text.substring(0, end));
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static double phpFloat(Object value) {
		if (value == null) {
			return 0.0d;
		}
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value).trim());
		} catch (NumberFormatException ex) {
			return 0.0d;
		}
	}

	private static String phpDate(Object epochSeconds) {
		long epoch = phpInt(epochSeconds);
		try {
			return PHP_DATE.format(Instant.ofEpochSecond(epoch));
		} catch (DateTimeException ex) {
			return PHP_DATE.format(Instant.EPOCH);
		}
	}

	private static String removeEmoji(Object value) {
		if (value == null) {
			return "";
		}
		String text = String.valueOf(value);
		StringBuilder kept = new StringBuilder();
		text.codePoints().forEach(codePoint -> {
			if (codePoint <= 0xFFFF) {
				kept.appendCodePoint(codePoint);
			}
		});
		return kept.toString();
	}

	private static String text(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static Map<String, String> paymentLabels() {
		Map<String, String> labels = new LinkedHashMap<>();
		labels.put("amorepay", "微信支付(amorepay)");
		labels.put("wxpay", "微信支付");
		labels.put("wxpayh5", "微信支付");
		labels.put("wxpayjs", "微信支付");
		labels.put("wxpayapp", "微信支付");
		labels.put("wxpaypos", "微信条码支付");
		labels.put("wxpaypc", "微信PC支付");
		labels.put("pos", "刷卡");
		labels.put("point", "积分");
		labels.put("dhpoint", "积分");
		labels.put("deposit", "预存款支付");
		labels.put("alipay", "支付宝支付");
		labels.put("alipayh5", "支付宝支付");
		labels.put("alipayapp", "支付宝支付");
		labels.put("alipaypos", "支付宝条码支付");
		labels.put("alipaymini", "支付宝小程序");
		labels.put("hfpay", "汇付支付");
		return Map.copyOf(labels);
	}
}

interface OrdersSaasErpOrderBundleReader {

	Optional<Map<String, Object>> read(long companyId, String orderId, String sourceType);
}

@Service
class OrdersSaasErpOrderBundleLoader implements OrdersSaasErpOrderBundleReader {

	private static final String PLATFORM_SELF_OPERATED = "平台自营";

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final ObjectMapper objectMapper = new ObjectMapper();

	OrdersSaasErpOrderBundleLoader(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
	}

	@Override
	public Optional<Map<String, Object>> read(long companyId, String orderId, String sourceType) {
		if (sourceType == null || sourceType.isBlank()) {
			return Optional.empty();
		}
		Long parsedOrderId = parseOrderId(orderId);
		if (parsedOrderId == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<NormalOrders> query = new LambdaQueryWrapper<>();
		query.eq(NormalOrders::getOrderId, parsedOrderId);
		if (companyId > 0) {
			query.eq(NormalOrders::getCompanyId, companyId);
		}
		query.last("LIMIT 1");
		NormalOrders order = normalOrdersMapper.selectOne(query);
		if (order == null) {
			return Optional.empty();
		}
		long effectiveCompanyId = companyId > 0 ? companyId : (order.getCompanyId() == null ? 0L : order.getCompanyId());
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, effectiveCompanyId)
								.eq(NormalOrdersItems::getOrderId, parsedOrderId));
		if (items == null) {
			items = List.of();
		}

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", order.getOrderId());
		orderInfo.put("company_id", order.getCompanyId());
		orderInfo.put("user_id", order.getUserId());
		orderInfo.put("order_status", displayedStatus(order));
		orderInfo.put("receipt_type", order.getReceiptType());
		orderInfo.put("ziti_status", order.getZitiStatus());
		orderInfo.put("delivery_status", order.getDeliveryStatus());
		orderInfo.put("pay_type", order.getPayType());
		orderInfo.put("update_time", order.getUpdateTime());
		orderInfo.put("create_time", order.getCreateTime());
		orderInfo.put("item_fee", order.getItemFee());
		orderInfo.put("total_fee", order.getTotalFee());
		orderInfo.put("freight_fee", order.getFreightFee());
		orderInfo.put("fee_rate", order.getFeeRate());
		orderInfo.put("discount_fee", displayedDiscountFee(order));
		orderInfo.put("point_fee", order.getPointFee() == null ? 0 : order.getPointFee());
		orderInfo.put("receiver_name", order.getReceiverName());
		orderInfo.put("receiver_mobile", order.getReceiverMobile());
		orderInfo.put("receiver_state", order.getReceiverState());
		orderInfo.put("receiver_city", order.getReceiverCity());
		orderInfo.put("receiver_district", order.getReceiverDistrict());
		orderInfo.put("receiver_address", order.getReceiverAddress());
		orderInfo.put("receiver_zip", order.getReceiverZip());
		orderInfo.put("delivery_corp", resolveDeliveryCorp(order, items, parsedOrderId));
		orderInfo.put("remark", order.getRemark());
		orderInfo.put("pack", order.getPack());
		orderInfo.put("invoice", order.getInvoice());
		orderInfo.put("discount_info", discountInfo(order));
		orderInfo.put("items", itemRows(items));

		Map<String, Object> bundle = new LinkedHashMap<>();
		bundle.put("orderInfo", orderInfo);
		bundle.put("tradeInfo", tradeInfo(effectiveCompanyId, parsedOrderId));
		bundle.put("distributor", distributor(effectiveCompanyId, order.getDistributorId()));
		return Optional.of(bundle);
	}

	private static String displayedStatus(NormalOrders order) {
		String status = order.getOrderStatus();
		if (!"NOTPAY".equals(status) || "drug".equals(order.getOrderClass())) {
			return status;
		}
		long autoCancel = phpInt(order.getAutoCancelTime());
		long now = System.currentTimeMillis() / 1000L;
		if (autoCancel - now > 0) {
			return status;
		}
		String payType = order.getPayType() == null ? "" : order.getPayType();
		int offline = order.getOfflinePaymentStatus() == null ? -1 : order.getOfflinePaymentStatus();
		if (!"offline_pay".equals(payType) || offline == -1) {
			return "CANCEL";
		}
		if (offline != 0) {
			return "CANCEL";
		}
		return status;
	}

	private static int displayedDiscountFee(NormalOrders order) {
		int discountFee = order.getDiscountFee() == null ? 0 : order.getDiscountFee();
		if (discountFee != 0) {
			return discountFee;
		}
		int member = order.getMemberDiscount() == null ? 0 : order.getMemberDiscount();
		int coupon = order.getCouponDiscount() == null ? 0 : order.getCouponDiscount();
		return member + coupon;
	}

	private List<Object> discountInfo(NormalOrders order) {
		if (phpTruthy(order.getDiscountInfo())) {
			List<Object> rows = new ArrayList<>();
			for (Object row : readList(order.getDiscountInfo())) {
				if (phpTruthy(row)) {
					rows.add(row);
				}
			}
			return rows;
		}
		List<Object> rows = new ArrayList<>();
		appendDiscountDesc(rows, order.getCouponDiscountDesc(), "coupon_discount");
		appendDiscountDesc(rows, order.getMemberDiscountDesc(), "member_discount");
		return rows;
	}

	private void appendDiscountDesc(List<Object> rows, String raw, String type) {
		if (!phpTruthy(raw)) {
			return;
		}
		try {
			Object decoded = objectMapper.readValue(raw, Object.class);
			if (!(decoded instanceof Map<?, ?> map)) {
				return;
			}
			Map<String, Object> copy = new LinkedHashMap<>();
			map.forEach((key, value) -> copy.put(String.valueOf(key), value));
			copy.put("type", type);
			rows.add(copy);
		} catch (JsonProcessingException ex) {
			// PHP json_decode failure does not add a promotion row.
		}
	}

	private List<Object> readList(String raw) {
		try {
			Object decoded = objectMapper.readValue(raw, Object.class);
			if (decoded instanceof Collection<?> collection) {
				return new ArrayList<>(collection);
			}
			if (decoded instanceof Map<?, ?> map) {
				return new ArrayList<>(map.values());
			}
		} catch (JsonProcessingException ex) {
			return List.of();
		}
		return List.of();
	}

	private String resolveDeliveryCorp(NormalOrders order, List<NormalOrdersItems> items, long orderId) {
		boolean oldDelivery = phpTruthy(order.getDeliveryCode());
		if (!oldDelivery) {
			for (NormalOrdersItems item : items) {
				if (phpTruthy(item.getDeliveryCode())) {
					oldDelivery = true;
					break;
				}
			}
		}
		if (oldDelivery) {
			return order.getDeliveryCorp();
		}
		OrdersDelivery delivery =
				ordersDeliveryMapper.selectOne(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getOrderId, orderId)
								.last("LIMIT 1"));
		if (delivery == null || delivery.getDeliveryCorp() == null) {
			return "";
		}
		return delivery.getDeliveryCorp();
	}

	private static List<Map<String, Object>> itemRows(List<NormalOrdersItems> items) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (NormalOrdersItems item : items) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", item.getItemId());
			row.put("item_bn", item.getItemBn());
			row.put("price", item.getPrice());
			row.put("item_name", item.getItemName());
			row.put("num", item.getNum());
			row.put("item_fee", item.getItemFee());
			row.put("order_item_type", item.getOrderItemType());
			row.put("discount_fee", item.getDiscountFee());
			row.put("point_fee", item.getPointFee() == null ? 0 : item.getPointFee());
			row.put("item_point", item.getItemPoint() == null ? 0 : item.getItemPoint());
			row.put("total_fee", item.getTotalFee());
			row.put("order_id", item.getOrderId());
			row.put("delivery_code", item.getDeliveryCode());
			rows.add(row);
		}
		return rows;
	}

	private Map<String, Object> tradeInfo(long companyId, long orderId) {
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.orderByDesc(Trade::getTimeStart)
								.last("LIMIT 1"));
		if (trade == null) {
			return Map.of();
		}
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("payType", trade.getPayType());
		info.put("payFee", trade.getPayFee());
		info.put("orderId", trade.getOrderId());
		info.put("tradeId", trade.getTradeId());
		info.put("openId", trade.getOpenId());
		info.put("mchId", trade.getMchId());
		info.put("timeStart", trade.getTimeStart());
		info.put("timeExpire", trade.getTimeExpire());
		info.put("transactionId", trade.getTransactionId());
		info.put("curPayFee", trade.getCurPayFee());
		return info;
	}

	private Map<String, Object> distributor(long companyId, Long distributorId) {
		long id = distributorId == null ? 0L : distributorId;
		if (id > 0L) {
			DistributionDistributorPeek row =
					distributionDistributorPeekMapper.selectOne(
							new LambdaQueryWrapper<DistributionDistributorPeek>()
									.eq(DistributionDistributorPeek::getCompanyId, companyId)
									.eq(DistributionDistributorPeek::getDistributorId, id)
									.last("LIMIT 1"));
			if (row == null) {
				return Map.of();
			}
			Map<String, Object> distributor = new LinkedHashMap<>();
			distributor.put("shop_code", row.getShopCode());
			distributor.put("name", row.getName());
			return distributor;
		}
		DistributionDistributorPeek self =
				distributionDistributorPeekMapper.selectOne(
						new LambdaQueryWrapper<DistributionDistributorPeek>()
								.eq(DistributionDistributorPeek::getCompanyId, companyId)
								.eq(DistributionDistributorPeek::getDistributorSelf, 1)
								.last("LIMIT 1"));
		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("shop_code", self == null || self.getShopCode() == null ? "" : self.getShopCode());
		distributor.put("name", self == null || self.getName() == null ? PLATFORM_SELF_OPERATED : self.getName());
		return distributor;
	}

	private static Long parseOrderId(String orderId) {
		if (orderId == null || orderId.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(orderId);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean phpTruthy(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof Number number) {
			return number.doubleValue() != 0.0d;
		}
		if (value instanceof CharSequence text) {
			String s = text.toString();
			return !s.isEmpty() && !"0".equals(s);
		}
		return true;
	}

	private static long phpInt(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		String text = String.valueOf(value).trim();
		if (text.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
