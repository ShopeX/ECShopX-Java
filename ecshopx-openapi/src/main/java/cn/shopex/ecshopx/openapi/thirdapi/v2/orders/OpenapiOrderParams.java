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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiOrderParams {

	private OpenapiOrderParams() {}

	public static long resolveOrderId(String orderIdParam, Map<String, Object> body) {
		Object raw = body != null ? body.get("order_id") : null;
		if (raw != null) {
			if (raw instanceof Number n) {
				long id = n.longValue();
				if (id > 0L) {
					return id;
				}
				throw new ResourceException("此订单不存在！");
			}
			return parsePositiveOrderId(String.valueOf(raw).trim());
		}
		if (orderIdParam != null && !orderIdParam.isBlank()) {
			return parsePositiveOrderId(orderIdParam.trim());
		}
		throw new ResourceException("此订单不存在！");
	}

	public static String mergePickupcode(String pickupcodeParam, Map<String, Object> body) {
		if (body != null && body.containsKey("pickupcode")) {
			Object raw = body.get("pickupcode");
			if (raw != null) {
				String t = String.valueOf(raw).trim();
				if (StringUtils.hasText(t)) {
					return t;
				}
			}
		}
		if (pickupcodeParam != null) {
			String t = pickupcodeParam.trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		return null;
	}

	private static long parsePositiveOrderId(String s) {
		try {
			long id = Long.parseLong(s);
			if (id <= 0L) {
				throw new ResourceException("此订单不存在！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("此订单不存在！");
		}
	}
}
