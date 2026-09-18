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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.dto.front.UpdateEmployeePurchaseOrderReceiverRequest;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersReceiverUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseOrderUpdateReceiverService {

	private final OrdersRelActivityMapper ordersRelActivityMapper;
	private final NormalOrdersReceiverUpdateService normalOrdersReceiverUpdateService;

	public EmployeePurchaseOrderUpdateReceiverService(
			OrdersRelActivityMapper ordersRelActivityMapper,
			NormalOrdersReceiverUpdateService normalOrdersReceiverUpdateService) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
		this.normalOrdersReceiverUpdateService = normalOrdersReceiverUpdateService;
	}

	public Map<String, Object> updateReceiver(
			long companyId, long userId, UpdateEmployeePurchaseOrderReceiverRequest req) {
		LambdaQueryWrapper<OrdersRelActivity> w = new LambdaQueryWrapper<>();
		w.eq(OrdersRelActivity::getCompanyId, companyId).eq(OrdersRelActivity::getOrderId, req.getOrderId());
		OrdersRelActivity rel = ordersRelActivityMapper.selectOne(w);
		if (rel == null) {
			throw new ResourceException("内购订单关联信息不存在");
		}
		Integer closeModifyTime = rel.getCloseModifyTime();
		long now = System.currentTimeMillis() / 1000L;
		int effectiveClose = closeModifyTime == null ? 0 : closeModifyTime;
		if (effectiveClose < now) {
			throw new ResourceException("已超过可修改时间");
		}

		Map<String, Object> base =
				normalOrdersReceiverUpdateService.updateReceiverForEmployeePurchase(
						companyId,
						userId,
						req.getOrderId(),
						req.getReceiverName(),
						req.getReceiverMobile(),
						req.getReceiverZip(),
						req.getReceiverState(),
						req.getReceiverCity(),
						req.getReceiverDistrict(),
						req.getReceiverAddress());

		Map<String, Object> row =
				ordersRelActivityMapper.selectDetailForOrder(companyId, req.getOrderId());
		if (row == null) {
			return base;
		}

		Map<String, Object> detail = new HashMap<>();
		detail.put("order_id", toLong(row.get("order_id")));
		detail.put("user_id", toLong(row.get("user_id")));
		detail.put("enterprise_id", toLong(row.get("enterprise_id")));
		detail.put("activity_id", toLong(row.get("activity_id")));
		detail.put("enterprise_name", row.get("enterprise_name") == null ? null : row.get("enterprise_name").toString());
		detail.put("type", row.get("type") == null ? "" : row.get("type").toString());
		detail.put("employee_name", row.get("employee_name") == null ? null : row.get("employee_name").toString());
		base.put("orders_purchase_info", detail);

		base.put("order_id", toLong(row.get("order_id")));
		base.put("company_id", toLong(row.get("company_id")));
		base.put("enterprise_id", toLong(row.get("enterprise_id")));
		base.put("activity_id", toLong(row.get("activity_id")));
		base.put("user_id", toLong(row.get("user_id")));
		base.put("if_share_store", toBoolean(row.get("if_share_store")));
		base.put("close_modify_time", toIntegerOrNull(row.get("close_modify_time")));
		return base;
	}

	private static long toLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean toBoolean(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(v.toString().trim());
	}

	private static Integer toIntegerOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
