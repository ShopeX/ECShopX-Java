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

package cn.shopex.ecshopx.goods.service.marketing;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuListService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketingActivityItemListCatalogSupport {

	private final ItemsMapper itemsMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService;

	public MarketingActivityItemListCatalogSupport(
			ItemsMapper itemsMapper,
			DistributorListQueryService distributorListQueryService,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsRelTagsRepository itemsRelTagsRepository,
			EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService) {
		this.itemsMapper = itemsMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.employeePurchaseItemsSkuListService = employeePurchaseItemsSkuListService;
	}

	public Map<String, Object> loadFullStoreMarketingActivityItemListData(
			long companyId, List<Long> distributorScope, int page, int pageSize, boolean orderByItemIdDesc) {
		List<Integer> distInts = resolveDistributorIntsForFullStoreList(companyId, distributorScope);
		if (distInts == null || distInts.isEmpty()) {
			return emptyRel();
		}
		int p = Math.max(1, page);
		int ps = Math.max(1, pageSize);
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getDistributorId, distInts);
		if (orderByItemIdDesc) {
			w.orderByDesc(Items::getItemId);
		}
		Page<Items> pg = new Page<>(p, ps, false);
		Page<Items> result = itemsMapper.selectPage(pg, w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Items row : result.getRecords()) {
			if (row.getItemId() != null) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("item_id", row.getItemId());
				list.add(m);
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		Long total = itemsMapper.selectCount(w);
		out.put("total_count", total == null ? 0L : total);
		out.put("list", list);
		return out;
	}

	/**
	 * @return {@code null} when the requested distributor scope is invalid (no rows), matching empty list semantics.
	 */
	private List<Integer> resolveDistributorIntsForFullStoreList(long companyId, List<Long> distributorScope) {
		if (distributorScope == null || distributorScope.isEmpty()) {
			return List.of();
		}
		boolean onlyZero =
				distributorScope.stream().allMatch(d -> d == null || d == 0L);
		if (onlyZero) {
			List<Integer> out = new ArrayList<>();
			out.add(0);
			for (Long id : distributorListQueryService.listValidDistributorIdsForCompany(companyId)) {
				addIntIfInRange(out, id);
			}
			return out;
		}
		Set<Long> wanted = new LinkedHashSet<>();
		for (Long d : distributorScope) {
			if (d != null && d > 0L) {
				wanted.add(d);
			}
		}
		if (wanted.isEmpty()) {
			return List.of();
		}
		List<Distributor> rows = distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(wanted));
		Set<Long> ok = new LinkedHashSet<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() == null) {
				continue;
			}
			String v = d.getIsValid() == null ? "" : String.valueOf(d.getIsValid()).trim();
			if ("true".equalsIgnoreCase(v)) {
				ok.add(d.getDistributorId());
			}
		}
		if (ok.size() != wanted.size()) {
			return null;
		}
		List<Integer> out = new ArrayList<>();
		for (Long id : wanted) {
			addIntIfInRange(out, id);
		}
		return out;
	}

	private static void addIntIfInRange(List<Integer> out, Long id) {
		if (id == null) {
			return;
		}
		if (id < Integer.MIN_VALUE || id > Integer.MAX_VALUE) {
			return;
		}
		out.add(id.intValue());
	}

	private static Map<String, Object> emptyRel() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", 0L);
		m.put("list", List.of());
		return m;
	}

	public Map<String, Object> loadSkuItemsPageForMarketingActivityItemList(
			long companyId, List<Long> itemIds, int page, int pageSize) {
		int p = Math.max(1, page);
		int ps = Math.min(2000, Math.max(1, pageSize));
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId);
		if (itemIds != null && !itemIds.isEmpty()) {
			List<Long> distinct =
					itemIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
			if (!distinct.isEmpty()) {
				w.in(Items::getItemId, distinct);
			}
		}
		w.orderByDesc(Items::getItemId);
		Page<Items> pg = new Page<>(p, ps, false);
		Page<Items> result = itemsMapper.selectPage(pg, w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Items row : result.getRecords()) {
			list.add(EmployeePurchaseItemsSkuListService.itemEntityToInitialSkuMap(row));
		}
		employeePurchaseItemsSkuListService.applyPostQuerySkuEnrichment(companyId, list, false);
		Map<String, Object> out = new LinkedHashMap<>();
		Long total = itemsMapper.selectCount(w);
		out.put("total_count", total == null ? 0L : total);
		out.put("list", list);
		return out;
	}

	public List<Long> listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(long companyId, List<Long> tagIds) {
		return itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(companyId, tagIds);
	}

	public long countByCompanyIdAndTagIdsIn(long companyId, List<Long> tagIds) {
		return itemsRelTagsRepository.countByCompanyIdAndTagIdsIn(companyId, tagIds);
	}

	public List<Long> listItemIdsForCategoryFilter(
			long companyId, List<Long> activityMainCategoryIds, int page, int pageSize) {
		if (activityMainCategoryIds == null || activityMainCategoryIds.isEmpty()) {
			return List.of();
		}
		List<String> catKeys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, activityMainCategoryIds);
		if (catKeys.isEmpty()) {
			return List.of();
		}
		int p = Math.max(1, page);
		int ps = Math.max(1, pageSize);
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemCategory, catKeys);
		Page<Items> pg = new Page<>(p, ps, false);
		Page<Items> result = itemsMapper.selectPage(pg, w);
		return result.getRecords().stream().map(Items::getItemId).filter(Objects::nonNull).toList();
	}

	public long countItemsForCategoryFilter(long companyId, List<Long> activityMainCategoryIds) {
		if (activityMainCategoryIds == null || activityMainCategoryIds.isEmpty()) {
			return 0L;
		}
		List<String> catKeys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, activityMainCategoryIds);
		if (catKeys.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemCategory, catKeys);
		Long c = itemsMapper.selectCount(w);
		return c == null ? 0L : c;
	}

	public List<Long> listItemIdsForBrandFilter(long companyId, List<Long> brandIds, int page, int pageSize) {
		if (brandIds == null || brandIds.isEmpty()) {
			return List.of();
		}
		List<Integer> brandInts = new ArrayList<>();
		for (Long b : brandIds) {
			if (b == null || b <= 0L || b > Integer.MAX_VALUE) {
				continue;
			}
			brandInts.add(b.intValue());
		}
		if (brandInts.isEmpty()) {
			return List.of();
		}
		int p = Math.max(1, page);
		int ps = Math.max(1, pageSize);
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getBrandId, brandInts);
		Page<Items> pg = new Page<>(p, ps, false);
		Page<Items> result = itemsMapper.selectPage(pg, w);
		return result.getRecords().stream().map(Items::getItemId).filter(Objects::nonNull).toList();
	}

	public long countItemsForBrandFilter(long companyId, List<Long> brandIds) {
		if (brandIds == null || brandIds.isEmpty()) {
			return 0L;
		}
		List<Integer> brandInts = new ArrayList<>();
		for (Long b : brandIds) {
			if (b == null || b <= 0L || b > Integer.MAX_VALUE) {
				continue;
			}
			brandInts.add(b.intValue());
		}
		if (brandInts.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getBrandId, brandInts);
		Long c = itemsMapper.selectCount(w);
		return c == null ? 0L : c;
	}
}
