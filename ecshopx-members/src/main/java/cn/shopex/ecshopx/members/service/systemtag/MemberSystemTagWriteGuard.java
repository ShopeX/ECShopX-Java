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
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemberSystemTagWriteGuard {

	public static final String MSG_SYSTEM_TAG_READONLY = "系统标签不可编辑";

	private final MemberTagsMapper memberTagsMapper;

	public MemberSystemTagWriteGuard(MemberTagsMapper memberTagsMapper) {
		this.memberTagsMapper = memberTagsMapper;
	}

	public void rejectWrite(MemberTags tag) {
		if (tag != null && MemberSystemTagCodes.isSystemKind(tag.getTagKind())) {
			throw new ResourceException(MSG_SYSTEM_TAG_READONLY);
		}
	}

	public void rejectCreateReservedCode(String tagCode) {
		if (MemberSystemTagCodes.isReservedCode(tagCode)) {
			throw new ResourceException(MSG_SYSTEM_TAG_READONLY);
		}
	}

	public void rejectCreatePayload(Map<String, Object> merged) {
		if (merged == null) {
			return;
		}
		rejectSystemKindValue(merged.get("tag_kind"));
		rejectSystemKindValue(merged.get("type"));
		Object code = merged.get("tag_code");
		if (code == null) {
			code = merged.get("code");
		}
		rejectCreateReservedCode(code == null ? null : String.valueOf(code));
	}

	public void rejectTagIds(long companyId, Collection<Long> tagIds) {
		if (memberTagsMapper == null || tagIds == null || tagIds.isEmpty()) {
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
		for (MemberTags row : rows) {
			rejectWrite(row);
		}
	}

	public void rejectTagId(long companyId, long tagId) {
		rejectTagIds(companyId, List.of(tagId));
	}

	private void rejectSystemKindValue(Object raw) {
		if (raw == null) {
			return;
		}
		if (MemberSystemTagCodes.isSystemKind(String.valueOf(raw).trim())) {
			throw new ResourceException(MSG_SYSTEM_TAG_READONLY);
		}
	}
}
