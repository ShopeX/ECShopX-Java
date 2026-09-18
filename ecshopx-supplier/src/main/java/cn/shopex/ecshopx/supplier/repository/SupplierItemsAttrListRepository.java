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

package cn.shopex.ecshopx.supplier.repository;

import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.mapper.SupplierItemsAttrMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SupplierItemsAttrListRepository {

	private final SupplierItemsAttrMapper mapper;

	public SupplierItemsAttrListRepository(SupplierItemsAttrMapper mapper) {
		this.mapper = mapper;
	}

	/** 销售类目 JSON：attribute_type = category */
	public List<SupplierItemsAttr> listCategoryAttrsByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).in(SupplierItemsAttr::getItemId, itemIds).eq(SupplierItemsAttr::getAttributeType, "category")
				.eq(SupplierItemsAttr::getIsDel, 0L);
		return mapper.selectList(w);
	}

	public List<SupplierItemsAttr> listByCompanyAndItemIdAllTypes(long companyId, long itemId) {
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).eq(SupplierItemsAttr::getItemId, itemId).eq(SupplierItemsAttr::getIsDel, 0L);
		return mapper.selectList(w);
	}

	public List<SupplierItemsAttr> listByCompanyItemIdsAndAttributeType(long companyId, Collection<Long> itemIds, String attributeType) {
		if (itemIds == null || itemIds.isEmpty() || !StringUtils.hasText(attributeType)) {
			return List.of();
		}
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).in(SupplierItemsAttr::getItemId, itemIds).eq(SupplierItemsAttr::getAttributeType, attributeType)
				.eq(SupplierItemsAttr::getIsDel, 0L);
		return mapper.selectList(w);
	}

	public int deleteByCompanyIdAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).in(SupplierItemsAttr::getItemId, itemIds);
		return mapper.delete(w);
	}
}
