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

package cn.shopex.ecshopx.promotions.service.systemtag;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MemberCrowdMatcher {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MemberRelTagsMapper memberRelTagsMapper;

	public MemberCrowdMatcher(MemberRelTagsMapper memberRelTagsMapper) {
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	public static boolean isFullCrowdMarketingType(String marketingType) {
		return "full_minus".equals(marketingType)
				|| "full_discount".equals(marketingType)
				|| "full_gift".equals(marketingType);
	}

	public static boolean isLimitedTimeSale(String seckillType) {
		return "limited_time_sale".equals(seckillType);
	}

	public boolean matchesFullCrowdActivity(
			long companyId, long userId, String marketingType, String memberTagIdsJson, String mode) {
		if (!isFullCrowdMarketingType(marketingType)) {
			return true;
		}
		return matchesMemberTags(companyId, userId, memberTagIdsJson, mode);
	}

	public boolean matchesLimitedTimeSale(
			long companyId, long userId, String seckillType, String memberTagIdsJson, String mode) {
		if (!isLimitedTimeSale(seckillType)) {
			return true;
		}
		return matchesMemberTags(companyId, userId, memberTagIdsJson, mode);
	}

	public boolean matchesMemberTags(long companyId, long userId, String memberTagIdsJson, String mode) {
		List<Long> configured = parseIds(memberTagIdsJson);
		if (configured.isEmpty()) {
			return true;
		}
		if (userId <= 0L) {
			return false;
		}
		Set<Long> held = loadUserTagIds(companyId, userId);
		boolean hit = false;
		for (Long id : configured) {
			if (held.contains(id)) {
				hit = true;
				break;
			}
		}
		boolean exclude = "exclude".equalsIgnoreCase(mode == null ? "" : mode.trim());
		return exclude ? !hit : hit;
	}

	private Set<Long> loadUserTagIds(long companyId, long userId) {
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getUserId, userId));
		Set<Long> ids = new HashSet<>();
		if (rels == null) {
			return ids;
		}
		for (MemberRelTags rel : rels) {
			if (rel != null && rel.getTagId() != null && rel.getTagId() > 0L) {
				ids.add(rel.getTagId());
			}
		}
		return ids;
	}

	public static List<Long> parseIds(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Long> raw = JSON.readValue(json, new TypeReference<List<Long>>() {});
			if (raw == null || raw.isEmpty()) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (Long id : raw) {
				if (id != null && id > 0L) {
					out.add(id);
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}
}
