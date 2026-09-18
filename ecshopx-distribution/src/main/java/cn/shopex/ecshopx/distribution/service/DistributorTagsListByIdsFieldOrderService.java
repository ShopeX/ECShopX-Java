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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class DistributorTagsListByIdsFieldOrderService {

	private final DistributorTagsMapper distributorTagsMapper;

	public DistributorTagsListByIdsFieldOrderService(DistributorTagsMapper distributorTagsMapper) {
		this.distributorTagsMapper = distributorTagsMapper;
	}

	public List<Map<String, Object>> listOrdered(long companyId, List<Long> tagIds, int frontShow) {
		if (CollectionUtils.isEmpty(tagIds)) {
			return List.of();
		}
		StringBuilder fieldOrder = new StringBuilder();
		for (Long id : tagIds) {
			if (id == null) {
				continue;
			}
			if (fieldOrder.length() > 0) {
				fieldOrder.append(',');
			}
			fieldOrder.append(id);
		}
		if (fieldOrder.isEmpty()) {
			return List.of();
		}
		List<DistributorTags> rows =
				distributorTagsMapper.selectList(
						new LambdaQueryWrapper<DistributorTags>()
								.eq(DistributorTags::getCompanyId, companyId)
								.in(DistributorTags::getTagId, tagIds)
								.eq(DistributorTags::getFrontShow, frontShow)
								.last("ORDER BY FIELD(tag_id," + fieldOrder + ")"));
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (DistributorTags t : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("company_id", t.getCompanyId());
			m.put("tag_id", t.getTagId());
			m.put("tag_name", t.getTagName());
			m.put("tag_color", t.getTagColor());
			m.put("font_color", t.getFontColor());
			m.put("description", t.getDescription());
			m.put("tag_icon", t.getTagIcon());
			m.put("front_show", t.getFrontShow());
			m.put("created", t.getCreated());
			m.put("updated", t.getUpdated());
			out.add(m);
		}
		return out;
	}
}
