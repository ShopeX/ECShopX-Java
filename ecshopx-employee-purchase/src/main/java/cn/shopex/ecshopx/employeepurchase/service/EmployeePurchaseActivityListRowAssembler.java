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

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityListRowAssembler {

	private final ObjectMapper objectMapper;

	public EmployeePurchaseActivityListRowAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toRow(Activities row) {
		return toRow(row, List.of());
	}

	public Map<String, Object> toRow(Activities row, List<Integer> perCapitaLimitfees) {
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
		m.put("enterprise_id", parseEnterpriseIdCsv(row.getEnterpriseId()));
		m.put("display_time", row.getDisplayTime());
		m.put("employee_begin_time", row.getEmployeeBeginTime());
		m.put("employee_end_time", row.getEmployeeEndTime());
		m.put("employee_limitfee", row.getEmployeeLimitfee());

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
		m.put("status", row.getStatus());
		m.put("if_share_store", row.getIfShareStore());
		m.put("price_display_config", parsePriceConfigForResponse(row.getPriceDisplayConfig()));
		m.put(
				"is_discount_description_enabled",
				Boolean.TRUE.equals(row.getIsDiscountDescriptionEnabled()) ? "true" : "false");
		String disc = row.getDiscountDescription();
		m.put("discount_description", disc != null ? disc : "");
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put(
				"is_passphrase_enabled",
				Boolean.TRUE.equals(row.getIsPassphraseEnabled()) ? "true" : "false");

		appendPurchaseModeQuotaFields(m, row, perCapitaLimitfees);
		return m;
	}

	private static void appendPurchaseModeQuotaFields(
			Map<String, Object> m, Activities row, List<Integer> perCapitaLimitfees) {
		String mode = row.getPurchaseMode();
		if (!StringUtils.hasText(mode)) {
			m.put(
					"employee_quota_desc",
					row.getEmployeeLimitfee() == null
							? ""
							: EnterpriseConfigService.fenToDisplay(row.getEmployeeLimitfee()));
			m.put("prepaid_point_desc", "");
			return;
		}
		m.put("purchase_mode", mode);
		m.put("purchase_mode_desc", PurchaseModeSupport.desc(mode));
		String quotaDesc = EnterpriseConfigService.formatQuotaDesc(perCapitaLimitfees);
		if (PurchaseModeSupport.isPrepaidPoint(mode)) {
			m.put("employee_quota_desc", "");
			m.put("prepaid_point_desc", quotaDesc);
		} else {
			m.put("employee_quota_desc", quotaDesc);
			m.put("prepaid_point_desc", "");
		}
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
