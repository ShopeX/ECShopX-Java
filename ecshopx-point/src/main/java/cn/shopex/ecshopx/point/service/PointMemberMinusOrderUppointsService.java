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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointMemberMinusOrderUppointsService {

	private final PointMemberAddPointService pointMemberAddPointService;

	public PointMemberMinusOrderUppointsService(PointMemberAddPointService pointMemberAddPointService) {
		this.pointMemberAddPointService = pointMemberAddPointService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void minusOrderUppoints(Map<String, Object> orderData) {
		if (orderData == null) {
			return;
		}
		int uppointUse = intVal(orderData.get("uppoint_use"));
		if (uppointUse <= 0) {
			return;
		}
		long userId = longVal(orderData.get("user_id"));
		long companyId = longVal(orderData.get("company_id"));
		long orderId = longVal(orderData.get("order_id"));
		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("订单取消失败");
		}
		String record = "订单号：" + orderId + " 取消扣回抵扣积分";
		pointMemberAddPointService.addPointForManualAdjustment(userId, companyId, uppointUse, false, record);
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
}
