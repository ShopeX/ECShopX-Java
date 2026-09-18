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

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 供应商后台 {@code operate_source=supplier} 商品列表响应 wire 形态（与 PHP {@code SupplierItemsService::getItemsList} + {@code Items::getItemsList} 一致）。
 */
public final class SupplierAdminItemsListWireMapper {

	private static final Set<String> REMOVE_KEYS = Set.of(
			"type_labels",
			"promotion_activity",
			"activity_price",
			"data_source",
			"delivery_time",
			"is_taobao",
			"supplier_item_id",
			"itemId",
			"consumeType",
			"itemBn",
			"companyId");

	private SupplierAdminItemsListWireMapper() {
	}

	public static void apply(List<Map<String, Object>> rows, List<SupplierItems> sources, Map<Long, Map<String, Object>> rawNullables) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Map<Long, SupplierItems> byItemId = sources == null
				? Map.of()
				: sources.stream().filter(Objects::nonNull).filter(s -> s.getItemId() != null)
						.collect(Collectors.toMap(SupplierItems::getItemId, s -> s, (a, b) -> a, LinkedHashMap::new));
		for (Map<String, Object> row : rows) {
			long itemId = toLong(row.get("item_id"));
			Map<String, Object> raw = rawNullables != null ? rawNullables.get(itemId) : null;
			applyRow(row, byItemId.get(itemId), raw);
		}
	}

	private static void applyRow(Map<String, Object> row, SupplierItems src, Map<String, Object> rawNullableColumns) {
		REMOVE_KEYS.forEach(row::remove);
		if (src == null) {
			stringifyRowScalars(row);
			return;
		}

		row.put("nospec", parseNospecBoolean(src.getNospec()));
		row.put("pics", rawJsonColumn(src.getPics()));
		row.put("pics_create_qrcode", GoodsItemsListRowMapper.resolvePicsCreateQrcodeForRow(src.getPicsCreateQrcode()));
		row.put("supplier_goods_bn", src.getSupplierGoodsBn());
		row.put("brand_logo", src.getBrandLogo() != null ? src.getBrandLogo() : "");

		row.put("gross_profit_rate", ItemsListQueryRepository.listRowGrossProfitRatePhpParity(src.getPrice(), src.getCostPrice()));

		stringifyId(row, "item_id", src.getItemId());
		stringifyId(row, "goods_id", src.getGoodsId());
		stringifyId(row, "default_item_id", src.getDefaultItemId());
		stringifyId(row, "company_id", src.getCompanyId());
		stringifyId(row, "brand_id", src.getBrandId());
		stringifyId(row, "supplier_id", src.getSupplierId());
		stringifyId(row, "distributor_id", src.getDistributorId());
		stringifyId(row, "templates_id", src.getTemplatesId());
		stringifyId(row, "item_main_cat_id", src.getItemCategory());

		stringifyNum(row, "price", src.getPrice());
		stringifyNum(row, "cost_price", src.getCostPrice());
		stringifyNum(row, "market_price", src.getMarketPrice());
		stringifyNum(row, "sort", src.getSort());
		stringifyNum(row, "created", src.getCreated());
		stringifyNum(row, "updated", src.getUpdated());
		stringifyNum(row, "audit_date", src.getAuditDate());
		stringifyNum(row, "begin_date", src.getBeginDate());
		stringifyNum(row, "end_date", src.getEndDate());
		stringifyNum(row, "fixed_term", src.getFixedTerm());
		stringifyNum(row, "point", src.getPoint());
		stringifyNum(row, "tax_rate", src.getTaxRate());
		stringifyNum(row, "profit_type", src.getProfitType());
		stringifyNum(row, "origincountry_id", src.getOrigincountryId());
		stringifyNum(row, "taxstrategy_id", src.getTaxstrategyId());
		stringifyNum(row, "taxation_num", src.getTaxationNum());
		stringifyNum(row, "profit_fee", src.getProfitFee());
		stringifyNum(row, "type", src.getType());
		stringifyNum(row, "is_market", src.getIsMarket());
		stringifyNum(row, "is_epidemic", src.getIsEpidemic());
		stringifyNum(row, "is_medicine", src.getIsMedicine());
		stringifyNum(row, "is_prescription", src.getIsPrescription());

		row.put("is_default", boolToBitString(src.getIsDefault()));
		row.put("is_gift", boolToBitString(src.getIsGift()));
		row.put("is_package", boolToBitString(src.getIsPackage()));
		row.put("is_profit", boolToBitString(src.getIsProfit()));
		row.put("is_show_specimg", boolToBitString(src.getIsShowSpecimg()));
		row.put("enable_agreement", boolToBitString(src.getEnableAgreement()));
		row.put("is_point", wireNullableBit(rawNullableColumns != null ? rawNullableColumns.get("is_point") : src.getIsPoint()));

		row.put("sales", wireNullableNumber(rawNullableColumns != null ? rawNullableColumns.get("sales") : src.getSales()));
		row.put("start_num", wireNullableNumber(rawNullableColumns != null ? rawNullableColumns.get("start_num") : src.getStartNum()));

		Object catIds = row.get("item_cat_id");
		if (catIds instanceof List<?> list) {
			List<String> wired = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					wired.add(o.toString());
				}
			}
			row.put("item_cat_id", wired);
		}

		Object mainItemId = row.get("main_item_id");
		if (mainItemId instanceof Number n) {
			row.put("main_item_id", n.longValue() != 0L ? n.toString() : "0");
		} else if (mainItemId == null || !StringUtils.hasText(mainItemId.toString())) {
			row.put("main_item_id", "0");
		}

		if (!row.containsKey("itemName") && row.get("item_name") != null) {
			row.put("itemName", row.get("item_name"));
		}

		if (row.get("rebate") == null) {
			row.put("rebate", "");
		} else {
			row.put("rebate", row.get("rebate").toString());
		}

		if (row.get("weight") != null) {
			row.put("weight", row.get("weight").toString());
		}
	}

	private static void stringifyRowScalars(Map<String, Object> row) {
		stringifyId(row, "item_id", row.get("item_id"));
		stringifyId(row, "goods_id", row.get("goods_id"));
		stringifyId(row, "company_id", row.get("company_id"));
	}

	private static String rawJsonColumn(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		return t.isEmpty() ? null : raw;
	}

	private static boolean parseNospecBoolean(String nospec) {
		if (!StringUtils.hasText(nospec)) {
			return false;
		}
		String s = nospec.trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static String boolToBitString(Boolean value) {
		if (value == null) {
			return "0";
		}
		return Boolean.TRUE.equals(value) ? "1" : "0";
	}

	private static void stringifyId(Map<String, Object> row, String key, Object value) {
		if (value == null) {
			return;
		}
		row.put(key, value.toString());
	}

	private static void stringifyNum(Map<String, Object> row, String key, Object value) {
		if (value == null) {
			row.put(key, null);
			return;
		}
		row.put(key, value.toString());
	}

	private static Object wireNullableNumber(Object value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	private static Object wireNullableBit(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (value instanceof Number n) {
			return n.intValue() != 0 ? "1" : "0";
		}
		String s = value.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return "1".equals(s) || "true".equalsIgnoreCase(s) ? "1" : "0";
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
