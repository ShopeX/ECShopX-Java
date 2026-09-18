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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdminMemberBatchOperatingMemberQueryFilterSupport {

	static final String CHUNK_FILTER_QUERY_KEY = "q";

	private AdminMemberBatchOperatingMemberQueryFilterSupport() {}

	static Map<String, Object> wrapFilter(AdminMemberBatchOperatingMemberQueryFilter f) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put(CHUNK_FILTER_QUERY_KEY, f);
		return m;
	}

	static AdminMemberBatchOperatingMemberQueryFilter unwrap(Map<String, Object> chunkFilter) {
		Object o = chunkFilter.get(CHUNK_FILTER_QUERY_KEY);
		if (!(o instanceof AdminMemberBatchOperatingMemberQueryFilter f)) {
			throw new IllegalArgumentException("chunk filter");
		}
		return f;
	}

	static AdminMemberBatchOperatingMemberQueryFilter copyReplacingUserIds(
			AdminMemberBatchOperatingMemberQueryFilter src, List<Long> userIds) {
		AdminMemberBatchOperatingMemberQueryFilter c = shallowCopy(src);
		c.setUserIdsIn(userIds == null || userIds.isEmpty() ? null : new ArrayList<>(userIds));
		c.setUserIdsNotIn(null);
		return c;
	}

	private static AdminMemberBatchOperatingMemberQueryFilter shallowCopy(AdminMemberBatchOperatingMemberQueryFilter s) {
		AdminMemberBatchOperatingMemberQueryFilter c = new AdminMemberBatchOperatingMemberQueryFilter();
		c.setCompanyId(s.getCompanyId());
		c.setMembersGradeId(s.getMembersGradeId());
		c.setUserIdsIn(s.getUserIdsIn() == null ? null : new ArrayList<>(s.getUserIdsIn()));
		c.setUserIdsNotIn(s.getUserIdsNotIn() == null ? null : new ArrayList<>(s.getUserIdsNotIn()));
		c.setMobileEqEncrypted(s.getMobileEqEncrypted());
		c.setRemarksLike(s.getRemarksLike());
		c.setInviterId(s.getInviterId());
		c.setUserCardCode(s.getUserCardCode());
		c.setUsernameEqEncrypted(s.getUsernameEqEncrypted());
		c.setNameEq(s.getNameEq());
		c.setCreatedGte(s.getCreatedGte());
		c.setCreatedLte(s.getCreatedLte());
		c.setBirthdayGte(s.getBirthdayGte());
		c.setBirthdayLte(s.getBirthdayLte());
		c.setHaveConsume(s.getHaveConsume());
		c.setShopIds(s.getShopIds() == null ? null : new ArrayList<>(s.getShopIds()));
		c.setDistributorIds(s.getDistributorIds() == null ? null : new ArrayList<>(s.getDistributorIds()));
		c.setTagIds(s.getTagIds() == null ? null : new ArrayList<>(s.getTagIds()));
		c.setPointGte(s.getPointGte());
		c.setPointLte(s.getPointLte());
		c.setPointEq(s.getPointEq());
		return c;
	}
}
