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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * D3：商品同步 {@code shuyun.base.product.sync}；单次最多 {@link #BATCH_SIZE} 条。
 * 对齐 PHP {@code ShuyunOpenPlatformProductSyncService}（SKU 规格文案简化为「单规格」优先路径）。
 */
@Service
public class ProductSyncService {

	private static final Logger log = LoggerFactory.getLogger(ProductSyncService.class);
	public static final int BATCH_SIZE = 50;
	public static final String SKU_DETAIL_SINGLE_SPEC = "单规格";
	private static final DateTimeFormatter DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public ProductSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	/** @return true 成功或无待发；false 网关失败 / 租户不合格 */
	public boolean syncProductByDefaultItem(long companyId, long distributorId, long defaultItemId) {
		if (companyId < 1 || distributorId < 1 || defaultItemId < 1) {
			return true;
		}
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		List<Map<String, Object>> variants =
				jdbcTemplate.queryForList(
						"""
						SELECT item_id, default_item_id, goods_id, item_name, item_bn, item_category,
						       approve_status, price, updated, is_gift
						FROM items WHERE company_id=? AND default_item_id=? ORDER BY item_id ASC
						""",
						companyId,
						defaultItemId);
		if (variants.isEmpty()) {
			return true;
		}
		List<Long> variantIds =
				variants.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).toList();
		if (variantIds.isEmpty()) {
			return true;
		}
		String inClause = variantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		List<Map<String, Object>> diRows =
				jdbcTemplate.queryForList(
						"""
						SELECT item_id, price, is_can_sale FROM distribution_distributor_items
						WHERE company_id=? AND distributor_id=? AND item_id IN (%s)
						ORDER BY item_id ASC
						"""
								.formatted(inClause),
						companyId,
						distributorId);
		Map<Long, Map<String, Object>> diByItem = new LinkedHashMap<>();
		for (Map<String, Object> row : diRows) {
			diByItem.put(toLong(row.get("item_id")), row);
		}
		List<Map<String, Object>> distRows =
				jdbcTemplate.queryForList(
						"""
						SELECT distributor_id, distributor_self FROM distribution_distributor
						WHERE company_id=? AND distributor_id=? LIMIT 1
						""",
						companyId,
						distributorId);
		if (distRows.isEmpty()) {
			return true;
		}
		Map<String, Object> dist = distRows.get(0);
		Map<String, Object> product =
				buildProductBody(companyId, distributorId, defaultItemId, variants, diByItem, dist);
		if (product == null) {
			return true;
		}
		return postProducts(companyId, List.of(product), distributorId, defaultItemId);
	}

	private boolean postProducts(
			long companyId, List<Map<String, Object>> products, long distributorId, long defaultItemId) {
		List<Map<String, Object>> valid = new ArrayList<>();
		for (int i = 0; i < products.size(); i++) {
			Map<String, Object> row = products.get(i);
			String err = validateProductRow(row);
			if (err != null) {
				log.info(
						"Shuyun product sync skip invalid row companyId={} index={} reason={} productId={}",
						companyId,
						i,
						err,
						row == null ? null : row.get("product_id"));
				continue;
			}
			valid.add(row);
		}
		if (valid.isEmpty()) {
			return true;
		}
		try {
			for (int from = 0; from < valid.size(); from += BATCH_SIZE) {
				List<Map<String, Object>> chunk =
						valid.subList(from, Math.min(from + BATCH_SIZE, valid.size()));
				gatewayClient.postJson(
						companyId, ShuyunOpenPlatformGatewayActions.PRODUCT_SYNC, chunk, "offline");
			}
			log.info(
					"Shuyun product.sync ok companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun product.sync failed companyId={} distributorId={} defaultItemId={} err={}",
					companyId,
					distributorId,
					defaultItemId,
					e.getMessage());
			return false;
		}
	}

	private Map<String, Object> buildProductBody(
			long companyId,
			long distributorId,
			long defaultItemId,
			List<Map<String, Object>> variants,
			Map<Long, Map<String, Object>> diByItem,
			Map<String, Object> distributorRow) {
		Map<String, Object> main = null;
		for (Map<String, Object> row : variants) {
			if (toLong(row.get("item_id")) == defaultItemId) {
				main = row;
				break;
			}
		}
		if (main == null) {
			main = variants.get(0);
		}
		String categoryId = resolvePrimaryCategoryId(companyId, defaultItemId, main);
		if (!StringUtils.hasText(categoryId) || "0".equals(categoryId)) {
			log.warn(
					"Shuyun product sync skip: missing category_id companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId);
			return null;
		}
		String shopId = resolveShopId(toLong(distributorRow.get("distributor_id")));
		if (!StringUtils.hasText(shopId)) {
			return null;
		}
		boolean virtual = isTruthy(distributorRow.get("distributor_self"));
		List<Long> variantIds =
				variants.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).toList();
		Map<Long, String> skuDetailByItem = loadSkuDetailByItemId(variantIds);
		List<Map<String, Object>> skus = new ArrayList<>();
		for (Map<String, Object> row : variants) {
			long iid = toLong(row.get("item_id"));
			Map<String, Object> di = diByItem.get(iid);
			if (di == null) {
				continue;
			}
			String detail = skuDetailByItem.getOrDefault(iid, SKU_DETAIL_SINGLE_SPEC);
			skus.add(buildSkuRow(row, di, detail, virtual));
		}
		if (skus.isEmpty()) {
			return null;
		}
		boolean gift = isTruthy(main.get("is_gift"));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("shop_id", shopId);
		body.put(
				"product_id",
				ItemProductIdResolver.resolveFromItemRow(
						toLongBoxed(main.get("goods_id")),
						toLongBoxed(main.get("default_item_id")),
						toLongBoxed(main.get("item_id"))));
		body.put("product_name", stringVal(main.get("item_name")));
		body.put("category_id", categoryId);
		body.put("type", gift ? "SY_GIFT" : "SY_NORMAL");
		body.put("modified", formatTs(main.get("updated")));
		body.put("status", mapApproveStatus(stringVal(main.get("approve_status"))));
		body.put("price", TradeSyncService.fenToYuan(toInt(main.get("price"))));
		body.put("skus", skus);
		String bn = stringVal(main.get("item_bn"));
		if (StringUtils.hasText(bn)) {
			body.put("outer_product_id", bn);
		}
		return body;
	}

	private Map<String, Object> buildSkuRow(
			Map<String, Object> itemRow,
			Map<String, Object> diRow,
			String skuDetail,
			boolean virtualDistributor) {
		int priceFen = toInt(diRow.get("price"));
		if (priceFen <= 0) {
			priceFen = toInt(itemRow.get("price"));
		}
		int skuStatus;
		if (virtualDistributor) {
			skuStatus = "onsale".equals(stringVal(itemRow.get("approve_status"))) ? 1 : 0;
		} else {
			skuStatus = isTruthy(diRow.get("is_can_sale")) ? 1 : 0;
		}
		Map<String, Object> sku = new LinkedHashMap<>();
		sku.put("sku_id", String.valueOf(toLong(itemRow.get("item_id"))));
		sku.put(
				"sku_detail",
				StringUtils.hasText(skuDetail) ? skuDetail : SKU_DETAIL_SINGLE_SPEC);
		sku.put("price", TradeSyncService.fenToYuan(priceFen));
		sku.put("status", skuStatus);
		return sku;
	}

	/**
	 * 多规格：规格名:规格值 逗号拼接；仅一维或无规格 → 「单规格」。对齐 PHP
	 * {@code ShuyunOpenPlatformProductSyncService::buildShuyunSkuDetailByItemId}。
	 */
	private Map<Long, String> loadSkuDetailByItemId(List<Long> variantIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (variantIds == null || variantIds.isEmpty()) {
			return out;
		}
		String inClause = variantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT rel.item_id AS item_id,
						       ia.attribute_name AS attribute_name,
						       COALESCE(NULLIF(TRIM(rel.custom_attribute_value), ''),
						                iv.attribuattribute_valuete_name) AS value_name
						FROM items_rel_attributes rel
						JOIN items_attributes ia ON ia.attribute_id = rel.attribute_id
						LEFT JOIN items_attribute_values iv
						  ON iv.attribute_value_id = rel.attribute_value_id
						WHERE rel.attribute_type = 'item_spec'
						  AND rel.item_id IN (%s)
						ORDER BY rel.item_id ASC, rel.attribute_sort ASC, rel.attribute_id ASC
						"""
								.formatted(inClause));
		Map<Long, List<String>> partsByItem = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long iid = toLong(row.get("item_id"));
			String name = stringVal(row.get("attribute_name"));
			String val = stringVal(row.get("value_name"));
			if (!StringUtils.hasText(name) && !StringUtils.hasText(val)) {
				continue;
			}
			partsByItem.computeIfAbsent(iid, k -> new ArrayList<>()).add(name + ":" + val);
		}
		for (Map.Entry<Long, List<String>> e : partsByItem.entrySet()) {
			List<String> parts = e.getValue();
			out.put(
					e.getKey(),
					parts.size() > 1 ? String.join(",", parts) : SKU_DETAIL_SINGLE_SPEC);
		}
		return out;
	}

	private String resolvePrimaryCategoryId(long companyId, long defaultItemId, Map<String, Object> main) {
		List<Map<String, Object>> rels =
				jdbcTemplate.queryForList(
						"""
						SELECT category_id FROM items_rel_cats
						WHERE company_id=? AND item_id=? ORDER BY category_id ASC
						""",
						companyId,
						defaultItemId);
		for (Map<String, Object> rel : rels) {
			long cid = toLong(rel.get("category_id"));
			if (cid < 1) {
				continue;
			}
			Integer level =
					jdbcTemplate.query(
							"""
							SELECT category_level FROM items_category
							WHERE company_id=? AND category_id=? LIMIT 1
							""",
							rs -> rs.next() ? rs.getInt(1) : null,
							companyId,
							cid);
			if (level != null && level == 3) {
				return String.valueOf(cid);
			}
		}
		String raw = stringVal(main.get("item_category"));
		if (StringUtils.hasText(raw) && raw.chars().allMatch(Character::isDigit)) {
			return String.valueOf(Long.parseLong(raw));
		}
		return "";
	}

	private String resolveShopId(long distributorId) {
		if (distributorId < 1) {
			return "";
		}
		String suffix =
				properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}

	static String validateProductRow(Map<String, Object> row) {
		if (row == null) {
			return "row_null";
		}
		if (!StringUtils.hasText(stringVal(row.get("shop_id")))) {
			return "missing_shop_id";
		}
		if (!StringUtils.hasText(stringVal(row.get("product_id")))) {
			return "missing_product_id";
		}
		if (!StringUtils.hasText(stringVal(row.get("product_name")))) {
			return "missing_product_name";
		}
		String cat = stringVal(row.get("category_id"));
		if (!StringUtils.hasText(cat) || "0".equals(cat)) {
			return "missing_category_id";
		}
		if (!StringUtils.hasText(stringVal(row.get("modified")))) {
			return "missing_modified";
		}
		String st = stringVal(row.get("status"));
		if (!StringUtils.hasText(st) || !st.startsWith("SY_")) {
			return "missing_or_invalid_status";
		}
		Object price = row.get("price");
		if (!(price instanceof Number)) {
			return "missing_or_invalid_price";
		}
		Object skus = row.get("skus");
		if (!(skus instanceof List<?> list) || list.isEmpty()) {
			return "missing_skus";
		}
		for (Object sku : list) {
			if (!(sku instanceof Map<?, ?> m)) {
				return "invalid_sku_row";
			}
			if (!StringUtils.hasText(stringVal(m.get("sku_id")))) {
				return "missing_sku_id";
			}
		}
		return null;
	}

	private static String mapApproveStatus(String approveStatus) {
		return switch (approveStatus) {
			case "onsale", "only_show" -> "SY_ONLINE";
			default -> "SY_OFFLINE";
		};
	}

	private static String formatTs(Object v) {
		int ts = toInt(v);
		long epoch = ts > 0 ? ts : Instant.now().getEpochSecond();
		return DT.format(Instant.ofEpochSecond(epoch));
	}

	private static boolean isTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static Long toLongBoxed(Object v) {
		long n = toLong(v);
		return n == 0L ? null : n;
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(stringVal(v));
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(stringVal(v));
		} catch (Exception e) {
			return 0;
		}
	}
}
