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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.mapper.ItemsAttributeValuesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsAttributeValuesRepository {

	private final ItemsAttributeValuesMapper mapper;

	public ItemsAttributeValuesRepository(ItemsAttributeValuesMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 按 company_id + attribute_id 拉取值列表，分页 1/100（与现网 attribute_values 列表一致）。
	 */
	public List<ItemsAttributeValues> listByAttributeId(long companyId, long attributeId) {
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).eq(ItemsAttributeValues::getAttributeId, attributeId)
				.orderByAsc(ItemsAttributeValues::getSort).last("LIMIT 100");
		return mapper.selectList(w);
	}

	public ItemsAttributeValues selectByAttributeCompanyAndValue(long attributeId, long companyId, String attributeValue) {
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getAttributeId, attributeId).eq(ItemsAttributeValues::getCompanyId, companyId)
				.eq(ItemsAttributeValues::getAttributeValue, attributeValue);
		return mapper.selectOne(w);
	}

	public void deleteByCompanyAndAttributeId(long companyId, long attributeId) {
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).eq(ItemsAttributeValues::getAttributeId, attributeId);
		mapper.delete(w);
	}

	public void deleteByAttributeValueIds(long companyId, long attributeId, Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).eq(ItemsAttributeValues::getAttributeId, attributeId)
				.in(ItemsAttributeValues::getAttributeValueId, ids);
		mapper.delete(w);
	}

	public void updateById(ItemsAttributeValues entity) {
		mapper.updateById(entity);
	}

	public void insert(ItemsAttributeValues entity) {
		mapper.insert(entity);
	}

	public long countByCompanyAndValueIdsIn(long companyId, Collection<Long> attributeValueIds) {
		if (attributeValueIds == null || attributeValueIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).in(ItemsAttributeValues::getAttributeValueId, attributeValueIds);
		return mapper.selectCount(w);
	}

	public List<ItemsAttributeValues> listByCompanyAndAttributeValueIdsIn(long companyId, Collection<Long> attributeValueIds) {
		if (attributeValueIds == null || attributeValueIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).in(ItemsAttributeValues::getAttributeValueId, attributeValueIds).last("LIMIT 500");
		return mapper.selectList(w);
	}

	/**
	 * 按商户、属性、OMS 值 ID 批量查询，用于 OMS 同步更新时预绑定 attribute_value_id（限定 attribute_id 避免跨属性 oms_value_id 冲突）。
	 */
	public List<ItemsAttributeValues> listByCompanyAndAttributeAndOmsValueIdsIn(long companyId, long attributeId, Collection<Long> omsValueIds) {
		if (omsValueIds == null || omsValueIds.isEmpty()) {
			return List.of();
		}
		List<Long> filtered = new ArrayList<>();
		for (Long id : omsValueIds) {
			if (id != null && id != 0L) {
				filtered.add(id);
			}
		}
		if (filtered.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).eq(ItemsAttributeValues::getAttributeId, attributeId)
				.in(ItemsAttributeValues::getOmsValueId, filtered).last("LIMIT 500");
		return mapper.selectList(w);
	}

	public List<ItemsAttributeValues> listByCompanyAttributeIdsAndAttributeValuesIn(long companyId, Collection<Long> attributeIds, Collection<String> attributeValues) {
		if (attributeIds == null || attributeIds.isEmpty() || attributeValues == null || attributeValues.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsAttributeValues> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributeValues::getCompanyId, companyId).in(ItemsAttributeValues::getAttributeId, attributeIds).in(ItemsAttributeValues::getAttributeValue, attributeValues).last("LIMIT 100");
		return mapper.selectList(w);
	}
}
