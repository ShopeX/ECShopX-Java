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

package cn.shopex.ecshopx.common.auth;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/** Resolve Operator or POS JWT claims from the current request (POS preferred when both present). */
public final class DualJwtSupport {

	private DualJwtSupport() {}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> claims(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		Object pos = request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA);
		if (pos instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		Object op = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (op instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return null;
	}

	public static boolean isPosSession(HttpServletRequest request) {
		return request != null && request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA) instanceof Map<?, ?>;
	}

	public static Long longClaim(Map<String, Object> claims, String key) {
		if (claims == null) {
			return null;
		}
		Object raw = claims.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return null;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** company_id from Operator or POS JWT. */
	public static long requireCompanyId(HttpServletRequest request) {
		Map<String, Object> jwt = claims(request);
		if (jwt == null) {
			throw new UnauthorizedException("未登录");
		}
		Long companyId = longClaim(jwt, "company_id");
		if (companyId == null || companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		return companyId;
	}

	/**
	 * Acting id for cart / create / checkout: Operator JWT {@code operator_id}; POS JWT
	 * {@code pos_store_account_id} (fallback {@code id}). Order经手仍由 {@code PosOrderBinder} 写列。
	 */
	public static long requireActingOperatorId(HttpServletRequest request) {
		Map<String, Object> jwt = claims(request);
		if (jwt == null) {
			throw new UnauthorizedException("未登录");
		}
		if (isPosSession(request)) {
			Long posId = longClaim(jwt, "pos_store_account_id");
			if (posId == null) {
				posId = longClaim(jwt, "id");
			}
			if (posId == null || posId <= 0L) {
				throw new UnauthorizedException("未登录");
			}
			return posId;
		}
		Long operatorId = longClaim(jwt, "operator_id");
		if (operatorId == null || operatorId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		return operatorId;
	}
}
