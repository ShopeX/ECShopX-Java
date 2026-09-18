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

package cn.shopex.ecshopx.merchant.repository;

import cn.shopex.ecshopx.merchant.domain.MerchantType;
import cn.shopex.ecshopx.merchant.mapper.MerchantTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MerchantTypeRepository {

	private final MerchantTypeMapper merchantTypeMapper;

	public MerchantTypeRepository(MerchantTypeMapper merchantTypeMapper) {
		this.merchantTypeMapper = merchantTypeMapper;
	}

	public MerchantType findByCompanyIdAndId(long companyId, long id) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getId, id);
		return merchantTypeMapper.selectOne(w);
	}

	public List<Long> listIdsByParentIdAndCompanyId(long companyId, long parentId) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getParentId, parentId);
		w.select(MerchantType::getId);
		return merchantTypeMapper.selectList(w).stream()
				.map(MerchantType::getId)
				.filter(Objects::nonNull)
				.toList();
	}

	public long countByParentIdAndCompanyId(long companyId, long parentId) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getParentId, parentId);
		return merchantTypeMapper.selectCount(w);
	}

	public boolean deleteByCompanyIdAndId(long companyId, long id) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getId, id);
		merchantTypeMapper.delete(w);
		return true;
	}

	public boolean deleteByCompanyIdAndParentId(long companyId, long parentId) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getParentId, parentId);
		merchantTypeMapper.delete(w);
		return true;
	}

	public List<MerchantType> listMerchantTypesForTypeList(
			long companyId,
			String nameContains,
			List<Long> idInFilter,
			LinkedHashMap<String, String> orderBy) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId);
		if (StringUtils.hasText(nameContains)) {
			w.like(MerchantType::getName, "%" + nameContains + "%");
		}
		if (idInFilter != null && !idInFilter.isEmpty()) {
			w.in(MerchantType::getId, idInFilter);
		}
		applyTypeListOrder(w, orderBy);
		return merchantTypeMapper.selectList(w);
	}

	public List<MerchantType> listMerchantTypesByRelatedAnchor(
			long companyId,
			long anchorId,
			boolean filterByParentId,
			LinkedHashMap<String, String> orderBy) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId);
		if (filterByParentId) {
			w.eq(MerchantType::getParentId, anchorId);
		} else {
			w.eq(MerchantType::getId, anchorId);
		}
		applyTypeListOrder(w, orderBy);
		return merchantTypeMapper.selectList(w);
	}

	private static void applyTypeListOrder(LambdaQueryWrapper<MerchantType> w, LinkedHashMap<String, String> orderBy) {
		if (orderBy != null) {
			String sortDir = orderBy.get("sort");
			if ("asc".equals(sortDir)) {
				w.orderByAsc(MerchantType::getSort);
			} else if ("desc".equals(sortDir)) {
				w.orderByDesc(MerchantType::getSort);
			}
		}
		w.orderByDesc(MerchantType::getUpdated);
	}

	/**
	 * Visible-type list: {@code company_id} + {@code is_show = true} + optional {@code name} LIKE, then either
	 * {@code parent_id = 0} + {@code id IN}, or {@code parent_id IN}, or {@code parent_id = eqParentId}.
	 */
	public long countVisibleTypes(
			long companyId,
			long eqParentId,
			List<Long> parentIdIn,
			List<Long> idIn,
			String nameContains) {
		LambdaQueryWrapper<MerchantType> w = visibleTypesWhere(companyId, eqParentId, parentIdIn, idIn, nameContains);
		return merchantTypeMapper.selectCount(w);
	}

	public List<MerchantType> listVisibleTypes(
			long companyId,
			long eqParentId,
			List<Long> parentIdIn,
			List<Long> idIn,
			String nameContains,
			boolean orderSortAscCreatedAsc) {
		return listVisibleTypes(
				companyId, eqParentId, parentIdIn, idIn, nameContains, orderSortAscCreatedAsc, null, null);
	}

	public List<MerchantType> listVisibleTypes(
			long companyId,
			long eqParentId,
			List<Long> parentIdIn,
			List<Long> idIn,
			String nameContains,
			boolean orderSortAscCreatedAsc,
			Integer limit,
			Integer offset) {
		LambdaQueryWrapper<MerchantType> w = visibleTypesWhere(companyId, eqParentId, parentIdIn, idIn, nameContains);
		if (orderSortAscCreatedAsc) {
			w.orderByAsc(MerchantType::getSort).orderByAsc(MerchantType::getCreated);
		}
		if (limit != null && offset != null) {
			w.last("LIMIT " + limit + " OFFSET " + offset);
		}
		return merchantTypeMapper.selectList(w);
	}

	private static LambdaQueryWrapper<MerchantType> visibleTypesWhere(
			long companyId,
			long eqParentId,
			List<Long> parentIdIn,
			List<Long> idIn,
			String nameContains) {
		LambdaQueryWrapper<MerchantType> w = new LambdaQueryWrapper<>();
		w.eq(MerchantType::getCompanyId, companyId).eq(MerchantType::getIsShow, true);
		if (StringUtils.hasText(nameContains)) {
			w.like(MerchantType::getName, "%" + nameContains + "%");
		}
		if (idIn != null && !idIn.isEmpty()) {
			w.eq(MerchantType::getParentId, 0L).in(MerchantType::getId, idIn);
		} else if (parentIdIn != null && !parentIdIn.isEmpty()) {
			w.in(MerchantType::getParentId, parentIdIn);
		} else {
			w.eq(MerchantType::getParentId, eqParentId);
		}
		return w;
	}
}
