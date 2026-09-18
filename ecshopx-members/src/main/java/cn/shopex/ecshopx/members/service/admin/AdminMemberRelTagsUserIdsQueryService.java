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

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberRelTagsUserIdsQueryService {

	private final MemberRelTagsMapper memberRelTagsMapper;

	public AdminMemberRelTagsUserIdsQueryService(MemberRelTagsMapper memberRelTagsMapper) {
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	public List<Long> getUserIdsByTagids(long companyId, String tagidQuery) {
		if (tagidQuery == null || !StringUtils.hasText(tagidQuery.trim())) {
			return Collections.emptyList();
		}
		String t = tagidQuery.trim();
		if ("0".equals(t)) {
			return Collections.emptyList();
		}
		final long tagId;
		try {
			tagId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		List<MemberRelTags> rows =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getTagId, tagId)
								.select(MemberRelTags::getUserId)
								.orderByAsc(MemberRelTags::getUserId)
								.last("LIMIT 100 OFFSET 0"));
		return rows.stream().map(MemberRelTags::getUserId).collect(Collectors.toList());
	}

	public List<Long> listUserIdsByTagIds(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<MemberRelTags> rows =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.in(MemberRelTags::getTagId, tagIds)
								.select(MemberRelTags::getUserId));
		return rows.stream().map(MemberRelTags::getUserId).distinct().collect(Collectors.toList());
	}
}
