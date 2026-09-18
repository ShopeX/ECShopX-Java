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

import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsTagsDeleteService {

	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;

	public ItemsTagsDeleteService(ItemsRelTagsRepository itemsRelTagsRepository, ItemsTagsRepository itemsTagsRepository) {
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteTag(long companyId, long tagId) {
		if (itemsRelTagsRepository.countByCompanyIdAndTagId(companyId, tagId) > 0) {
			itemsRelTagsRepository.deleteByCompanyIdAndTagId(companyId, tagId);
		}
		itemsTagsRepository.deleteByCompanyIdAndTagId(companyId, tagId);
	}
}
