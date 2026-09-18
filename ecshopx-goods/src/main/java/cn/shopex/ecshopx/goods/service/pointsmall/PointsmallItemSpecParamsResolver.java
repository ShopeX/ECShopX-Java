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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemSpecParamsResolver {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final PointsmallNormalItemPreRelService pointsmallNormalItemPreRelService;
	private final ObjectMapper objectMapper;

	public PointsmallItemSpecParamsResolver(
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			PointsmallNormalItemPreRelService pointsmallNormalItemPreRelService,
			ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.pointsmallNormalItemPreRelService = pointsmallNormalItemPreRelService;
		this.objectMapper = objectMapper;
	}

	public void resolve(Map<String, Object> data, Map<String, Object> skuParams, long companyId, String itemType, boolean forceCreate) {
		String approve = str(skuParams.get("approve_status"));
		if (!Set.of("onsale", "offline_sale", "instock", "only_show").contains(approve)) {
			throw new ResourceException("请选择正确的商品状态");
		}
		data.put("item_bn", str(skuParams.get("item_bn")));
		data.put("weight", dbl(skuParams.get("weight")));
		if (skuParams.get("volume") != null && StringUtils.hasText(str(skuParams.get("volume")))) {
			data.put("volume", dbl(skuParams.get("volume")));
		}
		data.put("barcode", str(skuParams.get("barcode")));

		int priceFen = moneyToFen(skuParams.get("price"));
		int point = skuParams.get("point") != null ? (int) toLong(skuParams.get("point")) : 0;
		String payClass = str(skuParams.get("pay_class"));
		if (!StringUtils.hasText(payClass) || !Set.of("point", "online", "mix").contains(payClass)) {
			throw new ResourceException("请选择正确的支付方式");
		}
		if ("point".equals(payClass)) {
			priceFen = 0;
		} else if ("online".equals(payClass)) {
			point = 0;
		}
		if (List.of("online", "mix").contains(payClass) && priceFen <= 0) {
			throw new ResourceException("请填写正确的销售价格");
		}
		if (List.of("point", "mix").contains(payClass) && point <= 0) {
			throw new ResourceException("请填写正确的积分价格");
		}
		data.put("point", point);
		data.put("pay_class", payClass);
		data.put("price", priceFen);

		data.put("cost_price", skuParams.containsKey("cost_price") ? moneyToFen(skuParams.get("cost_price")) : 0);
		data.put("market_price", skuParams.get("market_price") != null && StringUtils.hasText(str(skuParams.get("market_price")))
				? moneyToFen(skuParams.get("market_price"))
				: 0);
		data.put("item_unit", data.get("item_unit") != null ? data.get("item_unit").toString() : "个");
		data.put("store", skuParams.get("store") != null ? (int) toLong(skuParams.get("store")) : 0);
		data.put("approve_status", approve);
		data.put("is_default", skuParams.get("is_default") != null ? skuParams.get("is_default") : true);

		if ("normal".equals(itemType)) {
			pointsmallNormalItemPreRelService.apply(data, skuParams, companyId, forceCreate);
		}

		List<Long> saleCats = parseItemCategoryIds(skuParams.get("item_category"));
		if (!saleCats.isEmpty()) {
			long n = itemsCategoryRepository.countByCompanyAndCategoryIdsAndMainFlag(companyId, saleCats, false);
			if (n != saleCats.size()) {
				throw new ResourceException("选中的分类不存在 或 错误");
			}
		}
		Object mainCat = skuParams.get("item_main_cat_id");
		if (mainCat != null && toLong(mainCat) > 0) {
			long mid = toLong(mainCat);
			long n = itemsCategoryRepository.countByCompanyAndCategoryIdsAndMainFlag(companyId, List.of(mid), true);
			if (n != 1) {
				throw new ResourceException("您选中的主类目不存在");
			}
		}
		Object brandId = skuParams.get("brand_id");
		if (brandId != null && toLong(brandId) > 0) {
			long bid = toLong(brandId);
			var brandEnt = itemsAttributesRepository.selectByCompanyAndAttributeId(companyId, bid);
			if (brandEnt == null || !"brand".equals(brandEnt.getAttributeType())) {
				throw new ResourceException("您选中的品牌不存在");
			}
		}
		validateParamRows(companyId, maybeParseJsonList(skuParams.get("item_params"), false), false);
		validateParamRows(companyId, maybeParseJsonList(skuParams.get("item_spec"), true), true);

		Object tpl = skuParams.get("templates_id");
		if (tpl != null && toLong(tpl) > 0) {
			long tid = toLong(tpl);
			if (!shippingTemplatesQueryRepository.existsByTemplateIdAndCompanyId(tid, companyId)) {
				throw new ResourceException("您选中的运费模板不存在");
			}
			data.put("templates_id", (int) tid);
		}
	}

	private List<Map<String, Object>> maybeParseJsonList(Object raw, boolean specField) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				List<Map<String, Object>> parsed = objectMapper.readValue(s, new TypeReference<>() {
				});
				return parsed != null ? parsed : List.of();
			} catch (JsonProcessingException e) {
				throw new ResourceException(specField ? "您选中的规格不存在" : "您选中的参数不存在");
			}
		}
		return asMapList(raw);
	}

	private void validateParamRows(long companyId, List<Map<String, Object>> rows, boolean spec) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> attrIds = new HashSet<>();
		Set<Long> valIds = new HashSet<>();
		for (Map<String, Object> v : rows) {
			if (v.get(spec ? "spec_id" : "attribute_id") != null) {
				attrIds.add(toLong(v.get(spec ? "spec_id" : "attribute_id")));
			}
			if (v.get(spec ? "spec_value_id" : "attribute_value_id") != null) {
				valIds.add(toLong(v.get(spec ? "spec_value_id" : "attribute_value_id")));
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

	private List<Long> parseItemCategoryIds(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				collectCategoryLongIdsFromObject(o, out);
			}
			return filterPositiveCategoryIds(out);
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return List.of();
			}
			if (t.startsWith("[")) {
				throw new ResourceException("选中的分类不存在 或 错误");
			}
			return filterPositiveCategoryIds(List.of(parseCategoryPlainLong(t)));
		}
		String asStr = str(v);
		if (asStr.isEmpty()) {
			return List.of();
		}
		return filterPositiveCategoryIds(List.of(parseCategoryPlainLong(asStr)));
	}

	/** Align with {@link PointsmallItemsCreateServiceImpl#parseCategoryIds}: ignore non-positive IDs. */
	private static List<Long> filterPositiveCategoryIds(List<Long> ids) {
		List<Long> out = new ArrayList<>();
		for (long id : ids) {
			if (id > 0) {
				out.add(id);
			}
		}
		return out;
	}

	private void collectCategoryLongIdsFromObject(Object o, List<Long> out) {
		if (o == null) {
			return;
		}
		if (o instanceof List<?> nested) {
			for (Object x : nested) {
				collectCategoryLongIdsFromObject(x, out);
			}
			return;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return;
			}
			if (t.startsWith("[")) {
				throw new ResourceException("选中的分类不存在 或 错误");
			}
			out.add(parseCategoryPlainLong(t));
			return;
		}
		if (o instanceof Number n) {
			out.add(n.longValue());
			return;
		}
		out.add(parseCategoryPlainLong(o.toString().trim()));
	}

	private long parseCategoryPlainLong(String t) {
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("选中的分类不存在 或 错误");
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
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}

	private static double dbl(Object o) {
		if (o == null) {
			return 0.0;
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0.0;
		}
		return Double.parseDouble(s);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
