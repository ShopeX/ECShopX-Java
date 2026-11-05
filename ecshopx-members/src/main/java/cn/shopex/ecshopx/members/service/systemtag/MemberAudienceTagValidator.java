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

package cn.shopex.ecshopx.members.service.systemtag;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MemberAudienceTagValidator {

	public static final String MSG_TAG_NOT_USABLE = "标签不存在或不可用";

	private final MemberTagsMapper memberTagsMapper;

	public MemberAudienceTagValidator(MemberTagsMapper memberTagsMapper) {
		this.memberTagsMapper = memberTagsMapper;
	}

	public void assertUsable(long companyId, long distributorId, Collection<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}
		List<Long> ids =
				tagIds.stream().filter(id -> id != null && id > 0L).distinct().toList();
		if (ids.isEmpty()) {
			return;
		}
		List<MemberTags> rows =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, ids));
		Set<Long> found =
				rows.stream().map(MemberTags::getTagId).filter(Objects::nonNull).collect(Collectors.toSet());
		for (Long id : ids) {
			if (!found.contains(id)) {
				throw new ResourceException(MSG_TAG_NOT_USABLE);
			}
		}
		for (MemberTags tag : rows) {
			if (MemberSystemTagCodes.isSystemKind(tag.getTagKind())) {
				continue;
			}
			Long did = tag.getDistributorId() == null ? 0L : tag.getDistributorId();
			if (did != distributorId) {
				throw new ResourceException(MSG_TAG_NOT_USABLE);
			}
		}
	}
}
