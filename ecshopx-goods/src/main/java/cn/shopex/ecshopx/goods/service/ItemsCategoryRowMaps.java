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

import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import java.util.LinkedHashMap;
import java.util.Map;

/** 将分类行转为与现网接口一致的 Map 结构（列表 / 树节点 vs 详情）。 */
public final class ItemsCategoryRowMaps {

	private ItemsCategoryRowMaps() {
	}

	/** 详情接口（getCategoryInfo）：与现网详情 JSON 标量类型一致（不含列表字段 commission_ratio）。 */
	public static Map<String, Object> fromEntity(ItemsCategory e) {
		Map<String, Object> m = new LinkedHashMap<>();
		putDetailRow(m, e.getCategoryId(), e.getCompanyId(), e.getRegionauthId(), e.getCategoryName(), e.getCategoryCode(),
				e.getParentId(), e.getCategoryLevel(), e.getIsMainCategory(), e.getIsShowFront(), e.getPath(),
				e.getDistributorId(), e.getSort(), e.getGoodsParams(), e.getGoodsSpec(), e.getImageUrl(),
				e.getCrossborderTaxRate(), e.getCreated(), e.getUpdated(), e.getCustomizePageId(),
				e.getCategoryIdTaobao(), e.getParentIdTaobao(), e.getTaobaoCategoryInfo(),
				e.getInvoiceTaxRate(), e.getInvoiceTaxRateId());
		return m;
	}

	/**
	 * 全量列表 / 树（MyBatis 实体行）：与 {@link #fromFlexibleMap} 相同；{@code sort} 默认字符串，顶层数值化见 {@link ItemsCategoryQueryService}。
	 */
	public static Map<String, Object> fromListEntity(ItemsCategory e) {
		Map<String, Object> m = new LinkedHashMap<>();
		putListRow(m, e.getCategoryId(), e.getCompanyId(), e.getRegionauthId(), e.getCategoryName(), e.getCategoryCode(),
				e.getParentId(), e.getCategoryLevel(), e.getIsMainCategory(), e.getIsShowFront(), e.getPath(),
				e.getDistributorId(), e.getSort(), e.getGoodsParams(), e.getGoodsSpec(), e.getImageUrl(),
				e.getCrossborderTaxRate(), e.getCreated(), e.getUpdated(), e.getCustomizePageId(),
				e.getCommissionRatio(), e.getCategoryIdTaobao(), e.getParentIdTaobao(), e.getTaobaoCategoryInfo(),
				e.getInvoiceTaxRate(), e.getInvoiceTaxRateId());
		return m;
	}

	public static Map<String, Object> fromFlexibleMap(Map<String, Object> raw) {
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		Long categoryId = readLong(raw, "category_id");
		Long companyId = readLong(raw, "company_id");
		Long regionauthId = readLong(raw, "regionauth_id");
		String categoryName = readString(raw, "category_name");
		String categoryCode = readString(raw, "category_code");
		Long parentId = readLong(raw, "parent_id");
		Integer categoryLevel = readInteger(raw, "category_level");
		Boolean isMain = readTinyIntAsBool(raw, "is_main_category");
		Integer isShowFront = readInteger(raw, "is_show_front");
		String path = readString(raw, "path");
		Long distributorId = readLong(raw, "distributor_id");
		Long sort = readLong(raw, "sort");
		String goodsParams = readString(raw, "goods_params");
		String goodsSpec = readString(raw, "goods_spec");
		String imageUrl = readString(raw, "image_url");
		String cross = readString(raw, "crossborder_tax_rate");
		Integer created = readInteger(raw, "created");
		Integer updated = readInteger(raw, "updated");
		Long customizePageId = readLong(raw, "customize_page_id");
		Long catTaobao = readLong(raw, "category_id_taobao");
		Long parentTaobao = readLong(raw, "parent_id_taobao");
		String taobaoInfo = readString(raw, "taobao_category_info");
		String invoiceTax = readString(raw, "invoice_tax_rate");
		Long invoiceTaxId = readLong(raw, "invoice_tax_rate_id");
		Integer commissionRatio = readInteger(raw, "commission_ratio");
		Map<String, Object> m = new LinkedHashMap<>();
		putListRow(m, categoryId, companyId, regionauthId, categoryName, categoryCode, parentId, categoryLevel, isMain,
				isShowFront, path, distributorId, sort, goodsParams, goodsSpec, imageUrl, cross, created, updated,
				customizePageId, commissionRatio, catTaobao, parentTaobao, taobaoInfo, invoiceTax, invoiceTaxId);
		Object hc = raw.get("has_children");
		if (hc == null) {
			hc = raw.get("HAS_CHILDREN");
		}
		if (hc != null) {
			int v = hc instanceof Number ? ((Number) hc).intValue() : Integer.parseInt(hc.toString());
			// 单层列表接口约定：has_children 输出为字符串 "0" / "1"
			m.put("has_children", strInt(v));
		}
		return m;
	}

	/** 列表 / 树：字符串标量；{@code sort} 默认字符串（嵌套节点）；顶层由 QueryService 改为 JSON number。 */
	private static void putListRow(Map<String, Object> m, Long categoryId, Long companyId, Long regionauthId,
			String categoryName, String categoryCode, Long parentId, Integer categoryLevel, Boolean isMain,
			Integer isShowFront, String path, Long distributorId, Long sort, String goodsParams, String goodsSpec,
			String imageUrl, String cross, Integer created, Integer updated, Long customizePageId, Integer commissionRatio,
			Long catTaobao, Long parentTaobao, String taobaoInfo, String invoiceTax, Long invoiceTaxId) {
		m.put("category_id", strLong(categoryId));
		m.put("company_id", strLong(companyId));
		m.put("category_name", categoryName);
		m.put("category_code", categoryCode);
		m.put("parent_id", strLong(parentId));
		m.put("category_level", strInt(categoryLevel != null ? categoryLevel : 1));
		m.put("is_main_category", strInt(Boolean.TRUE.equals(isMain) ? 1 : 0));
		m.put("is_show_front", strInt(isShowFront != null ? isShowFront : 1));
		m.put("path", path != null ? path : "0");
		m.put("distributor_id", strLong(distributorId));
		m.put("sort", strLong(sort != null ? sort : 0L));
		m.put("goods_params", goodsParams);
		m.put("goods_spec", goodsSpec);
		m.put("image_url", imageUrl);
		m.put("crossborder_tax_rate", cross);
		m.put("created", strInt(created != null ? created : 0));
		m.put("updated", strInt(updated != null ? updated : 0));
		m.put("customize_page_id", formatCustomizePageId(customizePageId, isMain));
		m.put("commission_ratio", strInt(commissionRatio != null ? commissionRatio : 0));
		m.put("category_id_taobao", strLong(catTaobao));
		m.put("parent_id_taobao", strLong(parentTaobao));
		m.put("taobao_category_info", taobaoInfo);
		m.put("invoice_tax_rate_id", invoiceTaxId == null ? null : strLong(invoiceTaxId));
		m.put("invoice_tax_rate", invoiceTax);
		m.put("regionauth_id", strLong(regionauthId));
	}

	/** 详情：level 为数值；主类目布尔；{@code sort} 为 JSON 数字；{@code is_show_front} 为 {@code "0"}/{@code "1"} 字符串。 */
	private static void putDetailRow(Map<String, Object> m, Long categoryId, Long companyId, Long regionauthId,
			String categoryName, String categoryCode, Long parentId, Integer categoryLevel, Boolean isMain,
			Integer isShowFront, String path, Long distributorId, Long sort, String goodsParams, String goodsSpec,
			String imageUrl, String cross, Integer created, Integer updated, Long customizePageId,
			Long catTaobao, Long parentTaobao, String taobaoInfo, String invoiceTax, Long invoiceTaxId) {
		m.put("category_id", strLong(categoryId));
		m.put("company_id", strLong(companyId));
		m.put("category_name", categoryName);
		m.put("category_code", categoryCode);
		m.put("parent_id", strLong(parentId));
		m.put("category_level", categoryLevel != null ? categoryLevel : 1);
		m.put("is_main_category", isMain != null ? isMain : Boolean.FALSE);
		m.put("is_show_front", strInt(isShowFront != null ? isShowFront : 1));
		m.put("path", path != null ? path : "0");
		m.put("distributor_id", strLong(distributorId));
		m.put("sort", sort != null ? sort : 0L);
		m.put("goods_params", goodsParams);
		m.put("goods_spec", goodsSpec);
		m.put("image_url", imageUrl);
		m.put("crossborder_tax_rate", cross);
		m.put("created", created);
		m.put("updated", updated);
		m.put("customize_page_id", customizePageId);
		m.put("category_id_taobao", strLong(catTaobao));
		m.put("parent_id_taobao", strLong(parentTaobao));
		m.put("taobao_category_info", taobaoInfo);
		m.put("invoice_tax_rate_id", invoiceTaxId);
		m.put("invoice_tax_rate", invoiceTax);
		m.put("regionauth_id", strLong(regionauthId));
	}

	private static Object formatCustomizePageId(Long customizePageId, Boolean isMain) {
		if (customizePageId == null) {
			return null;
		}
		if (customizePageId == 0) {
			return Boolean.TRUE.equals(isMain) ? "0" : null;
		}
		return strLong(customizePageId);
	}

	private static String strLong(Long v) {
		if (v == null) {
			return "0";
		}
		return String.valueOf(v);
	}

	private static String strInt(int v) {
		return String.valueOf(v);
	}

	private static Long readLong(Map<String, Object> raw, String key) {
		Object v = getCi(raw, key);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer readInteger(Map<String, Object> raw, String key) {
		Object v = getCi(raw, key);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String readString(Map<String, Object> raw, String key) {
		Object v = getCi(raw, key);
		return v != null ? v.toString() : null;
	}

	private static Boolean readTinyIntAsBool(Map<String, Object> raw, String key) {
		Integer i = readInteger(raw, key);
		if (i == null) {
			return Boolean.FALSE;
		}
		return i != 0;
	}

	private static Object getCi(Map<String, Object> raw, String key) {
		if (raw.containsKey(key)) {
			return raw.get(key);
		}
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}
}
