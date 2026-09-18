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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.support.PointsmallItemsNospecSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsmallItemsDeleteService {

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService;
	private final PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService;

	public PointsmallItemsDeleteService(
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService,
			PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.pointsmallItemsRelCatsWriteService = pointsmallItemsRelCatsWriteService;
		this.pointsmallItemStoreRedisWriteService = pointsmallItemStoreRedisWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteByItemId(long companyId, long itemId) {
		if (itemId < 1L) {
			throw new BadRequestException("商品id不能为空");
		}
		PointsmallItems row = pointsmallItemsMapper.selectById(itemId);
		if (row == null || row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ResourceException("删除商品信息有误");
		}

		Long rowItemId = row.getItemId();
		if (rowItemId == null) {
			throw new ResourceException("删除商品信息有误");
		}

		List<Long> itemIds;
		long defaultItemId;
		if (PointsmallItemsNospecSupport.isMultiSpec(row.getNospec())) {
			Long did = row.getDefaultItemId();
			long defId = did != null ? did : rowItemId;
			List<PointsmallItems> skus = pointsmallItemsMapper.listSkusByCompanyAndDefaultItemId(companyId, defId);
			itemIds = new ArrayList<>();
			for (PointsmallItems s : skus) {
				if (s.getItemId() != null) {
					itemIds.add(s.getItemId());
				}
			}
			if (itemIds.isEmpty()) {
				itemIds.add(rowItemId);
			}
			defaultItemId = defId;
		} else {
			itemIds = List.of(rowItemId);
			defaultItemId = rowItemId;
		}

		for (Long skuId : itemIds) {
			pointsmallItemsMapper.deleteById(skuId);
			pointsmallItemStoreRedisWriteService.deleteItemStore(skuId);
			LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
			w.eq(PointsmallItemRelAttributes::getItemId, skuId).eq(PointsmallItemRelAttributes::getCompanyId, companyId);
			pointsmallItemRelAttributesMapper.delete(w);
		}

		pointsmallItemsRelCatsWriteService.deleteRelCatsByItemId(companyId, defaultItemId);
	}
}
