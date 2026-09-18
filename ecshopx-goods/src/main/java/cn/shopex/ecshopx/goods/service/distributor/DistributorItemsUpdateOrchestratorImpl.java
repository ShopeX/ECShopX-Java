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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformProductSyncPort;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorItemsUpdateOrchestrator;
import cn.shopex.ecshopx.distribution.service.distributor.dto.DistributorItemsUpdateColumnPatch;
import cn.shopex.ecshopx.distribution.service.distributor.dto.DistributorItemsUpdateRowFilter;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributorItemsUpdateOrchestratorImpl implements DistributorItemsUpdateOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(DistributorItemsUpdateOrchestratorImpl.class);

	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemStoreService itemStoreService;
	private final ShuyunOpenPlatformProductSyncPort openPlatformProductSyncPort;

	public DistributorItemsUpdateOrchestratorImpl(
			ItemsRepository itemsRepository,
			SupplierItemsRepository supplierItemsRepository,
			DistributorItemsRepository distributorItemsRepository,
			ItemStoreService itemStoreService,
			ShuyunOpenPlatformProductSyncPort openPlatformProductSyncPort) {
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.itemStoreService = itemStoreService;
		this.openPlatformProductSyncPort = openPlatformProductSyncPort;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> merged) {
		try {
			long companyId = resolveCompanyId(merged, operatorJwt);
			DistributorItemsUpdateParamParser.Parsed parsed = DistributorItemsUpdateParamParser.parse(merged);
			DistributorItemsUpdateRowFilter rowFilter = buildRowFilter(companyId, parsed.distributorIds(), parsed);
			if (parsed.distributorIds().isEmpty()) {
				throw new BadRequestException("请选择店铺");
			}

			assertSupplierRulesIfNeeded(companyId, rowFilter, parsed);

			DistributorItemsUpdateColumnPatch patch = DistributorItemsUpdateParamParser.toColumnPatch(parsed);
			List<DistributorItems> itemList = distributorItemsRepository.listForUpdateByFilter(rowFilter);

			boolean inventoryOnly =
					patch.store().isPresent()
							&& patch.isCanSale().isEmpty()
							&& patch.isTotalStore().isEmpty()
							&& patch.price().isEmpty();
			// 对齐 PHP：仅改库存时由 ItemStore 事件驱动商品同步，避免双派发
			if (patch.anyPresent()) {
				log.info("updateDistributorItem filter={} patch={}", rowFilter, patch);
				distributorItemsRepository.updateByFilterWithGoodsCanSaleSync(rowFilter, patch);
				if (!inventoryOnly) {
					dispatchShuyunProductSync(companyId, parsed.distributorIds(), itemList);
				}
			}
			if (patch.store().isPresent()) {
				long storeLong = patch.store().get();
				int storeInt = storeLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : storeLong < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) storeLong;
				for (DistributorItems row : itemList) {
					itemStoreService.saveItemStore(row.getItemId(), storeInt, row.getDistributorId());
				}
			}
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			log.warn("updateDistributorItem failed: {}", e.toString(), e);
			throw new ResourceException("更新店铺商品失败");
		}
	}

	private void dispatchShuyunProductSync(
			long companyId, List<Long> distributorIds, List<DistributorItems> itemList) {
		if (itemList == null || itemList.isEmpty() || distributorIds == null || distributorIds.isEmpty()) {
			return;
		}
		Set<String> seen = new LinkedHashSet<>();
		for (Long distributorId : distributorIds) {
			if (distributorId == null || distributorId < 1) {
				continue;
			}
			for (DistributorItems row : itemList) {
				if (row == null || row.getItemId() == null) {
					continue;
				}
				long itemId = row.getItemId();
				long def = row.getDefaultItemId() == null ? 0L : row.getDefaultItemId();
				if (def < 1) {
					Items it = itemsRepository.getByItemIdAndCompany(itemId, companyId);
					if (it != null && it.getDefaultItemId() != null && it.getDefaultItemId() > 0) {
						def = it.getDefaultItemId();
					} else {
						def = itemId;
					}
				}
				String key = distributorId + ":" + def;
				if (!seen.add(key)) {
					continue;
				}
				openPlatformProductSyncPort.dispatchIfAuthAllows(companyId, distributorId, def);
			}
		}
	}

	private long resolveCompanyId(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		Object fromMerged = merged != null ? merged.get("company_id") : null;
		if (fromMerged != null) {
			return toLong(fromMerged);
		}
		return toLong(operatorJwt.get("company_id"));
	}

	private DistributorItemsUpdateRowFilter buildRowFilter(long companyId, List<Long> distributorIds, DistributorItemsUpdateParamParser.Parsed parsed) {
		Optional<Long> singleG = parsed.singleGoodsId();
		Optional<List<Long>> multiG = parsed.multiGoodsIds().filter(l -> !l.isEmpty());
		boolean hasGoods = singleG.isPresent() || multiG.isPresent();

		if (hasGoods) {
			if (singleG.isPresent()) {
				return new DistributorItemsUpdateRowFilter(companyId, distributorIds, singleG.get(), null, null);
			}
			return new DistributorItemsUpdateRowFilter(companyId, distributorIds, null, multiG.get(), null);
		}
		Optional<Long> itemOpt = parsed.itemIdFromRequest();
		if (itemOpt.isEmpty()) {
			throw new BadRequestException("请先选择商品");
		}
		long itemId = itemOpt.get();
		if (parsed.isDefaultKeyPresent()) {
			Items it = itemsRepository.getByItemIdAndCompany(itemId, companyId);
			if (it == null || it.getGoodsId() == null || it.getGoodsId() <= 0) {
				throw new ResourceException("缺少必填字段: goods_id");
			}
			return new DistributorItemsUpdateRowFilter(companyId, distributorIds, it.getGoodsId(), null, null);
		}
		return new DistributorItemsUpdateRowFilter(companyId, distributorIds, null, null, itemId);
	}

	private void assertSupplierRulesIfNeeded(long companyId, DistributorItemsUpdateRowFilter filter, DistributorItemsUpdateParamParser.Parsed parsed) {
		List<Items> skuRows = listSkuRowsForSupplierRules(companyId, filter);
		List<Long> supplierSkuIds = collectSupplierSkuItemIds(skuRows);

		boolean wantSale = parsed.isCanSaleKeyPresent() && parsed.isCanSaleNormalized();
		if (wantSale) {
			long cnt = supplierItemsRepository.countSupplierItemsForDistributorUpdate(companyId, true, orNullIfEmpty(supplierSkuIds));
			if (cnt > 0) {
				throw new ResourceException("供应商商品不可售");
			}
			assertHeadquartersSaleAllowed(companyId, filter);
		}
	}

	private List<Items> listSkuRowsForSupplierRules(long companyId, DistributorItemsUpdateRowFilter filter) {
		if (filter.itemId() != null) {
			return itemsRepository.listItemIdSupplierItemIdByCompanyAndItemId(companyId, filter.itemId());
		}
		if (filter.goodsId() != null) {
			return itemsRepository.listItemIdSupplierItemIdByCompanyAndGoodsIds(companyId, List.of(filter.goodsId()));
		}
		if (filter.goodsIds() != null && !filter.goodsIds().isEmpty()) {
			return itemsRepository.listItemIdSupplierItemIdByCompanyAndGoodsIds(companyId, filter.goodsIds());
		}
		return List.of();
	}

	private static Collection<Long> orNullIfEmpty(List<Long> supplierSkuIds) {
		return supplierSkuIds == null || supplierSkuIds.isEmpty() ? null : supplierSkuIds;
	}

	private static List<Long> collectSupplierSkuItemIds(List<Items> rows) {
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Items it : rows) {
			Integer sid = it.getSupplierItemId();
			if (sid != null && sid > 0) {
				out.add(sid.longValue());
			}
		}
		return out;
	}

	private void assertHeadquartersSaleAllowed(long companyId, DistributorItemsUpdateRowFilter filter) {
		if (filter.goodsId() != null) {
			assertGoodsSpuNotAllInstock(companyId, List.of(filter.goodsId()));
		} else if (filter.goodsIds() != null && !filter.goodsIds().isEmpty()) {
			assertGoodsSpuNotAllInstock(companyId, filter.goodsIds());
		} else if (filter.itemId() != null) {
			if (itemsRepository.countInstockByCompanyAndItemId(companyId, filter.itemId()) > 0) {
				throw new ResourceException("确认商品总部销售状态");
			}
		}
	}

	private void assertGoodsSpuNotAllInstock(long companyId, List<Long> goodsIds) {
		List<Items> rows = itemsRepository.listApproveStatusByCompanyAndGoodsIds(companyId, goodsIds);
		Map<Long, List<Items>> byGoods = new LinkedHashMap<>();
		for (Items it : rows) {
			Long gid = it.getGoodsId();
			if (gid == null) {
				continue;
			}
			byGoods.computeIfAbsent(gid, k -> new ArrayList<>()).add(it);
		}
		for (List<Items> skus : byGoods.values()) {
			if (skus.isEmpty()) {
				continue;
			}
			boolean allInstock = true;
			for (Items it : skus) {
				if (!"instock".equals(it.getApproveStatus())) {
					allInstock = false;
					break;
				}
			}
			if (allInstock) {
				throw new ResourceException("确认商品总部销售状态");
			}
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
