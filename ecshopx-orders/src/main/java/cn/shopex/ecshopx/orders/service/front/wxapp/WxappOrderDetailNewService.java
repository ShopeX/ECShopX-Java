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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.front.WxappOrderDetailMembercardBundlePort;
import cn.shopex.ecshopx.common.order.front.WxappOrderDetailSupplierBundlePort;
import cn.shopex.ecshopx.deposit.service.DepositTradeWxappPaymentService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.admin.OrderTradeInfoPhpParityMaps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailNewService {

	private static final Logger log = LoggerFactory.getLogger(WxappOrderDetailNewService.class);

	private final DepositTradeWxappPaymentService depositTradeWxappPaymentService;
	private final WxappOrderDetailTradePrefixSupport wxappOrderDetailTradePrefixSupport;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort;
	private final WxappOrderDetailSupplierBundlePort wxappOrderDetailSupplierBundlePort;
	private final WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler;
	private final OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService;
	private final ObjectMapper objectMapper;

	public WxappOrderDetailNewService(
			DepositTradeWxappPaymentService depositTradeWxappPaymentService,
			WxappOrderDetailTradePrefixSupport wxappOrderDetailTradePrefixSupport,
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort,
			WxappOrderDetailSupplierBundlePort wxappOrderDetailSupplierBundlePort,
			WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler,
			OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService,
			ObjectMapper objectMapper) {
		this.depositTradeWxappPaymentService = depositTradeWxappPaymentService;
		this.wxappOrderDetailTradePrefixSupport = wxappOrderDetailTradePrefixSupport;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminEntityOrderDetailTypePolicy = adminEntityOrderDetailTypePolicy;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.wxappOrderDetailMembercardBundlePort = wxappOrderDetailMembercardBundlePort;
		this.wxappOrderDetailSupplierBundlePort = wxappOrderDetailSupplierBundlePort;
		this.wxappOrderInvoiceDetailAssembler = wxappOrderInvoiceDetailAssembler;
		this.orderCheckoutInvoiceStatusService = orderCheckoutInvoiceStatusService;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	public Object getOrderDetailNew(Map<String, Object> auth, String orderIdRaw) {
		if (orderIdRaw == null || orderIdRaw.isBlank() || "0".equals(orderIdRaw.trim())) {
			throw new BadRequestException("订单号必填");
		}
		String orderId = orderIdRaw.trim();
		String prefix = firstTwoUnicodeUpper(orderId);

		if ("CZ".equals(prefix)) {
			Map<String, Object> deposit = depositTradeWxappPaymentService.getDepositTradeRowMapOrThrow(orderId);
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("order_id", deposit.get("deposit_trade_id"));
			result.put("user_id", deposit.get("user_id"));
			result.put("order_type", "recharge");
			result.put("pay_type", deposit.get("pay_type"));
			result.put("point", Integer.valueOf(0));
			int moneyFen = intVal(deposit.get("money"));
			BigDecimal yuan =
					BigDecimal.valueOf(moneyFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			result.put("title", "充值" + yuan.toPlainString() + "元");
			result.put("total_fee", moneyFen);
			result.put("create_time", intVal(deposit.get("time_start")));
			Object te = deposit.get("time_expire");
			result.put("payDate", te == null ? "" : String.valueOf(te).trim());
			Object tx = deposit.get("transaction_id");
			result.put("tradeId", tx == null ? "" : String.valueOf(tx).trim());
			String ts = deposit.get("trade_status") == null ? "" : String.valueOf(deposit.get("trade_status")).trim();
			result.put("payStatus", "SUCCESS".equalsIgnoreCase(ts) ? "success" : "fail");
			if (!userMatchesOrder(auth, result)) {
				return Collections.emptyList();
			}
			return result;
		}

		if ("TD".equals(prefix)) {
			String canonical = wxappOrderDetailTradePrefixSupport.resolveCanonicalOrderId(orderId);
			if (canonical == null || canonical.isBlank()) {
				throw new BadRequestException("无此订单");
			}
			orderId = canonical.trim();
		}

		long companyId = requireCompanyId(auth);
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderId);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new BadRequestException("无此订单");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		String detailFrom = "front_list";
		Map<String, Object> bundle;
		if (adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderId, true, detailFrom);
		} else if ("membercard".equals(effective)) {
			bundle = wxappOrderDetailMembercardBundlePort.buildMembercardOrderDetailBundle(companyId, orderId, true);
		} else if ("supplier_order".equals(effective)) {
			bundle =
					wxappOrderDetailSupplierBundlePort.buildSupplierOrderDetailBundle(
							companyId, orderId, true, detailFrom);
		} else {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		Map<String, Object> tradeInfo = (Map<String, Object>) bundle.get("tradeInfo");
		if (orderInfo == null) {
			throw new BadRequestException("无此订单");
		}
		if (tradeInfo != null && !tradeInfo.isEmpty()) {
			tradeInfo =
					OrderTradeInfoPhpParityMaps.tradeSnakeToPhpCamel(
							new LinkedHashMap<>(tradeInfo), objectMapper);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("order_id", orderId);
		result.put("user_id", orderInfo.get("user_id"));
		result.put("order_type", orderInfo.get("order_type"));
		result.put("pay_type", orderInfo.get("pay_type"));
		result.put("point", orderInfo.get("point") == null ? 0 : intVal(orderInfo.get("point")));
		Object title = orderInfo.get("title");
		result.put("title", title == null ? "" : String.valueOf(title));
		result.put("total_fee", intVal(orderInfo.get("total_fee")));
		result.put("create_time", intVal(orderInfo.get("create_time")));
		if (tradeInfo == null || tradeInfo.get("timeExpire") == null) {
			result.put("payDate", "");
		} else {
			result.put("payDate", String.valueOf(tradeInfo.get("timeExpire")));
		}
		if (tradeInfo == null || tradeInfo.get("tradeId") == null) {
			result.put("tradeId", "");
		} else {
			result.put("tradeId", String.valueOf(tradeInfo.get("tradeId")));
		}
		if (tradeInfo != null && "SUCCESS".equals(String.valueOf(tradeInfo.get("tradeState")))) {
			result.put("payStatus", "success");
		} else {
			result.put("payStatus", "fail");
		}

		if (!userMatchesOrder(auth, result)) {
			return Collections.emptyList();
		}

		Map<String, Object> orderInfoCopy = copyOrderInfoForInvoice(orderInfo);
		List<Map<String, Object>> invoiceList =
				wxappOrderInvoiceDetailAssembler.listInvoicesForWxappDetail(companyId, orderIdNum);
		Map<String, Object> keyed = new LinkedHashMap<>();
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
		Map<String, Object> invRow = invRowObj instanceof Map<?, ?> ? (Map<String, Object>) invRowObj : null;
		orderInfoCopy.put("invoice_id", invRow != null && invRow.get("id") != null ? invRow.get("id") : 0);
		Object invoiceInfo = invRow == null ? null : invRow.get("invoice_info");
		if (invoiceInfo instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> infoCopy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				infoCopy.put(String.valueOf(e.getKey()), e.getValue());
			}
			orderInfoCopy.put("invoice_info", infoCopy);
		} else {
			orderInfoCopy.put("invoice_info", new LinkedHashMap<String, Object>());
		}
		int checkoutSt = orderCheckoutInvoiceStatusService.checkoutInvoiceStatus(orderInfoCopy);
		orderInfoCopy.put("invoice_status", checkoutSt);
		orderInfoCopy.put("invoice_able", checkoutSt != 0);
		orderInfoCopy.put(
				"invoice_amount", wxappOrderInvoiceDetailAssembler.sumInvoiceAmountCents(companyId, orderIdNum));
		log.info(":invoice_status:{} checkoutSt={}", "getOrderDetailNew", checkoutSt);

		Map<String, Object> wrapper = new LinkedHashMap<>();
		wrapper.put("invoice_record", orderInfoCopy);
		result.put("orderInfo", wrapper);

		return result;
	}

	private static boolean userMatchesOrder(Map<String, Object> auth, Map<String, Object> result) {
		Object resultUserId = result.get("user_id");
		if (resultUserId == null) {
			resultUserId = 0;
		}
		return longVal(resultUserId) == longVal(auth.get("user_id"));
	}

	private static Map<String, Object> copyOrderInfoForInvoice(Map<String, Object> orderInfo) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : orderInfo.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if ("items".equals(k) && v instanceof List<?> rawList) {
				List<Map<String, Object>> newList = new ArrayList<>();
				for (Object el : rawList) {
					if (el instanceof Map<?, ?> m) {
						LinkedHashMap<String, Object> itemCopy = new LinkedHashMap<>();
						for (Map.Entry<?, ?> ie : m.entrySet()) {
							itemCopy.put(String.valueOf(ie.getKey()), ie.getValue());
						}
						newList.add(itemCopy);
					}
				}
				copy.put(k, newList);
			} else {
				copy.put(k, v);
			}
		}
		return copy;
	}

	private static long requireCompanyId(Map<String, Object> auth) {
		Object c = auth.get("company_id");
		if (c == null) {
			throw new BadRequestException("缺少 company_id");
		}
		long companyId = longVal(c);
		if (companyId <= 0L) {
			throw new BadRequestException("缺少 company_id");
		}
		return companyId;
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

	private static String firstTwoUnicodeUpper(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int cps = s.codePointCount(0, s.length());
		int n = Math.min(2, cps);
		int end = s.offsetByCodePoints(0, n);
		return s.substring(0, end).toUpperCase(Locale.ROOT);
	}
}
