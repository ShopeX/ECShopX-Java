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
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseCreateService {

	private final EnterprisesMapper enterprisesMapper;
	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;

	public EnterpriseCreateService(
			EnterprisesMapper enterprisesMapper, EnterpriseEmailBoxMapper enterpriseEmailBoxMapper) {
		this.enterprisesMapper = enterprisesMapper;
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		try {
			long companyId = readCompanyId(operatorJwt);
			long distributorId = readDistributorId(operatorJwt);
			int operatorId = readOperatorId(operatorJwt);

			LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);
			params.put("company_id", companyId);
			params.put("distributor_id", distributorId);
			params.put("operator_id", operatorId);

			validateParams(params);
			applyAuthTypeAndEmployeeCheckRules(params);
			checkExist(params, 0L);

			int now = (int) (System.currentTimeMillis() / 1000);
			Enterprises entity = buildEnterpriseEntity(params, now);
			enterprisesMapper.insert(entity);

			Map<String, Object> result = toEnterpriseRowMap(entity);

			Object authTypeObj = params.get("auth_type");
			if (authTypeObj != null && "email".equals(authTypeObj.toString().trim())) {
				params.put("enterprise_id", entity.getId());
				Map<String, Object> relData = saveRelEmailBox(params, now);
				result.put("relay_host", nullToEmpty(relData.get("relay_host")));
				result.put("smtp_port", nullToEmpty(relData.get("smtp_port")));
				result.put("email_user", nullToEmpty(relData.get("user")));
				result.put("email_password", nullToEmpty(relData.get("password")));
				result.put("email_suffix", nullToEmpty(relData.get("suffix")));
			}

			return result;
		} catch (BadRequestException | ResourceException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateEnterprise(
			String enterpriseIdRaw,
			Map<String, Object> requestFields,
			Map<String, Object> operatorJwt) {
		try {
			long companyId = readCompanyId(operatorJwt);
			long distributorId = readDistributorId(operatorJwt);

			if (!StringUtils.hasText(enterpriseIdRaw)) {
				throw new ResourceException("未查询到更新数据");
			}
			long parsedEnterpriseId;
			try {
				parsedEnterpriseId = Long.parseLong(enterpriseIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("未查询到更新数据");
			}
			if (parsedEnterpriseId <= 0) {
				throw new ResourceException("未查询到更新数据");
			}

			LinkedHashMap<String, Object> params = new LinkedHashMap<>(requestFields);
			params.put("company_id", companyId);
			params.put("distributor_id", distributorId);

			validateParams(params);
			applyAuthTypeAndEmployeeCheckRules(params);

			String empRaw =
					params.get("is_employee_check_enabled") == null
							? ""
							: params.get("is_employee_check_enabled").toString().trim();
			boolean isEmployeeCheckEnabled = "true".equalsIgnoreCase(empRaw);
			params.put("is_employee_check_enabled", isEmployeeCheckEnabled);

			params.put("enterprise_id", parsedEnterpriseId);
			checkExist(params, parsedEnterpriseId);

			Enterprises existing =
					enterprisesMapper.selectOne(
							new LambdaQueryWrapper<Enterprises>()
									.eq(Enterprises::getId, parsedEnterpriseId)
									.eq(Enterprises::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException("未查询到更新数据");
			}

			int now = (int) (System.currentTimeMillis() / 1000);
			LambdaUpdateWrapper<Enterprises> uw =
					new LambdaUpdateWrapper<Enterprises>()
							.eq(Enterprises::getId, parsedEnterpriseId)
							.eq(Enterprises::getCompanyId, companyId)
							.set(Enterprises::getDistributorId, toIntBounded(params.get("distributor_id")))
							.set(Enterprises::getIsEmployeeCheckEnabled, isEmployeeCheckEnabled)
							.set(Enterprises::getName, params.get("name").toString())
							.set(Enterprises::getEnterpriseSn, params.get("enterprise_sn").toString())
							.set(Enterprises::getAuthType, params.get("auth_type").toString())
							.set(Enterprises::getSort, (Integer) params.get("sort"))
							.set(Enterprises::getUpdated, now);

			if (requestFields.containsKey("logo")) {
				Object logoObj = requestFields.get("logo");
				String logoVal;
				if (logoObj == null) {
					logoVal = null;
				} else {
					logoVal = logoObj.toString().trim();
				}
				uw.set(Enterprises::getLogo, logoVal);
			}
			if (requestFields.containsKey("qr_code_bg_image")) {
				Object qrObj = requestFields.get("qr_code_bg_image");
				String qrVal;
				if (qrObj == null) {
					qrVal = null;
				} else {
					qrVal = qrObj.toString().trim();
				}
				uw.set(Enterprises::getQrCodeBgImage, qrVal);
			}

			int rows = enterprisesMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}

			Enterprises entity =
					enterprisesMapper.selectOne(
							new LambdaQueryWrapper<Enterprises>()
									.eq(Enterprises::getId, parsedEnterpriseId)
									.eq(Enterprises::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}

			Map<String, Object> result = toEnterpriseRowMap(entity);

			Object authTypeObj = params.get("auth_type");
			if (authTypeObj != null && "email".equals(authTypeObj.toString().trim())) {
				Map<String, Object> relData = saveRelEmailBox(params, now);
				result.put("relay_host", nullToEmpty(relData.get("relay_host")));
				result.put("smtp_port", nullToEmpty(relData.get("smtp_port")));
				result.put("email_user", nullToEmpty(relData.get("user")));
				result.put("email_password", nullToEmpty(relData.get("password")));
				result.put("email_suffix", nullToEmpty(relData.get("suffix")));
			}

			return result;
		} catch (BadRequestException | ResourceException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void setSort(Map<String, Object> params, Map<String, Object> operatorJwt) {
		try {
			long companyId = readCompanyId(operatorJwt);

			Object enterpriseIdObj = params.get("enterprise_id");
			if (enterpriseIdObj == null || enterpriseIdObj.toString().trim().isEmpty()) {
				throw new BadRequestException("企业ID不能为空");
			}
			long enterpriseId = toLong(enterpriseIdObj);
			if (enterpriseId <= 0) {
				throw new BadRequestException("企业ID不能为空");
			}

			if (!params.containsKey("sort") || params.get("sort") == null) {
				throw new BadRequestException("填写的排序编号超出范围");
			}
			int sortVal;
			try {
				sortVal = parseSortInt(params.get("sort"));
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("填写的排序编号超出范围");
			}
			if (sortVal < 0 || sortVal > 2147483647) {
				throw new BadRequestException("填写的排序编号超出范围");
			}

			Enterprises existing =
					enterprisesMapper.selectOne(
							new LambdaQueryWrapper<Enterprises>()
									.eq(Enterprises::getId, enterpriseId)
									.eq(Enterprises::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException("记录已经删除！");
			}

			int now = (int) (System.currentTimeMillis() / 1000);
			LambdaUpdateWrapper<Enterprises> uw =
					new LambdaUpdateWrapper<Enterprises>()
							.eq(Enterprises::getId, enterpriseId)
							.eq(Enterprises::getCompanyId, companyId)
							.set(Enterprises::getSort, sortVal)
							.set(Enterprises::getUpdated, now);
			int rows = enterprisesMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} catch (BadRequestException | ResourceException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Map<String, Object> params, Map<String, Object> operatorJwt) {
		try {
			long companyId = readCompanyId(operatorJwt);

			Object enterpriseIdObj = params.get("enterprise_id");
			if (enterpriseIdObj == null || enterpriseIdObj.toString().trim().isEmpty()) {
				throw new BadRequestException("企业ID不能为空");
			}
			long enterpriseId = toLong(enterpriseIdObj);
			if (enterpriseId <= 0) {
				throw new BadRequestException("企业ID不能为空");
			}

			if (!params.containsKey("disabled")) {
				throw new BadRequestException("状态不能为空");
			}
			Object disabledObj = params.get("disabled");
			// 后管 el-switch 提交 boolean；form-urlencoded 则为 "true"/"false"；兼容历史 0/1
			if (disabledObj == null) {
				throw new BadRequestException("状态不能为空");
			}
			if (disabledObj instanceof String s && s.trim().isEmpty()) {
				throw new BadRequestException("状态不能为空");
			}
			boolean parsedBoolean = parseDisabledLoose(disabledObj);

			Enterprises existing =
					enterprisesMapper.selectOne(
							new LambdaQueryWrapper<Enterprises>()
									.eq(Enterprises::getId, enterpriseId)
									.eq(Enterprises::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException("记录已经删除！");
			}

			int now = (int) (System.currentTimeMillis() / 1000);
			LambdaUpdateWrapper<Enterprises> uw =
					new LambdaUpdateWrapper<Enterprises>()
							.eq(Enterprises::getId, enterpriseId)
							.eq(Enterprises::getCompanyId, companyId)
							.set(Enterprises::getDisabled, parsedBoolean)
							.set(Enterprises::getUpdated, now);
			int rows = enterprisesMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} catch (BadRequestException | ResourceException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	private void validateParams(LinkedHashMap<String, Object> params) {
		Object nameObj = params.get("name");
		if (nameObj == null || nameObj.toString().trim().isEmpty()) {
			throw new BadRequestException("名称不能为空");
		}
		params.put("name", nameObj.toString().trim());

		Object snObj = params.get("enterprise_sn");
		if (snObj == null || snObj.toString().trim().isEmpty()) {
			throw new BadRequestException("企业编码不能为空");
		}
		params.put("enterprise_sn", snObj.toString().trim());

		Object authObj = params.get("auth_type");
		if (authObj == null || authObj.toString().trim().isEmpty()) {
			throw new BadRequestException("登录类型不能为空");
		}
		params.put("auth_type", authObj.toString().trim());

		if (!params.containsKey("sort") || params.get("sort") == null) {
			throw new BadRequestException("填写的排序编号超出范围");
		}
		int sortVal;
		try {
			sortVal = parseSortInt(params.get("sort"));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("填写的排序编号超出范围");
		}
		if (sortVal < 0 || sortVal > 2147483647) {
			throw new BadRequestException("填写的排序编号超出范围");
		}
		params.put("sort", sortVal);

		String authTrim = params.get("auth_type").toString();
		if ("email".equals(authTrim)) {
			requireNonBlankTrim(params, "relay_host", "SMTP服务器不能为空");
			requireNonBlankTrim(params, "smtp_port", "邮箱端口号不能为空");
			requireNonBlankTrim(params, "email_user", "请填写正确的发件邮箱");
			requireNonBlankTrim(params, "email_password", "邮箱密码不能为空");
			requireNonBlankTrim(params, "email_suffix", "收件邮箱后缀不能为空");
		}
	}

	private static void requireNonBlankTrim(LinkedHashMap<String, Object> params, String key, String message) {
		Object v = params.get(key);
		if (v == null || v.toString().trim().isEmpty()) {
			throw new BadRequestException(message);
		}
		params.put(key, v.toString().trim());
	}

	private static int parseSortInt(Object sortObj) {
		if (sortObj instanceof Number n) {
			long lv = n.longValue();
			if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
				throw new IllegalArgumentException();
			}
			return n.intValue();
		}
		String s = sortObj.toString().trim();
		if (s.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (s.contains(".") || s.contains("e") || s.contains("E")) {
			throw new IllegalArgumentException();
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
	}

	private void applyAuthTypeAndEmployeeCheckRules(LinkedHashMap<String, Object> params) {
		String authTrim = params.get("auth_type").toString();
		if ("mobile".equals(authTrim) || "account".equals(authTrim)) {
			params.put("is_employee_check_enabled", "true");
		} else if ("email".equals(authTrim)) {
			params.put("is_employee_check_enabled", "false");
		}
	}

	private void checkExist(LinkedHashMap<String, Object> params, long excludeEnterpriseId) {
		long companyId = toLong(params.get("company_id"));
		String nameTrim = params.get("name").toString();
		String snTrim = params.get("enterprise_sn").toString();

		Enterprises nameRow =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getName, nameTrim)
								.last("LIMIT 1"));
		if (nameRow != null && !Objects.equals(nameRow.getId(), excludeEnterpriseId)) {
			throw new ResourceException("企业名称不能重复");
		}

		Enterprises snRow =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getEnterpriseSn, snTrim)
								.last("LIMIT 1"));
		if (snRow != null && !Objects.equals(snRow.getId(), excludeEnterpriseId)) {
			throw new ResourceException("企业编码不能重复");
		}

		String authTrim = params.get("auth_type").toString();
		if ("email".equals(authTrim)) {
			String emailSuffixTrim = params.get("email_suffix").toString();
			EnterpriseEmailBox suffixRow =
					enterpriseEmailBoxMapper.selectOne(
							new LambdaQueryWrapper<EnterpriseEmailBox>()
									.eq(EnterpriseEmailBox::getCompanyId, companyId)
									.eq(EnterpriseEmailBox::getSuffix, emailSuffixTrim)
									.last("LIMIT 1"));
			if (suffixRow != null && !Objects.equals(suffixRow.getEnterpriseId(), excludeEnterpriseId)) {
				throw new ResourceException("企业收件邮箱后缀不能重复");
			}
		}
	}

	private Enterprises buildEnterpriseEntity(LinkedHashMap<String, Object> params, int now) {
		Enterprises entity = new Enterprises();
		entity.setCompanyId(toLong(params.get("company_id")));
		entity.setDistributorId(toIntBounded(params.get("distributor_id")));
		entity.setOperatorId(toIntBounded(params.get("operator_id")));
		entity.setName(params.get("name").toString());
		entity.setEnterpriseSn(params.get("enterprise_sn").toString());

		Object logoObj = params.get("logo");
		if (logoObj != null && !logoObj.toString().trim().isEmpty()) {
			entity.setLogo(logoObj.toString().trim());
		} else {
			entity.setLogo(null);
		}

		Object qrObj = params.get("qr_code_bg_image");
		if (qrObj != null && !qrObj.toString().trim().isEmpty()) {
			entity.setQrCodeBgImage(qrObj.toString().trim());
		} else {
			entity.setQrCodeBgImage(null);
		}

		entity.setAuthType(params.get("auth_type").toString());
		entity.setSort((Integer) params.get("sort"));

		String s =
				params.get("is_employee_check_enabled") == null
						? ""
						: params.get("is_employee_check_enabled").toString().trim();
		entity.setIsEmployeeCheckEnabled("true".equals(s));

		entity.setDisabled(false);
		entity.setCreated(now);
		entity.setUpdated(now);
		return entity;
	}

	private Map<String, Object> toEnterpriseRowMap(Enterprises e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("qr_code_bg_image", e.getQrCodeBgImage());
		m.put("is_employee_check_enabled", e.getIsEmployeeCheckEnabled());
		m.put("name", e.getName());
		m.put("enterprise_sn", e.getEnterpriseSn());
		m.put("logo", e.getLogo());
		m.put("auth_type", e.getAuthType());
		m.put("disabled", Boolean.TRUE.equals(e.getDisabled()));
		m.put("sort", e.getSort());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private Map<String, Object> saveRelEmailBox(LinkedHashMap<String, Object> params, int now) {
		long companyId = toLong(params.get("company_id"));
		long enterpriseId = toLong(params.get("enterprise_id"));
		String relayHost = params.get("relay_host").toString();
		String smtpPort = params.get("smtp_port").toString();
		String user = params.get("email_user").toString();
		String password = params.get("email_password").toString();
		String suffix = params.get("email_suffix").toString();

		long cnt =
				enterpriseEmailBoxMapper.selectCount(
						new LambdaQueryWrapper<EnterpriseEmailBox>()
								.eq(EnterpriseEmailBox::getCompanyId, companyId)
								.eq(EnterpriseEmailBox::getEnterpriseId, enterpriseId));

		if (cnt > 0) {
			EnterpriseEmailBox existing =
					enterpriseEmailBoxMapper.selectOne(
							new LambdaQueryWrapper<EnterpriseEmailBox>()
									.eq(EnterpriseEmailBox::getCompanyId, companyId)
									.eq(EnterpriseEmailBox::getEnterpriseId, enterpriseId)
									.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException("未查询到更新数据");
			}
			existing.setRelayHost(relayHost);
			existing.setSmtpPort(smtpPort);
			existing.setUser(user);
			existing.setPassword(password);
			existing.setSuffix(suffix);
			existing.setUpdated(now);
			enterpriseEmailBoxMapper.updateById(existing);
			return emailBoxToMap(existing);
		}

		EnterpriseEmailBox row = new EnterpriseEmailBox();
		row.setCompanyId(companyId);
		row.setEnterpriseId(enterpriseId);
		row.setRelayHost(relayHost);
		row.setSmtpPort(smtpPort);
		row.setUser(user);
		row.setPassword(password);
		row.setSuffix(suffix);
		row.setCreated(now);
		row.setUpdated(now);
		enterpriseEmailBoxMapper.insert(row);
		return emailBoxToMap(row);
	}

	private static Map<String, Object> emailBoxToMap(EnterpriseEmailBox b) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", b.getId());
		m.put("company_id", b.getCompanyId());
		m.put("enterprise_id", b.getEnterpriseId());
		m.put("relay_host", b.getRelayHost());
		m.put("smtp_port", b.getSmtpPort());
		m.put("user", b.getUser());
		m.put("password", b.getPassword());
		m.put("suffix", b.getSuffix());
		m.put("created", b.getCreated());
		m.put("updated", b.getUpdated());
		return m;
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : o.toString();
	}

	private static int toIntBounded(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
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

	private static boolean parseDisabledLoose(Object o) {
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
}
