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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MemberTagsCheckAndProcessService {

	private final MemberTagsMapper memberTagsMapper;

	private final MemberRelTagsMapper memberRelTagsMapper;

	public MemberTagsCheckAndProcessService(
			MemberTagsMapper memberTagsMapper, MemberRelTagsMapper memberRelTagsMapper) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	public List<Map<String, Object>> checkAndProcessTag(long companyId, long userId, List<Long> tagIds) {
		if (userId <= 0L || tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		List<Long> distinctTagIds = tagIds.stream().distinct().toList();
		Map<Long, Map<String, Object>> resultByTagId = new LinkedHashMap<>();
		for (Long tagId : distinctTagIds) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("user_id", userId);
			row.put("tag_id", tagId);
			row.put("related", Boolean.FALSE);
			resultByTagId.put(tagId, row);
		}
		LambdaQueryWrapper<MemberTags> tagQw =
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.in(MemberTags::getTagId, distinctTagIds);
		List<MemberTags> tagList = memberTagsMapper.selectList(tagQw);
		if (tagList == null || tagList.isEmpty()) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Long tagId : distinctTagIds) {
				out.add(resultByTagId.get(tagId));
			}
			return out;
		}
		List<Long> localIds = tagList.stream().map(MemberTags::getTagId).filter(Objects::nonNull).toList();
		if (!localIds.isEmpty()) {
			LambdaQueryWrapper<MemberRelTags> relQw =
					new LambdaQueryWrapper<MemberRelTags>()
							.eq(MemberRelTags::getUserId, userId)
							.in(MemberRelTags::getTagId, localIds);
			List<MemberRelTags> userTagRows = memberRelTagsMapper.selectList(relQw);
			for (MemberRelTags ur : userTagRows) {
				Long tid = ur.getTagId();
				if (tid == null) {
					continue;
				}
				Map<String, Object> cell = resultByTagId.get(tid);
				if (cell != null) {
					cell.put("related", Boolean.TRUE);
				}
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long tagId : distinctTagIds) {
			out.add(resultByTagId.get(tagId));
		}
		return out;
	}
}
