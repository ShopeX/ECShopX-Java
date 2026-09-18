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

package cn.shopex.ecshopx.community.integration.members;

import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import cn.shopex.ecshopx.members.integration.community.AdminMemberListChiefStoresPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberListChiefStoresPortImpl implements AdminMemberListChiefStoresPort {

	private final CommunityChiefMapper communityChiefMapper;

	public AdminMemberListChiefStoresPortImpl(CommunityChiefMapper communityChiefMapper) {
		this.communityChiefMapper = communityChiefMapper;
	}

	@Override
	public void applyChiefFlagsAndStoreInfo(long companyId, List<Map<String, Object>> memberRows, List<Long> pageUserIds) {
		if (memberRows == null || memberRows.isEmpty() || pageUserIds == null || pageUserIds.isEmpty()) {
			return;
		}
		List<CommunityChief> chiefs =
				communityChiefMapper.selectList(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getCompanyId, companyId)
								.in(CommunityChief::getUserId, pageUserIds));
		Map<Long, Long> userIdToChiefId = new LinkedHashMap<>();
		for (CommunityChief c : chiefs) {
			if (c.getUserId() == null || c.getChiefId() == null || c.getChiefId() <= 0L) {
				continue;
			}
			userIdToChiefId.putIfAbsent(c.getUserId(), c.getChiefId());
		}
		for (Map<String, Object> row : memberRows) {
			long uid = extractUserId(row.get("user_id"));
			Long chiefId = userIdToChiefId.get(uid);
			boolean chief = chiefId != null && chiefId > 0L;
			row.put("is_chief", chief ? Integer.valueOf(1) : Integer.valueOf(0));
		}
	}

	private static long extractUserId(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
