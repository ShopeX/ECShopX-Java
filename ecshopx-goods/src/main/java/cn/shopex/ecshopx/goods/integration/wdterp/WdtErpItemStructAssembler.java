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

package cn.shopex.ecshopx.goods.integration.wdterp;

import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.DistributorItemsDetailMergeService;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WdtErpItemStructAssembler {

	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;

	public WdtErpItemStructAssembler(
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService) {
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
	}

	public WdtErpItemStruct getItemStruct(long companyId, long itemId, long distributorId) {
		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		Map<String, Object> data = loadDetailMap(companyId, itemId, distributorId, productModel);
		if (data == null || data.isEmpty() || data.get("item_id") == null) {
			return null;
		}
		return buildFromDetail(data);
	}

	private Map<String, Object> loadDetailMap(long companyId, long itemId, long distributorId, String productModel) {
		if ("standard".equals(productModel) && distributorId > 0) {
			return distributorItemsDetailMergeService.merge(companyId, itemId, distributorId, null, productModel);
		}
		return platformItemsDetailCoreService.build(companyId, itemId, null);
	}

	private WdtErpItemStruct buildFromDetail(Map<String, Object> data) {
		Map<String, Object> goodsPush = getGoodsPushStruct(data);
		List<Map<String, Object>> apiGoodsUpload = getApiGoodsUploadStruct(data);
		return new WdtErpItemStruct(goodsPush, apiGoodsUpload);
	}

	private Map<String, Object> getGoodsPushStruct(Map<String, Object> data) {
		Map<String, Object> goods = new LinkedHashMap<>();
		goods.put("goods_no", str(data.get("item_bn")));
		goods.put("goods_name", str(data.get("item_name")));
		List<Map<String, Object>> specList = new ArrayList<>();
		if (isNospec(data.get("nospec"))) {
			Map<String, Object> spec = new LinkedHashMap<>();
			spec.put("spec_no", str(data.get("item_bn")));
			spec.put("spec_name", str(data.get("item_name")));
			spec.put("retail_price", centsToYuanDouble(data.get("price")));
			specList.add(spec);
		} else {
			Object specObj = data.get("spec_items");
			if (specObj instanceof List<?> l) {
				for (Object row : l) {
					if (!(row instanceof Map<?, ?> sm)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> specItem = (Map<String, Object>) (Map<?, ?>) sm;
					Map<String, Object> spec = new LinkedHashMap<>();
					spec.put("spec_no", str(specItem.get("item_bn")));
					spec.put("spec_name", buildSpecNameFromItemSpec(specItem.get("item_spec")));
					spec.put("retail_price", centsToYuanDouble(specItem.get("price")));
					specList.add(spec);
				}
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("goods", goods);
		out.put("specList", specList);
		return out;
	}

	private List<Map<String, Object>> getApiGoodsUploadStruct(Map<String, Object> data) {
		List<Map<String, Object>> itemStruct = new ArrayList<>();
		if (isNospec(data.get("nospec"))) {
			Map<String, Object> good = new LinkedHashMap<>();
			good.put("goods_id", data.get("goods_id"));
			good.put("spec_id", str(data.get("item_bn")));
			good.put("goods_no", str(data.get("item_bn")));
			good.put("spec_no", str(data.get("item_bn")));
			good.put("goods_name", str(data.get("item_name")));
			good.put("spec_name", str(data.get("item_name")));
			good.put("status", getStatus(str(data.get("approve_status"))));
			good.put("price", centsToYuanDouble(data.get("price")));
			good.put("stock_num", data.get("store"));
			itemStruct.add(good);
		} else {
			Object specObj = data.get("spec_items");
			if (specObj instanceof List<?> l) {
				for (Object row : l) {
					if (!(row instanceof Map<?, ?> sm)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> specItem = (Map<String, Object>) (Map<?, ?>) sm;
					Map<String, Object> good = new LinkedHashMap<>();
					good.put("goods_id", data.get("goods_id"));
					good.put("spec_id", specItem.get("item_id"));
					good.put("goods_no", str(specItem.get("item_bn")));
					good.put("spec_no", str(specItem.get("item_bn")));
					good.put("goods_name", str(data.get("item_name")));
					good.put("spec_name", buildSpecNameFromItemSpec(specItem.get("item_spec")));
					good.put("status", getStatus(str(specItem.get("approve_status"))));
					good.put("price", centsToYuanDouble(specItem.get("price")));
					good.put("stock_num", specItem.get("store"));
					itemStruct.add(good);
				}
			}
		}
		return itemStruct;
	}

	private static String buildSpecNameFromItemSpec(Object itemSpecRaw) {
		if (!(itemSpecRaw instanceof List<?> parts)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Object p : parts) {
			if (!(p instanceof Map<?, ?> m)) {
				continue;
			}
			Object sn = m.get("spec_name");
			Object svn = m.get("spec_value_name");
			sb.append(sn != null ? sn.toString() : "").append(':').append(svn != null ? svn.toString() : "").append(',');
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	private static int getStatus(String approveStatus) {
		if (!StringUtils.hasText(approveStatus)) {
			return 0;
		}
		return switch (approveStatus) {
			case "onsale" -> 1;
			case "offline_sale", "instock", "only_show" -> 2;
			default -> 0;
		};
	}

	private static boolean isNospec(Object raw) {
		if (raw == null) {
			return false;
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

	private static double centsToYuanDouble(Object cents) {
		if (cents == null) {
			return 0.0;
		}
		long v = cents instanceof Number n ? n.longValue() : Long.parseLong(cents.toString().trim());
		return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
	}

	private static String str(Object o) {
		return o != null ? o.toString() : "";
	}

	public record WdtErpItemStruct(Map<String, Object> goodsPush, List<Map<String, Object>> apiGoodsUpload) {
	}
}
