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

package cn.shopex.ecshopx.adapay.repository;

import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class AdapayMemberDistributorListRepository {

	private final AdapayMemberMapper adapayMemberMapper;

	public AdapayMemberDistributorListRepository(AdapayMemberMapper adapayMemberMapper) {
		this.adapayMemberMapper = adapayMemberMapper;
	}

	public Set<Long> listDistributorIdsWithOpenAccount(long companyId, Collection<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Set.of();
		}
		LambdaQueryWrapper<AdapayMember> w = new LambdaQueryWrapper<>();
		w.eq(AdapayMember::getCompanyId, companyId)
				.eq(AdapayMember::getOperatorType, "distributor")
				.eq(AdapayMember::getAuditState, "E");
		List<Integer> opIds = distributorIds.stream().map(Long::intValue).distinct().toList();
		w.in(AdapayMember::getOperatorId, opIds);
		w.select(AdapayMember::getOperatorId);
		List<AdapayMember> rows = adapayMemberMapper.selectList(w);
		Set<Long> out = new LinkedHashSet<>();
		for (AdapayMember m : rows) {
			if (m.getOperatorId() != null) {
				out.add(m.getOperatorId().longValue());
			}
		}
		return out;
	}
}
