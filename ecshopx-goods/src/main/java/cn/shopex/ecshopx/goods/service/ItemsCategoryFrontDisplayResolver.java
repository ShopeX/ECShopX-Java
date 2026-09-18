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

package cn.shopex.ecshopx.goods.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 前台分类列表 / 自定义分类页绑定的 product_model 决策（ECX-9537）。
 */
@Component
public class ItemsCategoryFrontDisplayResolver {

	/**
	 * platform 且商城页（distributor_id=0）固定查管理分类，忽略请求 is_main_category。
	 */
	public boolean resolveFrontCategoryListIsMainCategory(String productModel, long requestDistributorId,
			String requestIsMainCategoryRaw) {
		if (isPlatformMallPage(productModel, requestDistributorId)) {
			return true;
		}
		return parseQueryBoolean(requestIsMainCategoryRaw, false);
	}

	/**
	 * platform 商城页不做销售→管理 fallback；其余仅在首次为销售分类时允许 fallback。
	 */
	public boolean shouldFallbackFrontCategoryList(String productModel, long requestDistributorId,
			boolean resolvedIsMainCategory) {
		if (isPlatformMallPage(productModel, requestDistributorId)) {
			return false;
		}
		return !resolvedIsMainCategory;
	}

	/**
	 * standard 共用平台销售分类（distributor 维度为 0）；其余用请求店铺 id。
	 */
	public long resolveCategoryDistributorIdForFront(String productModel, long requestDistributorId) {
		if ("standard".equals(productModel)) {
			return 0L;
		}
		return requestDistributorId;
	}

	/** 仅 platform 自定义分类页绑定一级管理分类；其余绑定一级销售分类。 */
	public boolean isCustomizePageBindMainCategory(String productModel) {
		return "platform".equals(productModel);
	}

	public String getCustomizePageBindCategoryErrorMessage(String productModel) {
		return isCustomizePageBindMainCategory(productModel) ? "只能绑定一级管理分类" : "只能绑定一级销售分类";
	}

	/**
	 * @return 失败文案；通过返回 {@code null}
	 */
	public String validateCustomizePageBindCategory(int categoryLevel, boolean isMainCategory, String productModel) {
		boolean requiresMain = isCustomizePageBindMainCategory(productModel);
		if (categoryLevel != 1 || isMainCategory != requiresMain) {
			return getCustomizePageBindCategoryErrorMessage(productModel);
		}
		return null;
	}

	/** 递归写入节点 is_main_category（JSON boolean），反映最终查询数据源。 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> injectFrontCategoryListIsMainCategoryFlag(List<Map<String, Object>> categories,
			boolean isMainCategory) {
		if (categories == null || categories.isEmpty()) {
			return categories == null ? List.of() : categories;
		}
		for (Map<String, Object> node : categories) {
			node.put("is_main_category", isMainCategory);
			Object ch = node.get("children");
			if (ch instanceof List<?> list && !list.isEmpty()) {
				injectFrontCategoryListIsMainCategoryFlag((List<Map<String, Object>>) list, isMainCategory);
			}
		}
		return categories;
	}

	public static boolean isPlatformMallPage(String productModel, long requestDistributorId) {
		return "platform".equals(productModel) && requestDistributorId == 0L;
	}

	/** 对齐 PHP {@code filter_var(..., FILTER_VALIDATE_BOOLEAN)} 常用真值。 */
	public static boolean parseQueryBoolean(String raw, boolean defaultValue) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultValue;
		}
		String t = raw.trim();
		if ("1".equals(t) || "true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t) || "on".equalsIgnoreCase(t)) {
			return true;
		}
		if ("0".equals(t) || "false".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t) || "off".equalsIgnoreCase(t)) {
			return false;
		}
		try {
			return Integer.parseInt(t) != 0;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
