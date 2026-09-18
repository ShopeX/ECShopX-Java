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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class GoodsItemsListRowMapper {

	private static final ObjectMapper OM = new ObjectMapper();

	private GoodsItemsListRowMapper() {
	}

	public static Map<String, Object> toRowFromSupplier(SupplierItems supplierRow, long mainItemId) {
		Map<String, Object> m = toRow(toItemsEntity(supplierRow));
		m.put("main_item_id", mainItemId);
		return m;
	}

	static Items toItemsEntity(SupplierItems s) {
		Items it = new Items();
		it.setItemId(s.getItemId());
		it.setItemType(s.getItemType());
		it.setItemCategory(s.getItemCategory());
		it.setConsumeType(s.getConsumeType());
		it.setItemName(s.getItemName());
		it.setItemBn(s.getItemBn());
		it.setBarcode(s.getBarcode());
		it.setBrief(s.getBrief());
		it.setCompanyId(s.getCompanyId());
		it.setPrice(s.getPrice());
		it.setCostPrice(s.getCostPrice());
		it.setItemUnit(s.getItemUnit());
		it.setSpecialType(s.getSpecialType());
		it.setItemAddressProvince(s.getItemAddressProvince());
		it.setItemAddressCity(s.getItemAddressCity());
		it.setRegionsId(s.getRegionsId());
		it.setRegions(s.getRegions());
		it.setStore(s.getStore());
		it.setSales(s.getSales());
		it.setRebateConf(s.getRebateConf());
		it.setRebate(s.getRebate());
		it.setRebateType(s.getRebateType());
		it.setApproveStatus(s.getApproveStatus());
		it.setAuditStatus(s.getAuditStatus());
		it.setAuditReason(s.getAuditReason());
		it.setMarketPrice(s.getMarketPrice());
		it.setGoodsFunction(s.getGoodsFunction());
		it.setGoodsSeries(s.getGoodsSeries());
		it.setGoodsColor(s.getGoodsColor());
		it.setGoodsBrand(s.getGoodsBrand());
		it.setIsDefault(s.getIsDefault());
		it.setDefaultItemId(s.getDefaultItemId());
		it.setGoodsId(s.getGoodsId());
		it.setNospec(s.getNospec());
		it.setWeight(s.getWeight());
		it.setSort(s.getSort());
		it.setIsEpidemic(s.getIsEpidemic());
		it.setTemplatesId(s.getTemplatesId());
		it.setPics(s.getPics());
		it.setPicsCreateQrcode(s.getPicsCreateQrcode());
		it.setVideoType(s.getVideoType());
		it.setVideos(s.getVideos());
		it.setVideoPicUrl(s.getVideoPicUrl());
		it.setIntro(s.getIntro());
		it.setPurchaseAgreement(s.getPurchaseAgreement());
		it.setIsShowSpecimg(s.getIsShowSpecimg());
		it.setEnableAgreement(s.getEnableAgreement());
		it.setDateType(s.getDateType());
		it.setBeginDate(s.getBeginDate());
		it.setEndDate(s.getEndDate());
		it.setFixedTerm(s.getFixedTerm());
		it.setBrandLogo(s.getBrandLogo());
		it.setIsPoint(s.getIsPoint());
		it.setPoint(s.getPoint());
		it.setDistributorId(s.getDistributorId());
		it.setVolume(s.getVolume());
		it.setItemSource(s.getItemSource());
		it.setBrandId(s.getBrandId());
		it.setTaxRate(s.getTaxRate());
		it.setCrossborderTaxRate(s.getCrossborderTaxRate());
		it.setProfitType(s.getProfitType());
		it.setOrigincountryId(s.getOrigincountryId());
		it.setTaxstrategyId(s.getTaxstrategyId());
		it.setTaxationNum(s.getTaxationNum());
		it.setProfitFee(s.getProfitFee());
		it.setType(s.getType());
		it.setIsProfit(s.getIsProfit());
		it.setIsMedicine(s.getIsMedicine());
		it.setIsPrescription(s.getIsPrescription());
		it.setCreated(s.getCreated());
		it.setUpdated(s.getUpdated());
		it.setIsGift(s.getIsGift());
		it.setIsPackage(s.getIsPackage());
		it.setTdkContent(s.getTdkContent());
		it.setSupplierId(s.getSupplierId());
		it.setSupplierItemId(0);
		it.setIsMarket(s.getIsMarket());
		it.setGoodsBn(s.getGoodsBn());
		it.setSupplierGoodsBn(s.getSupplierGoodsBn());
		it.setAuditDate(s.getAuditDate());
		it.setStartNum(s.getStartNum());
		it.setDeliveryTime(0);
		it.setIsTaobao(0);
		it.setDataSource("supplier_goods");
		return it;
	}

	public static Map<String, Object> toRow(Items it) {
		Map<String, Object> m = new LinkedHashMap<>();
		putNum(m, "item_id", it.getItemId());
		putNum(m, "goods_id", it.getGoodsId());
		m.put("item_name", it.getItemName());
		putNum(m, "price", it.getPrice());
		putNum(m, "store", it.getStore());
		m.put("nospec", parseNospecBoolean(it.getNospec()));
		m.put("item_category", it.getItemCategory());
		m.put("item_main_cat_id", parseMainCatId(it.getItemCategory()));
		m.put("item_cat_id", List.of());
		m.put("item_type", it.getItemType());
		m.put("consume_type", it.getConsumeType() != null ? it.getConsumeType() : "every");
		m.put("pics", resolvePicsForListRow(it.getPics()));
		m.put("type_labels", List.of());
		m.put("brand_logo", it.getBrandLogo());
		m.put("goods_brand", it.getGoodsBrand());
		m.put("promotion_activity", List.of());
		m.put("activity_price", null);
		m.put("tagList", List.of());
		m.put("operator_name", "");
		m.put("distributor_name", List.of());
		m.put("item_holder", it.getSupplierId() != null && it.getSupplierId() > 0 ? "supplier" : "self");
		m.put("supplier_name", "");
		m.put("gross_profit_rate", ItemsListQueryRepository.listRowGrossProfitRate(it.getPrice(), it.getCostPrice()));
		m.put("commission_ratio", 0);
		m.put("itemMainCatName", "");
		m.put("itemCatName", List.of());
		putNum(m, "supplier_id", it.getSupplierId());
		putNum(m, "distributor_id", it.getDistributorId());
		putNum(m, "brand_id", it.getBrandId());
		m.put("approve_status", it.getApproveStatus());
		m.put("audit_status", listAuditStatusForResponse(it.getAuditStatus()));
		m.put("audit_reason", it.getAuditReason());
		m.put("item_bn", it.getItemBn());
		m.put("barcode", it.getBarcode());
		m.put("brief", it.getBrief());
		m.put("is_default", it.getIsDefault());
		putNum(m, "default_item_id", it.getDefaultItemId());
		if (it.getSupplierItemId() != null) {
			putNum(m, "supplier_item_id", it.getSupplierItemId());
		} else {
			m.put("supplier_item_id", 0);
		}

		putNum(m, "market_price", it.getMarketPrice());
		putNum(m, "cost_price", it.getCostPrice());
		m.put("goods_bn", it.getGoodsBn());
		putNullableLong(m, "templates_id", it.getTemplatesId() != null ? it.getTemplatesId().longValue() : null);
		putNum(m, "sort", it.getSort());
		m.put("weight", weightToListString(it.getWeight()));
		m.put("volume", it.getVolume());
		m.put("rebate", rebateToString(it.getRebate()));
		m.put("rebate_conf", parseJsonColumn(it.getRebateConf()));
		m.put("rebate_type", it.getRebateType() != null ? it.getRebateType() : "default");
		m.put("is_gift", it.getIsGift() != null ? it.getIsGift() : false);
		putNum(m, "is_market", it.getIsMarket());
		m.put("pics_create_qrcode", resolvePicsCreateQrcodeForRow(it.getPicsCreateQrcode()));
		m.put("video_type", it.getVideoType() != null ? it.getVideoType() : "local");
		m.put("videos", it.getVideos());
		m.put("video_pic_url", it.getVideoPicUrl());
		m.put("intro", it.getIntro());
		m.put("purchase_agreement", it.getPurchaseAgreement());
		m.put("enable_agreement", it.getEnableAgreement() != null ? it.getEnableAgreement() : false);
		m.put("is_show_specimg", it.getIsShowSpecimg() != null ? it.getIsShowSpecimg() : false);
		m.put("date_type", it.getDateType());
		putNullableInteger(m, "begin_date", it.getBeginDate());
		putNullableInteger(m, "end_date", it.getEndDate());
		putNullableInteger(m, "fixed_term", it.getFixedTerm());
		m.put("is_point", it.getIsPoint());
		putNullableInteger(m, "point", it.getPoint());
		m.put("item_source", it.getItemSource() != null ? it.getItemSource() : "mall");
		m.put("data_source", it.getDataSource() != null ? it.getDataSource() : "");
		putNum(m, "tax_rate", it.getTaxRate());
		m.put("crossborder_tax_rate", it.getCrossborderTaxRate() != null ? it.getCrossborderTaxRate() : "");
		putNum(m, "origincountry_id", it.getOrigincountryId());
		putNum(m, "taxstrategy_id", it.getTaxstrategyId());
		putNum(m, "taxation_num", it.getTaxationNum());
		m.put("regions_id", it.getRegionsId());
		m.put("regions", it.getRegions());
		m.put("special_type", it.getSpecialType() != null ? it.getSpecialType() : "normal");
		m.put("item_address_province", it.getItemAddressProvince());
		m.put("item_address_city", it.getItemAddressCity());
		putNum(m, "profit_type", it.getProfitType());
		putNum(m, "profit_fee", it.getProfitFee());
		m.put("is_profit", it.getIsProfit() != null ? it.getIsProfit() : false);
		m.put("is_package", it.getIsPackage() != null ? it.getIsPackage() : false);
		putNum(m, "type", it.getType());
		putNum(m, "start_num", it.getStartNum());
		putNum(m, "delivery_time", it.getDeliveryTime());
		m.put("item_unit", it.getItemUnit());
		m.put("goods_function", it.getGoodsFunction());
		m.put("goods_series", it.getGoodsSeries());
		m.put("goods_color", it.getGoodsColor());
		putNullableInteger(m, "sales", it.getSales());
		putNum(m, "created", it.getCreated());
		putNullableInteger(m, "updated", it.getUpdated());
		putNum(m, "is_medicine", it.getIsMedicine());
		putNum(m, "is_prescription", it.getIsPrescription());
		m.put("tdk_content", it.getTdkContent());
		putNum(m, "is_epidemic", it.getIsEpidemic());
		putNum(m, "company_id", it.getCompanyId());
		putNullableInteger(m, "audit_date", it.getAuditDate());
		putNum(m, "is_taobao", it.getIsTaobao());

		addCamelCaseMirrors(m);
		return m;
	}

	/**
	 * Adds camelCase aliases for selected list fields so clients expecting legacy key names receive the same values as snake_case entries.
	 */
	private static void addCamelCaseMirrors(Map<String, Object> m) {
		m.put("itemId", m.get("item_id"));
		m.put("consumeType", m.get("consume_type"));
		m.put("itemName", m.get("item_name"));
		m.put("itemBn", m.get("item_bn"));
		m.put("companyId", m.get("company_id"));
	}

	/** Empty or unset DB value is exposed as JSON null on list APIs (not a default literal). */
	private static Object listAuditStatusForResponse(String auditStatus) {
		if (auditStatus == null) {
			return null;
		}
		String t = auditStatus.trim();
		return t.isEmpty() ? null : t;
	}

	private static String rebateToString(Integer rebate) {
		if (rebate == null) {
			return "";
		}
		return Long.toString(rebate.longValue());
	}

	private static Object parseJsonColumn(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		try {
			JsonNode node = OM.readTree(t);
			if (node.isNull()) {
				return null;
			}
			return node;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static void putNullableInteger(Map<String, Object> m, String k, Integer v) {
		m.put(k, v);
	}

	private static void putNullableLong(Map<String, Object> m, String k, Long v) {
		m.put(k, v);
	}

	/** List API exposes weight as a decimal string (e.g. {@code "0"}), not a JSON number. */
	private static String weightToListString(Double w) {
		if (w == null) {
			return null;
		}
		return BigDecimal.valueOf(w).stripTrailingZeros().toPlainString();
	}

	private static boolean parseNospecBoolean(String nospec) {
		if (!StringUtils.hasText(nospec)) {
			return false;
		}
		String s = nospec.trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static Object parseMainCatId(String itemCategory) {
		if (!StringUtils.hasText(itemCategory)) {
			return "";
		}
		try {
			return Long.parseLong(itemCategory.trim());
		} catch (NumberFormatException e) {
			return itemCategory.trim();
		}
	}

	/**
	 * {@code pics_create_qrcode} is a JSON boolean array for wire responses; empty or invalid values become {@code []}.
	 */
	public static List<Object> resolvePicsCreateQrcodeForRow(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		try {
			JsonNode root = OM.readTree(t);
			if (root == null || root.isNull()) {
				return List.of();
			}
			if (root.isArray()) {
				return OM.convertValue(root, new TypeReference<List<Object>>() {
				});
			}
			if (root.isTextual()) {
				String inner = root.asText();
				if (!StringUtils.hasText(inner)) {
					return List.of();
				}
				JsonNode innerRoot = OM.readTree(inner.trim());
				if (innerRoot.isArray()) {
					return OM.convertValue(innerRoot, new TypeReference<List<Object>>() {
					});
				}
			}
		} catch (Exception ignored) {
		}
		if (t.startsWith("[")) {
			try {
				return OM.readValue(t, new TypeReference<List<Object>>() {
				});
			} catch (Exception ignored) {
			}
		}
		return List.of();
	}

	/** {@code pics} is normally a JSON array; a JSON string column value is returned as-is for correct wire typing. */
	public static Object resolvePicsForListRow(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return List.of();
		}
		String t = picsJson.trim();
		try {
			JsonNode root = OM.readTree(t);
			if (root.isArray()) {
				return OM.convertValue(root, new TypeReference<List<String>>() {
				});
			}
			if (root.isTextual()) {
				String inner = root.asText();
				return inner.isEmpty() ? List.of() : inner;
			}
		} catch (Exception ignored) {
		}
		if (t.startsWith("[")) {
			try {
				return OM.readValue(t, new TypeReference<List<String>>() {
				});
			} catch (Exception ignored) {
			}
		}
		return List.of();
	}

	/** Nullable numeric columns: keep null in the row map instead of coercing to zero. */
	@SuppressWarnings("unchecked")
	public static List<Long> parseSupplierCategoryIds(String attrData) {
		if (!StringUtils.hasText(attrData)) {
			return List.of();
		}
		try {
			JsonNode root = OM.readTree(attrData.trim());
			JsonNode category = root.get("category");
			if (category == null || !category.isArray()) {
				return List.of();
			}
			List<Long> ids = new ArrayList<>();
			for (JsonNode n : category) {
				if (n.isNumber()) {
					ids.add(n.longValue());
				} else if (n.isTextual()) {
					try {
						ids.add(Long.parseLong(n.asText().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return ids;
		} catch (Exception ignored) {
			return List.of();
		}
	}

	private static void putNum(Map<String, Object> m, String k, Object v) {
		if (v == null) {
			m.put(k, null);
			return;
		}
		if (v instanceof Number n) {
			m.put(k, n.longValue());
		} else {
			m.put(k, v);
		}
	}
}
