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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagV2FailException;
import cn.shopex.ecshopx.members.domain.MemberUserRelTagRow;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTaggedGetService {

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MemberRelTagsMapper memberRelTagsMapper;

	public OpenapiThirdApiV2MemberTaggedGetService(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MemberRelTagsMapper memberRelTagsMapper) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	public List<Map<String, Object>> executeOpenapiGetMemberTagged(long companyId, String mobileRaw) {
		if (!StringUtils.hasText(mobileRaw)) {
			throw new ResourceException("会员手机号必填");
		}
		String mobile = mobileRaw.trim();

		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getMobile, mobile)
								.last("LIMIT 1"));
		Long userId = member != null ? member.getUserId() : null;
		if (userId == null || userId <= 0) {
			throw memberNotFoundDefault();
		}

		String username = null;
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (info != null && StringUtils.hasText(info.getUsername())) {
			username = sensitiveFieldEncryptor.decrypt(info.getUsername());
		}

		List<MemberUserRelTagRow> taggedList =
				memberRelTagsMapper.selectUserRelTagList(companyId, userId);
		if (taggedList == null || taggedList.isEmpty()) {
			return null;
		}

		List<Map<String, Object>> result = new ArrayList<>(taggedList.size());
		for (MemberUserRelTagRow row : taggedList) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("mobile", member.getMobile());
			item.put("username", username);
			item.put("tag_id", row.getTagId());
			item.put("tag_name", row.getTagName());
			item.put("category_id", row.getCategoryId());
			item.put("description", row.getDescription());
			item.put("tag_color", row.getTagColor());
			item.put("font_color", row.getFontColor());
			result.add(item);
		}
		return result;
	}

	private static OpenapiMemberTagV2FailException memberNotFoundDefault() {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
	}
}
