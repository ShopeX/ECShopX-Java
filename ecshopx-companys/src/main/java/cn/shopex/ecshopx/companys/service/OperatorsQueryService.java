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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.config.CommonEncryptProperties;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.employee.AccountManagementOperatorsFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorsQueryService {

	private final OperatorsMapper operatorsMapper;
	private final CommonEncryptProperties commonEncryptProperties;

	public OperatorsQueryService(OperatorsMapper operatorsMapper, CommonEncryptProperties commonEncryptProperties) {
		this.operatorsMapper = operatorsMapper;
		this.commonEncryptProperties = commonEncryptProperties;
	}

	@PostConstruct
	public void assertEncryptMode() {
		if (commonEncryptProperties.isEncryptSensitiveData()) {
			throw new IllegalStateException("encrypt-sensitive-data=true is not implemented in this migration iteration");
		}
	}

	public Map<String, Object> getInfo(Map<String, Object> filter) {
		Object loginName = filter.get("login_name");
		Object mobile = filter.get("mobile");
		Object operatorType = filter.get("operator_type");
		Object companyId = filter.get("company_id");
		Object passportUid = filter.get("passport_uid");
		Object operatorId = filter.get("operator_id");

		String loginNameArg = loginName != null ? loginName.toString() : null;
		String mobileArg = mobile != null ? mobile.toString() : null;
		String operatorTypeArg = operatorType != null ? operatorType.toString() : null;
		String passportUidArg = passportUid != null ? passportUid.toString() : null;
		Long companyIdArg = toLong(companyId);
		Long operatorIdArg = toLong(operatorId);

		if (selectInfoWouldApplyOnlyOperatorId(loginNameArg, mobileArg, operatorTypeArg, companyIdArg, passportUidArg)
				&& operatorIdArg == null
				&& operatorId != null) {
			String raw = operatorId.toString().trim();
			if (!raw.isEmpty()) {
				return null;
			}
		}

		return operatorsMapper.selectInfo(
				loginNameArg,
				mobileArg,
				operatorTypeArg,
				companyIdArg,
				passportUidArg,
				operatorIdArg);
	}

	/**
	 * True when no {@code selectInfo} WHERE clause would apply except possibly {@code operator_id}
	 * (same criteria as OperatorsMapper.xml {@code <if>} tests).
	 */
	private static boolean selectInfoWouldApplyOnlyOperatorId(
			String loginName,
			String mobile,
			String operatorType,
			Long companyId,
			String passportUid) {
		boolean hasLogin = loginName != null && !loginName.isEmpty();
		boolean hasMobile = mobile != null && !mobile.isEmpty();
		boolean hasOperatorType = operatorType != null && !operatorType.isEmpty();
		boolean hasCompany = companyId != null;
		boolean hasPassport = passportUid != null && !passportUid.isEmpty();
		return !hasLogin && !hasMobile && !hasOperatorType && !hasCompany && !hasPassport;
	}

	public Map<String, Object> getOperatorByMobile(String mobile, String operatorType) {
		return operatorsMapper.getOperatorByMobile(mobile, operatorType);
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** operator_id → username（商品列表 operator_name 等批量补全） */
	public Map<Long, String> mapUsernameByOperatorIds(long companyId, Collection<Long> operatorIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (operatorIds == null || operatorIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getCompanyId, companyId).in(Operators::getOperatorId, operatorIds);
		for (Operators op : operatorsMapper.selectList(w)) {
			if (op.getOperatorId() != null) {
				out.put(op.getOperatorId(), op.getUsername() != null ? op.getUsername() : "");
			}
		}
		return out;
	}

	/** operator_id → login_name（后台操作人展示） */
	public long countAccountManagementList(AccountManagementOperatorsFilter f) {
		return operatorsMapper.countAccountManagementList(f);
	}

	public List<Operators> pageAccountManagementList(
			AccountManagementOperatorsFilter f,
			int offset,
			int limit,
			List<AccountManagementOperatorsFilter.OrderBy> orderBy) {
		return operatorsMapper.pageAccountManagementList(f, offset, limit, orderBy);
	}

	public Map<Long, String> mapLoginNameByOperatorIds(long companyId, Collection<Long> operatorIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (operatorIds == null || operatorIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getCompanyId, companyId).in(Operators::getOperatorId, operatorIds);
		for (Operators op : operatorsMapper.selectList(w)) {
			if (op.getOperatorId() != null) {
				out.put(op.getOperatorId(), op.getLoginName() != null ? op.getLoginName() : "");
			}
		}
		return out;
	}
}
