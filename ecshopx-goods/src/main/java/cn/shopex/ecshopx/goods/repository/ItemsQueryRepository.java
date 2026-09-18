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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 商品只读查询（多规格 default_item_id 等）。
 */
@Repository
public class ItemsQueryRepository {

	private final ItemsMapper mapper;

	public ItemsQueryRepository(ItemsMapper mapper) {
		this.mapper = mapper;
	}

	/** company_id 且 item_id IN itemIds；itemIds 为空返回空列表。 */
	public List<Items> listByCompanyIdAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds);
		return mapper.selectList(w);
	}

	/**
	 * 传入的 itemId 为多规格默认的 default_item_id，查出同组商品行（最多 100 条）。
	 */
	public List<Items> listByDefaultItemIdAndCompanyId(long defaultItemId, long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getDefaultItemId, defaultItemId).eq(Items::getCompanyId, companyId).last("LIMIT 100");
		return mapper.selectList(w);
	}

	/** item_id IN itemIds 且 company_id；itemIds 为空返回 0。 */
	public long countByCompanyAndItemIdIn(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds);
		return mapper.selectCount(w);
	}

	/** 仅按 company_id 计数（全表匹配该公司下的商品行）。 */
	public long countByCompanyId(long companyId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId);
		return mapper.selectCount(w);
	}

	/** 仅按 company_id 分页列表，按 item_id 升序以保证顺序稳定。 */
	public List<Items> listPageByCompanyIdOrderByItemIdAsc(long companyId, int page, int pageSize) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).orderByAsc(Items::getItemId);
		Page<Items> p = new Page<>(page, pageSize, false);
		return mapper.selectPage(p, w).getRecords();
	}

	/** item_category 与 categoryIds（转为与库一致的 String）匹配且 company_id；categoryIds 为空返回 0。 */
	public long countByCompanyAndItemCategoryIn(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return 0L;
		}
		List<String> cats = categoryIds.stream().map(String::valueOf).toList();
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemCategory, cats);
		return mapper.selectCount(w);
	}

	/**
	 * 分类下商品通过 items_rel_attributes 仍关联给定属性 ID 时的行数；attributeIds 为空返回 0（不在 Mapper 层查询）。
	 */
	public long countItemsLinkedToAttributesInCategory(long companyId, long categoryId, List<Long> attributeIds) {
		if (attributeIds == null || attributeIds.isEmpty()) {
			return 0L;
		}
		return mapper.countItemsLinkedToAttributesInCategory(companyId, String.valueOf(categoryId), attributeIds);
	}

	public long countEpidemicItems(long companyId, Integer distributorIdOrNull) {
		LambdaQueryWrapper<Items> w = baseEpidemicWrapper(companyId, distributorIdOrNull);
		return mapper.selectCount(w);
	}

	public List<Items> pageEpidemicItems(long companyId, Integer distributorIdOrNull, int page, int pageSize) {
		LambdaQueryWrapper<Items> w = baseEpidemicWrapper(companyId, distributorIdOrNull);
		w.select(Items::getItemId, Items::getCompanyId, Items::getStore, Items::getItemBn, Items::getBarcode, Items::getItemName);
		Page<Items> p = new Page<>(page, pageSize, false);
		return mapper.selectPage(p, w).getRecords();
	}

	private static LambdaQueryWrapper<Items> baseEpidemicWrapper(long companyId, Integer distributorIdOrNull) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getIsEpidemic, 1);
		if (distributorIdOrNull != null && distributorIdOrNull > -1) {
			w.eq(Items::getDistributorId, distributorIdOrNull);
		}
		return w;
	}
}
