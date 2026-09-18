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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.orders.repository.SupplierItemDeleteOrderGuardRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsAttrListRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierItemsDeleteService {

	private static final String ERR_EMPTY_ID = "商品id不能为空";
	private static final String ERR_DELETE_INFO = "删除商品信息有误";
	private static final String ERR_SHOP_MISMATCH = "店铺商品信息有误，不可删除";
	private static final String ERR_IS_MARKET = "商品可售状态,不能删除";

	private final SupplierItemsRepository supplierItemsRepository;
	private final SupplierItemsAttrListRepository supplierItemsAttrListRepository;
	private final SupplierItemDeleteOrderGuardRepository supplierItemDeleteOrderGuardRepository;
	private final ItemsRepository itemsRepository;
	private final PlatformItemsDeleteService platformItemsDeleteService;

	public SupplierItemsDeleteService(SupplierItemsRepository supplierItemsRepository,
			SupplierItemsAttrListRepository supplierItemsAttrListRepository,
			SupplierItemDeleteOrderGuardRepository supplierItemDeleteOrderGuardRepository, ItemsRepository itemsRepository,
			PlatformItemsDeleteService platformItemsDeleteService) {
		this.supplierItemsRepository = supplierItemsRepository;
		this.supplierItemsAttrListRepository = supplierItemsAttrListRepository;
		this.supplierItemDeleteOrderGuardRepository = supplierItemDeleteOrderGuardRepository;
		this.itemsRepository = itemsRepository;
		this.platformItemsDeleteService = platformItemsDeleteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteSupplierItems(Map<String, Object> merged, long pathItemId, long distributorIdQuery) {
		long companyId = longProp(merged, "company_id");
		if (pathItemId < 1) {
			throw new ResourceException(ERR_EMPTY_ID);
		}
		SupplierItems row = supplierItemsRepository.getByItemIdAndCompany(pathItemId, companyId);
		if (row == null) {
			throw new ResourceException(ERR_DELETE_INFO);
		}
		if (!distributorMatches(distributorIdQuery, row.getDistributorId())) {
			throw new ResourceException(ERR_SHOP_MISMATCH);
		}
		if (row.getIsMarket() != null && row.getIsMarket() == 1) {
			throw new ResourceException(ERR_IS_MARKET);
		}

		List<Long> supplierSkuIds;
		if (ItemDeleteSpec.isMultiSpecForSupplier(row.getNospec())) {
			List<SupplierItems> skus = supplierItemsRepository.listByDefaultItemIdAndCompany(row.getDefaultItemId(), companyId);
			supplierSkuIds = skus.stream().map(SupplierItems::getItemId).collect(Collectors.toCollection(ArrayList::new));
			if (supplierSkuIds.isEmpty()) {
				supplierSkuIds = new ArrayList<>(List.of(row.getItemId()));
			}
		} else {
			supplierSkuIds = List.of(row.getItemId());
		}

		List<Items> poolRows = itemsRepository.listByCompanyIdAndSupplierItemIds(companyId, supplierSkuIds);
		List<Long> poolItemIds = poolRows.stream().map(Items::getItemId).distinct().collect(Collectors.toList());
		long supplierId = row.getSupplierId() != null ? row.getSupplierId().longValue() : 0L;
		supplierItemDeleteOrderGuardRepository.assertNoBlockingOrdersOrAftersales(companyId, supplierId, poolItemIds);

		for (long skuId : supplierSkuIds) {
			supplierItemsRepository.deleteByItemIdsAndCompany(companyId, List.of(skuId));
			supplierItemsAttrListRepository.deleteByCompanyIdAndItemIds(companyId, List.of(skuId));
		}

		Items pool = itemsRepository.getBySupplierItemIdAndCompany(pathItemId, companyId);
		if (pool != null) {
			platformItemsDeleteService.deletePoolItemsForSupplierRecursion(merged, pool.getItemId(), distributorIdQuery);
		}
	}

	private static long longProp(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		return Long.parseLong(v.toString());
	}

	private static boolean distributorMatches(long queryDistributorId, Integer rowDistributorId) {
		long r = rowDistributorId == null ? 0L : rowDistributorId.longValue();
		return Objects.equals(queryDistributorId, r);
	}
}
