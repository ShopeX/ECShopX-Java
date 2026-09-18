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
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerParentIdService {

	private final OperatorsQueryService operatorsQueryService;

	public DealerParentIdService(OperatorsQueryService operatorsQueryService) {
		this.operatorsQueryService = operatorsQueryService;
	}

	/**
	 * 在已确认 JWT 为经销商身份的场景下，将当前 JWT 的 operator_id（含子账号）解析为用于 SQL 筛选的主经销商 operator_id。
	 * 语义与重构前 {@link #getDealerParentId} 内 dealer 分支一致。
	 *
	 * @param companyId 当前公司 ID
	 * @param operatorIdFromJwt JWT 中的 operator_id（已解析为 long，子账号仍为子账号 ID）
	 * @return 主经销商 operator_id（若本身为主账号则原样返回）
	 * @throws cn.shopex.ecshopx.common.exception.ResourceException 无 operators 记录或子账号 dealer_parent_id 无效时，message 为「没有账号信息」
	 */
	public long resolveDealerPrincipalOperatorId(long companyId, long operatorIdFromJwt) {
		Map<String, Object> filter = new HashMap<>(4);
		filter.put("company_id", companyId);
		filter.put("operator_id", operatorIdFromJwt);
		Map<String, Object> operatorInfo = operatorsQueryService.getInfo(filter);
		if (operatorInfo == null || operatorInfo.isEmpty()) {
			throw new ResourceException("没有账号信息");
		}
		if (isDealerSubAccount(operatorInfo)) {
			long parentId = parseLongClaim(operatorInfo.get("dealer_parent_id"));
			if (parentId <= 0L) {
				throw new ResourceException("没有账号信息");
			}
			return parentId;
		}
		return operatorIdFromJwt;
	}

	public Map<String, Object> getDealerParentId(long companyId, Map<String, Object> jwtMap) {
		String operatorTypeRaw = stringVal(jwtMap.get("operator_type")).trim();
		String operatorTypeNorm = operatorTypeRaw.toLowerCase(Locale.ROOT);

		long variableOperatorId = parseLongClaim(jwtMap.get("operator_id"));

		if ("distributor".equals(operatorTypeNorm)) {
			variableOperatorId = parseLongClaim(jwtMap.get("distributor_id"));
		}

		if ("dealer".equals(operatorTypeNorm)) {
			variableOperatorId = resolveDealerPrincipalOperatorId(companyId, variableOperatorId);
		}

		if (!"dealer".equals(operatorTypeNorm)) {
			throw new ResourceException("登陆类型不是经销商");
		}

		Map<String, Object> out = new LinkedHashMap<>(3);
		out.put("operator_type", operatorTypeRaw);
		out.put("operator_id", variableOperatorId);
		out.put("dealer_parent_id", variableOperatorId);
		return out;
	}

	private static boolean isDealerSubAccount(Map<String, Object> operatorInfo) {
		if (!operatorInfo.containsKey("is_dealer_main")) {
			return false;
		}
		Object v = operatorInfo.get("is_dealer_main");
		if (v == null) {
			return false;
		}
		return !isTruthyDealerMain(v);
	}

	private static boolean isTruthyDealerMain(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s) || "false".equals(s)) {
			return false;
		}
		return true;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLongClaim(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
