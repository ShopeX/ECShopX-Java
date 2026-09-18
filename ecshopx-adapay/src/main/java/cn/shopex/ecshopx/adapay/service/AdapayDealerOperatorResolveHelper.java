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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayDealerOperatorResolveHelper {

	private final OperatorsCommandService operatorsCommandService;
	private final OperatorsQueryService operatorsQueryService;

	public AdapayDealerOperatorResolveHelper(
			OperatorsCommandService operatorsCommandService, OperatorsQueryService operatorsQueryService) {
		this.operatorsCommandService = operatorsCommandService;
		this.operatorsQueryService = operatorsQueryService;
	}

	/**
	 * 经销商主账号 {@code operator_id}，用于 {@code filter.dealer_id} 与条数校验。
	 */
	public long resolveMainDealerOperatorIdOrThrow(long jwtOperatorId) {
		return resolveMergedOperatorIdOrThrow(jwtOperatorId, true);
	}

	/**
	 * 操作日志 {@code rel_id}：经销商归并到主账号，其余身份为当前 {@code operator_id}。
	 */
	public long resolveRelOperatorIdForLogOrThrow(long jwtOperatorId) {
		return resolveMergedOperatorIdOrThrow(jwtOperatorId, false);
	}

	private long resolveMergedOperatorIdOrThrow(long jwtOperatorId, boolean dealerOnly) {
		operatorsCommandService.initDealerParentIdForCurrentDealer(jwtOperatorId);
		Map<String, Object> info = operatorsQueryService.getInfo(Map.of("operator_id", jwtOperatorId));
		if (info == null || info.isEmpty()) {
			throw new ResourceException("没有账号信息");
		}
		String type = str(info.get("operator_type"));
		if ("dealer".equalsIgnoreCase(type)) {
			boolean main = isDealerMainTruthy(info.get("is_dealer_main"));
			if (!main) {
				String parent = str(info.get("dealer_parent_id"));
				if (!StringUtils.hasText(parent)) {
					throw new ResourceException("没有账号信息");
				}
				try {
					return Long.parseLong(parent.trim());
				} catch (NumberFormatException e) {
					throw new ResourceException("没有账号信息");
				}
			}
			return requireOperatorId(info);
		}
		if (dealerOnly) {
			throw new ResourceException("没有账号信息");
		}
		return requireOperatorId(info);
	}

	private static long requireOperatorId(Map<String, Object> info) {
		Object id = info.get("operator_id");
		if (id == null) {
			throw new ResourceException("没有账号信息");
		}
		if (id instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(id.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("没有账号信息");
		}
	}

	private static boolean isDealerMainTruthy(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		return !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
