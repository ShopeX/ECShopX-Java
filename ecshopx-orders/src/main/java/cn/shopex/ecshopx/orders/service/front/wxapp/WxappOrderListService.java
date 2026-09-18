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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.TradeRateSettingRedisService;
import cn.shopex.ecshopx.orders.service.normal.OrderDiscountInfoSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderListService {

	private static final Set<String> PROMOTION_DISCOUNT_TYPES =
			Set.of("full_minus", "full_discount", "member_tag_targeted_promotion");

	private final WxappOrderListFilterAssembler wxappOrderListFilterAssembler;
	private final WxappOrderListTypeDispatchService wxappOrderListTypeDispatchService;
	private final WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler;
	private final OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService;
	private final TradeRateSettingRedisService tradeRateSettingRedisService;
	private final ObjectMapper objectMapper;
	private final WxappNormalOrderListPhpParityService wxappNormalOrderListPhpParityService;
	private final InvoiceSettingRedisService invoiceSettingRedisService;

	public WxappOrderListService(
			WxappOrderListFilterAssembler wxappOrderListFilterAssembler,
			WxappOrderListTypeDispatchService wxappOrderListTypeDispatchService,
			WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler,
			OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService,
			TradeRateSettingRedisService tradeRateSettingRedisService,
			ObjectMapper objectMapper,
			WxappNormalOrderListPhpParityService wxappNormalOrderListPhpParityService,
			InvoiceSettingRedisService invoiceSettingRedisService) {
		this.wxappOrderListFilterAssembler = wxappOrderListFilterAssembler;
		this.wxappOrderListTypeDispatchService = wxappOrderListTypeDispatchService;
		this.wxappOrderInvoiceDetailAssembler = wxappOrderInvoiceDetailAssembler;
		this.orderCheckoutInvoiceStatusService = orderCheckoutInvoiceStatusService;
		this.tradeRateSettingRedisService = tradeRateSettingRedisService;
		this.objectMapper = objectMapper;
		this.wxappNormalOrderListPhpParityService = wxappNormalOrderListPhpParityService;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
	}

	@SuppressWarnings("unchecked")
	public Object getOrderList(HttpServletRequest request, Map<String, Object> auth) {
		if (WxappOrderListFilterAssembler.looseAuthUserIdFalsy(auth.get("user_id"))) {
			LinkedHashMap<String, Object> early = new LinkedHashMap<>();
			early.put("list", new ArrayList<>());
			early.put("total_count", new ArrayList<>());
			return early;
		}

		Map<String, Object> paramMap =
				new LinkedHashMap<>(cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap.toObjectMap(request));
		long companyId = longVal(auth.get("company_id"));
		WxappOrderListFilterAssembler.Built built = wxappOrderListFilterAssembler.built(companyId, auth, paramMap);

		if (built.step13EmptyWhenFalsyUser() && WxappOrderListFilterAssembler.looseAuthUserIdFalsy(auth.get("user_id"))) {
			return Collections.emptyList();
		}

		int page = parsePositiveIntOrDefault(paramMap.get("page"), 1);
		int limit = parsePositiveIntOrDefault(paramMap.get("pageSize"), 50);
		String from = stringParam(paramMap, "from", "front_list");
		String orderType = stringParam(built.filter(), "order_type", "service");

		Map<String, Object> core =
				wxappOrderListTypeDispatchService.getOrderList(orderType, built.filter(), page, limit, from, true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) core.get("list");
		if (list == null) {
			list = new ArrayList<>();
		}

		enrichWxappOrderListRows(companyId, list);
		wxappNormalOrderListPhpParityService.applyIfDecoratedNormalList(companyId, list);

		boolean rateStatus = resolveRateStatus(companyId);
		core.put("rate_status", rateStatus);
		return core;
	}

	public void enrichWxappOrderListRows(long companyId, List<Map<String, Object>> list) {
		Map<String, Map<String, Object>> invoiceHeadByOrderId = new LinkedHashMap<>();
		for (Map<String, Object> row : list) {
			long oid = longVal(row.get("order_id"));
			if (oid <= 0L) {
				continue;
			}
			List<Map<String, Object>> invs = wxappOrderInvoiceDetailAssembler.listInvoicesForWxappDetail(companyId, oid);
			if (!invs.isEmpty()) {
				invoiceHeadByOrderId.put(String.valueOf(oid), invs.get(0));
			}
		}

		for (Map<String, Object> row : list) {
			long oid = longVal(row.get("order_id"));
			String oidKey = String.valueOf(oid);
			row.put(
					"invoice_info",
					invoiceHeadByOrderId.containsKey(oidKey) ? invoiceHeadByOrderId.get(oidKey) : new ArrayList<>());
			int invoiceAmountCents = computeWxappListInvoiceAmountCents(companyId, row);
			row.put("invoice_amount", invoiceAmountCents);
			int invStatus = orderCheckoutInvoiceStatusService.checkoutInvoiceStatus(row);
			row.put("invoice_able", invStatus != 0);
			Map<String, Object> invHead = invoiceHeadByOrderId.get(oidKey);
			int invId = 0;
			if (invStatus != 0 && invHead != null && invHead.get("id") != null) {
				invId = intVal(invHead.get("id"));
			}
			row.put("invoice_id", invId);
		}

		for (Map<String, Object> row : list) {
			row.put("is_split", Boolean.FALSE);
			if (Boolean.TRUE.equals(row.get("is_logistics")) && "ziti".equals(str(row.get("receipt_type")))) {
				row.put("is_split", Boolean.TRUE);
			}
			int promotionDiscount = 0;
			Object discountRaw = row.get("discount_info");
			List<Map<String, Object>> discountList = parseDiscountInfo(discountRaw);
			for (Map<String, Object> d : discountList) {
				Object t = d.get("type");
				String typeStr = t == null ? "" : String.valueOf(t);
				if (PROMOTION_DISCOUNT_TYPES.contains(typeStr)) {
					promotionDiscount += intVal(d.get("discount_fee"));
				}
			}
			row.put("promotion_discount", promotionDiscount);

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
			if (items != null) {
				for (int k = 0; k < items.size(); k++) {
					Map<String, Object> item = items.get(k);
					int itemFeeNew =
							intVal(item.get("total_fee"))
									+ intVal(item.get("point_fee"))
									+ intVal(item.get("coupon_discount"))
									+ intVal(item.get("promotion_discount"));
					item.put("item_fee_new", itemFeeNew);
					int marketPrice = intVal(item.get("market_price"));
					int price = intVal(item.get("price"));
					int num = intVal(item.get("num"));
					int basePrice = marketPrice > 0 ? marketPrice : price;
					item.put("market_fee", basePrice * num);
				}
			}

			int itemFeeNewOrder =
					intVal(row.get("total_fee"))
							- intVal(row.get("freight_fee"))
							+ intVal(row.get("point_fee"))
							+ intVal(row.get("coupon_discount"))
							+ promotionDiscount;
			row.put("item_fee_new", itemFeeNewOrder);
		}
	}

	@SuppressWarnings("unchecked")
	private int computeWxappListInvoiceAmountCents(long companyId, Map<String, Object> orderRow) {
		List<Map<String, Object>> items = (List<Map<String, Object>>) orderRow.get("items");
		int sum = 0;
		if (items != null) {
			for (Map<String, Object> it : items) {
				int tf = intVal(it.get("total_fee"));
				int rf = intVal(it.get("refunded_fee"));
				int itemPriceFee = tf - rf;
				if (itemPriceFee > 0) {
					sum += itemPriceFee;
				}
			}
		}
		int freightInvoiceMode = 0;
		Object settingRaw = invoiceSettingRedisService.getInvoiceSetting(companyId);
		if (settingRaw instanceof Map<?, ?> sm && sm.get("freight_invoice") != null) {
			freightInvoiceMode = intVal(sm.get("freight_invoice"));
		}
		if (intVal(orderRow.get("freight_fee")) > 0 && freightInvoiceMode == 2) {
			sum += intVal(orderRow.get("freight_fee"));
		}
		return sum;
	}

	private List<Map<String, Object>> parseDiscountInfo(Object discountRaw) {
		return OrderDiscountInfoSupport.parseDiscountInfoRaw(discountRaw, objectMapper);
	}

	private boolean resolveRateStatus(long companyId) {
		Object raw = tradeRateSettingRedisService.getRateSettingStatus(companyId);
		if (!(raw instanceof Map<?, ?> m)) {
			return false;
		}
		Object rs = m.get("rate_status");
		if (rs instanceof Boolean b) {
			return b;
		}
		if (rs != null && "true".equalsIgnoreCase(String.valueOf(rs).trim())) {
			return true;
		}
		return false;
	}

	private static String stringParam(Map<String, Object> m, String key, String dflt) {
		Object v = m.get(key);
		if (v == null) {
			return dflt;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? dflt : s;
	}

	private static int parsePositiveIntOrDefault(Object raw, int dflt) {
		if (raw == null) {
			return dflt;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v > 0 ? v : dflt;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v > 0 ? v : dflt;
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
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

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
