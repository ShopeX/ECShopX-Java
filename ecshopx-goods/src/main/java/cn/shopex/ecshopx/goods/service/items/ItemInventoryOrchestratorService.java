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

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class ItemInventoryOrchestratorService {

	private final ItemStoreService itemStoreService;
	private final SupplierItemStoreService supplierItemStoreService;
	private final InventoryDeductTargetResolver inventoryDeductTargetResolver;
	private final ItemsMapper itemsMapper;

	public ItemInventoryOrchestratorService(
			ItemStoreService itemStoreService,
			SupplierItemStoreService supplierItemStoreService,
			InventoryDeductTargetResolver inventoryDeductTargetResolver,
			ItemsMapper itemsMapper) {
		this.itemStoreService = itemStoreService;
		this.supplierItemStoreService = supplierItemStoreService;
		this.inventoryDeductTargetResolver = inventoryDeductTargetResolver;
		this.itemsMapper = itemsMapper;
	}

	/**
	 * 扣减/返还库存；{@code num} 为正扣减，为负返还。扣减不足时返回 false（Redis 已回滚）。
	 */
	public boolean minusItemStore(ItemInventoryLineContext ctx, int num) {
		if (ctx == null || ctx.itemId() <= 0L || num == 0) {
			return false;
		}
		Items item = loadItem(ctx.companyId(), ctx.itemId());
		ItemInventoryLineContext resolved = ensureSupplierItemId(ctx, item);
		return switch (inventoryDeductTargetResolver.resolve(resolved)) {
			case SUPPLIER_ITEMS -> minusSupplier(resolved, num);
			case SHOP_DISTRIBUTOR_ITEMS -> itemStoreService.minusItemStore(
					resolved.itemId(), num, resolved.distributorId(), true, resolved.companyId());
			case PLATFORM_ITEMS -> itemStoreService.minusItemStore(
					resolved.itemId(), num, 0L, false, resolved.companyId());
		};
	}

	/** 返还库存；{@code restoreQty} 为正，内部走与扣减相同的目标池判定。 */
	public void restoreItemStore(ItemInventoryLineContext ctx, int restoreQty) {
		if (ctx == null || ctx.itemId() <= 0L || restoreQty <= 0) {
			return;
		}
		minusItemStore(ctx, -restoreQty);
	}

	private boolean minusSupplier(ItemInventoryLineContext ctx, int num) {
		long supplierItemId = ctx.supplierItemId();
		if (supplierItemId <= 0L) {
			return false;
		}
		return supplierItemStoreService.minusSupplierItemStore(supplierItemId, num, ctx.companyId());
	}

	private ItemInventoryLineContext ensureSupplierItemId(ItemInventoryLineContext ctx, Items item) {
		if (ctx.supplierItemId() > 0L) {
			return ctx;
		}
		if (ctx.supplierId() <= 0L || !InventoryDeductTargetResolver.isLogisticsReceipt(ctx.receiptType())) {
			return ctx;
		}
		if (item == null || item.getSupplierItemId() == null || item.getSupplierItemId() <= 0) {
			return ctx;
		}
		return ItemInventoryLineContext.of(
				ctx.companyId(),
				ctx.itemId(),
				ctx.supplierId(),
				ctx.distributorId(),
				ctx.isTotalStore(),
				ctx.receiptType(),
				item.getSupplierItemId().longValue());
	}

	private Items loadItem(long companyId, long itemId) {
		if (companyId <= 0L || itemId <= 0L) {
			return null;
		}
		return itemsMapper.selectOne(new LambdaQueryWrapper<Items>()
				.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemId, itemId)
				.last("LIMIT 1"));
	}
}
