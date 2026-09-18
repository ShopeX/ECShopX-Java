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

package cn.shopex.ecshopx.orders.service.admin.orderlist;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminOrderListTypeRegistry {

	/**
	 * @param orderType raw {@code order_type} request value; {@code null} should be normalized to {@code ""} by the
	 *     caller; never trimmed
	 * @param orderClassAfterPointDeposit {@code order_class} after point/deposit have been mapped to {@code pay_type}
	 *     and cleared for dispatch resolution
	 * @param mutableFilter query conditions and echo fields; updated in place for {@code order_type}, {@code
	 *     order_class}, and {@code type} when applicable
	 */
	public Optional<AdminOrderListExecutorKind> resolveOrderListDispatch(
			String orderType, String orderClassAfterPointDeposit, Map<String, Object> mutableFilter) {
		String orderClass = orderClassAfterPointDeposit == null ? "" : orderClassAfterPointDeposit;
		boolean hasClass = !orderClass.isEmpty();

		if (hasClass
				&& ("normal".equalsIgnoreCase(orderType) || "service".equalsIgnoreCase(orderType))
				&& !orderType.equalsIgnoreCase(orderClass)
				&& !"normal".equalsIgnoreCase(orderClass)
				&& !"service".equalsIgnoreCase(orderClass)) {
			String combined =
					orderType.toLowerCase(Locale.ROOT) + "_" + orderClass.toLowerCase(Locale.ROOT);
			mutableFilter.put("order_type", orderType);
			mutableFilter.put("order_class", orderClass);
			if ("crossborder".equalsIgnoreCase(orderClass)) {
				mutableFilter.remove("order_class");
				mutableFilter.put("type", 1);
				return mapDispatchKey(orderType.toLowerCase(Locale.ROOT));
			}
			return mapDispatchKey(combined);
		}

		mutableFilter.put("order_type", orderType);
		mutableFilter.remove("order_class");
		return mapDispatchKey(orderType.toLowerCase(Locale.ROOT));
	}

	private static Optional<AdminOrderListExecutorKind> mapDispatchKey(String key) {
		if (!StringUtils.hasText(key)) {
			return Optional.empty();
		}
		return switch (key) {
			case "service" -> Optional.of(AdminOrderListExecutorKind.SERVICE);
			case "bargain", "normal_bargain" -> Optional.of(AdminOrderListExecutorKind.BARGAIN_NORMAL);
			case "normal" -> Optional.of(AdminOrderListExecutorKind.NORMAL);
			case "supplier_order" -> Optional.of(AdminOrderListExecutorKind.SUPPLIER_ORDER);
			case "service_groups", "groups" -> Optional.of(AdminOrderListExecutorKind.GROUPS_SERVICE);
			case "normal_groups" -> Optional.of(AdminOrderListExecutorKind.GROUPS_NORMAL);
			case "membercard" -> Optional.of(AdminOrderListExecutorKind.MEMBERCARD);
			case "normal_seckill" -> Optional.of(AdminOrderListExecutorKind.SECKILL_NORMAL);
			case "service_seckill" -> Optional.of(AdminOrderListExecutorKind.SECKILL_SERVICE);
			case "normal_drug" -> Optional.of(AdminOrderListExecutorKind.DRUG_NORMAL);
			case "normal_shopguide" -> Optional.of(AdminOrderListExecutorKind.SHOPGUIDE_NORMAL);
			case "normal_pointsmall" -> Optional.of(AdminOrderListExecutorKind.POINTSMALL_NORMAL);
			case "normal_excard" -> Optional.of(AdminOrderListExecutorKind.EXCARD_NORMAL);
			case "normal_community" -> Optional.of(AdminOrderListExecutorKind.COMMUNITY_NORMAL);
			case "normal_shopadmin" -> Optional.of(AdminOrderListExecutorKind.SHOPADMIN_NORMAL);
			case "normal_employee_purchase" -> Optional.of(AdminOrderListExecutorKind.EMPLOYEE_PURCHASE_NORMAL);
			default -> Optional.empty();
		};
	}
}
