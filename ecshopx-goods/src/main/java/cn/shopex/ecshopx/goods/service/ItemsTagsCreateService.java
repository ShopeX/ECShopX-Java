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
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsTagsCreateService {

	private final ItemsTagsRepository repository;
	private final ItemsTagsMultiLangApplier itemsTagsMultiLangApplier;

	public ItemsTagsCreateService(ItemsTagsRepository repository, ItemsTagsMultiLangApplier itemsTagsMultiLangApplier) {
		this.repository = repository;
		this.itemsTagsMultiLangApplier = itemsTagsMultiLangApplier;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createTag(long companyId, long distributorId, Map<String, Object> input, String countryCode) {
		String tagName = input.get("tag_name").toString().trim();
		String tagColor = input.get("tag_color").toString();
		String fontColor = input.get("font_color").toString();
		String description = null;
		Object descObj = input.get("description");
		if (descObj != null) {
			description = descObj.toString();
		}

		int frontShow = 0;
		if (input.containsKey("front_show")) {
			Object fs = input.get("front_show");
			if (fs instanceof Number n) {
				frontShow = n.intValue();
			} else {
				frontShow = "1".equals(fs.toString().trim()) ? 1 : 0;
			}
		}

		ItemsTags existing = repository.selectByCompanyAndTagNameAndDistributor(companyId, tagName, distributorId);
		if (existing != null) {
			throw new ResourceException("标签名称不能重复");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		ItemsTags entity = new ItemsTags();
		entity.setCompanyId(companyId);
		entity.setTagName(tagName);
		entity.setTagColor(tagColor);
		entity.setFontColor(fontColor);
		entity.setDescription(description);
		entity.setDistributorId(distributorId);
		entity.setFrontShow(frontShow);
		entity.setCreated(now);
		entity.setUpdated(now);

		repository.insert(entity);
		Long tagId = entity.getTagId();
		if (tagId == null) {
			throw new ResourceException("创建标签失败");
		}

		itemsTagsMultiLangApplier.afterTagInsert(companyId, tagId, tagName, description, countryCode);
		return ItemsTagsRowMaps.toTagRowMap(entity);
	}
}
