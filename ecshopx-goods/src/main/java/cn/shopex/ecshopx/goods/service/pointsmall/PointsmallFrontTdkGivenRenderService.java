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

import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.kaquan.service.discount.WxShopsSettingForWechatCardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallFrontTdkGivenRenderService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final WxShopsSettingForWechatCardService wxShopsSettingForWechatCardService;

	public PointsmallFrontTdkGivenRenderService(ItemsCategoryRepository itemsCategoryRepository,
			WxShopsSettingForWechatCardService wxShopsSettingForWechatCardService) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.wxShopsSettingForWechatCardService = wxShopsSettingForWechatCardService;
	}

	public Map<String, Object> render(Map<String, Object> tdkSlice, Map<String, Object> detailRow) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("title", replaceCommas(expandField(tdkSlice.get("title"), detailRow)));
		out.put("mate_description", replaceCommas(expandField(tdkSlice.get("mate_description"), detailRow)));
		out.put("mate_keywords", replaceCommas(expandField(tdkSlice.get("mate_keywords"), detailRow)));
		return out;
	}

	private static String replaceCommas(String s) {
		return s == null ? "" : s.replace(',', '_');
	}

	private String expandField(Object rawTemplate, Map<String, Object> data) {
		if (rawTemplate == null) {
			return "";
		}
		String t = rawTemplate.toString().trim();
		if (t.isEmpty()) {
			return "";
		}
		String[] tokens = t.split(",", -1);
		List<String> pieces = new ArrayList<>();
		for (String token : tokens) {
			String v = token == null ? "" : token.trim();
			if (v.isEmpty()) {
				continue;
			}
			if (v.length() < 2 || !v.startsWith("{") || !v.endsWith("}")) {
				pieces.add(v);
				continue;
			}
			String inner = v.substring(1, v.length() - 1).trim();
			Optional<String> part = resolveToken(inner, data);
			part.ifPresent(pieces::add);
		}
		return String.join(",", pieces);
	}

	private Optional<String> resolveToken(String key, Map<String, Object> data) {
		long companyId = toLong(data.get("company_id"));
		return switch (key) {
			case "goods_brand" -> Optional.of(nz(data.get("goods_brand")));
			case "goods_price" -> Optional.of(formatGoodsPrice(data.get("price")));
			case "goods_name" -> Optional.of(nz(data.get("item_name")));
			case "goods_category" -> Optional.of(categoryInfoById(companyId, firstCategoryId(data.get("item_category"))).name);
			case "goods_brief" -> briefPiece(data.get("brief"));
			case "search_keywords" -> keywordsPiece(data.get("keywords"));
			case "category" -> Optional.of(categoryInfo(companyId, toLong(data.get("category_id"))).name);
			case "category_path" -> Optional.of(categoryInfo(companyId, toLong(data.get("category_id"))).path);
			case "shop_name" -> Optional.of(shopBrandName(companyId));
			default -> Optional.of(key + "-无");
		};
	}

	private static Optional<String> briefPiece(Object brief) {
		if (brief == null) {
			return Optional.empty();
		}
		String s = brief.toString().trim();
		return s.isEmpty() ? Optional.empty() : Optional.of(s);
	}

	private static Optional<String> keywordsPiece(Object keywords) {
		if (keywords == null) {
			return Optional.empty();
		}
		String s = keywords.toString().trim();
		return s.isEmpty() ? Optional.empty() : Optional.of(s);
	}

	private static String formatGoodsPrice(Object priceObj) {
		if (priceObj == null) {
			return "￥0";
		}
		BigDecimal fen;
		try {
			if (priceObj instanceof Number n) {
				fen = BigDecimal.valueOf(n.longValue());
			} else {
				fen = new BigDecimal(priceObj.toString().trim());
			}
		} catch (Exception e) {
			return "￥0";
		}
		BigDecimal yuan = fen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).stripTrailingZeros();
		return "￥" + yuan.toPlainString();
	}

	private static Long firstCategoryId(Object itemCategoryRaw) {
		if (!(itemCategoryRaw instanceof List<?> list) || list.isEmpty()) {
			return null;
		}
		Object first = list.get(0);
		if (first == null) {
			return null;
		}
		try {
			long v = Long.parseLong(first.toString().trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private CategoryNames categoryInfoById(long companyId, Long categoryId) {
		if (categoryId == null || categoryId <= 0) {
			return new CategoryNames("", "");
		}
		return categoryInfo(companyId, categoryId.longValue());
	}

	private CategoryNames categoryInfo(long companyId, long catId) {
		if (catId <= 0 || companyId <= 0) {
			return new CategoryNames("", "");
		}
		ItemsCategory row = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, catId).orElse(null);
		if (row == null) {
			return new CategoryNames("", "");
		}
		String name = nz(row.getCategoryName());
		String pathStr = row.getPath();
		if (!StringUtils.hasText(pathStr)) {
			return new CategoryNames(name, name);
		}
		List<Long> pathIds = new ArrayList<>();
		for (String p : pathStr.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				long id = Long.parseLong(p.trim());
				if (id > 0) {
					pathIds.add(id);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		if (pathIds.isEmpty()) {
			return new CategoryNames(name, name);
		}
		List<ItemsCategory> rows = itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, pathIds);
		Map<Long, String> idToName = rows.stream().collect(Collectors.toMap(ItemsCategory::getCategoryId, c -> nz(c.getCategoryName()), (a, b) -> a));
		List<String> pathNames = new ArrayList<>();
		for (Long id : pathIds) {
			String n = idToName.get(id);
			if (StringUtils.hasText(n)) {
				pathNames.add(n);
			}
		}
		return new CategoryNames(name, String.join("/", pathNames));
	}

	private String shopBrandName(long companyId) {
		if (companyId <= 0) {
			return "";
		}
		Map<String, Object> shop = wxShopsSettingForWechatCardService.load(companyId);
		Object bn = shop.get("brand_name");
		return bn != null ? bn.toString() : "";
	}

	private static String nz(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private record CategoryNames(String name, String path) {
	}
}
