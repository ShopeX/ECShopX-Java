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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderInvoiceDetailService {

	private static final Logger log = LoggerFactory.getLogger(OrderInvoiceDetailService.class);

	private static final Set<String> FREIGHT_ITEM_BNS = Set.of("shippingFeeLine", "shippingFeeLine888");

	private static final String DEFAULT_SHIIP_INVOICE_MAIN_IMG =
			"https://b-img-cdn.yuanyuanke.cn/ecshopx-vshop/fv_freight.png";

	@Value("${SHIIP_INVOICE_MAIN_IMG:}")
	private String shipInvoiceMainImgFromEnv;

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceItemMapper orderInvoiceItemMapper;
	private final MembersMapper membersMapper;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final ObjectMapper objectMapper;

	public OrderInvoiceDetailService(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceItemMapper orderInvoiceItemMapper,
			MembersMapper membersMapper,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			ObjectMapper objectMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceItemMapper = orderInvoiceItemMapper;
		this.membersMapper = membersMapper;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInvoiceDetail(long companyId, String idRaw) {
		String trimmed = idRaw == null ? "" : idRaw.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		final long invoiceId;
		try {
			invoiceId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		OrderInvoice inv =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.eq(OrderInvoice::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (inv == null) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		LinkedHashMap<String, Object> result = OrderInvoiceApiRowSupport.toColumnNamesData(inv);

		Members m = inv.getUserId() == null ? null : membersMapper.selectById(inv.getUserId());
		result.put("user_mobile", m == null || m.getMobile() == null ? "" : m.getMobile());
		result.put("user_card_code", m == null || m.getUserCardCode() == null ? "" : m.getUserCardCode());

		List<OrderInvoiceItem> itemRows =
				orderInvoiceItemMapper.selectList(
						new LambdaQueryWrapper<OrderInvoiceItem>()
								.eq(OrderInvoiceItem::getInvoiceId, invoiceId)
								.eq(OrderInvoiceItem::getCompanyId, companyId)
								.orderByAsc(OrderInvoiceItem::getId));

		List<Map<String, Object>> invoiceItems = new ArrayList<>();
		for (OrderInvoiceItem row : itemRows) {
			invoiceItems.add(OrderInvoiceItemApiRowSupport.toRow(row));
		}

		String orderIdForBundle = inv.getOrderId() == null ? "" : inv.getOrderId().trim();
		if (!StringUtils.hasText(orderIdForBundle)) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		}
		try {
			Long.parseLong(orderIdForBundle);
		} catch (NumberFormatException e) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		}

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdForBundle, false);
		} catch (BadRequestException e) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		} catch (ResourceException e) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		if (orderInfo == null) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		}
		Object itemsObj = orderInfo.get("items");
		List<?> itemsList = itemsObj instanceof List<?> ? (List<?>) itemsObj : Collections.emptyList();
		Map<String, Map<String, Object>> byOid = new LinkedHashMap<>();
		for (Object o : itemsList) {
			if (!(o instanceof Map<?, ?> om)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> orderRow = (Map<String, Object>) om;
			Object idVal = orderRow.get("id");
			if (idVal != null) {
				byOid.put(String.valueOf(idVal), orderRow);
			}
		}

		String freightImg = resolveFreightInvoiceMainImg();
		for (Map<String, Object> line : invoiceItems) {
			Object oidObj = line.get("oid");
			Map<String, Object> orderRow = oidObj == null ? null : byOid.get(String.valueOf(oidObj));
			int itemId = orderRow == null ? 0 : intVal(orderRow.get("item_id"));
			line.put("item_id", itemId);
			String mainImg = orderRow == null ? "" : nullToEmpty(orderRow.get("pic"));
			line.put("main_img", mainImg);
			String specDesc = orderRow == null ? "" : nullToEmpty(orderRow.get("item_spec_desc"));
			line.put("item_spec_desc", specDesc);
			String itemBn = line.get("item_bn") == null ? "" : String.valueOf(line.get("item_bn"));
			if (FREIGHT_ITEM_BNS.contains(itemBn)) {
				line.put("main_img", freightImg);
			}
		}

		String orderIdRawForRefund = inv.getOrderId() == null ? "" : inv.getOrderId().trim();
		if (!StringUtils.hasText(orderIdRawForRefund) || "0".equals(orderIdRawForRefund)) {
			result.put("refundDetail", new ArrayList<>());
		} else {
			result.put("refundDetail", buildRefundDetailForInvoice(companyId, orderIdRawForRefund));
		}

		boolean pending = "pending".equals(inv.getInvoiceStatus());
		if (pending) {
			applyPendingRefundRecalc(result, invoiceItems, inv);
		} else {
			for (Map<String, Object> line : invoiceItems) {
				line.put("refund_fee", 0);
				line.put("refund_num", 0);
			}
			result.put("refund_fee", 0);
		}

		List<Map<String, Object>> invoices = buildInvoicesList(inv);
		result.put("invoices", invoices);
		result.put("invoice_items", invoiceItems);

		log.debug(
				"invoice detail id={} items={} invoices={}",
				invoiceId,
				invoiceItems.size(),
				invoices.size());

		return result;
	}

	public Map<String, Object> getUserInvoiceDetail(long companyId, String idRaw) {
		return getInvoiceDetail(companyId, idRaw);
	}

	private void applyPendingRefundRecalc(
			LinkedHashMap<String, Object> result,
			List<Map<String, Object>> invoiceItems,
			OrderInvoice inv) {
		Object rd = result.get("refundDetail");
		Map<?, ?> itemRefundFee = null;
		if (rd instanceof Map<?, ?> rdMap) {
			Object irf = rdMap.get("itemRefundFee");
			if (irf instanceof Map<?, ?> m) {
				itemRefundFee = m;
			}
		}

		int invoiceAmountSum = 0;
		int ticketRefundSum = 0;
		for (Map<String, Object> row : invoiceItems) {
			int oa = firstNonNullInt(row.get("original_amount"), row.get("amount"), 0);
			int on = firstNonNullInt(row.get("original_num"), row.get("num"), 0);
			Object itemIdObj = row.get("item_id");
			String itemIdKey = itemIdObj == null ? "0" : String.valueOf(itemIdObj);
			boolean hasFee =
					itemRefundFee != null
							&& itemRefundFee.containsKey(itemIdKey)
							&& itemRefundFee.get(itemIdKey) != null;
			if (hasFee) {
				Object innerObj = itemRefundFee.get(itemIdKey);
				@SuppressWarnings("unchecked")
				Map<String, Object> inner =
						innerObj instanceof Map<?, ?> im ? (Map<String, Object>) im : Collections.emptyMap();
				int rf = intVal(inner.get("refund_fee"));
				int rn = intVal(inner.get("num"));
				row.put("refund_fee", rf);
				row.put("refund_num", rn);
				int newAmount = oa - rf;
				row.put("amount", newAmount);
				int newNum = on - rn;
				row.put("num", newNum);
				invoiceAmountSum += newAmount;
				ticketRefundSum += rf;
			} else {
				row.put("refund_fee", 0);
				row.put("refund_num", 0);
				row.put("amount", oa);
				row.put("num", on);
				invoiceAmountSum += oa;
			}
		}
		result.put("invoice_amount", invoiceAmountSum);
		result.put("refund_fee", ticketRefundSum);
	}

	private List<Map<String, Object>> buildInvoicesList(OrderInvoice inv) {
		List<Map<String, Object>> out = new ArrayList<>();
		appendInvoiceSide(out, inv.getQueryContent(), true);
		appendInvoiceSide(out, inv.getRedContent(), false);
		return out;
	}

	private void appendInvoiceSide(List<Map<String, Object>> out, String raw, boolean blue) {
		if (!StringUtils.hasText(raw)) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(raw.trim());
			JsonNode objNode = resolveRootObjectNode(root);
			if (objNode == null || !objNode.isObject()) {
				return;
			}
			Map<String, Object> one = blue ? mapBlueInvoice(objNode) : mapRedInvoice(objNode);
			if (!one.isEmpty()) {
				out.add(one);
			}
		} catch (Exception ignored) {
			// skip malformed JSON
		}
	}

	private JsonNode resolveRootObjectNode(JsonNode root) {
		if (root == null || root.isNull()) {
			return null;
		}
		if (root.isObject()) {
			return root;
		}
		if (root.isArray() && root.size() > 0) {
			JsonNode first = root.get(0);
			if (first != null && first.isObject()) {
				return first;
			}
			log.debug("invoice content root is array without object element");
			return null;
		}
		return null;
	}

	private Map<String, Object> mapBlueInvoice(JsonNode n) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("invoice_type", "blue");
		Object invoiceNo = jsonScalar(n, "invoiceNo", "invoice_no");
		m.put("invoice_no", invoiceNo != null ? invoiceNo : "");
		String invoiceCode = jsonText(n, "invoiceCode", "invoice_code");
		String serialNo = jsonText(n, "serialNo", "serial_no");
		if (StringUtils.hasText(invoiceCode)) {
			m.put("invoice_code", invoiceCode);
		} else {
			m.put("invoice_code", serialNo != null ? serialNo : "");
		}
		Object invoiceTime = jsonRaw(n, "invoiceTime", "invoice_time");
		Integer ct = epochSeconds(invoiceTime);
		m.put("create_time", ct != null ? ct : 0);
		return m;
	}

	private Map<String, Object> mapRedInvoice(JsonNode n) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("invoice_type", "red");
		Object invoiceNo = jsonScalar(n, "invoiceNo", "invoice_no");
		m.put("invoice_no", invoiceNo != null ? invoiceNo : 0);
		String invoiceCode = jsonText(n, "invoiceCode", "invoice_code");
		if (StringUtils.hasText(invoiceCode)) {
			m.put("invoice_code", invoiceCode);
		} else {
			Object sn = jsonScalar(n, "serialNo", "serial_no");
			m.put("invoice_code", sn != null ? sn : 0);
		}
		Object invoiceTime = jsonRaw(n, "invoiceTime", "invoice_time");
		if (invoiceTime == null || (invoiceTime instanceof JsonNode jn && jn.isNull())) {
			invoiceTime = 0;
		}
		Integer ct = epochSeconds(invoiceTime);
		m.put("create_time", ct != null ? ct : 0);
		return m;
	}

	private static Object jsonScalar(JsonNode n, String camel, String snake) {
		JsonNode j = n.get(camel);
		if (j == null || j.isNull()) {
			j = n.get(snake);
		}
		if (j == null || j.isNull()) {
			return null;
		}
		if (j.isNumber()) {
			return j.numberValue();
		}
		if (j.isTextual()) {
			String t = j.asText();
			try {
				return Long.parseLong(t.trim());
			} catch (NumberFormatException e) {
				return t;
			}
		}
		return j.asText();
	}

	private static JsonNode jsonRaw(JsonNode n, String camel, String snake) {
		JsonNode j = n.get(camel);
		if (j == null || j.isNull()) {
			j = n.get(snake);
		}
		return j;
	}

	private static String jsonText(JsonNode n, String camel, String snake) {
		JsonNode j = jsonRaw(n, camel, snake);
		if (j == null || j.isNull()) {
			return null;
		}
		if (j.isTextual()) {
			return j.asText();
		}
		if (j.isNumber()) {
			return String.valueOf(j.numberValue());
		}
		return j.asText();
	}

	private Integer epochSeconds(Object raw) {
		if (raw instanceof JsonNode jn) {
			if (jn.isNumber()) {
				return (int) jn.longValue();
			}
			if (jn.isTextual()) {
				Long v = DateExpressionParser.parseToEpochSecond(jn.asText(), ZoneId.systemDefault());
				return v != null ? v.intValue() : null;
			}
			return null;
		}
		Long v = DateExpressionParser.parseToEpochSecond(raw, ZoneId.systemDefault());
		return v != null ? v.intValue() : null;
	}

	private Map<String, Object> buildRefundDetailForInvoice(long companyId, String orderIdRaw) {
		try {
			Long orderIdLong;
			try {
				orderIdLong = Long.parseLong(orderIdRaw.trim());
			} catch (NumberFormatException e) {
				return emptyRefundDetailShape();
			}

			List<Aftersales> mains =
					aftersalesMapper.selectList(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.eq(Aftersales::getOrderId, orderIdLong)
									.orderByAsc(Aftersales::getCreateTime));

			LinkedHashMap<String, Map<String, Object>> itemRefundFeeMap = new LinkedHashMap<>();
			LinkedHashMap<String, Map<String, Object>> itemsAftersalesMap = new LinkedHashMap<>();

			for (Aftersales main : mains) {
				if (main.getAftersalesBn() == null) {
					continue;
				}
				int mainStatus = main.getAftersalesStatus() == null ? -1 : main.getAftersalesStatus();
				List<AftersalesDetail> details =
						aftersalesDetailMapper.selectList(
								new LambdaQueryWrapper<AftersalesDetail>()
										.eq(AftersalesDetail::getAftersalesBn, main.getAftersalesBn())
										.eq(AftersalesDetail::getCompanyId, companyId)
										.eq(AftersalesDetail::getUserId, main.getUserId()));

				for (AftersalesDetail detail : details) {
					int itemId = detail.getItemId() == null ? 0 : detail.getItemId().intValue();
					String itemKey = String.valueOf(itemId);
					LinkedHashMap<String, Object> asRow = new LinkedHashMap<>();
					asRow.put("aftersales_bn", String.valueOf(main.getAftersalesBn()));
					asRow.put("aftersales_status", mainStatus);
					asRow.put("create_time", main.getCreateTime() != null ? main.getCreateTime() : 0);
					itemsAftersalesMap.put(itemKey, asRow);

					if (mainStatus == 2) {
						Map<String, Object> agg =
								itemRefundFeeMap.computeIfAbsent(itemKey, k -> new LinkedHashMap<>());
						int prevRf = intVal(agg.get("refund_fee"));
						int prevRp = intVal(agg.get("refund_point"));
						int prevNum = intVal(agg.get("num"));
						int df = detail.getRefundFee() != null ? detail.getRefundFee() : 0;
						int dp = detail.getRefundPoint() != null ? detail.getRefundPoint() : 0;
						int dn = detail.getNum() != null ? detail.getNum() : 0;
						agg.put("refund_fee", prevRf + df);
						agg.put("refund_point", prevRp + dp);
						agg.put("num", prevNum + dn);
					}
				}
			}

			Map<String, Object> out = new LinkedHashMap<>();
			out.put(
					"itemRefundFee",
					itemRefundFeeMap.isEmpty() ? Collections.emptyList() : itemRefundFeeMap);
			out.put(
					"itemsAftersales",
					itemsAftersalesMap.isEmpty() ? Collections.emptyList() : itemsAftersalesMap);
			return out;
		} catch (Exception e) {
			log.error("buildRefundDetailForInvoice failed", e);
			return emptyRefundDetailShape();
		}
	}

	private static Map<String, Object> emptyRefundDetailShape() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("itemRefundFee", Collections.emptyList());
		m.put("itemsAftersales", Collections.emptyList());
		return m;
	}

	private String resolveFreightInvoiceMainImg() {
		if (StringUtils.hasText(shipInvoiceMainImgFromEnv)) {
			return shipInvoiceMainImgFromEnv.trim();
		}
		return DEFAULT_SHIIP_INVOICE_MAIN_IMG;
	}

	private static int firstNonNullInt(Object a, Object b, int d) {
		if (a != null) {
			return intVal(a);
		}
		if (b != null) {
			return intVal(b);
		}
		return d;
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
