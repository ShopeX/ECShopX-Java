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

package cn.shopex.ecshopx.companys.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.OpenapiJurisdictionSysuserInput;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserBusinessFail;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserLegacyZeroCode;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserResult;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserSuccess;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsOpenService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1JurisdictionSysuserService {

	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsOpenService operatorsOpenService;
	private final OperatorsMapper operatorsMapper;

	public OpenapiThirdApiV1JurisdictionSysuserService(
			OperatorsQueryService operatorsQueryService,
			OperatorsOpenService operatorsOpenService,
			OperatorsMapper operatorsMapper) {
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsOpenService = operatorsOpenService;
		this.operatorsMapper = operatorsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public SysuserResult executeOpenapiSysuser(long companyId, OpenapiJurisdictionSysuserInput input) {
		Map<String, Object> admin =
				operatorsQueryService.getInfo(
						new HashMap<>(
								Map.of(
										"mobile", nullToEmpty(input.shopexId()),
										"operator_type", "admin")));
		if (!companyIdMatches(companyId, admin)) {
			return new SysuserBusinessFail("配置错误");
		}

		Map<String, Object> operator =
				operatorsQueryService.getInfo(
						new HashMap<>(
								Map.of(
										"mobile", nullToEmpty(input.mobile()),
										"operator_type", nullToEmpty(input.operatorType()))));
		if (operator != null && !operator.isEmpty()) {
			Long adminCompanyId = toLong(admin.get("company_id"));
			if (adminCompanyId == null || !companyIdMatches(adminCompanyId, operator)) {
				return new SysuserBusinessFail("帐号已存在");
			}
			return updateOperator(companyId, input);
		}
		return addOperator(companyId, input);
	}

	private SysuserResult addOperator(long companyId, OpenapiJurisdictionSysuserInput input) {
		if ("admin".equals(input.operatorType())) {
			Map<String, Object> openData = new HashMap<>();
			openData.put("eid", nullToEmpty(input.eid()));
			openData.put("mobile", nullToEmpty(input.mobile()));
			openData.put("passport_uid", nullToEmpty(input.passportUid()));
			openData.put("password", randomPlainPassword());
			try {
				Map<String, Object> result = operatorsOpenService.open(openData);
				if (result == null || result.isEmpty()) {
					return new SysuserBusinessFail("保存失败");
				}
				return new SysuserSuccess();
			} catch (ResourceException ex) {
				if ("账号已开通".equals(ex.getMessage())) {
					return new SysuserLegacyZeroCode("账号已开通");
				}
				return new SysuserLegacyZeroCode(ex.getMessage());
			}
		}

		Map<String, Object> created = createOperatorLite(input, companyId);
		if (created == null || created.isEmpty()) {
			return new SysuserBusinessFail("保存失败");
		}
		return new SysuserSuccess();
	}

	private SysuserResult updateOperator(long companyId, OpenapiJurisdictionSysuserInput input) {
		LambdaQueryWrapper<Operators> query = new LambdaQueryWrapper<>();
		query.eq(Operators::getCompanyId, companyId).eq(Operators::getMobile, input.mobile());
		Operators existing = operatorsMapper.selectOne(query);
		if (existing == null) {
			return new SysuserLegacyZeroCode("未查询到更新数据");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Operators> update = new LambdaUpdateWrapper<>();
		update.eq(Operators::getCompanyId, companyId).eq(Operators::getMobile, input.mobile());
		if (StringUtils.hasText(input.username())) {
			update.set(Operators::getUsername, input.username());
		}
		if (StringUtils.hasText(input.password())) {
			update.set(Operators::getPassword, input.password());
		}
		update.set(Operators::getUpdated, now);

		int rows = operatorsMapper.update(null, update);
		if (rows <= 0) {
			return new SysuserBusinessFail("保存失败");
		}
		return new SysuserSuccess();
	}

	private Map<String, Object> createOperatorLite(OpenapiJurisdictionSysuserInput input, long companyId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Operators op = new Operators();
		op.setOperatorType(nullToEmpty(input.operatorType()));
		op.setLoginName(nullToEmpty(input.loginName()));
		op.setMobile(nullToEmpty(input.mobile()));
		op.setCompanyId(companyId);
		op.setUsername(nullToEmpty(input.username()));
		op.setRegionauthId(0L);
		op.setShopIds("[]");
		op.setDistributorIds("[]");
		op.setPassword(randomBcryptPassword());
		op.setCreated(now);
		op.setUpdated(now);
		operatorsMapper.insert(op);
		Long operatorId = op.getOperatorId();
		if (operatorId == null) {
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("operator_id", operatorId);
		out.put("company_id", companyId);
		out.put("mobile", op.getMobile());
		return out;
	}

	private static boolean companyIdMatches(long expected, Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return false;
		}
		Long actual = toLong(row.get("company_id"));
		return actual != null && actual == expected;
	}

	private static String randomBcryptPassword() {
		return BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt());
	}

	private static String randomPlainPassword() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private static String nullToEmpty(String value) {
		return value != null ? value : "";
	}

	private static Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
