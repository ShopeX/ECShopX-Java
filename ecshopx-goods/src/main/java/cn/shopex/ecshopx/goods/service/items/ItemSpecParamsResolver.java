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
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ItemSpecParamsResolver {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;

	public ItemSpecParamsResolver(
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
	}

	public void resolve(Map<String, Object> data, Map<String, Object> skuParams, ItemsCreateContext ctx) {
		String approve = str(skuParams.get("approve_status"));
		if (approve.isEmpty()
				|| !Set.of("onsale", "offline_sale", "instock", "only_show").contains(approve)) {
			throw new ResourceException("请选择正确的商品状态");
		}
		data.put("item_bn", str(skuParams.get("item_bn")));
		data.put("weight", dbl(skuParams.get("weight")));
		if (skuParams.get("volume") != null) {
			data.put("volume", dbl(skuParams.get("volume")));
		}
		data.put("barcode", str(skuParams.get("barcode")));
		data.put("price", moneyToFen(skuParams.get("price")));
		boolean gift = ctx.isGift();
		if (toInt(data.get("price")) <= 0 && !gift) {
			throw new ResourceException("非赠品商品销售价必须大于0");
		}
		data.put("cost_price", skuParams.containsKey("cost_price") ? moneyToFen(skuParams.get("cost_price")) : 0);
		data.put("market_price", skuParams.get("market_price") != null && !str(skuParams.get("market_price")).isEmpty()
				? moneyToFen(skuParams.get("market_price"))
				: 0);
		data.put("profit_fee", skuParams.containsKey("profit_fee") ? moneyToFen(skuParams.get("profit_fee")) : 0);
		data.put("item_unit", data.get("item_unit") != null ? data.get("item_unit").toString() : "个");
		data.put("approve_status", approve);
		data.put("is_default", skuParams.get("is_default") != null ? Boolean.TRUE.equals(skuParams.get("is_default")) : true);
		data.put("point", skuParams.get("point") != null ? (int) toLong(skuParams.get("point")) : 0);
		if (skuParams.get("start_num") != null) {
			data.put("start_num", (int) toLong(skuParams.get("start_num")));
		}
		if (skuParams.get("delivery_time") != null) {
			String deliveryTimeRaw = str(skuParams.get("delivery_time"));
			if (!deliveryTimeRaw.isEmpty()) {
				data.put("delivery_time", parseDeliveryTimeDays(deliveryTimeRaw));
			}
		}

		if (!ctx.isSupplierMode()) {
			List<Long> saleCats = parseLongIds(skuParams.get("item_category"));
			if (!saleCats.isEmpty()) {
				long n = itemsCategoryRepository.countByCompanyAndCategoryIdsAndMainFlag(ctx.getCompanyId(), saleCats, false);
				if (n != saleCats.size()) {
					throw new ResourceException("选中的分类不存在 或 错误");
				}
			}
			Object mainCat = skuParams.get("item_main_cat_id");
			if (mainCat != null && toLong(mainCat) > 0) {
				long mid = toLong(mainCat);
				long n = itemsCategoryRepository.countByCompanyAndCategoryIdsAndMainFlag(ctx.getCompanyId(), List.of(mid), true);
				if (n != 1) {
					throw new ResourceException("您选中的主类目不存在");
				}
			}
		}
		Object brandId = skuParams.get("brand_id");
		if (brandId != null && toLong(brandId) > 0) {
			long bid = toLong(brandId);
			var brandEnt = itemsAttributesRepository.selectByCompanyAndAttributeId(ctx.getCompanyId(), bid);
			if (brandEnt == null || !"brand".equals(brandEnt.getAttributeType())) {
				throw new ResourceException("您选中的品牌不存在");
			}
		}
		validateParamRows(ctx.getCompanyId(), skuParams.get("item_params"), false);
		validateParamRows(ctx.getCompanyId(), skuParams.get("item_spec"), true);

		Object tpl = skuParams.get("templates_id");
		if (tpl != null && toLong(tpl) > 0) {
			long tid = toLong(tpl);
			if (!shippingTemplatesQueryRepository.existsByTemplateIdAndCompanyId(tid, ctx.getCompanyId())) {
				throw new ResourceException("您选中的运费模板不存在");
			}
			data.put("templates_id", (int) tid);
		}
	}

	private void validateParamRows(long companyId, Object raw, boolean spec) {
		if (raw == null) {
			return;
		}
		List<Map<String, Object>> rows = asMapList(raw);
		if (rows.isEmpty()) {
			return;
		}
		Set<Long> attrIds = new HashSet<>();
		Set<Long> valIds = new HashSet<>();
		for (Map<String, Object> v : rows) {
			long attrId = toLong(v.get(spec ? "spec_id" : "attribute_id"));
			if (attrId > 0) {
				attrIds.add(attrId);
			}
			long valId = toLong(v.get(spec ? "spec_value_id" : "attribute_value_id"));
			if (valId > 0) {
				valIds.add(valId);
			}
		}
		String type = spec ? "item_spec" : "item_params";
		if (!attrIds.isEmpty()) {
			long n = itemsAttributesRepository.countByCompanyTypeAndAttributeIds(companyId, type, attrIds);
			if (n != attrIds.size()) {
				throw new ResourceException(spec ? "您选中的规格不存在" : "您选中的参数不存在");
			}
		}
		if (!valIds.isEmpty()) {
			long n = itemsAttributeValuesRepository.countByCompanyAndValueIdsIn(companyId, valIds);
			if (n != valIds.size()) {
				throw new ResourceException(spec ? "您选中的规格值不存在" : "您选中的参数值不存在");
			}
		}
	}

	private static List<Map<String, Object>> asMapList(Object raw) {
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(row);
				}
			}
			return out;
		}
		return List.of();
	}

	private static List<Long> parseLongIds(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					out.add(toLong(o));
				}
			}
			return out;
		}
		if (v.toString().trim().isEmpty()) {
			return List.of();
		}
		return List.of(toLong(v));
	}

	private static int parseDeliveryTimeDays(String raw) {
		try {
			long days = Long.parseLong(raw);
			if (days < 0 || days > Integer.MAX_VALUE) {
				throw new ResourceException("发货时间格式错误，请填写非负整数天数（如 2）");
			}
			return (int) days;
		} catch (NumberFormatException e) {
			throw new ResourceException("发货时间格式错误，请填写非负整数天数（如 2）");
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
		try {
			return new BigDecimal(s).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("价格格式错误：" + s);
		}
	}

	private static int toInt(Object o) {
		return (int) toLong(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("数值格式错误：" + s);
		}
	}

	private static double dbl(Object o) {
		if (o == null) {
			return 0.0;
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0.0;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("数值格式错误：" + s);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
