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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EmployeeCreateService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"([a-z0-9]*[-_\\.]?[a-z0-9]+)*@([a-z0-9]*[-_]?[a-z0-9]+)+[\\.][a-z]{2,3}([\\.][a-z]{2})?",
			Pattern.CASE_INSENSITIVE);

	private final EnterprisesMapper enterprisesMapper;
	private final EmployeesMapper employeesMapper;

	public EmployeeCreateService(EnterprisesMapper enterprisesMapper, EmployeesMapper employeesMapper) {
		this.enterprisesMapper = enterprisesMapper;
		this.employeesMapper = employeesMapper;
	}

	public Map<String, Object> create(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		long distributorId = readDistributorId(operatorJwt);
		int operatorId = readOperatorId(operatorJwt);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);
		params.put("company_id", companyId);
		params.put("distributor_id", distributorId);
		params.put("operator_id", operatorId);
		params.put("user_id", 0L);

		Object nameObj = params.get("name");
		if (nameObj == null || nameObj.toString().trim().isEmpty()) {
			throw new BadRequestException("姓名必填");
		}
		String nameTrimmed = nameObj.toString().trim();
		params.put("name", nameTrimmed);

		long enterpriseId = parseEnterpriseId(params.get("enterprise_id"));
		params.put("enterprise_id", enterpriseId);

		Enterprises enterprise = enterprisesMapper.selectOne(
				new LambdaQueryWrapper<Enterprises>()
						.eq(Enterprises::getCompanyId, companyId)
						.eq(Enterprises::getId, enterpriseId)
						.last("LIMIT 1"));
		if (enterprise == null) {
			throw new ResourceException("企业不存在");
		}
		if (Boolean.FALSE.equals(enterprise.getIsEmployeeCheckEnabled())) {
			throw new ResourceException("该企业不需要添加员工");
		}

		String authType = enterprise.getAuthType();
		checkParams(params, companyId, enterpriseId, authType);

		int now = (int) (System.currentTimeMillis() / 1000);
		Employees entity = buildEmployeesEntity(params, now);
		employeesMapper.insert(entity);

		return toColumnNamesMap(entity);
	}

	public Map<String, Object> updateOneBy(
			String employeeIdRaw, Map<String, Object> mergedInput, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		if (mergedInput.containsKey("name")) {
			params.put("name", mergedInput.get("name"));
		} else {
			params.put("name", null);
		}
		if (mergedInput.containsKey("mobile")) {
			params.put("mobile", mergedInput.get("mobile"));
		} else {
			params.put("mobile", null);
		}
		if (mergedInput.containsKey("account")) {
			params.put("account", mergedInput.get("account"));
		} else {
			params.put("account", null);
		}
		if (mergedInput.containsKey("auth_code")) {
			params.put("auth_code", mergedInput.get("auth_code"));
		} else {
			params.put("auth_code", null);
		}
		if (mergedInput.containsKey("email")) {
			params.put("email", mergedInput.get("email"));
		} else {
			params.put("email", null);
		}
		params.put("distributor_id", readDistributorId(operatorJwt));
		params.put("operator_id", readOperatorId(operatorJwt));

		Employees entity =
				employeesMapper.selectOne(buildEmployeeUpdateLookup(companyId, employeeIdRaw).last("LIMIT 1"));
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}

		if (params.get("name") != null) {
			entity.setName(params.get("name").toString());
		}
		if (params.get("mobile") != null) {
			entity.setMobile(stringOrNull(params.get("mobile")));
		}
		if (params.get("account") != null) {
			entity.setAccount(stringOrNull(params.get("account")));
		}
		if (params.get("auth_code") != null) {
			entity.setAuthCode(stringOrNull(params.get("auth_code")));
		}
		if (params.get("email") != null) {
			entity.setEmail(stringOrNull(params.get("email")));
		}
		entity.setDistributorId(toIntBounded(params.get("distributor_id")));
		entity.setOperatorId(toIntBounded(params.get("operator_id")));

		entity.setUpdated((int) (System.currentTimeMillis() / 1000));

		int affected = employeesMapper.updateById(entity);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnNamesMap(entity);
	}

	public Map<String, Object> deleteByCompanyAndEmployeeId(
			String employeeIdRaw, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		LambdaQueryWrapper<Employees> wrapper = buildEmployeeDeleteCondition(companyId, employeeIdRaw);
		employeesMapper.delete(wrapper);
		return Map.of("status", Boolean.TRUE);
	}

	/**
	 * Delete condition mirrors update id resolution when non-empty, but null/blank path uses id = -1 so no rows match
	 * (idempotent success). Non-numeric tokens use CAST(id AS CHAR) equality like update lookup.
	 */
	private static LambdaQueryWrapper<Employees> buildEmployeeDeleteCondition(
			long companyId, String employeeIdRaw) {
		LambdaQueryWrapper<Employees> w =
				new LambdaQueryWrapper<Employees>().eq(Employees::getCompanyId, companyId);
		if (employeeIdRaw == null) {
			return w.eq(Employees::getId, -1L);
		}
		String trimmed = employeeIdRaw.trim();
		if (trimmed.isEmpty()) {
			return w.eq(Employees::getId, -1L);
		}
		try {
			long id = Long.parseLong(trimmed);
			if (id <= 0) {
				return w.eq(Employees::getId, -1L);
			}
			return w.eq(Employees::getId, id);
		} catch (NumberFormatException e) {
			return w.apply("CAST(id AS CHAR) = {0}", trimmed);
		}
	}

	/**
	 * Numeric path segments use primary-key equality; non-numeric segments are matched as text so lookup returns no
	 * row when the token does not identify a stored id.
	 */
	private static LambdaQueryWrapper<Employees> buildEmployeeUpdateLookup(long companyId, String employeeIdRaw) {
		if (employeeIdRaw == null) {
			throw new BadRequestException("employeeId 无效");
		}
		String trimmed = employeeIdRaw.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("employeeId 无效");
		}
		LambdaQueryWrapper<Employees> w =
				new LambdaQueryWrapper<Employees>().eq(Employees::getCompanyId, companyId);
		try {
			long id = Long.parseLong(trimmed);
			if (id <= 0) {
				throw new BadRequestException("employeeId 无效");
			}
			return w.eq(Employees::getId, id);
		} catch (NumberFormatException e) {
			return w.apply("CAST(id AS CHAR) = {0}", trimmed);
		}
	}

	private void checkParams(
			LinkedHashMap<String, Object> params, long companyId, long enterpriseId, String authType) {
		String at = authType == null ? "" : authType;
		switch (at) {
			case "qr_code":
			case "mobile":
				checkMobileBranch(params, companyId, enterpriseId);
				break;
			case "email":
				checkEmailBranch(params, companyId, enterpriseId);
				break;
			case "account":
				checkAccountBranch(params, companyId, enterpriseId);
				break;
			default:
				throw new ResourceException("验证类型有误！");
		}
	}

	private void checkMobileBranch(LinkedHashMap<String, Object> params, long companyId, long enterpriseId) {
		String mobileRaw = stringOrNull(params.get("mobile"));
		if (mobileRaw == null) {
			throw new BadRequestException("请填写正确的手机号");
		}
		String mobileTrim = mobileRaw.trim();
		if (!MOBILE_PATTERN.matcher(mobileTrim).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
		params.put("mobile", mobileTrim);
		long cnt = employeesMapper.selectCount(
				new LambdaQueryWrapper<Employees>()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getMobile, mobileTrim));
		if (cnt > 0) {
			throw new ResourceException("手机号已经存在");
		}
	}

	private void checkEmailBranch(LinkedHashMap<String, Object> params, long companyId, long enterpriseId) {
		String emailRaw = stringOrNull(params.get("email"));
		if (emailRaw == null || emailRaw.trim().isEmpty()) {
			throw new BadRequestException("请填写邮箱");
		}
		String emailTrim = emailRaw.trim();
		if (!EMAIL_PATTERN.matcher(emailTrim).matches()) {
			throw new BadRequestException("请填写正确的邮箱");
		}
		params.put("email", emailTrim);
		long cnt = employeesMapper.selectCount(
				new LambdaQueryWrapper<Employees>()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getEmail, emailTrim));
		if (cnt > 0) {
			throw new ResourceException("邮箱已经存在");
		}
	}

	private void checkAccountBranch(LinkedHashMap<String, Object> params, long companyId, long enterpriseId) {
		boolean issetBoth =
				params.containsKey("account")
						&& params.containsKey("auth_code")
						&& params.get("account") != null
						&& params.get("auth_code") != null;
		boolean accountFalsy = issetBoth && isBlankOrZero(params.get("account"));
		boolean authCodeFalsy =
				!params.containsKey("auth_code")
						|| params.get("auth_code") == null
						|| isBlankOrZero(params.get("auth_code"));
		if ((issetBoth && accountFalsy) || authCodeFalsy) {
			throw new BadRequestException("请填写账号密码");
		}
		String accountTrim = params.get("account").toString().trim();
		params.put("account", accountTrim);
		long cnt = employeesMapper.selectCount(
				new LambdaQueryWrapper<Employees>()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getAccount, accountTrim));
		if (cnt > 0) {
			throw new ResourceException("账号已经存在");
		}
	}

	private static boolean isBlankOrZero(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		String s = o.toString().trim();
		return s.isEmpty() || "0".equals(s);
	}

	private Employees buildEmployeesEntity(LinkedHashMap<String, Object> params, int now) {
		Employees e = new Employees();
		if (params.containsKey("company_id") && params.get("company_id") != null) {
			e.setCompanyId(toLongObject(params.get("company_id")));
		}
		if (params.containsKey("distributor_id") && params.get("distributor_id") != null) {
			e.setDistributorId(toIntBounded(params.get("distributor_id")));
		}
		if (params.containsKey("operator_id") && params.get("operator_id") != null) {
			e.setOperatorId(toIntBounded(params.get("operator_id")));
		}
		if (params.containsKey("name") && params.get("name") != null) {
			e.setName(params.get("name").toString());
		}
		if (params.containsKey("mobile") && params.get("mobile") != null) {
			e.setMobile(stringOrNull(params.get("mobile")));
		}
		if (params.containsKey("account") && params.get("account") != null) {
			e.setAccount(stringOrNull(params.get("account")));
		}
		if (params.containsKey("email") && params.get("email") != null) {
			e.setEmail(stringOrNull(params.get("email")));
		}
		if (params.containsKey("enterprise_id") && params.get("enterprise_id") != null) {
			e.setEnterpriseId(toLongObject(params.get("enterprise_id")));
		}
		if (params.containsKey("auth_code") && params.get("auth_code") != null) {
			e.setAuthCode(stringOrNull(params.get("auth_code")));
		}
		e.setUserId(0L);
		if (params.containsKey("member_mobile") && params.get("member_mobile") != null) {
			e.setMemberMobile(stringOrNull(params.get("member_mobile")));
		}
		e.setCreated(now);
		e.setUpdated(now);
		if (params.containsKey("disabled") && params.get("disabled") != null) {
			e.setDisabled(parseBooleanLoose(params.get("disabled")));
		}
		return e;
	}

	private static Boolean parseBooleanLoose(Object o) {
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = o.toString().trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return Boolean.parseBoolean(s);
	}

	private static Long toLongObject(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static int toIntBounded(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	private Map<String, Object> toColumnNamesMap(Employees e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("name", e.getName());
		m.put("mobile", e.getMobile());
		m.put("account", e.getAccount());
		m.put("email", e.getEmail());
		m.put("enterprise_id", e.getEnterpriseId());
		m.put("auth_code", e.getAuthCode());
		m.put("user_id", e.getUserId());
		m.put("member_mobile", e.getMemberMobile());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("disabled", e.getDisabled());
		return m;
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readDistributorId(Map<String, Object> operatorJwt) {
		Object v = operatorJwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	private static int readOperatorId(Map<String, Object> operatorJwt) {
		Object o = operatorJwt.get("operator_id");
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringOrNull(Object o) {
		return o == null ? null : o.toString();
	}

	private static long parseEnterpriseId(Object o) {
		if (o == null) {
			throw new BadRequestException("企业必填");
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("企业必填");
		}
		try {
			if (o instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业必填");
		}
	}
}
