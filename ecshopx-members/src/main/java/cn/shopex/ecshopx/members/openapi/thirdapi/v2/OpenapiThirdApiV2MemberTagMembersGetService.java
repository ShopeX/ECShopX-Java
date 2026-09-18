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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagV2FailException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.MemberUserRelTagRow;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberListQueryFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagMembersGetService {

	private final MemberTagsMapper memberTagsMapper;
	private final MembersMapper membersMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberTagMembersGetService(
			MemberTagsMapper memberTagsMapper,
			MembersMapper membersMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.memberTagsMapper = memberTagsMapper;
		this.membersMapper = membersMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> executeOpenapiGetTagMembers(
			long companyId, int page, int pageSize, String tagIdRaw) {
		if (!StringUtils.hasText(tagIdRaw)) {
			throw new OpenapiMemberTagV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "会员标签必填");
		}
		String tagIdTrimmed = tagIdRaw.trim();
		Long tagId;
		try {
			tagId = Long.parseLong(tagIdTrimmed);
		} catch (NumberFormatException e) {
			throw new OpenapiMemberTagV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "会员标签必填");
		}

		MemberTags tagInfo = memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagId, tagId)
						.last("LIMIT 1"));
		if (tagInfo == null) {
			throw new OpenapiMemberTagV2FailException(
					OpenapiErrorCode.MEMBER_TAG_NOT_FOUND, "该标签不存在");
		}

		OpenapiMemberListQueryFilter filter = new OpenapiMemberListQueryFilter();
		filter.setCompanyId(companyId);
		filter.setJoinRelTags(true);
		filter.setTagIdIn(List.of(tagId));

		long totalCount = membersMapper.countMemberListForOpenapi(filter);
		Page<Map<String, Object>> mpPage = new Page<>(page, pageSize, false);
		List<Map<String, Object>> memberRows = membersMapper.selectMemberListForOpenapi(mpPage, filter);

		if (memberRows == null || memberRows.isEmpty()) {
			return OpenapiThirdApiV2MemberTagListService.formatListStruct(
					totalCount, List.of(), page, pageSize);
		}

		List<Long> userIds = memberRows.stream()
				.map(row -> longOrNull(row.get("user_id")))
				.filter(id -> id != null && id > 0)
				.toList();

		List<MemberUserRelTagRow> allTags =
				memberRelTagsMapper.selectUserRelTagListByUserIds(companyId, userIds);

		Map<Long, List<MemberUserRelTagRow>> tagsByUserId = new LinkedHashMap<>();
		for (MemberUserRelTagRow tagRow : allTags) {
			if (tagRow.getUserId() == null) {
				continue;
			}
			tagsByUserId.computeIfAbsent(tagRow.getUserId(), k -> new ArrayList<>()).add(tagRow);
		}

		List<Map<String, Object>> list = new ArrayList<>(memberRows.size());
		for (Map<String, Object> row : memberRows) {
			Long userId = longOrNull(row.get("user_id"));
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("mobile", decryptOrEmpty(row.get("mobile")));
			item.put("username", decryptOrEmpty(row.get("username")));
			List<Map<String, Object>> tagList = new ArrayList<>();
			for (MemberUserRelTagRow t : tagsByUserId.getOrDefault(userId, List.of())) {
				Map<String, Object> tagItem = new LinkedHashMap<>();
				tagItem.put("tag_id", t.getTagId());
				tagItem.put("tag_name", t.getTagName());
				tagList.add(tagItem);
			}
			item.put("tag_list", tagList);
			list.add(item);
		}
		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private String decryptOrEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw);
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return sensitiveFieldEncryptor.decrypt(s);
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
