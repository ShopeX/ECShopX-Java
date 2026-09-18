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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.DistributorRelTags;
import cn.shopex.ecshopx.distribution.dto.DistributorTagRelRow;
import cn.shopex.ecshopx.distribution.mapper.DistributorRelTagsMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DistributorTagRelQueryService {

	private final DistributorRelTagsMapper distributorRelTagsMapper;
	private final DistributorAdminListRepository distributorAdminListRepository;

	public DistributorTagRelQueryService(
			DistributorRelTagsMapper distributorRelTagsMapper,
			DistributorAdminListRepository distributorAdminListRepository) {
		this.distributorRelTagsMapper = distributorRelTagsMapper;
		this.distributorAdminListRepository = distributorAdminListRepository;
	}

	public List<Long> listDistributorIdsByCompanyAndTagIds(
			long companyId, List<Long> tagIds, List<Long> optionalDistributorScope) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<DistributorRelTags> w = new LambdaQueryWrapper<>();
		w.eq(DistributorRelTags::getCompanyId, companyId);
		if (tagIds.size() == 1) {
			w.eq(DistributorRelTags::getTagId, tagIds.get(0));
		} else {
			w.in(DistributorRelTags::getTagId, tagIds);
		}
		if (optionalDistributorScope != null && !optionalDistributorScope.isEmpty()) {
			w.in(DistributorRelTags::getDistributorId, optionalDistributorScope);
		}
		w.select(DistributorRelTags::getDistributorId);
		List<DistributorRelTags> rows = distributorRelTagsMapper.selectList(w);
		Set<Long> seen = new LinkedHashSet<>();
		for (DistributorRelTags r : rows) {
			if (r.getDistributorId() != null) {
				seen.add(r.getDistributorId());
			}
		}
		return new ArrayList<>(seen);
	}

	public List<DistributorTagRelRow> listRelWithTagsByCompanyAndDistributorIds(long companyId, List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return List.of();
		}
		return distributorAdminListRepository.selectRelWithTags(companyId, distributorIds);
	}
}
