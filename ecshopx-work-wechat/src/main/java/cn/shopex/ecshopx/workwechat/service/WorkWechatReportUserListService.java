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

package cn.shopex.ecshopx.workwechat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatReportUserListService {

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatCorpUserApiService workWechatCorpUserApiService;

	public WorkWechatReportUserListService(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatCorpUserApiService workWechatCorpUserApiService) {
		this.workWechatConfigService = workWechatConfigService;
		this.workWechatCorpUserApiService = workWechatCorpUserApiService;
	}

	public List<Map<String, Object>> listUsersForDepartment(long companyId, long departmentId) {
		Map<String, Object> config = workWechatConfigService.loadParsedWorkWechatConfig(companyId);
		workWechatConfigService.validateGuideAppAgentForCorpApi(config);
		List<Map<String, Object>> raw = workWechatCorpUserApiService.getDetailedDepartmentUsers(companyId,
				departmentId);
		List<Map<String, Object>> result = new ArrayList<>(raw.size());
		for (Map<String, Object> item : raw) {
			Map<String, Object> row = new LinkedHashMap<>(item);
			row.put("id", item.get("userid"));
			result.add(row);
		}
		return result;
	}
}
