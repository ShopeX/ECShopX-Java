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

import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.DistributorItemsDetailMergeService;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Builds Jushuitan item_add / shop_item_add payload fragments from local catalog rows (aligned with legacy ERP upload job).
 */
@Service
public class JushuitanItemStructAssembler {

	private static final int SHOP_CHUNK = 50;

	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final PointsmallItemsMapper pointsmallItemsMapper;

	public JushuitanItemStructAssembler(
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			PointsmallItemsMapper pointsmallItemsMapper) {
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	/**
	 * @return {@code null} when the item cannot be resolved
	 */
	public JushuitanItemStructBundle build(long companyId, long itemId, long distributorId, String shopId, String itemType) {
		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		Map<String, Object> data = loadDetailMap(companyId, itemId, distributorId, itemType, productModel);
		if (data == null || data.isEmpty() || data.get("item_id") == null) {
			return null;
		}
		return buildFromDetail(data, shopId, itemType);
	}

	private Map<String, Object> loadDetailMap(long companyId, long itemId, long distributorId, String itemType, String productModel) {
		if ("pointsmall".equals(itemType)) {
			return loadPointsmallDetail(companyId, itemId);
		}
		if ("standard".equals(productModel) && distributorId > 0) {
			return distributorItemsDetailMergeService.merge(companyId, itemId, distributorId, null, productModel);
		}
		return platformItemsDetailCoreService.build(companyId, itemId, null);
	}

	private Map<String, Object> loadPointsmallDetail(long companyId, long itemId) {
		LambdaQueryWrapper<PointsmallItems> w = Wrappers.lambdaQuery();
		w.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getItemId, itemId);
		PointsmallItems row = pointsmallItemsMapper.selectOne(w);
		if (row == null) {
			return Map.of();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", row.getItemId());
		m.put("goods_id", row.getGoodsId());
		m.put("item_bn", row.getItemBn());
		m.put("item_name", row.getItemName());
		m.put("price", row.getPrice());
		m.put("cost_price", row.getCostPrice());
		m.put("market_price", row.getMarketPrice());
		m.put("nospec", row.getNospec());
		m.put("spec_items", List.of());
		return m;
	}

	@SuppressWarnings("unchecked")
	private JushuitanItemStructBundle buildFromDetail(Map<String, Object> data, String shopId, String itemType) {
		List<Map<String, Object>> itemSkus = new ArrayList<>();
		List<List<Map<String, Object>>> shopChunks = new ArrayList<>();
		if (isNospec(data.get("nospec"))) {
			Map<String, Object> sku = baseSkuMap(data, itemType, false);
			Map<String, Object> shopSku = baseShopSkuMap(data, shopId);
			itemSkus.add(sku);
			shopChunks.add(List.of(shopSku));
		} else {
			Object specObj = data.get("spec_items");
			List<Map<String, Object>> specItems = List.of();
			if (specObj instanceof List<?> l) {
				specItems = (List<Map<String, Object>>) (List<?>) l;
			}
			List<Map<String, Object>> shopAccum = new ArrayList<>();
			for (Map<String, Object> specItem : specItems) {
				Map<String, Object> sku = new LinkedHashMap<>();
				sku.put("sku_id", specItem.get("item_bn"));
				sku.put("i_id", data.get("goods_id"));
				sku.put("name", data.get("item_name"));
				sku.put("sku_code", specItem.get("item_bn"));
				putMoney(sku, "s_price", specItem.get("price"), itemType, specItem.get("market_price"));
				putMoney(sku, "c_price", specItem.get("cost_price"), itemType, null);
				sku.put("enabled", 1);
				itemSkus.add(sku);
				Map<String, Object> shopSku = new LinkedHashMap<>();
				shopSku.put("sku_id", specItem.get("item_bn"));
				shopSku.put("i_id", data.get("goods_id"));
				shopSku.put("sku_code", specItem.get("item_bn"));
				shopSku.put("shop_i_id", data.get("goods_id"));
				shopSku.put("shop_sku_id", specItem.get("item_bn"));
				shopSku.put("name", data.get("item_name"));
				shopSku.put("shop_id", shopId);
				shopAccum.add(shopSku);
				if (shopAccum.size() == SHOP_CHUNK) {
					shopChunks.add(List.copyOf(shopAccum));
					shopAccum.clear();
				}
			}
			if (!shopAccum.isEmpty()) {
				shopChunks.add(List.copyOf(shopAccum));
			}
		}
		return new JushuitanItemStructBundle(itemSkus, shopChunks);
	}

	private static Map<String, Object> baseSkuMap(Map<String, Object> data, String itemType, boolean fromSpec) {
		Map<String, Object> sku = new LinkedHashMap<>();
		sku.put("sku_id", data.get("item_bn"));
		sku.put("i_id", data.get("goods_id"));
		sku.put("name", data.get("item_name"));
		sku.put("sku_code", data.get("item_bn"));
		if (!fromSpec) {
			putMoney(sku, "s_price", data.get("price"), itemType, data.get("market_price"));
			putMoney(sku, "c_price", data.get("cost_price"), itemType, null);
		}
		sku.put("enabled", 1);
		return sku;
	}

	private static Map<String, Object> baseShopSkuMap(Map<String, Object> data, String shopId) {
		Map<String, Object> shopSku = new LinkedHashMap<>();
		shopSku.put("sku_id", data.get("item_bn"));
		shopSku.put("i_id", data.get("goods_id"));
		shopSku.put("sku_code", data.get("item_bn"));
		shopSku.put("shop_i_id", data.get("goods_id"));
		shopSku.put("shop_sku_id", data.get("item_bn"));
		shopSku.put("name", data.get("item_name"));
		shopSku.put("shop_id", shopId);
		return shopSku;
	}

	private static void putMoney(Map<String, Object> sku, String key, Object priceCents, String itemType, Object marketPriceCents) {
		Object cents = priceCents;
		if ("pointsmall".equals(itemType) && "s_price".equals(key) && marketPriceCents != null) {
			cents = marketPriceCents;
		}
		sku.put(key, centsToYuanDouble(cents));
	}

	private static double centsToYuanDouble(Object cents) {
		if (cents == null) {
			return 0.0;
		}
		long v = cents instanceof Number n ? n.longValue() : Long.parseLong(cents.toString().trim());
		return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
	}

	private static boolean isNospec(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = raw.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	public record JushuitanItemStructBundle(List<Map<String, Object>> itemSkus, List<List<Map<String, Object>>> shopItemChunks) {
	}
}
