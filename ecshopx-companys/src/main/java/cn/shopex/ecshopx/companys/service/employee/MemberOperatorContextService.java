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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 解析当前登录账号在下游业务中应使用的 operator 上下文（店铺 id / 经销商主账号 id 等）。
 */
@Service
public class MemberOperatorContextService {

	private final OperatorsQueryService operatorsQueryService;

	public MemberOperatorContextService(OperatorsQueryService operatorsQueryService) {
		this.operatorsQueryService = operatorsQueryService;
	}

	public Map<String, Object> resolve(long jwtOperatorId, String jwtOperatorType, Long jwtDistributorId) {
		Map<String, Object> out = new HashMap<>(4);
		out.put("operator_type", jwtOperatorType);
		if ("distributor".equals(jwtOperatorType)) {
			long id = jwtDistributorId != null ? jwtDistributorId : 0L;
			out.put("operator_id", id);
			return out;
		}
		if ("dealer".equals(jwtOperatorType)) {
			Map<String, Object> filter = new HashMap<>(2);
			filter.put("operator_id", jwtOperatorId);
			Map<String, Object> row = operatorsQueryService.getInfo(filter);
			if (row == null || row.isEmpty()) {
				throw new ResourceException("没有账号信息");
			}
			boolean main = isDealerMain(row);
			long oid = jwtOperatorId;
			if (!main) {
				Object parent = row.get("dealer_parent_id");
				if (parent != null && !parent.toString().isEmpty()) {
					try {
						oid = Long.parseLong(parent.toString());
					} catch (NumberFormatException ignored) {
						oid = jwtOperatorId;
					}
				}
			}
			out.put("operator_id", oid);
			return out;
		}
		out.put("operator_id", jwtOperatorId);
		return out;
	}

	private static boolean isDealerMain(Map<String, Object> row) {
		Object v = row.get("is_dealer_main");
		if (v == null) {
			return true;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return !"0".equals(String.valueOf(v));
	}
}
