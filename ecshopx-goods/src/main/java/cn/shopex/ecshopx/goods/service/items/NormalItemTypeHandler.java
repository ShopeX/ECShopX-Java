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
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NormalItemTypeHandler implements ItemTypeHandler {

	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final ItemStoreService itemStoreService;

	public NormalItemTypeHandler(
			ItemsRepository itemsRepository,
			SupplierItemsRepository supplierItemsRepository,
			ItemStoreService itemStoreService) {
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.itemStoreService = itemStoreService;
	}

	@Override
	public void preRelItemParams(Map<String, Object> data, Map<String, Object> skuParams, ItemsCreateContext ctx) {
		Object rebateObj = skuParams.get("rebate");
		if (rebateObj != null) {
			try {
				double r = Double.parseDouble(rebateObj.toString());
				if (r < 0) {
					throw new ResourceException("请输入正确的分销佣金");
				}
			} catch (NumberFormatException e) {
				throw new ResourceException("请输入正确的分销佣金");
			}
		}
		Object storeObj = skuParams.get("store");
		if (storeObj == null) {
			throw new ResourceException("库存为0-999999999的整数");
		}
		int store = (int) toLong(storeObj);
		if (store < 0 || store > 999999999) {
			throw new ResourceException("库存为0-999999999的整数");
		}
		data.put("store", store);
		if (skuParams.containsKey("cost_price")) {
			data.put("cost_price", moneyToFen(skuParams.get("cost_price")));
		}
		if (rebateObj != null) {
			data.put("rebate", moneyToFen(rebateObj));
		}
		ensureItemBn(data, skuParams, ctx);
	}

	private void ensureItemBn(Map<String, Object> data, Map<String, Object> skuParams, ItemsCreateContext ctx) {
		String itemBn = str(data.get("item_bn"));
		if (itemBn.isEmpty()) {
			itemBn = itemStoreService.nextBn("KC");
			data.put("item_bn", itemBn);
		}
		String goodsBn = str(data.get("goods_bn"));
		if (goodsBn.isEmpty()) {
			goodsBn = itemStoreService.nextBn("PC");
			data.put("goods_bn", goodsBn);
		}
		Long existingItemId = skuParams.get("item_id") != null ? toLong(skuParams.get("item_id")) : null;
		if (ctx.isSupplierMode()) {
			SupplierItems found = supplierItemsRepository.findByItemBnAndCompany(itemBn, ctx.getCompanyId());
			if (found != null) {
				if (existingItemId == null) {
					throw new ResourceException("该货号商品已存在: " + itemBn);
				}
				if (found.getItemId() != null && found.getItemId().longValue() != existingItemId.longValue()) {
					throw new ResourceException("该货号商品已存在: " + itemBn);
				}
			}
			Items poolItem = itemsRepository.findByItemBnAndCompany(itemBn, ctx.getCompanyId());
			if (poolItem != null) {
				int supplierItemId = poolItem.getSupplierItemId() != null ? poolItem.getSupplierItemId() : 0;
				if (supplierItemId > 0) {
					if (existingItemId == null) {
						throw new ResourceException("该货号商品已存在: " + itemBn);
					}
					if (supplierItemId != existingItemId.intValue()) {
						throw new ResourceException("该货号商品已存在: " + itemBn);
					}
				}
			}
		} else {
			Items found = itemsRepository.findByItemBnAndCompany(itemBn, ctx.getCompanyId());
			if (found != null) {
				if (existingItemId == null) {
					throw new ResourceException("SKU编码重复: " + itemBn);
				}
				if (found.getItemId() != null && found.getItemId().longValue() != existingItemId.longValue()) {
					throw new ResourceException("SKU编码不能重复: " + itemBn);
				}
			}
		}
	}

	private static int moneyToFen(Object v) {
		if (v == null) {
			return 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		return new BigDecimal(s).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
