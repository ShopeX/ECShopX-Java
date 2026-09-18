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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.espier.upload.EspierMarketingStyleItemsLookupPort;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EspierMarketingStyleItemsLookupPortImpl implements EspierMarketingStyleItemsLookupPort {

	private final ItemsRepository itemsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final OperatorCartCompanyProductModelReader productModelReader;

	public EspierMarketingStyleItemsLookupPortImpl(
			ItemsRepository itemsRepository,
			DistributorItemsRepository distributorItemsRepository,
			DistributorListQueryService distributorListQueryService,
			OperatorCartCompanyProductModelReader productModelReader) {
		this.itemsRepository = itemsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.productModelReader = productModelReader;
	}

	@Override
	public Map<String, Map<String, Object>> lookupForMarketing(
			long companyId, long distributorId, Collection<String> itemBns) {
		Collection<String> bns = normalizeBns(itemBns);
		if (bns.isEmpty()) {
			return Map.of();
		}
		if (distributorId <= 0L) {
			return toBnMap(itemsRepository.listByCompanyAndItemBns(companyId, bns, 0L));
		}
		String productModel = productModelReader.getProductModel(companyId);
		if ("standard".equals(productModel)) {
			return lookupStandardShop(companyId, distributorId, bns);
		}
		return toBnMap(itemsRepository.listByCompanyAndItemBns(companyId, bns, distributorId));
	}

	@Override
	public Map<String, Map<String, Object>> lookupByItemBn(long companyId, Collection<String> itemBns) {
		Collection<String> bns = normalizeBns(itemBns);
		if (bns.isEmpty()) {
			return Map.of();
		}
		return toBnMap(itemsRepository.listByCompanyAndItemBns(companyId, bns, null));
	}

	private Map<String, Map<String, Object>> lookupStandardShop(
			long companyId, long distributorId, Collection<String> itemBns) {
		List<Distributor> distRows =
				distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
		Distributor d = distRows != null && !distRows.isEmpty() ? distRows.get(0) : null;
		if (d == null || !isValidDistributor(d)) {
			return Map.of();
		}
		List<Items> hq = itemsRepository.listByCompanyAndItemBns(companyId, itemBns, 0L);
		if (hq.isEmpty()) {
			return Map.of();
		}
		boolean needsRelJoin = !isDistributorSelfTrue(d);
		if (!needsRelJoin) {
			return toBnMap(hq);
		}
		List<Long> itemIds = hq.stream()
				.map(Items::getItemId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());
		Set<Long> linked = distributorItemsRepository
				.listByDistributorAndItemIds(companyId, distributorId, itemIds)
				.stream()
				.map(DistributorItems::getItemId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		List<Items> filtered = hq.stream()
				.filter(it -> it.getItemId() != null && linked.contains(it.getItemId()))
				.collect(Collectors.toList());
		return toBnMap(filtered);
	}

	private static boolean isValidDistributor(Distributor d) {
		String v = d.getIsValid();
		return v == null || "true".equalsIgnoreCase(v.trim());
	}

	private static boolean isDistributorSelfTrue(Distributor d) {
		Integer self = d.getDistributorSelf();
		return self != null && self != 0;
	}

	private static Collection<String> normalizeBns(Collection<String> itemBns) {
		if (itemBns == null || itemBns.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> out = new LinkedHashSet<>();
		for (String bn : itemBns) {
			if (bn == null) {
				continue;
			}
			String t = bn.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
	}

	private static Map<String, Map<String, Object>> toBnMap(List<Items> rows) {
		LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
		if (rows == null || rows.isEmpty()) {
			return out;
		}
		for (Items it : rows) {
			if (it == null || !StringUtils.hasText(it.getItemBn()) || it.getItemId() == null) {
				continue;
			}
			String bn = it.getItemBn().trim();
			out.put(bn, toEnrichmentRow(it));
		}
		return out;
	}

	private static Map<String, Object> toEnrichmentRow(Items it) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", it.getItemId());
		m.put("default_item_id", it.getDefaultItemId() != null ? it.getDefaultItemId() : 0L);
		m.put("pics", GoodsItemsListRowMapper.resolvePicsForListRow(it.getPics()));
		m.put("market_price", it.getMarketPrice() != null ? it.getMarketPrice() : 0);
		m.put("item_name", it.getItemName() != null ? it.getItemName() : "");
		m.put("item_type", StringUtils.hasText(it.getItemType()) ? it.getItemType() : "services");
		m.put("store", it.getStore());
		m.put("price", it.getPrice());
		return m;
	}
}
