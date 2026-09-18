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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Builds internal list-query parameters from JWT and HTTP query; invalid pagination yields {@link BadRequestException}.
 */
public class GoodsItemsListParamBuilder {

	private static final String LIST_ERR_MSG = "获取商品列表出错.";

	private final LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	private final int page;
	private final int pageSize;

	public GoodsItemsListParamBuilder(HttpServletRequest request, Map<String, Object> jwt) {
		this(request, jwt, null, false);
	}

	/**
	 * @param maxPageSize 非空时，解析后的 pageSize 不超过该上限（非 SKU 列表）
	 */
	public GoodsItemsListParamBuilder(HttpServletRequest request, Map<String, Object> jwt, Integer maxPageSize) {
		this(request, jwt, maxPageSize, false);
	}

	/**
	 * @param skuEndpoint {@code true}：GET /goods/sku 专用映射（pageSize 允许 -1，不套用 maxPageSize）
	 */
	public GoodsItemsListParamBuilder(HttpServletRequest request, Map<String, Object> jwt, Integer maxPageSize, boolean skuEndpoint) {
		String ps = request.getParameter("page");
		String pz = request.getParameter("pageSize");
		boolean missingPage = !StringUtils.hasText(ps);
		boolean missingPz = !StringUtils.hasText(pz);
		if (missingPage || missingPz) {
			LinkedHashMap<String, List<String>> err = new LinkedHashMap<>();
			if (missingPage) {
				err.put("page", List.of("validation.required"));
			}
			if (missingPz) {
				err.put("pageSize", List.of("validation.required"));
			}
			if (skuEndpoint) {
				throw new BadRequestException(LIST_ERR_MSG, err, 422);
			}
			throw new BadRequestException(LIST_ERR_MSG, err);
		}
		try {
			this.page = Integer.parseInt(ps.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(LIST_ERR_MSG);
		}
		int parsedPageSize;
		try {
			parsedPageSize = Integer.parseInt(pz.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(LIST_ERR_MSG);
		}
		if (!skuEndpoint && maxPageSize != null && maxPageSize > 0) {
			parsedPageSize = Math.min(parsedPageSize, maxPageSize);
		}
		this.pageSize = parsedPageSize;
		if (page < 1) {
			throw new BadRequestException(LIST_ERR_MSG);
		}

		Object cid = jwt.get("company_id");
		if (cid == null) {
			throw new ResourceException("公司信息缺失");
		}
		params.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));

		if (skuEndpoint) {
			fillSkuEndpointParams(request);
		} else {
			fillStandardListParams(request);
		}
	}

	private void fillSkuEndpointParams(HttpServletRequest request) {
		if (request.getParameter("type") != null) {
			params.put("type", request.getParameter("type"));
		}
		String itemName = request.getParameter("item_name");
		if (StringUtils.hasText(itemName)) {
			params.put("item_name", itemName.trim());
		}
		String keywords = request.getParameter("keywords");
		if (StringUtils.hasText(keywords)) {
			params.put("item_name", keywords.trim());
		}
		putIfHasText(request, "consume_type", "consume_type");
		putIfHasText(request, "templates_id", "templates_id");
		putRegionsIdIfPresent(request);
		if (request.getParameter("nospec") != null) {
			params.put("nospec", request.getParameter("nospec"));
		}

		String isGiftRaw = request.getParameter("is_gift");
		int isGiftInt = 0;
		if (StringUtils.hasText(isGiftRaw) && "true".equalsIgnoreCase(isGiftRaw.trim())) {
			isGiftInt = 1;
		}
		params.put("is_gift", isGiftInt);

		String approveRaw = request.getParameter("approve_status");
		if (StringUtils.hasText(approveRaw) && isGiftInt != 1) {
			params.put("approve_status", approveRaw.trim());
		}

		String didParam = request.getParameter("distributor_id");
		long distributorVal = 0L;
		if (StringUtils.hasText(didParam)) {
			try {
				distributorVal = Long.parseLong(didParam.trim());
			} catch (NumberFormatException e) {
				distributorVal = 0L;
			}
		}
		params.put("distributor_id", distributorVal);

		if (StringUtils.hasText(approveRaw)) {
			String a = approveRaw.trim();
			if ("processing".equals(a) || "rejected".equals(a)) {
				params.put("audit_status", a);
			} else {
				params.put("approve_status", a);
			}
		}

		putAuditStatusIfPhpTruthy(request);

		String rebateStr = request.getParameter("rebate");
		if (StringUtils.hasText(rebateStr)) {
			try {
				int rv = Integer.parseInt(rebateStr.trim());
				if (rv >= 0 && rv <= 3) {
					params.put("rebate", rv);
				}
			} catch (NumberFormatException ignored) {
				// omit invalid rebate
			}
		}
		putIfHasText(request, "rebate_type", "rebate_type");

		putItemIdQueryParamIfPresent(request);

		List<Long> mainCatIds = lastNonZeroQueryIdList(request, "main_cat_id");
		if (!mainCatIds.isEmpty()) {
			params.put("main_cat_id_list", mainCatIds);
		}

		List<Long> categoryIds = lastNonZeroQueryIdList(request, "category");
		if (!categoryIds.isEmpty()) {
			params.put("category_list", categoryIds);
		}

		String itemType = request.getParameter("item_type");
		params.put("item_type", StringUtils.hasText(itemType) ? itemType.trim() : "services");

		if (request.getParameter("store_gt") != null && StringUtils.hasText(request.getParameter("store_gt"))) {
			params.put("store_gt", request.getParameter("store_gt"));
		}
		if (request.getParameter("store_lt") != null && StringUtils.hasText(request.getParameter("store_lt"))) {
			params.put("store_lt", request.getParameter("store_lt"));
		}
		if (request.getParameter("price_gt") != null && StringUtils.hasText(request.getParameter("price_gt"))) {
			params.put("price_gt", request.getParameter("price_gt"));
		}
		if (request.getParameter("price_lt") != null && StringUtils.hasText(request.getParameter("price_lt"))) {
			params.put("price_lt", request.getParameter("price_lt"));
		}

		String st = request.getParameter("special_type");
		if (StringUtils.hasText(st)) {
			String t = st.trim();
			if ("normal".equals(t) || "drug".equals(t)) {
				params.put("special_type", t);
			}
		}

		putIfHasText(request, "is_warning", "is_warning");

		putTagIdListIfPresent(request);

		String brandId = request.getParameter("brand_id");
		if (StringUtils.hasText(brandId)) {
			try {
				int b = Integer.parseInt(brandId.trim());
				if (b != 0) {
					params.put("brand_id", b);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		putIfHasText(request, "operate_source", "operate_source");
		putIfHasText(request, "is_sku", "is_sku");
		putIfHasText(request, "item_bn", "item_bn");
	}

	private void fillStandardListParams(HttpServletRequest request) {
		String itemType = request.getParameter("item_type");
		params.put("item_type", StringUtils.hasText(itemType) ? itemType.trim() : "services");

		putIfHasText(request, "type", "type");
		putIfHasText(request, "operate_source", "operate_source");
		putIfHasText(request, "item_source", "item_source");
		putIfHasText(request, "item_name", "item_name");
		putIfHasText(request, "created_time_start", "created_time_start");
		putIfHasText(request, "created_time_end", "created_time_end");
		putIfHasText(request, "consume_type", "consume_type");
		putIfHasText(request, "templates_id", "templates_id");
		if (request.getParameter("is_market") != null && StringUtils.hasText(request.getParameter("is_market"))) {
			params.put("is_market", request.getParameter("is_market"));
		}
		putIfHasText(request, "goods_bn", "goods_bn");
		putIfHasText(request, "keywords", "keywords");
		putIfHasText(request, "approve_status", "approve_status");
		putAuditStatusIfPhpTruthy(request);
		putIfHasText(request, "from_page", "from_page");
		putIfHasText(request, "distributor_approve_status", "distributor_approve_status");
		putIfHasText(request, "rebate_type", "rebate_type");
		putIfHasText(request, "supplier_name", "supplier_name");
		putIfHasText(request, "special_type", "special_type");
		putIfHasText(request, "is_warning", "is_warning");
		putIfHasText(request, "is_sku", "is_sku");
		putIfHasText(request, "pathSource", "pathSource");
		putIfHasText(request, "item_bn", "item_bn");
		putIfHasText(request, "barcode", "barcode");
		putIfHasText(request, "is_gift", "is_gift");

		putRegionsIdIfPresent(request);

		if (request.getParameter("nospec") != null) {
			params.put("nospec", request.getParameter("nospec"));
		}
		if (request.getParameter("is_taobao") != null) {
			params.put("is_taobao", request.getParameter("is_taobao"));
		}
		if (request.getParameter("supplier_id") != null && StringUtils.hasText(request.getParameter("supplier_id"))) {
			params.put("supplier_id", request.getParameter("supplier_id"));
		}
		if (request.getParameter("distributor_id") != null) {
			params.put("distributor_id", request.getParameter("distributor_id"));
		}
		if (request.getParameter("rebate") != null && StringUtils.hasText(request.getParameter("rebate"))) {
			params.put("rebate", request.getParameter("rebate"));
		}
		putItemIdQueryParamIfPresent(request);
		if (request.getParameter("store_status") != null) {
			params.put("store_status", request.getParameter("store_status"));
		}
		if (request.getParameter("store_gt") != null && StringUtils.hasText(request.getParameter("store_gt"))) {
			params.put("store_gt", request.getParameter("store_gt"));
		}
		if (request.getParameter("store_lt") != null && StringUtils.hasText(request.getParameter("store_lt"))) {
			params.put("store_lt", request.getParameter("store_lt"));
		}
		if (request.getParameter("price_gt") != null && StringUtils.hasText(request.getParameter("price_gt"))) {
			params.put("price_gt", request.getParameter("price_gt"));
		}
		if (request.getParameter("price_lt") != null && StringUtils.hasText(request.getParameter("price_lt"))) {
			params.put("price_lt", request.getParameter("price_lt"));
		}
		String brandIdParam = request.getParameter("brand_id");
		if (StringUtils.hasText(brandIdParam)) {
			try {
				int b = Integer.parseInt(brandIdParam.trim());
				if (b != 0) {
					params.put("brand_id", b);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		if (request.getParameter("is_medicine") != null && StringUtils.hasText(request.getParameter("is_medicine"))) {
			params.put("is_medicine", request.getParameter("is_medicine"));
		}
		if (request.getParameter("is_prescription") != null && StringUtils.hasText(request.getParameter("is_prescription"))) {
			params.put("is_prescription", request.getParameter("is_prescription"));
		}

		putTagIdListIfPresent(request);

		List<Long> mainCatIds = lastNonZeroQueryIdList(request, "main_cat_id");
		if (!mainCatIds.isEmpty()) {
			params.put("main_cat_id_list", mainCatIds);
		}

		List<Long> categoryIds = lastNonZeroQueryIdList(request, "category");
		if (!categoryIds.isEmpty()) {
			params.put("category_list", categoryIds);
		}
	}

	private void putTagIdListIfPresent(HttpServletRequest request) {
		String[] tagIds = request.getParameterValues("tag_id[]");
		if (tagIds == null || tagIds.length == 0) {
			tagIds = request.getParameterValues("tag_id");
		}
		if (tagIds == null || tagIds.length == 0) {
			return;
		}
		List<Long> t = new ArrayList<>();
		for (String s : tagIds) {
			if (StringUtils.hasText(s)) {
				long id = Long.parseLong(s.trim());
				if (id != 0L) {
					t.add(id);
				}
			}
		}
		if (!t.isEmpty()) {
			params.put("tag_id_list", t);
		}
	}

	private void putItemIdQueryParamIfPresent(HttpServletRequest request) {
		List<String> parts = new ArrayList<>();
		String[] bracketVals = request.getParameterValues("item_id[]");
		if (bracketVals != null) {
			for (String s : bracketVals) {
				if (StringUtils.hasText(s)) {
					parts.add(s.trim());
				}
			}
		}
		if (parts.isEmpty()) {
			String[] plainVals = request.getParameterValues("item_id");
			if (plainVals != null) {
				for (String s : plainVals) {
					if (StringUtils.hasText(s)) {
						parts.add(s.trim());
					}
				}
			}
		}
		if (parts.isEmpty()) {
			String single = request.getParameter("item_id");
			if (StringUtils.hasText(single)) {
				for (String s : single.split(",")) {
					if (StringUtils.hasText(s)) {
						parts.add(s.trim());
					}
				}
			}
		}
		if (!parts.isEmpty()) {
			params.put("item_id", String.join(",", parts));
		}
	}

	/** Repeated query params: use the last non-zero id only. */
	private static List<Long> lastNonZeroQueryIdList(HttpServletRequest request, String paramBase) {
		String[] values = request.getParameterValues(paramBase + "[]");
		if (values == null || values.length == 0) {
			values = request.getParameterValues(paramBase);
		}
		return lastNonZeroQueryIdList(values);
	}

	private static List<Long> lastNonZeroQueryIdList(String[] values) {
		if (values == null || values.length == 0) {
			return List.of();
		}
		for (int i = values.length - 1; i >= 0; i--) {
			String s = values[i];
			if (StringUtils.hasText(s)) {
				long id = Long.parseLong(s.trim());
				if (id != 0L) {
					return List.of(id);
				}
			}
		}
		return List.of();
	}

	private void putIfHasText(HttpServletRequest request, String paramName, String mapKey) {
		String v = request.getParameter(paramName);
		if (StringUtils.hasText(v)) {
			params.put(mapKey, v.trim());
		}
	}

	private void putAuditStatusIfPhpTruthy(HttpServletRequest request) {
		String auditRaw = request.getParameter("audit_status");
		if (auditRaw != null && isPhpTruthy(auditRaw) && !"rebate".equalsIgnoreCase(auditRaw.trim())) {
			params.put("audit_status", auditRaw.trim());
		}
	}

	private static boolean isPhpTruthy(String value) {
		return StringUtils.hasText(value) && !"0".equals(value.trim());
	}

	private void putRegionsIdIfPresent(HttpServletRequest request) {
		String joined = joinRegionsQuery(request);
		if (StringUtils.hasText(joined)) {
			params.put("regions_id", joined);
		}
	}

	private static String joinRegionsQuery(HttpServletRequest request) {
		String[] vals = request.getParameterValues("regions_id[]");
		if (vals == null || vals.length == 0) {
			vals = request.getParameterValues("regions_id");
		}
		if (vals == null || vals.length == 0) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		for (String v : vals) {
			if (StringUtils.hasText(v)) {
				parts.add(v.trim());
			}
		}
		return parts.isEmpty() ? null : String.join(",", parts);
	}

	public LinkedHashMap<String, Object> toParamsMap() {
		return params;
	}

	public int getPage() {
		return page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public static int priceToCents(Object yuanOrNumber) {
		if (yuanOrNumber == null) {
			return 0;
		}
		BigDecimal v = new BigDecimal(yuanOrNumber.toString().trim());
		return v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}
}
