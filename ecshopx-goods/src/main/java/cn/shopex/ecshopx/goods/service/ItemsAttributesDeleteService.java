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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsAttributesDeleteService {

	private static final Logger log = LoggerFactory.getLogger(ItemsAttributesDeleteService.class);

	private static final String MSG_HAS_ASSOCIATED_ITEMS = "有关联商品，请先处理关联的商品";
	private static final String MSG_HAS_ASSOCIATED_POINTSMALL_ITEMS = "有关联积分商城商品，请先处理关联的积分商城商品";

	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final PointsmallItemsMapper pointsmallItemsMapper;

	public ItemsAttributesDeleteService(ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsQueryRepository itemsQueryRepository, ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			PointsmallItemsMapper pointsmallItemsMapper) {
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteAttr(long companyId, long attributeId) {
		checkDeleteAttr(companyId, attributeId);
		try {
			deleteRows(companyId, attributeId);
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			log.error("delete attribute failed: companyId={}, attributeId={}", companyId, attributeId, e);
			throw new ResourceException("删除失败");
		}
	}

	private void checkDeleteAttr(long companyId, long attributeId) {
		List<ItemRelAttributes> relRows = itemRelAttributesRepository.listByCompanyAndAttributeId(companyId, attributeId);
		if (!relRows.isEmpty()) {
			List<Long> itemIds = distinctNonNullItemIdsFromRel(relRows);
			if (!itemIds.isEmpty() && itemsQueryRepository.countByCompanyAndItemIdIn(companyId, itemIds) > 0) {
				throw new ResourceException(MSG_HAS_ASSOCIATED_ITEMS);
			}
		}

		LambdaQueryWrapper<PointsmallItemRelAttributes> pw = new LambdaQueryWrapper<>();
		pw.eq(PointsmallItemRelAttributes::getCompanyId, companyId).eq(PointsmallItemRelAttributes::getAttributeId, attributeId);
		List<PointsmallItemRelAttributes> psRel = pointsmallItemRelAttributesMapper.selectList(pw);
		if (!psRel.isEmpty()) {
			List<Long> psItemIds = distinctNonNullItemIdsFromPointsmall(psRel);
			if (!psItemIds.isEmpty()) {
				LambdaQueryWrapper<PointsmallItems> iw = new LambdaQueryWrapper<>();
				iw.eq(PointsmallItems::getCompanyId, companyId).in(PointsmallItems::getItemId, psItemIds);
				if (pointsmallItemsMapper.selectCount(iw) > 0) {
					throw new ResourceException(MSG_HAS_ASSOCIATED_POINTSMALL_ITEMS);
				}
			}
		}
	}

	private static List<Long> distinctNonNullItemIdsFromRel(List<ItemRelAttributes> rows) {
		Set<Long> set = new LinkedHashSet<>();
		for (ItemRelAttributes r : rows) {
			Long id = r.getItemId();
			if (id != null) {
				set.add(id);
			}
		}
		return new ArrayList<>(set);
	}

	private static List<Long> distinctNonNullItemIdsFromPointsmall(List<PointsmallItemRelAttributes> rows) {
		Set<Long> set = new LinkedHashSet<>();
		for (PointsmallItemRelAttributes r : rows) {
			Long id = r.getItemId();
			if (id != null) {
				set.add(id);
			}
		}
		return new ArrayList<>(set);
	}

	private void deleteRows(long companyId, long attributeId) {
		itemsAttributesRepository.deleteByCompanyAndAttributeId(companyId, attributeId);
		itemsAttributeValuesRepository.deleteByCompanyAndAttributeId(companyId, attributeId);
	}
}
