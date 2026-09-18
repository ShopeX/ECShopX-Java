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

import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.mapper.ItemsRelTagsMapper;
import cn.shopex.ecshopx.goods.repository.dto.RelItemTagNameRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsRelTagsRepository {

	private final ItemsRelTagsMapper mapper;

	public ItemsRelTagsRepository(ItemsRelTagsMapper mapper) {
		this.mapper = mapper;
	}

	public long countByCompanyIdAndTagId(long companyId, long tagId) {
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).eq(ItemsRelTags::getTagId, tagId);
		return mapper.selectCount(w);
	}

	public int deleteByCompanyIdAndTagId(long companyId, long tagId) {
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).eq(ItemsRelTags::getTagId, tagId);
		return mapper.delete(w);
	}

	/** 商品列表 enrichment：需完整关联行，不加行数上限。 */
	public List<ItemsRelTags> listRowsByCompanyIdAndItemIdIn(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).in(ItemsRelTags::getItemId, itemIds);
		return mapper.selectList(w);
	}

	/** 标签关联批量查询（有上限，与历史实现一致）。 */
	public List<ItemsRelTags> getLists(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).in(ItemsRelTags::getItemId, itemIds).last("LIMIT 1000");
		return mapper.selectList(w);
	}

	public ItemsRelTags getInfo(long companyId, long itemId) {
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).eq(ItemsRelTags::getItemId, itemId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public void create(ItemsRelTags row) {
		mapper.insert(row);
	}

	public void deleteBy(long companyId, long itemId) {
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).eq(ItemsRelTags::getItemId, itemId);
		mapper.delete(w);
	}

	public void deleteBy(long companyId, long itemId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).eq(ItemsRelTags::getItemId, itemId).in(ItemsRelTags::getTagId, tagIds);
		mapper.delete(w);
	}

	/**
	 * 按商户与标签（单值或 IN）查询关联行的 item_id，按 item_id 升序；去重并保持顺序。
	 */
	/** rel.item_id 为 SPU（default_item_id）侧关联键，与标签导出一致。 */
	public List<RelItemTagNameRow> listRelItemIdTagNameRows(long companyId, Collection<Long> relItemIds) {
		if (relItemIds == null || relItemIds.isEmpty()) {
			return List.of();
		}
		return mapper.selectRelItemIdTagNames(companyId, relItemIds);
	}

	public List<Long> listItemIdsByCompanyIdAndTagIds(long companyId, Collection<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId);
		if (tagIds.size() == 1) {
			w.eq(ItemsRelTags::getTagId, tagIds.iterator().next());
		} else {
			w.in(ItemsRelTags::getTagId, tagIds);
		}
		w.select(ItemsRelTags::getItemId).orderByAsc(ItemsRelTags::getItemId);
		List<ItemsRelTags> rows = mapper.selectList(w);
		Set<Long> seen = new LinkedHashSet<>();
		for (ItemsRelTags row : rows) {
			if (row.getItemId() != null) {
				seen.add(row.getItemId());
			}
		}
		return new ArrayList<>(seen);
	}

	public long countByCompanyIdAndTagIdsIn(long companyId, Collection<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId);
		if (tagIds.size() == 1) {
			w.eq(ItemsRelTags::getTagId, tagIds.iterator().next());
		} else {
			w.in(ItemsRelTags::getTagId, tagIds);
		}
		Long c = mapper.selectCount(w);
		return c == null ? 0L : c;
	}

	public List<Long> listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(long companyId, Collection<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId);
		if (tagIds.size() == 1) {
			w.eq(ItemsRelTags::getTagId, tagIds.iterator().next());
		} else {
			w.in(ItemsRelTags::getTagId, tagIds);
		}
		w.last("ORDER BY id ASC");
		List<ItemsRelTags> rows = mapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>(rows.size());
		for (ItemsRelTags row : rows) {
			if (row.getItemId() != null) {
				out.add(row.getItemId());
			}
		}
		return out;
	}
}
