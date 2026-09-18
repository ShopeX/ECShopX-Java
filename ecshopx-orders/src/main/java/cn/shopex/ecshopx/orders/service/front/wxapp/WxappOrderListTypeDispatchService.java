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
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListTypeRegistry;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderListTypeDispatchService {

	private final AdminOrderListTypeRegistry adminOrderListTypeRegistry;
	private final WxappFrontNormalOrderListService wxappFrontNormalOrderListService;
	private final WxappFrontServiceOrderListService wxappFrontServiceOrderListService;

	public WxappOrderListTypeDispatchService(
			AdminOrderListTypeRegistry adminOrderListTypeRegistry,
			WxappFrontNormalOrderListService wxappFrontNormalOrderListService,
			WxappFrontServiceOrderListService wxappFrontServiceOrderListService) {
		this.adminOrderListTypeRegistry = adminOrderListTypeRegistry;
		this.wxappFrontNormalOrderListService = wxappFrontNormalOrderListService;
		this.wxappFrontServiceOrderListService = wxappFrontServiceOrderListService;
	}

	public Map<String, Object> getOrderList(
			String orderType, Map<String, Object> filter, int page, int limit, String from, boolean needTotal) {
		String ot = orderType == null ? "" : orderType.trim();
		String blocked = ot.toLowerCase(Locale.ROOT);
		if ("supplier_order".equals(blocked) || "membercard".equals(blocked)) {
			throw new ResourceException("无此类型订单！");
		}
		Map<String, Object> f = filter;
		String orderClassForRegistry = orderClassForRegistry(f);
		Optional<AdminOrderListExecutorKind> opt =
				adminOrderListTypeRegistry.resolveOrderListDispatch(ot, orderClassForRegistry, f);
		if (opt.isEmpty()) {
			throw new ResourceException("无此类型订单！");
		}
		AdminOrderListExecutorKind kind = opt.get();
		if (kind == AdminOrderListExecutorKind.SUPPLIER_ORDER || kind == AdminOrderListExecutorKind.MEMBERCARD) {
			throw new ResourceException("无此类型订单！");
		}
		if (isServiceFamily(kind)) {
			return wxappFrontServiceOrderListService.getOrderList(kind, f, page, limit, from, needTotal);
		}
		return wxappFrontNormalOrderListService.getOrderList(kind, f, page, limit, from, needTotal);
	}

	private static String orderClassForRegistry(Map<String, Object> f) {
		Object oc = f.get("order_class");
		return oc == null ? "" : String.valueOf(oc).trim();
	}

	private static boolean isServiceFamily(AdminOrderListExecutorKind kind) {
		return kind == AdminOrderListExecutorKind.SERVICE
				|| kind == AdminOrderListExecutorKind.GROUPS_SERVICE
				|| kind == AdminOrderListExecutorKind.SECKILL_SERVICE;
	}
}
