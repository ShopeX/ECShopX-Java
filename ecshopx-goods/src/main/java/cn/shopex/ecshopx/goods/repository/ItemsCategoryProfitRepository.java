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

import cn.shopex.ecshopx.goods.domain.ItemsCategoryProfit;
import cn.shopex.ecshopx.goods.mapper.ItemsCategoryProfitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsCategoryProfitRepository {

	private final ItemsCategoryProfitMapper mapper;

	public ItemsCategoryProfitRepository(ItemsCategoryProfitMapper mapper) {
		this.mapper = mapper;
	}

	public List<ItemsCategoryProfit> listByCompanyAndCategoryIds(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemsCategoryProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategoryProfit::getCompanyId, companyId).in(ItemsCategoryProfit::getCategoryId, categoryIds);
		return mapper.selectList(w);
	}

	public void deleteByCompanyAndCategory(long companyId, long categoryId) {
		LambdaQueryWrapper<ItemsCategoryProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategoryProfit::getCompanyId, companyId).eq(ItemsCategoryProfit::getCategoryId, categoryId);
		mapper.delete(w);
	}

	public int insertRow(ItemsCategoryProfit row) {
		return mapper.insert(row);
	}

	/** 仅按类目 id 查询一条，不按公司过滤（与运营端商品分润默认链路一致）。 */
	public Optional<ItemsCategoryProfit> findByCategoryIdOnly(long categoryId) {
		LambdaQueryWrapper<ItemsCategoryProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategoryProfit::getCategoryId, categoryId).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}
}
