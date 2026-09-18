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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WeappRowQueryService {

	private final WeappMapper weappMapper;

	public WeappRowQueryService(WeappMapper weappMapper) {
		this.weappMapper = weappMapper;
	}

	public Optional<Weapp> findByCompanyAndWxa(long companyId, String wxaAppId) {
		LambdaQueryWrapper<Weapp> w = new LambdaQueryWrapper<>();
		w.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getAuthorizerAppid, wxaAppId.trim())
				.isNull(Weapp::getDeletedAt)
				.last("LIMIT 1");
		Weapp row = weappMapper.selectOne(w);
		return Optional.ofNullable(row);
	}

	public Optional<Map<String, Object>> getWeappInfo(long companyId, String wxaAppId) {
		return findByCompanyAndWxa(companyId, wxaAppId).map(this::toInfoMap);
	}

	public LinkedHashMap<String, Object> toAuthorizerListStyleMap(Weapp weapp) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_appid", weapp.getAuthorizerAppid());
		m.put("operator_id", weapp.getOperatorId());
		m.put("company_id", weapp.getCompanyId());
		m.put("reason", weapp.getReason());
		m.put("audit_status", weapp.getAuditStatus());
		m.put("release_status", weapp.getReleaseStatus());
		m.put("audit_time", weapp.getAuditTime());
		m.put("template_id", parseTemplateIdIntForPayload(weapp.getTemplateId()));
		m.put("template_name", weapp.getTemplateName());
		m.put("template_ver", weapp.getTemplateVer());
		m.put("release_ver", weapp.getReleaseVer());
		m.put("visitstatus", weapp.getVisitStatus());
		return m;
	}

	public Optional<Map<String, Object>> findWeappPayloadByCompanyAndTemplateName(long companyId, String templateName) {
		LambdaQueryWrapper<Weapp> w = new LambdaQueryWrapper<>();
		w.eq(Weapp::getCompanyId, companyId);
		if (templateName == null) {
			w.isNull(Weapp::getTemplateName);
		} else {
			w.eq(Weapp::getTemplateName, templateName);
		}
		w.isNull(Weapp::getDeletedAt).last("LIMIT 1");
		Weapp entity = weappMapper.selectOne(w);
		if (entity == null) {
			return Optional.empty();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_appid", entity.getAuthorizerAppid());
		m.put("operator_id", entity.getOperatorId());
		m.put("company_id", entity.getCompanyId());
		m.put("reason", entity.getReason());
		m.put("audit_status", entity.getAuditStatus());
		m.put("release_status", entity.getReleaseStatus());
		m.put("audit_time", entity.getAuditTime());
		m.put("template_id", parseTemplateIdIntForPayload(entity.getTemplateId()));
		m.put("template_name", entity.getTemplateName());
		m.put("template_ver", entity.getTemplateVer());
		m.put("release_ver", entity.getReleaseVer());
		m.put("visitstatus", entity.getVisitStatus());
		return Optional.of(m);
	}

	private static int parseTemplateIdIntForPayload(String rawTid) {
		int tid = 0;
		if (rawTid != null && !rawTid.isBlank()) {
			try {
				tid = Integer.parseInt(rawTid.trim());
			} catch (NumberFormatException e) {
				tid = 0;
			}
		}
		return tid;
	}

	private Map<String, Object> toInfoMap(Weapp e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_appid", e.getAuthorizerAppid());
		m.put("company_id", e.getCompanyId());
		m.put("operator_id", e.getOperatorId());
		m.put("template_name", e.getTemplateName());
		m.put("template_id", e.getTemplateId());
		m.put("template_ver", e.getTemplateVer());
		m.put("audit_status", e.getAuditStatus());
		m.put("release_status", e.getReleaseStatus());
		m.put("visitstatus", e.getVisitStatus());
		m.put("reason", e.getReason());
		m.put("audit_time", e.getAuditTime());
		m.put("release_ver", e.getReleaseVer());
		return m;
	}
}
