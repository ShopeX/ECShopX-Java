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
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiMemberCardGradeRelEnrichmentService {

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiMemberCardGradeRelEnrichmentService(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void appendInfoToList(long companyId, List<Map<String, Object>> list, boolean keepUserId) {
		if (list == null || list.isEmpty()) {
			return;
		}
		List<Long> userIds = list.stream()
				.map(row -> longOrNull(row.get("user_id")))
				.filter(id -> id != null)
				.distinct()
				.toList();
		if (userIds.isEmpty()) {
			return;
		}

		List<MembersContactByUserIdsLookupService.MemberContactRow> contactRows =
				membersMapper.selectInviterMobileRowsByUserIds(companyId, userIds);
		Map<Long, String> mobilePlainByUserId = new LinkedHashMap<>();
		for (MembersContactByUserIdsLookupService.MemberContactRow row : contactRows) {
			if (row.getUserId() == null) {
				continue;
			}
			mobilePlainByUserId.put(row.getUserId(), decryptOrEmpty(row.getMobileEnc()));
		}

		for (Map<String, Object> item : list) {
			Long userId = longOrNull(item.get("user_id"));
			item.put("mobile", mobilePlainByUserId.getOrDefault(userId, ""));
			if (!keepUserId) {
				item.remove("user_id");
			}
		}
	}

	public void appendDetailToList(long companyId, List<Map<String, Object>> list, boolean keepUserId) {
		if (list == null || list.isEmpty()) {
			return;
		}
		List<Long> userIds = list.stream()
				.map(row -> longOrNull(row.get("user_id")))
				.filter(id -> id != null)
				.distinct()
				.toList();
		if (userIds.isEmpty()) {
			return;
		}

		List<MembersInfo> infoRows =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.in(MembersInfo::getUserId, userIds)
								.select(MembersInfo::getUserId, MembersInfo::getUsername));
		Map<Long, String> usernamePlainByUserId = new LinkedHashMap<>();
		for (MembersInfo info : infoRows) {
			if (info.getUserId() == null) {
				continue;
			}
			usernamePlainByUserId.put(info.getUserId(), decryptOrEmpty(info.getUsername()));
		}

		for (Map<String, Object> item : list) {
			Long userId = longOrNull(item.get("user_id"));
			item.put("username", usernamePlainByUserId.getOrDefault(userId, ""));
			if (!keepUserId) {
				item.remove("user_id");
			}
		}
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
