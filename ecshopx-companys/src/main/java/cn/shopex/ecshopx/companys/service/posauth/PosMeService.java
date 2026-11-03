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

package cn.shopex.ecshopx.companys.service.posauth;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/** ECX-10534：GET /api/v1/pos/auth/me */
@Service
public class PosMeService {

	private final PosStoreAccountMapper mapper;
	private final DistributionDistributorSelfReadMapper shopMapper;
	private final MessageSource messageSource;

	public PosMeService(
			PosStoreAccountMapper mapper,
			DistributionDistributorSelfReadMapper shopMapper,
			MessageSource messageSource) {
		this.mapper = mapper;
		this.shopMapper = shopMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> me(Map<String, Object> jwtClaims) {
		Long id = longOrNull(jwtClaims.get("pos_store_account_id"));
		if (id == null) {
			id = longOrNull(jwtClaims.get("id"));
		}
		if (id == null || id <= 0L) {
			throw new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
		}
		PosStoreAccount row = mapper.selectById(id);
		if (row == null || !"enabled".equals(row.getStatus())) {
			throw new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
		}
		Map<String, Object> shop = null;
		if (row.getCompanyId() != null && row.getDistributorId() != null && row.getDistributorId() > 0) {
			shop = shopMapper.selectShopByCompanyAndId(row.getCompanyId(), row.getDistributorId());
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("employee_no", blank(row.getEmployeeNo()));
		out.put("name", blank(row.getName()));
		out.put("mobile", blank(row.getMobile()));
		out.put("email", blank(row.getEmail()));
		out.put("role", blank(row.getRole()));
		out.put("role_name", PosPermissionMatrix.roleName(row.getRole()));
		out.put("company_id", row.getCompanyId());
		out.put("distributor_id", row.getDistributorId());
		out.put("shop_code", shop == null ? "" : blank(shop.get("shop_code")));
		out.put("shop_name", shop == null ? "" : blank(shop.get("name")));
		out.put("status", blank(row.getStatus()));
		out.put("permissions", PosPermissionMatrix.permissionsForRole(row.getRole()));
		out.put("doc_policy", PosPermissionMatrix.docPolicyForRole(row.getRole()));
		return out;
	}

	private static String blank(Object v) {
		return v == null ? "" : v.toString();
	}

	private static Long longOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
