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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.port.order.OrderCancelScdRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Restores coupons / targeted-promotion quotas after an order reaches CANCEL + SUCCESS
 * (manual cancel update path) or on auto-cancel cron (no DM pay-status guard).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalOrderCancelDiscountRestoreService {

	private final OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;
	private final OrderCancelScdRestorePort orderCancelScdRestorePort;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final ObjectMapper objectMapper;

	/**
	 * Mirrors PHP {@code AbstractNormalOrder::update} coupon restore when status becomes CANCEL +
	 * SUCCESS: skip coupon restore when DM is open and pay_status is not NOTPAY; always restore SCD.
	 */
	public void restoreOnCancelSuccess(NormalOrders order) {
		restore(order, true);
	}

	/**
	 * Mirrors PHP auto-cancel ({@code scheduleCancelOrders} / offline-pay cancel): always attempt
	 * coupon restore (no DM pay-status gate).
	 */
	public void restoreOnAutoCancel(NormalOrders order) {
		restore(order, false);
	}

	private void restore(NormalOrders order, boolean applyDmPayStatusGuard) {
		if (order == null || order.getOrderId() == null) {
			return;
		}
		String discountInfoJson = order.getDiscountInfo();
		if (!StringUtils.hasText(discountInfoJson)) {
			return;
		}
		List<Map<String, Object>> discountList = parseDiscountInfo(discountInfoJson, order.getOrderId());
		if (discountList == null || discountList.isEmpty()) {
			return;
		}
		long companyId = order.getCompanyId() == null ? 0L : order.getCompanyId();
		long userId = order.getUserId() == null ? 0L : order.getUserId();
		long orderId = order.getOrderId();
		boolean dmOpen = companyId > 0L && dmCrmSettingReadPort.isPointIntegrationOpen(companyId);
		boolean notPay = "NOTPAY".equalsIgnoreCase(safe(order.getPayStatus()));
		boolean restoreCoupons = !applyDmPayStatusGuard || !dmOpen || notPay;

		for (Map<String, Object> discountEntry : discountList) {
			if (discountEntry == null || discountEntry.isEmpty()) {
				continue;
			}
			if (restoreCoupons) {
				for (String couponCode : expandCouponCodes(discountEntry.get("coupon_code"))) {
					try {
						orderCancelUserDiscountRestorePort.callbackUserCard(couponCode, orderId);
					} catch (Exception e) {
						log.debug(
								"[cancelDiscountRestore] callbackUserCard failed, orderId={}, code={}",
								orderId,
								couponCode,
								e);
					}
				}
			}
			String discountType =
					discountEntry.get("type") == null
							? null
							: String.valueOf(discountEntry.get("type")).trim();
			if ("member_tag_targeted_promotion".equals(discountType)) {
				orderCancelScdRestorePort.lessDiscount(companyId, userId, discountEntry);
			}
		}
	}

	static List<String> expandCouponCodes(Object couponCodes) {
		List<String> out = new ArrayList<>();
		if (couponCodes instanceof List<?> codes) {
			for (Object codeObj : codes) {
				if (codeObj == null) {
					continue;
				}
				String couponCode = String.valueOf(codeObj).trim();
				if (StringUtils.hasText(couponCode)) {
					out.add(couponCode);
				}
			}
			return out;
		}
		if (couponCodes instanceof String || couponCodes instanceof Number) {
			String couponCode = String.valueOf(couponCodes).trim();
			if (StringUtils.hasText(couponCode)) {
				out.add(couponCode);
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseDiscountInfo(String discountInfoJson, long orderId) {
		try {
			Object parsed = objectMapper.readValue(discountInfoJson, Object.class);
			if (parsed instanceof List<?> list) {
				List<Map<String, Object>> result = new ArrayList<>();
				for (Object item : list) {
					if (item instanceof Map<?, ?> m) {
						result.add((Map<String, Object>) m);
					}
				}
				return result;
			}
			if (parsed instanceof Map<?, ?> m) {
				return List.of((Map<String, Object>) m);
			}
			return List.of();
		} catch (Exception e) {
			log.debug("[cancelDiscountRestore] parse discount_info failed, orderId={}", orderId, e);
			return null;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
