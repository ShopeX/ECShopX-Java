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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.members.domain.MemberUserRelTagRow;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MemberTaskMemberTagReadPortImpl implements MemberTaskMemberTagReadPort {

	private final MemberRelTagsMapper memberRelTagsMapper;

	public MemberTaskMemberTagReadPortImpl(MemberRelTagsMapper memberRelTagsMapper) {
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	@Override
	public Set<Long> userTagIds(long companyId, long userId) {
		List<MemberUserRelTagRow> rows = memberRelTagsMapper.selectUserRelTagList(companyId, userId);
		if (rows == null || rows.isEmpty()) {
			return Set.of();
		}
		Set<Long> out = new HashSet<>();
		for (MemberUserRelTagRow row : rows) {
			if (row.getTagId() != null && row.getTagId() > 0L) {
				out.add(row.getTagId());
			}
		}
		return out;
	}
}
