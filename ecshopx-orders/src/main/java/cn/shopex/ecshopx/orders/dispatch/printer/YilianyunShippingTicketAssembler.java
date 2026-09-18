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

package cn.shopex.ecshopx.orders.dispatch.printer;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public class YilianyunShippingTicketAssembler {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Map<String, String> PAY_TYPE_LABELS =
			Map.ofEntries(
					Map.entry("wxpay", "微信支付"),
					Map.entry("wxpaypc", "微信支付"),
					Map.entry("wxpayh5", "微信支付"),
					Map.entry("wxpayjs", "微信支付"),
					Map.entry("wxpayapp", "微信支付"),
					Map.entry("wxpaypos", "微信支付"),
					Map.entry("hfpay", "微信支付"),
					Map.entry("adapay", "微信支付"),
					Map.entry("amorepay", "微信支付"),
					Map.entry("wechat", "微信支付"),
					Map.entry("alipay", "支付宝"),
					Map.entry("alipayh5", "支付宝"),
					Map.entry("alipayapp", "支付宝"),
					Map.entry("alipaypos", "支付宝"),
					Map.entry("alipaymini", "支付宝"),
					Map.entry("point", "积分支付"),
					Map.entry("deposit", "余额支付"),
					Map.entry("pos", "现金支付"),
					Map.entry("offline_pay", "线下支付"),
					Map.entry("offline", "线下转账"),
					Map.entry("chinaums", "微信支付-银联"),
					Map.entry("localpay", "零元订单"));

	public YilianyunShippingTicketModel assemble(
			OrderAssociations assoc,
			NormalOrders order,
			List<NormalOrdersItems> items,
			NormalOrdersRelZiti ziti,
			Map<String, Object> distributor,
			Operators operator,
			Long payFeeFen,
			String payTypeRaw,
			boolean hideReceiver) {
		Map<String, Object> dist = distributor == null ? Map.of() : distributor;
		String distStoreName = firstText(dist.get("store_name"), dist.get("name"));
		String storeName = firstText(assoc == null ? null : assoc.getStoreName(), distStoreName);
		String orderStoreName = firstText(distStoreName, storeName);
		Long orderId = order != null && order.getOrderId() != null
				? order.getOrderId()
				: assoc != null ? assoc.getOrderId() : null;

		String receiverName = order == null ? "" : text(order.getReceiverName());
		if (hideReceiver && StringUtils.hasText(receiverName)) {
			receiverName = maskName(receiverName);
		}
		String receiverMobile =
				order == null
						? ""
						: firstText(order.getReceiverMobile(), order.getMobile());
		String receiverAddress = buildReceiverAddress(order, ziti);
		String deliveryTime = buildDeliveryTime(order, ziti);

		return new YilianyunShippingTicketModel(
				storeName,
				orderId == null ? "" : String.valueOf(orderId),
				formatOrderTime(order, assoc),
				orderStoreName,
				operatorName(operator),
				mapItems(items),
				fenToYuan(payFeeFen),
				payTypeLabel(firstText(payTypeRaw, order == null ? null : order.getPayType())),
				receiverName,
				receiverMobile,
				receiverAddress,
				deliveryTime,
				order == null ? "" : text(order.getRemark()),
				firstText(dist.get("mobile"), dist.get("phone"), dist.get("contract_phone")),
				firstText(dist.get("store_address")));
	}

	private static List<YilianyunShippingTicketModel.Item> mapItems(List<NormalOrdersItems> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		List<YilianyunShippingTicketModel.Item> out = new ArrayList<>(items.size());
		for (NormalOrdersItems item : items) {
			if (item == null) {
				continue;
			}
			out.add(
					new YilianyunShippingTicketModel.Item(
							displayName(item.getItemName(), item.getItemSpecDesc()),
							fenToYuan(item.getPrice()),
							quantityText(item.getItemUnit(), item.getNum()),
							fenToYuan(item.getItemFee())));
		}
		return out;
	}

	static String displayName(String name, String spec) {
		String n = text(name);
		String s = text(spec);
		if (!StringUtils.hasText(s) || "单规格".equals(s) || "[]".equals(s) || "{}".equals(s)) {
			return n;
		}
		List<String> values = new ArrayList<>();
		for (String part : s.split("[,，]")) {
			String p = text(part);
			if (!StringUtils.hasText(p) || "单规格".equals(p)) {
				continue;
			}
			int colon = p.indexOf(':');
			if (colon < 0) {
				colon = p.indexOf('：');
			}
			String value = colon >= 0 ? text(p.substring(colon + 1)) : p;
			if (StringUtils.hasText(value)) {
				values.add(value);
			}
		}
		if (values.isEmpty()) {
			return n;
		}
		return n + "(" + String.join("、", values) + ")";
	}

	static String quantityText(String unit, Integer num) {
		int n = num == null ? 0 : num;
		String u = text(unit);
		if (!StringUtils.hasText(u)) {
			return "x" + n;
		}
		return "1" + u + " * " + n;
	}

	static String payTypeLabel(String payType) {
		String key = text(payType).toLowerCase();
		if (!StringUtils.hasText(key)) {
			return "";
		}
		return PAY_TYPE_LABELS.getOrDefault(key, text(payType));
	}

	static String fenToYuan(Number fenRaw) {
		long fen = fenRaw == null ? 0L : fenRaw.longValue();
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	static String maskName(String name) {
		String n = text(name);
		if (n.length() <= 1) {
			return "*";
		}
		return n.charAt(0) + "*".repeat(n.length() - 1);
	}

	private static String formatOrderTime(NormalOrders order, OrderAssociations assoc) {
		Integer epoch = order != null ? order.getCreateTime() : null;
		if (epoch == null && assoc != null) {
			epoch = assoc.getCreateTime();
		}
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI)
				.format(DATE_TIME);
	}

	private static String buildReceiverAddress(NormalOrders order, NormalOrdersRelZiti ziti) {
		if (order != null && "ziti".equalsIgnoreCase(text(order.getReceiptType())) && ziti != null) {
			return concat(
					ziti.getProvince(),
					ziti.getCity(),
					ziti.getArea(),
					ziti.getAddress());
		}
		if (order == null) {
			return "";
		}
		return concat(
				order.getReceiverState(),
				order.getReceiverCity(),
				order.getReceiverDistrict(),
				order.getReceiverAddress());
	}

	private static String buildDeliveryTime(NormalOrders order, NormalOrdersRelZiti ziti) {
		if (order != null && "ziti".equalsIgnoreCase(text(order.getReceiptType())) && ziti != null) {
			return joinWith(" ", ziti.getPickupDate(), ziti.getPickupTime());
		}
		if (order != null
				&& "merchant".equalsIgnoreCase(text(order.getReceiptType()))
				&& order.getSelfDeliveryTime() != null
				&& order.getSelfDeliveryTime() > 0) {
			return LocalDateTime.ofInstant(
							Instant.ofEpochSecond(order.getSelfDeliveryTime().longValue()), SHANGHAI)
					.format(DATE_TIME);
		}
		return "";
	}

	private static String operatorName(Operators operator) {
		if (operator == null) {
			return "";
		}
		return firstText(operator.getUsername(), operator.getLoginName());
	}

	private static String concat(String... parts) {
		return joinWith("", parts);
	}

	private static String joinWith(String sep, String... parts) {
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			String p = text(part);
			if (!StringUtils.hasText(p)) {
				continue;
			}
			if (sb.length() > 0 && sep != null && !sep.isEmpty()) {
				sb.append(sep);
			}
			sb.append(p);
		}
		return sb.toString();
	}

	private static String firstText(Object... values) {
		if (values == null) {
			return "";
		}
		for (Object value : values) {
			String t = text(value);
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		return "";
	}

	private static String text(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
