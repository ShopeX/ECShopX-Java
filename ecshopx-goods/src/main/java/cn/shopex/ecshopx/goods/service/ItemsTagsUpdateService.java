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
public class ItemsTagsUpdateService {

	private final ItemsTagsRepository repository;
	private final ItemsTagsMultiLangApplier itemsTagsMultiLangApplier;

	public ItemsTagsUpdateService(ItemsTagsRepository repository, ItemsTagsMultiLangApplier itemsTagsMultiLangApplier) {
		this.repository = repository;
		this.itemsTagsMultiLangApplier = itemsTagsMultiLangApplier;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateTag(long companyId, long jwtDistributorId, Map<String, Object> input, String countryCode) {
		long tagId = parseRequiredTagId(input);

		String tagName = input.get("tag_name").toString().trim();
		String tagColor = input.get("tag_color").toString();
		String fontColor = input.get("font_color").toString();

		String description;
		if (input.containsKey("description")) {
			Object descObj = input.get("description");
			description = descObj == null ? null : descObj.toString();
		} else {
			description = null;
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

		ItemsTags existing = repository.selectById(tagId);
		if (existing == null) {
			throw new ResourceException("标签不存在");
		}
		if (existing.getCompanyId() == null || !existing.getCompanyId().equals(companyId)) {
			throw new ResourceException("未查询到更新数据");
		}

		long existingDist = existing.getDistributorId() != null ? existing.getDistributorId() : 0L;
		if (jwtDistributorId != 0L && existingDist != jwtDistributorId) {
			throw new ResourceException("没有权限编辑该标签");
		}

		long effectiveDistributorId = existing.getDistributorId() != null ? existing.getDistributorId() : 0L;
		ItemsTags dup = repository.selectByCompanyAndTagNameAndDistributor(companyId, tagName, effectiveDistributorId);
		if (dup != null && !dup.getTagId().equals(tagId)) {
			throw new ResourceException("标签名称不能重复");
		}

		existing.setTagName(tagName);
		existing.setTagColor(tagColor);
		existing.setFontColor(fontColor);
		if (input.containsKey("description")) {
			existing.setDescription(description);
		}
		existing.setFrontShow(frontShow);
		existing.setUpdated((int) (System.currentTimeMillis() / 1000L));

		repository.update(existing);

		if (input.containsKey("tag_name") || input.containsKey("description")) {
			itemsTagsMultiLangApplier.afterTagUpdate(companyId, tagId, input, countryCode);
		}

		return ItemsTagsRowMaps.toTagRowMap(existing);
	}

	private static long parseRequiredTagId(Map<String, Object> input) {
		Object v = input.get("tag_id");
		if (v == null) {
			throw new ResourceException("标签ID不能为空");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("标签ID不能为空");
		}
	}
}
