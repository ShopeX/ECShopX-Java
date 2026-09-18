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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.community.domain.CommunityChiefApplyInfo;
import cn.shopex.ecshopx.community.mapper.CommunityChiefApplyInfoMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CommunityChiefFrontApplyQueryService {

	private final CommunityChiefApplyInfoMapper mapper;

	public CommunityChiefFrontApplyQueryService(CommunityChiefApplyInfoMapper mapper) {
		this.mapper = mapper;
	}

	public Map<String, Object> getApplyInfoData(long companyId, long userId, int distributorId) {
		CommunityChiefApplyInfo row =
				mapper.selectLatestByCompanyUserAndDistributor(companyId, userId, distributorId);
		if (row == null) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("approve_status", -1);
			return empty;
		}
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("apply_id", row.getApplyId());
		map.put("company_id", row.getCompanyId());
		map.put("distributor_id", row.getDistributorId());
		map.put("user_id", row.getUserId());
		map.put("chief_name", row.getChiefName());
		map.put("chief_mobile", row.getChiefMobile());
		map.put("extra_data", row.getExtraData());
		map.put("approve_status", row.getApproveStatus());
		map.put("refuse_reason", row.getRefuseReason());
		map.put("created_at", row.getCreatedAt());
		map.put("updated_at", row.getUpdatedAt());
		return map;
	}
}
