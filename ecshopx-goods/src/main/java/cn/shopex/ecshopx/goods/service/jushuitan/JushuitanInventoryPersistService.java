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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.orders.mapper.JushuitanOrderFrozenQuantityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanInventoryPersistService {

	private final ItemsMapper itemsMapper;
	private final JushuitanOrderFrozenQuantityMapper jushuitanOrderFrozenQuantityMapper;
	private final ItemStoreService itemStoreService;

	public JushuitanInventoryPersistService(
			ItemsMapper itemsMapper,
			JushuitanOrderFrozenQuantityMapper jushuitanOrderFrozenQuantityMapper,
			ItemStoreService itemStoreService) {
		this.itemsMapper = itemsMapper;
		this.jushuitanOrderFrozenQuantityMapper = jushuitanOrderFrozenQuantityMapper;
		this.itemStoreService = itemStoreService;
	}

	/**
	 * @param inventorys OpenAPI 返回的 inventorys 列表元素为 Map，含 sku_id、qty、virtual_qty 等字段
	 */
	public void persistInventoriesFromJushuitan(long companyId, List<Map<String, Object>> inventorys) {
		if (inventorys == null || inventorys.isEmpty()) {
			return;
		}
		Set<String> skuKeys = new LinkedHashSet<>();
		for (Map<String, Object> row : inventorys) {
			Object s = row.get("sku_id");
			if (s != null && StringUtils.hasText(s.toString())) {
				skuKeys.add(s.toString().trim());
			}
		}
		if (skuKeys.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemBn, skuKeys);
		List<Items> localRows = itemsMapper.selectList(w);
		Map<String, Long> bnToItemId = new LinkedHashMap<>();
		for (Items it : localRows) {
			String bn = it.getItemBn();
			Long iid = it.getItemId();
			if (StringUtils.hasText(bn) && iid != null && iid > 0) {
				bnToItemId.put(bn.trim(), iid);
			}
		}
		long nowEpochSeconds = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> val : inventorys) {
			Object skuObj = val.get("sku_id");
			if (skuObj == null || !StringUtils.hasText(skuObj.toString())) {
				continue;
			}
			String skuId = skuObj.toString().trim();
			Long itemId = bnToItemId.get(skuId);
			if (itemId == null || itemId <= 0) {
				continue;
			}
			Long freezL = jushuitanOrderFrozenQuantityMapper.sumFrozenNumForNotPayOrdersAfterCancelDeadline(
					companyId, itemId, nowEpochSeconds);
			double freez = freezL == null ? 0.0 : freezL.doubleValue();
			double qty = num(val.get("qty"));
			double virtualQty = num(val.get("virtual_qty"));
			double purchaseQty = num(val.get("purchase_qty"));
			double returnQty = num(val.get("return_qty"));
			double inQty = num(val.get("in_qty"));
			double orderLock = num(val.get("order_lock"));
			double storeD = qty + virtualQty + purchaseQty + returnQty + inQty - orderLock - freez;
			int storeInt = (int) Math.max(0L, Math.round(storeD));
			itemsMapper.update(null, new LambdaUpdateWrapper<Items>()
					.eq(Items::getCompanyId, companyId)
					.eq(Items::getItemId, itemId)
					.set(Items::getStore, storeInt));
			itemStoreService.saveItemStore(itemId, storeInt, 0L);
		}
	}

	private static double num(Object o) {
		if (o == null) {
			return 0.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
}
