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

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointMemberCancelOrderReturnPointsService {

	private final PointMemberAddPointService pointMemberAddPointService;

	public PointMemberCancelOrderReturnPointsService(PointMemberAddPointService pointMemberAddPointService) {
		this.pointMemberAddPointService = pointMemberAddPointService;
	}

	public boolean cancelOrderReturnBackPoints(Map<String, Object> orderData) {
		if (orderData == null) {
			return false;
		}
		Object pu = orderData.get("point_use");
		int pointUse = 0;
		if (pu instanceof Number n) {
			pointUse = n.intValue();
		} else if (pu != null) {
			try {
				pointUse = Integer.parseInt(String.valueOf(pu).trim());
			} catch (NumberFormatException ignored) {
				pointUse = 0;
			}
		}
		String payType = orderData.get("pay_type") == null ? "" : String.valueOf(orderData.get("pay_type")).trim();
		long userId = longVal(orderData.get("user_id"));
		long companyId = longVal(orderData.get("company_id"));
		long orderId = longVal(orderData.get("order_id"));
		if (pointUse > 0 && !"point".equalsIgnoreCase(payType)) {
			pointMemberAddPointService.addPointForOrderCancelReturn(userId, companyId, pointUse, orderId);
			return true;
		}
		return false;
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
