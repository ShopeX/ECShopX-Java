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

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * ECX-10906：对齐 PHP PaymentTenantScopeGuard::assertDistributorNameFilterAllowed。
 * 店铺端（distributor）不得通过 distributor_name 筛选扩大交易导出范围。
 */
public final class AdapayTradeDistributorNameScopeGuard {

	private AdapayTradeDistributorNameScopeGuard() {}

	public static void assertAllowed(Map<String, Object> jwtMap) {
		if (jwtMap == null) {
			return;
		}
		Object raw = jwtMap.get("operator_type");
		if (raw == null) {
			return;
		}
		String operatorType = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
		if (StringUtils.hasText(operatorType) && "distributor".equals(operatorType)) {
			throw new ForbiddenException("无权筛选其他店铺");
		}
	}
}
