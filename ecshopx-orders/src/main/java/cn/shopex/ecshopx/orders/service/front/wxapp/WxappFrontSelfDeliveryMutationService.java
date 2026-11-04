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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.admin.AdminCancelDeliveryStaffService;
import cn.shopex.ecshopx.orders.service.admin.AdminConfirmDeliveryPackagService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * C 端自配送写操作入口：先过 {@link WxappFrontMerchantSelfDeliveryAccessGuard}，再委托既有 Admin 服务。
 */
@Service
public class WxappFrontSelfDeliveryMutationService {

	/** Aligns with historical wxapp controller placeholders for OPL operator fields. */
	public static final String WXAPP_OPERATOR_TYPE = "";

	public static final long WXAPP_OPERATOR_ID = 0L;

	private final WxappFrontMerchantSelfDeliveryAccessGuard accessGuard;
	private final AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService;
	private final AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService;
	private final AdminCancelDeliveryStaffService adminCancelDeliveryStaffService;

	public WxappFrontSelfDeliveryMutationService(
			WxappFrontMerchantSelfDeliveryAccessGuard accessGuard,
			AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService,
			AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService,
			AdminCancelDeliveryStaffService adminCancelDeliveryStaffService) {
		this.accessGuard = accessGuard;
		this.adminOrderUpdateDeliveryService = adminOrderUpdateDeliveryService;
		this.adminConfirmDeliveryPackagService = adminConfirmDeliveryPackagService;
		this.adminCancelDeliveryStaffService = adminCancelDeliveryStaffService;
	}

	public Map<String, Object> updateDelivery(
			long companyId, long userId, String ordersDeliveryId, Map<String, Object> params) {
		accessGuard.assertAuthorizedForDeliveryId(companyId, userId, ordersDeliveryId);
		return adminOrderUpdateDeliveryService.updateDelivery(companyId, 0L, ordersDeliveryId, params);
	}

	public void confirmDeliveryPackag(long companyId, long userId, String orderIdRaw) {
		accessGuard.assertAuthorizedForOrder(companyId, userId, parseOrderIdRequired(orderIdRaw));
		adminConfirmDeliveryPackagService.confirmDeliveryPackag(
				companyId, WXAPP_OPERATOR_TYPE, WXAPP_OPERATOR_ID, orderIdRaw);
	}

	public void cancelDeliveryStaff(long companyId, long userId, String orderIdRaw) {
		accessGuard.assertAuthorizedForOrder(companyId, userId, parseOrderIdRequired(orderIdRaw));
		adminCancelDeliveryStaffService.cancelDeliveryStaff(
				companyId, WXAPP_OPERATOR_TYPE, WXAPP_OPERATOR_ID, orderIdRaw);
	}

	private static long parseOrderIdRequired(String orderIdRaw) {
		if (orderIdRaw == null || orderIdRaw.isBlank()) {
			throw new ResourceException("订单号缺少！");
		}
		try {
			long orderId = Long.parseLong(orderIdRaw.trim());
			if (orderId <= 0L) {
				throw new ResourceException("订单号缺少！");
			}
			return orderId;
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号缺少！");
		}
	}
}
