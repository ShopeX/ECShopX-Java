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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.ItemsCategoryInvoiceTaxRateAdminPort;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.mapper.ItemsCategoryMapper;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ItemsCategoryInvoiceTaxRateAdminPortImpl implements ItemsCategoryInvoiceTaxRateAdminPort {

	private final ItemsCategoryMapper itemsCategoryMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;

	public ItemsCategoryInvoiceTaxRateAdminPortImpl(ItemsCategoryMapper itemsCategoryMapper,
			ItemsCategoryRepository itemsCategoryRepository) {
		this.itemsCategoryMapper = itemsCategoryMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	@Override
	public void assertNoOccupiedCategories(long companyId, String encodedCategoryIdsJson) {
		if (encodedCategoryIdsJson == null || encodedCategoryIdsJson.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId)
				.gt(ItemsCategory::getInvoiceTaxRateId, 0)
				.eq(ItemsCategory::getCategoryId, encodedCategoryIdsJson);
		List<ItemsCategory> occupied = itemsCategoryMapper.selectList(w);
		if (occupied.isEmpty()) {
			return;
		}
		Set<String> names = new LinkedHashSet<>();
		for (ItemsCategory row : occupied) {
			String n = row.getCategoryName();
			if (n != null && !n.trim().isEmpty()) {
				names.add(n.trim());
			} else {
				names.add("（未命名）");
			}
		}
		throw new ResourceException("以下分类已绑定发票税率：" + String.join("、", names));
	}

	@Override
	public void assertCategoriesNotBoundToOtherTaxRate(long companyId, Collection<Long> categoryIds,
			long currentTaxRateId) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId)
				.in(ItemsCategory::getCategoryId, categoryIds)
				.gt(ItemsCategory::getInvoiceTaxRateId, 0);
		if (currentTaxRateId > 0) {
			w.ne(ItemsCategory::getInvoiceTaxRateId, currentTaxRateId);
		}
		List<ItemsCategory> occupied = itemsCategoryMapper.selectList(w);
		if (occupied.isEmpty()) {
			return;
		}
		Set<String> names = new LinkedHashSet<>();
		for (ItemsCategory row : occupied) {
			String n = row.getCategoryName();
			if (n != null && !n.trim().isEmpty()) {
				names.add(n.trim());
			} else {
				names.add("（未命名）");
			}
		}
		throw new ResourceException("以下分类已绑定发票税率：" + String.join("、", names));
	}

	@Override
	public void clearInvoiceTaxFieldsForInvoiceTaxRateId(long companyId, long invoiceTaxRateId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<ItemsCategory> uw = new LambdaUpdateWrapper<>();
		uw.eq(ItemsCategory::getCompanyId, companyId)
				.eq(ItemsCategory::getInvoiceTaxRateId, invoiceTaxRateId)
				.set(ItemsCategory::getInvoiceTaxRateId, 0L)
				.set(ItemsCategory::getInvoiceTaxRate, "0")
				.set(ItemsCategory::getUpdated, now);
		itemsCategoryMapper.update(null, uw);
	}

	@Override
	public void clearInvoiceTaxFieldsForCategoryIds(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		List<ItemsCategory> rows = itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, categoryIds);
		if (rows.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (ItemsCategory row : rows) {
			LambdaUpdateWrapper<ItemsCategory> uw = new LambdaUpdateWrapper<>();
			uw.eq(ItemsCategory::getCompanyId, companyId)
					.eq(ItemsCategory::getCategoryId, row.getCategoryId())
					.set(ItemsCategory::getInvoiceTaxRateId, 0L)
					.set(ItemsCategory::getInvoiceTaxRate, "0")
					.set(ItemsCategory::getUpdated, now);
			itemsCategoryMapper.update(null, uw);
		}
	}

	@Override
	public void applyInvoiceTaxRateToCategories(long companyId, Collection<Long> categoryIds, long invoiceTaxRateId,
			String invoiceTaxRate) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Long id : categoryIds) {
			LambdaUpdateWrapper<ItemsCategory> uw = new LambdaUpdateWrapper<>();
			uw.eq(ItemsCategory::getCompanyId, companyId)
					.eq(ItemsCategory::getCategoryId, id)
					.set(ItemsCategory::getInvoiceTaxRateId, invoiceTaxRateId)
					.set(ItemsCategory::getInvoiceTaxRate, invoiceTaxRate)
					.set(ItemsCategory::getUpdated, now);
			itemsCategoryMapper.update(null, uw);
		}
	}
}
