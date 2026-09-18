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

package cn.shopex.ecshopx.goods.service.jushuitan;

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsRelListCoreService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsJushuitanPageQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class JushuitanUploadItemsPageQueryService {

	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorItemsRelListCoreService distributorItemsRelListCoreService;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final PointsmallItemsJushuitanPageQueryService pointsmallItemsJushuitanPageQueryService;

	public JushuitanUploadItemsPageQueryService(
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorItemsRelListCoreService distributorItemsRelListCoreService,
			ItemsListQueryRepository itemsListQueryRepository,
			PointsmallItemsJushuitanPageQueryService pointsmallItemsJushuitanPageQueryService) {
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorItemsRelListCoreService = distributorItemsRelListCoreService;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.pointsmallItemsJushuitanPageQueryService = pointsmallItemsJushuitanPageQueryService;
	}

	public Map<String, Object> fetchPage(
			long companyId,
			String operatorType,
			long distributorId,
			String productModel,
			String itemTypeParam,
			Map<String, Object> filterBase,
			int page,
			int pageSize,
			Map<String, Object> filterResponseOut) {
		boolean distributorBranch = ("platform".equals(productModel) || "standard".equals(productModel))
				&& operatorType != null && "distributor".equalsIgnoreCase(operatorType.trim());
		if (distributorBranch) {
			return distributorItemsRelListCoreService.query(
					companyId, distributorId, filterBase, pageSize, page, filterResponseOut, "zh-CN");
		}
		if ("pointsmall".equals(itemTypeParam)) {
			return pointsmallItemsJushuitanPageQueryService.fetchPage(companyId, filterBase, page, pageSize);
		}
		Map<String, Object> p = new LinkedHashMap<>(filterBase);
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		long total = itemsListQueryRepository.countByParams(p);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total <= 0) {
			out.put("list", List.of());
			return out;
		}
		int offset = Math.max(0, (page - 1) * pageSize);
		var items = itemsListQueryRepository.selectPageByParamsItemIdDesc(p, offset, pageSize);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		out.put("list", rows);
		return out;
	}

	public String resolveProductModel(long companyId) {
		return itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
	}
}
