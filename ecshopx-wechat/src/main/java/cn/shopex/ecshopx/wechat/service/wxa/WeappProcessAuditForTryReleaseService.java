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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WeappProcessAuditForTryReleaseService {

	private final WeappMapper weappMapper;
	private final WeappCreateOrUpdateService weappCreateOrUpdateService;

	public WeappProcessAuditForTryReleaseService(
			WeappMapper weappMapper, WeappCreateOrUpdateService weappCreateOrUpdateService) {
		this.weappMapper = weappMapper;
		this.weappCreateOrUpdateService = weappCreateOrUpdateService;
	}

	public void applyAuditRejected(String wxaAppId, String reasonFromWechat) {
		String id = wxaAppId == null ? "" : wxaAppId.trim();
		Weapp row = weappMapper.selectById(id);
		if (row == null || row.getAuditStatus() == null || row.getAuditStatus() != 2) {
			return;
		}
		long t = Instant.now().getEpochSecond();
		Map<String, Object> saveData = new LinkedHashMap<>();
		saveData.put("audit_time", String.valueOf(t));
		saveData.put("audit_status", 1);
		saveData.put("reason", reasonFromWechat == null ? "" : reasonFromWechat);
		weappCreateOrUpdateService.saveWeapp(id, saveData);
	}

	public void applyReleased(String wxaAppId) {
		String id = wxaAppId == null ? "" : wxaAppId.trim();
		Weapp row = weappMapper.selectById(id);
		if (row == null) {
			return;
		}
		Integer as = row.getAuditStatus();
		String templateVer = row.getTemplateVer() == null ? "" : row.getTemplateVer();
		long t = Instant.now().getEpochSecond();
		if (as != null && as == 2) {
			Map<String, Object> saveData = new LinkedHashMap<>();
			saveData.put("audit_time", String.valueOf(t));
			saveData.put("audit_status", 0);
			saveData.put("release_status", 1);
			saveData.put("release_ver", templateVer);
			weappCreateOrUpdateService.saveWeapp(id, saveData);
			return;
		}
		if (as != null && as == 0) {
			Map<String, Object> saveData = new LinkedHashMap<>();
			saveData.put("release_status", 1);
			saveData.put("release_ver", templateVer);
			weappCreateOrUpdateService.saveWeapp(id, saveData);
		}
	}

	public void applyUndocodeauditSuccess(String wxaAppId) {
		String id = wxaAppId == null ? "" : wxaAppId.trim();
		Weapp row = weappMapper.selectById(id);
		if (row == null) {
			return;
		}
		LinkedHashMap<String, Object> saveData = new LinkedHashMap<>();
		saveData.put("company_id", row.getCompanyId());
		saveData.put("audit_status", 0);
		saveData.put("release_status", 1);
		weappCreateOrUpdateService.saveWeapp(id, saveData);
	}
}
