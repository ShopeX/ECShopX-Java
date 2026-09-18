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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseAdminService;
import cn.shopex.ecshopx.employeepurchase.support.ActivityListDisplayStatusQuery;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseActivityInfoService {

	private final ActivitiesMapper activitiesMapper;
	private final ObjectMapper objectMapper;
	private final ActivityPassphraseAdminService activityPassphraseAdminService;
	private final EnterpriseConfigService enterpriseConfigService;

	public EmployeePurchaseActivityInfoService(
			ActivitiesMapper activitiesMapper,
			ObjectMapper objectMapper,
			ActivityPassphraseAdminService activityPassphraseAdminService,
			EnterpriseConfigService enterpriseConfigService) {
		this.activitiesMapper = activitiesMapper;
		this.objectMapper = objectMapper;
		this.activityPassphraseAdminService = activityPassphraseAdminService;
		this.enterpriseConfigService = enterpriseConfigService;
	}

	public Map<String, Object> getActivityInfo(long companyId, String activityIdPath) {
		String trimmed = activityIdPath == null ? "" : activityIdPath.trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("活动不存在");
		}
		long activityPk;
		try {
			activityPk = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("活动不存在");
		}

		Activities row = activitiesMapper.selectOne(
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, activityPk));
		if (row == null) {
			throw new ResourceException("活动不存在");
		}

		int now = (int) (System.currentTimeMillis() / 1000);
		LinkedHashMap<String, Object> statusHolder = new LinkedHashMap<>();
		ActivityListDisplayStatusQuery.applyDisplayStatusRewrite(
				statusHolder,
				row.getStatus(),
				epochOrZero(row.getDisplayTime()),
				epochOrZero(row.getEmployeeBeginTime()),
				epochOrZero(row.getEmployeeEndTime()),
				row.getRelativeBeginTime() == null ? null : row.getRelativeBeginTime().longValue(),
				row.getRelativeEndTime() == null ? null : row.getRelativeEndTime().longValue(),
				now);
		String outStatus =
				statusHolder.get("status") == null
						? row.getStatus()
						: statusHolder.get("status").toString();
		String outStatusDesc =
				statusHolder.get("status_desc") == null ? null : statusHolder.get("status_desc").toString();

		List<Long> enterpriseIds = parseEnterpriseIdCsv(row.getEnterpriseId());
		boolean newContract = PurchaseModeSupport.isNewContractActivity(row);

		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("operator_id", row.getOperatorId());
		m.put("name", row.getName());
		m.put("title", row.getTitle());
		m.put("pages_template_id", row.getPagesTemplateId());
		m.put("pic", row.getPic());
		m.put("share_pic", row.getSharePic());
		m.put("list_pic", row.getListPic());
		m.put("enterprise_id", enterpriseIds);
		m.put("display_time", row.getDisplayTime());
		m.put("employee_begin_time", row.getEmployeeBeginTime());
		m.put("employee_end_time", row.getEmployeeEndTime());
		if (newContract) {
			m.put("purchase_mode", row.getPurchaseMode());
			m.put("purchase_mode_desc", PurchaseModeSupport.desc(row.getPurchaseMode()));
		} else {
			m.put("employee_limitfee", row.getEmployeeLimitfee());
		}

		boolean ifRel = Boolean.TRUE.equals(row.getIfRelativeJoin());
		m.put("if_relative_join", ifRel);
		if (ifRel) {
			m.put("invite_limit", row.getInviteLimit());
			m.put("relative_begin_time", row.getRelativeBeginTime());
			m.put("relative_end_time", row.getRelativeEndTime());
			m.put("if_share_limitfee", row.getIfShareLimitfee());
		} else {
			m.put("invite_limit", row.getInviteLimit() != null ? row.getInviteLimit() : 0);
			m.put("relative_begin_time", null);
			m.put("relative_end_time", null);
			m.put("if_share_limitfee", Boolean.TRUE.equals(row.getIfShareLimitfee()) ? 1 : 0);
		}
		m.put("relative_limitfee", row.getRelativeLimitfee());
		m.put("minimum_amount", row.getMinimumAmount());
		m.put("close_modify_hours_after_activity", row.getCloseModifyHoursAfterActivity());
		m.put("status", outStatus);
		if (outStatusDesc != null) {
			m.put("status_desc", outStatusDesc);
		}
		m.put("if_share_store", row.getIfShareStore());
		m.put("price_display_config", parsePriceConfigForResponse(row.getPriceDisplayConfig()));
		m.put(
				"is_discount_description_enabled",
				Boolean.TRUE.equals(row.getIsDiscountDescriptionEnabled()) ? "true" : "false");
		String disc = row.getDiscountDescription();
		m.put("discount_description", disc != null ? disc : "");
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		if (newContract) {
			boolean passphraseEnabled = Boolean.TRUE.equals(row.getIsPassphraseEnabled());
			m.put("is_passphrase_enabled", passphraseEnabled ? "true" : "false");
			m.put(
					"enterprise_configs",
					enterpriseConfigService.buildResponseList(companyId, activityPk, passphraseEnabled));
		} else {
			activityPassphraseAdminService.appendAdminPassphraseFields(row, m);
		}
		return m;
	}

	private static int epochOrZero(Integer v) {
		return v == null ? 0 : v.intValue();
	}

	private List<Long> parseEnterpriseIdCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : csv.split(",")) {
			String p = part.trim();
			if (p.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(p));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private Object parsePriceConfigForResponse(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (Exception e) {
			return null;
		}
	}
}
