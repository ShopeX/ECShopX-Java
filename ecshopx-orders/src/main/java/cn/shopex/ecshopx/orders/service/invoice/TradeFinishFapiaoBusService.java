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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.repository.UserOrderInvoiceRepository;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.statistics.TradePayFinishStatisticsBusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TradeFinishFapiaoBusService {

	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final UserOrderInvoiceRepository userOrderInvoiceRepository;
	private final ObjectMapper objectMapper;
	private final TradePayFinishStatisticsBusService tradePayFinishStatisticsBusService;

	public TradeFinishFapiaoBusService(
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			UserOrderInvoiceRepository userOrderInvoiceRepository,
			ObjectMapper objectMapper,
			TradePayFinishStatisticsBusService tradePayFinishStatisticsBusService) {
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.userOrderInvoiceRepository = userOrderInvoiceRepository;
		this.objectMapper = objectMapper;
		this.tradePayFinishStatisticsBusService = tradePayFinishStatisticsBusService;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}
		Long companyId = longFrom(tradeRowSnakeCase.get("company_id"));
		Long orderIdNum = longFrom(tradeRowSnakeCase.get("order_id"));
		Long userId = longFrom(tradeRowSnakeCase.get("user_id"));
		if (companyId == null || companyId <= 0L || orderIdNum == null || orderIdNum <= 0L || userId == null || userId <= 0L) {
			log.debug("trade finish fapiao skipped: missing company_id, order_id, or user_id");
			return;
		}
		String tradeSourceType = normalizeTradeSourceType(tradeRowSnakeCase.get("trade_source_type"));
		if (!StringUtils.hasText(tradeSourceType)) {
			log.debug("trade finish fapiao skipped: missing trade_source_type");
			return;
		}
		if (!tradePayFinishStatisticsBusService.isEligibleTradeSourceType(tradeSourceType)) {
			log.debug(
					"trade finish fapiao skipped: unsupported trade_source_type orderId={} type={}",
					orderIdNum,
					tradeSourceType);
			return;
		}
		if ("service".equals(tradePayFinishStatisticsBusService.resolveStatisticsType(tradeSourceType))) {
			log.debug(
					"trade finish fapiao skipped: service-order pipeline not loaded here orderId={} type={}",
					orderIdNum,
					tradeSourceType);
			return;
		}

		String orderIdStr = String.valueOf(orderIdNum);

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdStr, false);
		} catch (BadRequestException e) {
			log.debug("trade finish fapiao skipped: order bundle not found orderId={} msg={}", orderIdStr, e.getMessage());
			return;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		if (orderInfo == null || orderInfo.isEmpty()) {
			log.debug("trade finish fapiao skipped: empty orderInfo orderId={}", orderIdStr);
			return;
		}

		String orderClass = str(orderInfo.get("order_class"));
		if (!orderClassMatchesTradeSource(tradeSourceType, orderClass)) {
			log.debug(
					"trade finish fapiao skipped: trade_source_type mismatch with order_class orderId={} type={} orderClass={}",
					orderIdStr,
					tradeSourceType,
					orderClass);
			return;
		}

		Long bundleUserId = longFrom(orderInfo.get("user_id"));
		if (bundleUserId == null || bundleUserId != userId) {
			log.debug("trade finish fapiao skipped: user mismatch for orderId={}", orderIdStr);
			return;
		}

		Object invoiceRaw = orderInfo.get("invoice");
		if (!isTruthyInvoice(invoiceRaw)) {
			return;
		}

		String invoiceJson = invoiceToJsonString(invoiceRaw);
		if (!StringUtils.hasText(invoiceJson)) {
			return;
		}

		userOrderInvoiceRepository.saveInvoiceData(orderIdStr, companyId, userId, invoiceJson);
	}

	private static boolean orderClassMatchesTradeSource(String normalizedTradeSource, String orderClass) {
		String oc = orderClass == null ? "" : orderClass.trim();
		return switch (normalizedTradeSource) {
			case "normal", "normal_normal" -> "normal".equals(oc) || "bargain".equals(oc);
			case "normal_groups" -> "groups".equals(oc);
			case "normal_seckill" -> "seckill".equals(oc);
			case "normal_community" -> "community".equals(oc);
			case "bargain" -> "bargain".equals(oc);
			case "normal_shopguide" -> "shopguide".equals(oc);
			case "normal_pointsmall" -> "pointsmall".equals(oc);
			default -> false;
		};
	}

	private static String normalizeTradeSourceType(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
	}

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private String invoiceToJsonString(Object invoiceRaw) {
		if (invoiceRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "{}".equals(t) || "[]".equals(t)) {
				return "";
			}
			return t;
		}
		if (invoiceRaw instanceof Map<?, ?> m && m.isEmpty()) {
			return "";
		}
		if (invoiceRaw instanceof Map<?, ?>) {
			try {
				return objectMapper.writeValueAsString(invoiceRaw);
			} catch (JsonProcessingException e) {
				log.warn("trade finish fapiao: failed to serialize invoice map orderId context");
				return "";
			}
		}
		return "";
	}

	private static boolean isTruthyInvoice(Object invoiceRaw) {
		if (invoiceRaw == null) {
			return false;
		}
		if (invoiceRaw instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"{}".equals(t) && !"[]".equals(t);
		}
		if (invoiceRaw instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return false;
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
