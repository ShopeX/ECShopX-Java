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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemsRelCats;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsRelCatsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PointsmallItemsRelCatsWriteService {

	private final PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper;
	private final PointsmallItemsRelCatsWriteService self;

	public PointsmallItemsRelCatsWriteService(
			PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper,
			@Lazy PointsmallItemsRelCatsWriteService self) {
		this.pointsmallItemsRelCatsMapper = pointsmallItemsRelCatsMapper;
		this.self = self;
	}

	public void setItemsCategory(long companyId, long defaultItemId, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty() || defaultItemId <= 0) {
			return;
		}
		self.setItemsCategoryTransactional(companyId, List.of(defaultItemId), categoryIds);
	}

	public void deleteRelCatsByItemId(long companyId, long itemId) {
		if (companyId <= 0 || itemId <= 0) {
			return;
		}
		LambdaQueryWrapper<PointsmallItemsRelCats> qw = new LambdaQueryWrapper<>();
		qw.eq(PointsmallItemsRelCats::getCompanyId, companyId).eq(PointsmallItemsRelCats::getItemId, itemId);
		pointsmallItemsRelCatsMapper.delete(qw);
	}

	@Transactional(rollbackFor = Exception.class)
	public void setItemsCategoryTransactional(long companyId, List<Long> itemIds, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty() || itemIds == null || itemIds.isEmpty()) {
			return;
		}
		try {
			LambdaQueryWrapper<PointsmallItemsRelCats> qw = new LambdaQueryWrapper<>();
			qw.eq(PointsmallItemsRelCats::getCompanyId, companyId).in(PointsmallItemsRelCats::getItemId, itemIds);
			List<PointsmallItemsRelCats> existing = pointsmallItemsRelCatsMapper.selectList(qw);
			if (!existing.isEmpty()) {
				pointsmallItemsRelCatsMapper.delete(qw);
			}
			int now = (int) (System.currentTimeMillis() / 1000L);
			for (Long itemId : itemIds) {
				for (Long catId : categoryIds) {
					if (catId == null || catId <= 0) {
						continue;
					}
					PointsmallItemsRelCats row = new PointsmallItemsRelCats();
					row.setCompanyId(companyId);
					row.setItemId(itemId);
					row.setCategoryId(catId);
					row.setCreated(now);
					row.setUpdated(now);
					int n = pointsmallItemsRelCatsMapper.insert(row);
					if (n <= 0) {
						throw new ResourceException("商品关联分类出错，请检查后重试");
					}
				}
			}
		} catch (DataAccessException | PersistenceException e) {
			log.warn("pointsmall setItemsCategory persistence error", e);
			throw new ResourceException("商品关联分类出错，请检查后重试");
		}
	}
}
