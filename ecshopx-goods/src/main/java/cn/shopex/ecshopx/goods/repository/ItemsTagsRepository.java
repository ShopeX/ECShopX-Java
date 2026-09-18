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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsTagsMapper;
import cn.shopex.ecshopx.goods.support.ItemLangShardJdbc;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemsTagsRepository {

	private static final String TABLE_ITEMS_TAGS = "items_tags";

	private final ItemsTagsMapper mapper;
	private final ItemLangShardJdbc itemLangShardJdbc;

	public ItemsTagsRepository(ItemsTagsMapper mapper, ItemLangShardJdbc itemLangShardJdbc) {
		this.mapper = mapper;
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	public ItemsTags selectByCompanyAndTagNameAndDistributor(long companyId, String tagName, long distributorId) {
		String t = tagName == null ? "" : tagName.trim();
		LambdaQueryWrapper<ItemsTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsTags::getCompanyId, companyId).eq(ItemsTags::getTagName, t).eq(ItemsTags::getDistributorId, distributorId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public void insert(ItemsTags entity) {
		mapper.insert(entity);
	}

	public ItemsTags selectById(long tagId) {
		return mapper.selectById(tagId);
	}

	public void update(ItemsTags entity) {
		int rows = mapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public void deleteByCompanyIdAndTagId(long companyId, long tagId) {
		LambdaQueryWrapper<ItemsTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsTags::getCompanyId, companyId).eq(ItemsTags::getTagId, tagId);
		mapper.delete(w);
	}

	public IPage<ItemsTags> selectPageByFilter(Page<ItemsTags> page, ItemsTagsListFilter filter) {
		LambdaQueryWrapper<ItemsTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsTags::getCompanyId, filter.getCompanyId());
		if (filter.getTagIdsIn() != null && !filter.getTagIdsIn().isEmpty()) {
			w.in(ItemsTags::getTagId, filter.getTagIdsIn());
		}
		ItemsTagsListFilter.DistributorMode mode = filter.getDistributorMode();
		if (mode != null) {
			switch (mode) {
				case EQ -> w.eq(ItemsTags::getDistributorId, filter.getDistributorEq());
				case GT_ZERO -> w.gt(ItemsTags::getDistributorId, 0L);
				case IN -> w.in(ItemsTags::getDistributorId, filter.getDistributorIn());
				default -> {
				}
			}
		}
		if (StringUtils.hasText(filter.getTagNameContains())) {
			w.like(ItemsTags::getTagName, filter.getTagNameContains());
		}
		if (filter.isFrontShowSpecified() && filter.getFrontShowValue() != null) {
			w.eq(ItemsTags::getFrontShow, filter.getFrontShowValue());
		}
		w.orderByDesc(ItemsTags::getCreated);
		return mapper.selectPage(page, w);
	}

	public List<Long> selectTagIdsByKeywordForCompany(long companyId, String keyword, String countryCode) {
		String kw = keyword == null ? "" : keyword.trim();
		if (!StringUtils.hasText(kw)) {
			return List.of();
		}
		String cc = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";

		Set<Long> merged = new LinkedHashSet<>();
		List<Long> langIds = itemLangShardJdbc.listDataIdsByFieldLike(
				companyId, cc, TABLE_ITEMS_TAGS, TABLE_ITEMS_TAGS, "tag_name", "%" + kw + "%");
		merged.addAll(langIds);

		LambdaQueryWrapper<ItemsTags> tw = new LambdaQueryWrapper<>();
		tw.eq(ItemsTags::getCompanyId, companyId).like(ItemsTags::getTagName, kw).select(ItemsTags::getTagId);
		List<ItemsTags> tagRows = mapper.selectList(tw);
		for (ItemsTags t : tagRows) {
			if (t.getTagId() != null) {
				merged.add(t.getTagId());
			}
		}

		return new ArrayList<>(merged);
	}
}
