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

import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import cn.shopex.ecshopx.distribution.mapper.DistributorTagsMapper;
import cn.shopex.ecshopx.distribution.support.DistributorTagsRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorTagsGetInfoService {

	private final DistributorTagsMapper distributorTagsMapper;
	private final DistributorTagsListOutsideLangReadService distributorTagsListOutsideLangReadService;

	public DistributorTagsGetInfoService(
			DistributorTagsMapper distributorTagsMapper,
			DistributorTagsListOutsideLangReadService distributorTagsListOutsideLangReadService) {
		this.distributorTagsMapper = distributorTagsMapper;
		this.distributorTagsListOutsideLangReadService = distributorTagsListOutsideLangReadService;
	}

	public Map<String, Object> getOneRowWithLang(long companyId, long tagId, String requestLangTag) {
		LambdaQueryWrapper<DistributorTags> wrapper = new LambdaQueryWrapper<DistributorTags>()
				.eq(DistributorTags::getTagId, tagId)
				.eq(DistributorTags::getCompanyId, companyId);
		DistributorTags entity = distributorTagsMapper.selectOne(wrapper);
		if (entity == null) {
			return null;
		}
		Map<String, Object> row = DistributorTagsRowMapper.toRow(entity);
		ArrayList<Map<String, Object>> single = new ArrayList<>();
		single.add(row);
		distributorTagsListOutsideLangReadService.applyTagFields(companyId, requestLangTag, single);
		return row;
	}
}
