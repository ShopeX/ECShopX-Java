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

package cn.shopex.ecshopx.community.integration;

import cn.shopex.ecshopx.common.orders.port.OrderExportActivityIdsLookupPort;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderExportActivityIdsLookupPortImpl implements OrderExportActivityIdsLookupPort {

	private final CommunityActivityMapper communityActivityMapper;

	public OrderExportActivityIdsLookupPortImpl(CommunityActivityMapper communityActivityMapper) {
		this.communityActivityMapper = communityActivityMapper;
	}

	@Override
	public List<Long> lookupActivityIdsForOrderExport(
			long companyId, String activityNameContainsOrNull, String activityStatusOrNull) {
		LambdaQueryWrapper<CommunityActivity> w = new LambdaQueryWrapper<>();
		w.eq(CommunityActivity::getCompanyId, companyId);
		if (StringUtils.hasText(activityNameContainsOrNull)) {
			w.like(CommunityActivity::getActivityName, activityNameContainsOrNull.trim());
		}
		if (StringUtils.hasText(activityStatusOrNull)) {
			w.eq(CommunityActivity::getActivityStatus, activityStatusOrNull.trim());
		}
		w.select(CommunityActivity::getActivityId);
		List<CommunityActivity> rows = communityActivityMapper.selectList(w);
		List<Long> ids = new ArrayList<>();
		for (CommunityActivity row : rows) {
			if (row.getActivityId() != null && row.getActivityId() > 0L) {
				ids.add(row.getActivityId());
			}
		}
		return ids;
	}
}
