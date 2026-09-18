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

package cn.shopex.ecshopx.promotions.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberSpecificCrowdDiscountPort;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscount;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service("wxappMemberSpecificCrowdDiscountPortImpl")
public class WxappMemberSpecificCrowdDiscountPortImpl implements WxappMemberSpecificCrowdDiscountPort {

	private static final long STATUS_PUBLISHED = 2L;

	private final MemberRelTagsMapper memberRelTagsMapper;
	private final SpecificCrowdDiscountMapper specificCrowdDiscountMapper;

	public WxappMemberSpecificCrowdDiscountPortImpl(
			MemberRelTagsMapper memberRelTagsMapper,
			SpecificCrowdDiscountMapper specificCrowdDiscountMapper) {
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.specificCrowdDiscountMapper = specificCrowdDiscountMapper;
	}

	@Override
	public void apply(long companyId, long userId, Map<String, Object> result) {
		result.put("is_staff", Boolean.FALSE);
		if (userId <= 0L) {
			return;
		}
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getUserId, userId));
		if (rels == null || rels.isEmpty()) {
			return;
		}
		Set<Long> tagIds =
				rels.stream()
						.map(MemberRelTags::getTagId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		if (tagIds.isEmpty()) {
			return;
		}
		int now = (int) Instant.now().getEpochSecond();
		List<SpecificCrowdDiscount> candidates =
				specificCrowdDiscountMapper.selectList(
						new LambdaQueryWrapper<SpecificCrowdDiscount>()
								.eq(SpecificCrowdDiscount::getCompanyId, companyId)
								.eq(SpecificCrowdDiscount::getStatus, STATUS_PUBLISHED)
								.eq(SpecificCrowdDiscount::getSpecificType, "member_tag")
								.in(SpecificCrowdDiscount::getSpecificId, tagIds)
								.le(SpecificCrowdDiscount::getStartTime, (long) now)
								.ge(SpecificCrowdDiscount::getEndTime, (long) now));
		if (candidates == null || candidates.isEmpty()) {
			return;
		}
		SpecificCrowdDiscount best =
				candidates.stream()
						.filter(r -> r.getDiscount() != null && r.getDiscount() >= 1L && r.getDiscount() <= 100L)
						.min((a, b) -> Long.compare(a.getDiscount(), b.getDiscount()))
						.orElse(null);
		if (best == null) {
			return;
		}
		result.put("is_staff", Boolean.TRUE);
		long d = best.getDiscount();
		result.put("staff_discount", (100L - d) / 10L);
	}
}
