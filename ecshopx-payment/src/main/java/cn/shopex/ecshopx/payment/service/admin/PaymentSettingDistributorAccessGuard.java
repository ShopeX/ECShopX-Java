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

package cn.shopex.ecshopx.payment.service.admin;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ECX-10856/10863: gate admin payment-setting read/write by company ownership and store-operator JWT
 * scope.
 */
@Component
public class PaymentSettingDistributorAccessGuard {

	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	public PaymentSettingDistributorAccessGuard(
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort) {
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
	}

	/**
	 * ECX-10863: store operators always write their JWT store; HQ keeps the request distributor id.
	 */
	public static long resolveWriteDistributorId(
			String operatorType, long requestDistributorId, long jwtDistributorId) {
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(ot)) {
			return jwtDistributorId;
		}
		return requestDistributorId;
	}

	/**
	 * @return true when the caller may resolve Redis/settings for {@code requestDistributorId}
	 */
	public boolean mayRead(
			long companyId, long requestDistributorId, String operatorType, long jwtDistributorId) {
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(ot)) {
			if (jwtDistributorId <= 0L || requestDistributorId != jwtDistributorId) {
				return false;
			}
			return belongsToCompany(companyId, requestDistributorId);
		}
		if (requestDistributorId <= 0L) {
			return true;
		}
		return belongsToCompany(companyId, requestDistributorId);
	}

	private boolean belongsToCompany(long companyId, long distributorId) {
		if (companyId <= 0L || distributorId <= 0L) {
			return false;
		}
		Map<String, Object> row =
				distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
						companyId, distributorId);
		return row != null && !row.isEmpty();
	}

	static long parseJwtDistributorId(Map<?, ?> jwt) {
		if (jwt == null || jwt.isEmpty()) {
			return 0L;
		}
		Object raw = jwt.get("distributor_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	static String parseJwtOperatorType(Map<?, ?> jwt) {
		if (jwt == null || jwt.isEmpty()) {
			return "";
		}
		Object ot = jwt.get("operator_type");
		return ot == null ? "" : String.valueOf(ot).trim();
	}
}
