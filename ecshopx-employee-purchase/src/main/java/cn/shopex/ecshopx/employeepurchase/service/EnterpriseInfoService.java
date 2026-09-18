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

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseInfoService {

	private final EnterprisesMapper enterprisesMapper;
	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;

	public EnterpriseInfoService(
			EnterprisesMapper enterprisesMapper, EnterpriseEmailBoxMapper enterpriseEmailBoxMapper) {
		this.enterprisesMapper = enterprisesMapper;
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
	}

	public Object getEnterpriseInfo(String enterpriseId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);

		String trimmed = enterpriseId == null ? "" : enterpriseId.trim();
		if (trimmed.isEmpty()) {
			return List.of();
		}
		long parsedId;
		try {
			parsedId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return List.of();
		}

		Enterprises row =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getId, parsedId)
								.eq(Enterprises::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			return List.of();
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("id", row.getId());
		data.put("company_id", row.getCompanyId());
		data.put("distributor_id", row.getDistributorId());
		data.put("operator_id", row.getOperatorId());
		data.put("qr_code_bg_image", row.getQrCodeBgImage());
		data.put("name", row.getName());
		data.put("enterprise_sn", row.getEnterpriseSn());
		data.put("logo", row.getLogo());
		data.put("auth_type", row.getAuthType());
		data.put("disabled", Boolean.TRUE.equals(row.getDisabled()));
		data.put("sort", row.getSort());
		data.put("created", row.getCreated());
		data.put("updated", row.getUpdated());
		data.put(
				"is_employee_check_enabled",
				Boolean.TRUE.equals(row.getIsEmployeeCheckEnabled()) ? "true" : "false");

		if (Objects.equals("email", row.getAuthType())) {
			EnterpriseEmailBox box =
					enterpriseEmailBoxMapper.selectOne(
							new LambdaQueryWrapper<EnterpriseEmailBox>()
									.eq(EnterpriseEmailBox::getCompanyId, companyId)
									.eq(EnterpriseEmailBox::getEnterpriseId, row.getId())
									.last("LIMIT 1"));
			if (box != null) {
				data.put("relay_host", box.getRelayHost());
				data.put("smtp_port", box.getSmtpPort());
				data.put("email_user", box.getUser());
				data.put("email_password", box.getPassword());
				data.put("email_suffix", box.getSuffix());
			}
		}

		return data;
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
}
