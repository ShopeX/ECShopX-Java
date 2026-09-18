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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WeappCreateOrUpdateService {

	private final WeappMapper weappMapper;

	public WeappCreateOrUpdateService(WeappMapper weappMapper) {
		this.weappMapper = weappMapper;
	}

	public void saveWeapp(String wxaAppId, Map<String, Object> saveData) {
		String id = wxaAppId == null ? "" : wxaAppId.trim();
		Weapp existing = weappMapper.selectById(id);
		LocalDateTime now = LocalDateTime.now();
		String auditTimeEpoch = String.valueOf(Instant.now().getEpochSecond());
		Weapp row = new Weapp();
		row.setAuthorizerAppid(id);
		row.setCompanyId(longVal(saveData.get("company_id")));
		row.setOperatorId(longVal(saveData.get("operator_id")));
		row.setTemplateId(strVal(saveData.get("template_id"), existing != null ? existing.getTemplateId() : "0"));
		row.setTemplateName(strVal(saveData.get("template_name"), existing != null ? existing.getTemplateName() : ""));
		row.setTemplateVer(strVal(saveData.get("template_ver"), existing != null ? existing.getTemplateVer() : "1"));
		row.setAuditStatus(intVal(saveData.get("audit_status"), existing != null ? existing.getAuditStatus() : 3));
		row.setReleaseStatus(intVal(saveData.get("release_status"), existing != null ? existing.getReleaseStatus() : 0));
		row.setVisitStatus(intVal(saveData.get("visitstatus"), existing != null ? existing.getVisitStatus() : 1));
		row.setReason(strVal(saveData.get("reason"), ""));
		row.setReleaseVer(strVal(saveData.get("release_ver"), existing != null ? existing.getReleaseVer() : ""));
		row.setAuditTime(strVal(saveData.get("audit_time"), auditTimeEpoch));
		row.setUpdatedAt(now);
		if (existing == null) {
			row.setCreatedAt(now);
			row.setDeletedAt(null);
			weappMapper.insert(row);
		} else {
			row.setCreatedAt(existing.getCreatedAt());
			row.setDeletedAt(existing.getDeletedAt());
			weappMapper.updateById(row);
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o, Integer fallback) {
		if (o == null) {
			return fallback == null ? 0 : fallback;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return fallback == null ? 0 : fallback;
		}
	}

	private static String strVal(Object o, String fallback) {
		if (o == null) {
			return fallback == null ? "" : fallback;
		}
		String s = String.valueOf(o).trim();
		return StringUtils.hasText(s) ? s : (fallback == null ? "" : fallback);
	}
}
