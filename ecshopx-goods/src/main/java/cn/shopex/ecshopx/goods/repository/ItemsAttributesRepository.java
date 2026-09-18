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

import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.mapper.ItemsAttributesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemsAttributesRepository {

	private final ItemsAttributesMapper mapper;

	public ItemsAttributesRepository(ItemsAttributesMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 分页 1/100，按 attribute_sort 升序（与现网 getAttrList 一致）。
	 */
	public List<ItemsAttributes> listByAttributeIds(List<Long> attributeIds) {
		if (attributeIds == null || attributeIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.in(ItemsAttributes::getAttributeId, attributeIds).orderByAsc(ItemsAttributes::getAttributeSort).last("LIMIT 100");
		return mapper.selectList(w);
	}

	public List<ItemsAttributes> listByCompanyAndAttributeIdsIn(long companyId, Collection<Long> attributeIds) {
		if (attributeIds == null || attributeIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).in(ItemsAttributes::getAttributeId, attributeIds).orderByAsc(ItemsAttributes::getAttributeSort).last("LIMIT 200");
		return mapper.selectList(w);
	}

	public boolean existsBrandByCompanyAndName(long companyId, String attributeName) {
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, "brand")
				.eq(ItemsAttributes::getAttributeName, attributeName);
		return mapper.selectCount(w) > 0;
	}

	public ItemsAttributes selectByCompanyAndAttributeId(long companyId, long attributeId) {
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeId, attributeId);
		return mapper.selectOne(w);
	}

	public ItemsAttributes selectByCompanyAndAttributeTypeAndAttributeCode(long companyId, String attributeType, String attributeCode) {
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, attributeType).eq(ItemsAttributes::getAttributeCode, attributeCode);
		return mapper.selectOne(w);
	}

	public boolean existsBrandByCompanyAndNameExcludingAttributeId(long companyId, String attributeName, long excludeAttributeId) {
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, "brand")
				.eq(ItemsAttributes::getAttributeName, attributeName).ne(ItemsAttributes::getAttributeId, excludeAttributeId);
		return mapper.selectCount(w) > 0;
	}

	public void updateById(ItemsAttributes entity) {
		mapper.updateById(entity);
	}

	public void insert(ItemsAttributes entity) {
		mapper.insert(entity);
	}

	public int deleteByCompanyAndAttributeId(long companyId, long attributeId) {
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeId, attributeId);
		return mapper.delete(w);
	}

	/**
	 * 主表分页列表：筛选条件与列表查询接口一致；按 created 降序。
	 *
	 * @param attributeIdsOrNull null 或 empty：不按 attribute_id 过滤；size==1：eq；size&gt;1：in
	 * @param distributorIdEqOrNull 非 null 时 eq(distributor_id)（brand+platform 分支）
	 */
	public IPage<ItemsAttributes> selectPageByFilter(long companyId, String attributeType, String attributeNameContains,
			List<Long> attributeIdsOrNull, Long distributorIdEqOrNull, int pageNum, int pageSize) {
		Page<ItemsAttributes> mpPage = new Page<>(pageNum, pageSize);
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId);
		// 未传 attribute_type 时仅匹配类型为空的记录（与列表接口请求未带该参数时的筛选语义一致）。
		if (attributeType == null) {
			w.isNull(ItemsAttributes::getAttributeType);
		} else {
			w.eq(ItemsAttributes::getAttributeType, attributeType);
		}
		if (StringUtils.hasText(attributeNameContains)) {
			String escaped = escapeSqlLike(attributeNameContains.trim());
			w.like(ItemsAttributes::getAttributeName, "%" + escaped + "%");
		}
		if (attributeIdsOrNull != null && !attributeIdsOrNull.isEmpty()) {
			if (attributeIdsOrNull.size() == 1) {
				w.eq(ItemsAttributes::getAttributeId, attributeIdsOrNull.get(0));
			} else {
				w.in(ItemsAttributes::getAttributeId, attributeIdsOrNull);
			}
		}
		if (distributorIdEqOrNull != null) {
			w.eq(ItemsAttributes::getDistributorId, distributorIdEqOrNull);
		}
		w.orderByDesc(ItemsAttributes::getCreated);
		return mapper.selectPage(mpPage, w);
	}

	public long countByCompanyTypeAndAttributeIds(long companyId, String attributeType, Collection<Long> attributeIds) {
		if (attributeIds == null || attributeIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, attributeType).in(ItemsAttributes::getAttributeId, attributeIds);
		return mapper.selectCount(w);
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	public List<ItemsAttributes> listByCompanyAndItemSpecAttributeCodesIn(long companyId, Collection<String> attributeCodes) {
		if (attributeCodes == null || attributeCodes.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, "item_spec").in(ItemsAttributes::getAttributeCode, attributeCodes);
		return mapper.selectList(w);
	}

	public Optional<Long> findOmsBrandAttributeIdByCompanyAndName(long companyId, String attributeName) {
		if (attributeName == null || attributeName.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId).eq(ItemsAttributes::getAttributeType, "brand").eq(ItemsAttributes::getAttributeName, attributeName);
		ItemsAttributes entity = mapper.selectOne(w);
		return Optional.ofNullable(entity).map(ItemsAttributes::getAttributeId);
	}

	/** Brand attributes for the given ids, ordered by {@code attribute_sort} ascending (no row limit). */
	public List<ItemsAttributes> listBrandAttributesByCompanyAndAttributeIdsOrdered(long companyId, Collection<Long> attributeIds) {
		if (attributeIds == null || attributeIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId)
				.eq(ItemsAttributes::getAttributeType, "brand")
				.in(ItemsAttributes::getAttributeId, attributeIds)
				.orderByAsc(ItemsAttributes::getAttributeSort);
		return mapper.selectList(w);
	}

	public Optional<ItemsAttributes> findFirstBrandByCompanyAndName(long companyId, String attributeName) {
		if (!StringUtils.hasText(attributeName)) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId)
				.eq(ItemsAttributes::getAttributeType, "brand")
				.eq(ItemsAttributes::getAttributeName, attributeName.trim())
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public List<ItemsAttributes> listItemSpecByCompanyNamesAndIdsOrdered(
			long companyId, Collection<String> attributeNames, Collection<Long> attributeIds) {
		if (attributeNames == null || attributeNames.isEmpty() || attributeIds == null || attributeIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId)
				.eq(ItemsAttributes::getAttributeType, "item_spec")
				.in(ItemsAttributes::getAttributeName, attributeNames)
				.in(ItemsAttributes::getAttributeId, attributeIds)
				.orderByDesc(ItemsAttributes::getIsImage)
				.orderByAsc(ItemsAttributes::getAttributeId)
				.last("LIMIT 100");
		return mapper.selectList(w);
	}

	public ItemsAttributes findFirstItemSpecImageByCompanyNamesAndIds(
			long companyId, Collection<String> attributeNames, Collection<Long> attributeIds) {
		if (attributeNames == null || attributeNames.isEmpty() || attributeIds == null || attributeIds.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<ItemsAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemsAttributes::getCompanyId, companyId)
				.eq(ItemsAttributes::getAttributeType, "item_spec")
				.eq(ItemsAttributes::getIsImage, "true")
				.in(ItemsAttributes::getAttributeName, attributeNames)
				.in(ItemsAttributes::getAttributeId, attributeIds)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}
}
