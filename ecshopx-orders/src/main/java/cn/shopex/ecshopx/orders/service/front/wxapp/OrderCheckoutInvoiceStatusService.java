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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCheckoutInvoiceStatusService {

	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;

	public OrderCheckoutInvoiceStatusService(
			InvoiceSettingRedisService invoiceSettingRedisService,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper) {
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
	}

	/**
	 * 按订单行可开票金额（扣除已完结售后退款）、结合发票设置中的运费开票模式，汇总本单可展示的开票金额（分）。
	 */
	public int sumInvoiceAmountForWxappDetail(Map<String, Object> orderInfo) {
		long companyId = longVal(orderInfo.get("company_id"), 0L);
		long orderId = longVal(orderInfo.get("order_id"), 0L);
		Map<String, Map<String, Object>> itemRefundFee = buildItemRefundFeeMap(companyId, orderId);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items =
				orderInfo.get("items") instanceof List<?> l
						? (List<Map<String, Object>>) (List<?>) l
						: List.of();
		int invoiceAmount = 0;
		for (Map<String, Object> item : items) {
			Object iid = item.get("item_id");
			if (iid == null) {
				iid = item.get("id");
			}
			String itemKey = iid == null ? "0" : String.valueOf(iid);
			int refundFee = 0;
			Map<String, Object> rf = itemRefundFee.get(itemKey);
			if (rf != null && rf.get("refund_fee") != null) {
				refundFee = intVal(rf.get("refund_fee"));
			}
			int itemPriceFee = intVal(item.get("total_fee")) - refundFee;
			if (itemPriceFee <= 0) {
				continue;
			}
			invoiceAmount += itemPriceFee;
		}
		Object settingRaw = invoiceSettingRedisService.getInvoiceSetting(companyId);
		int freightInvoiceMode = 0;
		if (settingRaw instanceof Map<?, ?> sm) {
			Object fr = sm.get("freight_invoice");
			freightInvoiceMode = intVal(fr);
		}
		Object ff = orderInfo.get("freight_fee");
		if (freightFeePositive(ff) && freightInvoiceMode == 2) {
			invoiceAmount += intVal(ff);
		}
		return invoiceAmount;
	}

	public int checkoutInvoiceStatus(Map<String, Object> orderInfo) {
		long companyId = longVal(orderInfo.get("company_id"), 0L);
		Object raw = invoiceSettingRedisService.getInvoiceSetting(companyId);
		if (!(raw instanceof Map<?, ?> invAny)) {
			return 0;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> invoiceSetting = (Map<String, Object>) invAny;
		Object invoiceStatusRaw = invoiceSetting.get("invoice_status");
		if (isEmptyInvoiceStatus(invoiceStatusRaw)) {
			return 0;
		}
		if (isZeroLike(invoiceStatusRaw)) {
			return 0;
		}
		String orderStatus = stringVal(orderInfo.get("order_status"));
		if ("CANCEL".equals(orderStatus)) {
			return 0;
		}
		String cancelStatus = stringVal(orderInfo.get("cancel_status"));
		if ("WAIT_PROCESS".equals(cancelStatus)) {
			return 0;
		}
		if ("NOTPAY".equals(orderStatus)) {
			Object applyType = invoiceSetting.get("apply_type");
			if (applyTypeMatchesTwo(applyType)) {
				return 0;
			}
		}
		return 1;
	}

	private static boolean applyTypeMatchesTwo(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() == 2;
		}
		try {
			return Integer.parseInt(v.toString().trim()) == 2;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isEmptyInvoiceStatus(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s && !StringUtils.hasText(s)) {
			return true;
		}
		if (v instanceof Boolean b && !b) {
			return true;
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			return true;
		}
		if (v instanceof Map<?, ?> m && m.isEmpty()) {
			return true;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		String asStr = v.toString().trim();
		return "0".equals(asStr);
	}

	private static boolean isZeroLike(Object v) {
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if ("false".equalsIgnoreCase(t)) {
				return true;
			}
			try {
				return Integer.parseInt(t) == 0;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private Map<String, Map<String, Object>> buildItemRefundFeeMap(long companyId, long orderId) {
		List<Aftersales> mains =
				aftersalesMapper.selectList(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getOrderId, orderId)
								.orderByAsc(Aftersales::getCreateTime));
		LinkedHashMap<String, Map<String, Object>> itemRefundFeeMap = new LinkedHashMap<>();
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
				if (mainStatus != 2) {
					continue;
				}
				int itemId = detail.getItemId() == null ? 0 : detail.getItemId().intValue();
				String itemKey = String.valueOf(itemId);
				Map<String, Object> agg = itemRefundFeeMap.computeIfAbsent(itemKey, k -> new LinkedHashMap<>());
				int prevRf = intVal(agg.get("refund_fee"));
				int df = detail.getRefundFee() != null ? detail.getRefundFee() : 0;
				agg.put("refund_fee", prevRf + df);
			}
		}
		return itemRefundFeeMap;
	}

	private static boolean freightFeePositive(Object ff) {
		if (ff == null) {
			return false;
		}
		if (ff instanceof Number n) {
			return n.doubleValue() > 0.0d;
		}
		if (ff instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				return Double.parseDouble(s.trim()) > 0.0d;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
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

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
