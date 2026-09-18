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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.mapper.ItemsRelCatsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ItemsRelCatsWriteService {

	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsRelCatsMapper itemsRelCatsMapper;
	private final ItemsRelCatsWriteService self;

	public ItemsRelCatsWriteService(
			ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsRelCatsMapper itemsRelCatsMapper,
			@Lazy ItemsRelCatsWriteService self) {
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsRelCatsMapper = itemsRelCatsMapper;
		this.self = self;
	}

	public void setItemsCategory(long companyId, long defaultItemId, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}
		self.setItemsCategoryBatch(companyId, List.of(defaultItemId), categoryIds);
	}

	@Transactional(rollbackFor = Exception.class)
	public void setItemsCategoryBatch(long companyId, List<Long> itemIds, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		try {
			List<ItemsRelCats> existing = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
			if (!existing.isEmpty()) {
				itemsRelCatsRepository.deleteByCompanyIdAndItemIdIn(companyId, itemIds);
			}
			int now = (int) (System.currentTimeMillis() / 1000L);
			for (Long itemId : itemIds) {
				for (Long catId : categoryIds) {
					if (catId == null || catId <= 0) {
						continue;
					}
					ItemsRelCats row = new ItemsRelCats();
					row.setCompanyId(companyId);
					row.setItemId(itemId);
					row.setCategoryId(catId);
					row.setCreated(now);
					row.setUpdated(now);
					int n = itemsRelCatsMapper.insert(row);
					if (n <= 0) {
						throw new ResourceException("商品关联分类出错，请检查后重试");
					}
				}
			}
		} catch (DataAccessException | PersistenceException e) {
			log.warn("setItemsCategoryBatch persistence error", e);
			throw new ResourceException("商品关联分类出错，请检查后重试");
		}
	}
}
