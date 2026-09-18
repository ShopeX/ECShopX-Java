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

package cn.shopex.ecshopx.orders.service.supplier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierOrderListFilterBuilder {

	public record BuiltSupplierOrderFilter(
			LambdaQueryWrapper<SupplierOrder> wrapper, Map<String, Object> echoFilter) {}

	public BuiltSupplierOrderFilter buildQueryAndEchoFilter(long companyId, long supplierId, HttpServletRequest request) {
		if (supplierId > Integer.MAX_VALUE) {
			throw new BadRequestException("operator_id 无效");
		}
		LambdaQueryWrapper<SupplierOrder> w = new LambdaQueryWrapper<>();
		Map<String, Object> echo = new LinkedHashMap<>();
		w.eq(SupplierOrder::getCompanyId, companyId);
		int supplierIdInt = (int) supplierId;
		w.eq(SupplierOrder::getSupplierId, supplierIdInt);
		echo.put("company_id", companyId);
		echo.put("supplier_id", supplierIdInt);
		echo.put("is_check", 0);

		String orderIdRaw = trimParam(request, "order_id");
		if (StringUtils.hasText(orderIdRaw)) {
			try {
				long oid = Long.parseLong(orderIdRaw);
				w.eq(SupplierOrder::getOrderId, oid);
				echo.put("order_id", oid);
			} catch (NumberFormatException ignored) {
				// skip invalid order_id
			}
		}

		String mobile = trimParam(request, "mobile");
		if (StringUtils.hasText(mobile)) {
			w.eq(SupplierOrder::getReceiverMobile, mobile);
			echo.put("receiver_mobile", mobile);
		}

		trimParam(request, "shop_name");

		applyOrderDateFilter(request, w, echo);

		String status = trimParam(request, "order_status");
		if (StringUtils.hasText(status) && !"0".equals(status)) {
			applyOrderStatusFilter(status, w, echo);
		}

		return new BuiltSupplierOrderFilter(w, echo);
	}

	private static void applyOrderDateFilter(HttpServletRequest request, LambdaQueryWrapper<SupplierOrder> w, Map<String, Object> echo) {
		String[] raw = request.getParameterValues("order_date");
		if (raw == null || raw.length < 2) {
			raw = request.getParameterValues("order_date[]");
		}
		String startRaw = null;
		String endRaw = null;
		if (raw != null && raw.length >= 2) {
			startRaw = raw[0];
			endRaw = raw[1];
		} else {
			startRaw = request.getParameter("order_date[0]");
			endRaw = request.getParameter("order_date[1]");
		}
		if (!StringUtils.hasText(startRaw) || !StringUtils.hasText(endRaw)) {
			return;
		}
		ZoneId zone = ZoneId.systemDefault();
		Long start = parseEpochSeconds(startRaw, zone);
		Long end = parseEpochSeconds(endRaw, zone);
		if (start == null || end == null) {
			return;
		}
		int startSec = start.intValue();
		int endSec = end.intValue();
		w.ge(SupplierOrder::getCreateTime, startSec).le(SupplierOrder::getCreateTime, endSec);
		echo.put("order_date", List.of(startRaw, endRaw));
		echo.put("create_time|gte", startSec);
		echo.put("create_time|lte", endSec);
	}

	private static Long parseEpochSeconds(String raw, ZoneId zone) {
		try {
			return DateExpressionParser.parseToEpochSecond(raw, zone);
		} catch (Exception e) {
			return null;
		}
	}

	private static void applyOrderStatusFilter(String status, LambdaQueryWrapper<SupplierOrder> w, Map<String, Object> echo) {
		switch (status) {
			case "ordercancel" -> {
				w.eq(SupplierOrder::getOrderStatus, "CANCEL_WAIT_PROCESS");
				w.eq(SupplierOrder::getCancelStatus, "WAIT_PROCESS");
				echo.put("order_status", "CANCEL_WAIT_PROCESS");
				echo.put("cancel_status", "WAIT_PROCESS");
			}
			case "refundprocess" -> {
				w.eq(SupplierOrder::getOrderStatus, "CANCEL");
				w.eq(SupplierOrder::getCancelStatus, "NO_APPLY_CANCEL");
				echo.put("order_status", "CANCEL");
				echo.put("cancel_status", "NO_APPLY_CANCEL");
			}
			case "refundsuccess" -> {
				w.eq(SupplierOrder::getOrderStatus, "CANCEL");
				w.eq(SupplierOrder::getCancelStatus, "SUCCESS");
				echo.put("order_status", "CANCEL");
				echo.put("cancel_status", "SUCCESS");
			}
			case "notship" -> {
				w.eq(SupplierOrder::getOrderStatus, "PAYED");
				w.in(SupplierOrder::getCancelStatus, List.of("NO_APPLY_CANCEL", "FAILS"));
				w.eq(SupplierOrder::getReceiptType, "logistics");
				echo.put("order_status", "PAYED");
				echo.put("cancel_status", List.of("NO_APPLY_CANCEL", "FAILS"));
				echo.put("receipt_type", "logistics");
			}
			case "cancelapply" -> {
				w.eq(SupplierOrder::getOrderStatus, "PAYED");
				w.eq(SupplierOrder::getCancelStatus, "WAIT_PROCESS");
				echo.put("order_status", "PAYED");
				echo.put("cancel_status", "WAIT_PROCESS");
			}
			case "ziti" -> {
				w.eq(SupplierOrder::getReceiptType, "ziti");
				w.eq(SupplierOrder::getOrderStatus, "PAYED");
				w.eq(SupplierOrder::getZitiStatus, "PENDING");
				echo.put("receipt_type", "ziti");
				echo.put("order_status", "PAYED");
				echo.put("ziti_status", "PENDING");
			}
			case "shipping" -> {
				w.eq(SupplierOrder::getOrderStatus, "WAIT_BUYER_CONFIRM");
				w.in(SupplierOrder::getDeliveryStatus, List.of("DONE", "PARTAIL"));
				w.eq(SupplierOrder::getReceiptType, "logistics");
				echo.put("order_status", "WAIT_BUYER_CONFIRM");
				echo.put("delivery_status", List.of("DONE", "PARTAIL"));
				echo.put("receipt_type", "logistics");
			}
			case "finish" -> {
				w.eq(SupplierOrder::getOrderStatus, "DONE");
				echo.put("order_status", "DONE");
			}
			case "reviewpass" -> {
				w.eq(SupplierOrder::getOrderStatus, "REVIEW_PASS");
				echo.put("order_status", "REVIEW_PASS");
			}
			case "done_noinvoice" -> {
				w.eq(SupplierOrder::getOrderStatus, "DONE");
				w.isNotNull(SupplierOrder::getInvoice);
				w.eq(SupplierOrder::getIsInvoiced, Boolean.FALSE);
				echo.put("order_status", "DONE");
				echo.put("invoice|neq", null);
				echo.put("is_invoiced", 0);
			}
			case "done_invoice" -> {
				w.eq(SupplierOrder::getOrderStatus, "DONE");
				w.isNotNull(SupplierOrder::getInvoice);
				w.eq(SupplierOrder::getIsInvoiced, Boolean.TRUE);
				echo.put("order_status", "DONE");
				echo.put("invoice|neq", null);
				echo.put("is_invoiced", 1);
			}
			default -> {
				String upper = status.toUpperCase(Locale.ROOT);
				w.eq(SupplierOrder::getOrderStatus, upper);
				echo.put("order_status", upper);
			}
		}
	}

	private static String trimParam(HttpServletRequest request, String name) {
		String v = request.getParameter(name);
		return v == null ? "" : v.trim();
	}
}
