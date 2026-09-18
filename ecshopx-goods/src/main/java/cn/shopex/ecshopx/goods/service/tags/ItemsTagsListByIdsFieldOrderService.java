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

package cn.shopex.ecshopx.goods.service.tags;

import cn.shopex.ecshopx.distribution.service.DistributorBatchApiRowQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ItemsTagsListByIdsFieldOrderService {

	private final ItemsTagsMapper itemsTagsMapper;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;

	public ItemsTagsListByIdsFieldOrderService(
			ItemsTagsMapper itemsTagsMapper, DistributorBatchApiRowQueryService distributorBatchApiRowQueryService) {
		this.itemsTagsMapper = itemsTagsMapper;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
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
		List<ItemsTags> rows =
				itemsTagsMapper.selectList(
						new LambdaQueryWrapper<ItemsTags>()
								.eq(ItemsTags::getCompanyId, companyId)
								.in(ItemsTags::getTagId, tagIds)
								.eq(ItemsTags::getFrontShow, frontShow)
								.last("ORDER BY FIELD(tag_id," + fieldOrder + ")"));
		Set<Long> distIds = new LinkedHashSet<>();
		for (ItemsTags t : rows) {
			if (t.getDistributorId() != null && t.getDistributorId() > 0L) {
				distIds.add(t.getDistributorId());
			}
		}
		Map<Long, Map<String, Object>> distRows =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, distIds);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (ItemsTags t : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("tag_id", t.getTagId());
			m.put("company_id", t.getCompanyId());
			m.put("tag_name", t.getTagName());
			m.put("tag_color", t.getTagColor());
			m.put("distributor_id", t.getDistributorId());
			m.put("font_color", t.getFontColor());
			m.put("description", t.getDescription());
			m.put("tag_icon", t.getTagIcon());
			m.put("front_show", t.getFrontShow());
			m.put("created", t.getCreated());
			m.put("updated", t.getUpdated());
			long did = t.getDistributorId() == null ? 0L : t.getDistributorId();
			if (did > 0L) {
				Map<String, Object> dr = distRows.get(did);
				String nm = "";
				if (dr != null) {
					Object n = dr.get("name");
					if (n == null) {
						n = dr.get("distributorName");
					}
					nm = n != null ? n.toString() : "";
				}
				m.put("distributor_name", nm);
				m.put("is_platform", Boolean.FALSE);
			} else {
				m.put("distributor_name", "平台");
				m.put("is_platform", Boolean.TRUE);
			}
			out.add(m);
		}
		return out;
	}
}
