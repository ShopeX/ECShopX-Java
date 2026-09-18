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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.order.OrderDetailEmployeePurchaseEnrichmentPort;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailPostProcessService {

	@SuppressWarnings("unused")
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final WxappOrderDetailShippingFeeLineSupport wxappOrderDetailShippingFeeLineSupport;
	private final WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler;
	private final OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService;
	private final AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	private final ObjectProvider<OrderDetailEmployeePurchaseEnrichmentPort> employeePurchaseEnrichmentPort;

	public WxappOrderDetailPostProcessService(
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			WxappOrderDetailShippingFeeLineSupport wxappOrderDetailShippingFeeLineSupport,
			WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler,
			OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService,
			AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy,
			ObjectProvider<OrderDetailEmployeePurchaseEnrichmentPort> employeePurchaseEnrichmentPort) {
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.wxappOrderDetailShippingFeeLineSupport = wxappOrderDetailShippingFeeLineSupport;
		this.wxappOrderInvoiceDetailAssembler = wxappOrderInvoiceDetailAssembler;
		this.orderCheckoutInvoiceStatusService = orderCheckoutInvoiceStatusService;
		this.adminEntityOrderDetailTypePolicy = adminEntityOrderDetailTypePolicy;
		this.employeePurchaseEnrichmentPort = employeePurchaseEnrichmentPort;
	}

	@SuppressWarnings("unchecked")
	public void apply(
			boolean invoiceListTruthy,
			Map<String, Object> result,
			String effective,
			long companyId,
			long orderIdNum) {
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}
		enrichEmployeePurchaseFields(companyId, orderIdNum, orderInfo);
		Object itemsRaw = orderInfo.get("items");
		if (!(itemsRaw instanceof List<?>)) {
			throw new BadRequestException("参数错误");
		}
		List<Map<String, Object>> items = (List<Map<String, Object>>) itemsRaw;
		if (invoiceListTruthy
				&& freightFeeTruthy(orderInfo.get("freight_fee"))
				&& !wxappOrderInvoiceDetailAssembler.hasShippingFeeInvoice(companyId, orderIdNum)
				&& adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			items.add(wxappOrderDetailShippingFeeLineSupport.buildLine(orderInfo.get("freight_fee")));
		}
		java.util.List<Map<String, Object>> invoiceList =
				wxappOrderInvoiceDetailAssembler.listInvoicesForWxappDetail(companyId, orderIdNum);
		Map<String, Object> keyed = new java.util.LinkedHashMap<>();
		for (Map<String, Object> row : invoiceList) {
			Object oid = row.get("order_id");
			if (oid == null) {
				continue;
			}
			String k = String.valueOf(oid);
			if (!keyed.containsKey(k)) {
				keyed.put(k, row);
			}
		}
		String orderIdStr = String.valueOf(orderIdNum);
		Object invRowObj = keyed.get(orderIdStr);
		@SuppressWarnings("unchecked")
		Map<String, Object> invRow = invRowObj instanceof Map<?, ?> ? (Map<String, Object>) invRowObj : null;
		orderInfo.put("invoice_id", invRow != null && invRow.get("id") != null ? invRow.get("id") : 0);
		Object invoiceInfo = invRow == null ? null : invRow.get("invoice_info");
		if (invoiceInfo instanceof Map<?, ?> m) {
			java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				copy.put(String.valueOf(e.getKey()), e.getValue());
			}
			orderInfo.put("invoice_info", copy);
		} else {
			orderInfo.put("invoice_info", new java.util.LinkedHashMap<String, Object>());
		}
		int checkoutSt = orderCheckoutInvoiceStatusService.checkoutInvoiceStatus(orderInfo);
		orderInfo.put("invoice_status", checkoutSt);
		orderInfo.put("invoice_able", checkoutSt != 0);
		orderInfo.put("invoice_amount", orderCheckoutInvoiceStatusService.sumInvoiceAmountForWxappDetail(orderInfo));
	}

	private void enrichEmployeePurchaseFields(long companyId, long orderIdNum, Map<String, Object> orderInfo) {
		OrderDetailEmployeePurchaseEnrichmentPort port = employeePurchaseEnrichmentPort.getIfAvailable();
		if (port == null) {
			return;
		}
		String orderClass =
				orderInfo.get("order_class") == null ? "" : String.valueOf(orderInfo.get("order_class"));
		port.enrich(companyId, orderIdNum, orderClass, orderInfo);
		Object mode = orderInfo.get("purchase_mode");
		if (mode != null && !String.valueOf(mode).isBlank()) {
			orderInfo.put("is_employee_purchase", true);
			orderInfo.put("employee_purchase_tag", "企业购");
		}
	}

	private static boolean freightFeeTruthy(Object ff) {
		if (ff == null) {
			return false;
		}
		if (ff instanceof Boolean b) {
			return b;
		}
		if (ff instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (ff instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		return true;
	}
}
