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

package cn.shopex.ecshopx.orders.service.financial.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FinancialSalesreportCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final int PAGE_SIZE = 500;
	private static final Pattern ORDER_ID_NUMERIC = Pattern.compile("^[0-9]+$");

	private static final String SQL_ITEM_META = """
			SELECT i.item_id AS item_id,
				a.attribute_id AS brand_attr_id,
				a.attribute_name AS brand_name,
				i.item_category AS item_cat_raw,
				c.category_name AS main_cat_name
			FROM items i
			LEFT JOIN items_rel_attributes ra
				ON ra.item_id = i.item_id AND ra.company_id = i.company_id AND ra.attribute_type = 'brand'
			LEFT JOIN items_attributes a
				ON a.attribute_id = ra.attribute_id AND a.company_id = ra.company_id AND a.attribute_type = 'brand'
			LEFT JOIN items_category c
				ON c.company_id = i.company_id
				AND c.category_id = CAST(NULLIF(TRIM(i.item_category), '') AS UNSIGNED)
				AND c.is_main_category = 1
			WHERE i.company_id = :companyId AND i.item_id IN (:itemIds)
			""";

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final NamedParameterJdbcTemplate jdbc;

	public FinancialSalesreportCsvExportService(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersMapper normalOrdersMapper,
			ExportCsvFileService exportCsvFileService,
			NamedParameterJdbcTemplate jdbc) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.jdbc = jdbc;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long operatorId) {
		Object cid = filter.get("company_id");
		if (cid == null) {
			return Optional.empty();
		}
		long companyId = toLong(cid);

		LambdaQueryWrapper<NormalOrdersItems> base = FinancialSalesreportQuerySupport.toWrapper(filter);
		base.orderByAsc(NormalOrdersItems::getOrderId).orderByAsc(NormalOrdersItems::getId);

		List<NormalOrdersItems> all = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			Page<NormalOrdersItems> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<NormalOrdersItems> result = normalOrdersItemsMapper.selectPage(page, base);
			List<NormalOrdersItems> records = result.getRecords();
			if (records == null || records.isEmpty()) {
				break;
			}
			all.addAll(records);
			if (records.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (all.isEmpty()) {
			return Optional.empty();
		}

		List<Long> orderIds =
				all.stream().map(NormalOrdersItems::getOrderId).distinct().toList();
		Map<Long, NormalOrders> ordersById = loadOrders(orderIds);

		List<Long> itemIds = all.stream().map(NormalOrdersItems::getItemId).distinct().toList();
		Map<Long, ItemRowMeta> metaByItem = loadItemMeta(companyId, itemIds);

		LinkedHashMap<Long, LinkedHashMap<String, BucketAgg>> byOrder = new LinkedHashMap<>();
		for (NormalOrdersItems row : all) {
			long oid = row.getOrderId();
			ItemRowMeta meta = metaByItem.getOrDefault(row.getItemId(), ItemRowMeta.empty(row.getItemId()));
			String bucketKey = meta.bucketKey(row.getDeliveryTime());
			LinkedHashMap<String, BucketAgg> buckets = byOrder.computeIfAbsent(oid, k -> new LinkedHashMap<>());
			buckets.compute(bucketKey, (k, existing) -> {
				if (existing == null) {
					return BucketAgg.first(row, meta);
				}
				existing.add(row);
				return existing;
			});
		}

		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("barnd", "品牌");
		title.put("main_category", "商品品类");
		title.put("create_time", "下单日期");
		title.put("delivery_time", "发货日期");
		title.put("item_fee", "商品价格");
		title.put("discount_fee", "折扣金额");
		title.put("total_fee", "折后金额");

		List<Map<String, String>> rows = new ArrayList<>();
		for (Map.Entry<Long, LinkedHashMap<String, BucketAgg>> oe : byOrder.entrySet()) {
			long oid = oe.getKey();
			String orderIdStr = String.valueOf(oid);
			for (BucketAgg b : oe.getValue().values()) {
				rows.add(formatDataRow(orderIdStr, b));
			}
			rows.add(formatFreightRow(orderIdStr, ordersById.get(oid)));
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_财务销售报表";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Map<Long, NormalOrders> loadOrders(List<Long> orderIds) {
		Map<Long, NormalOrders> m = new HashMap<>();
		final int chunk = 500;
		for (int i = 0; i < orderIds.size(); i += chunk) {
			List<Long> sub = orderIds.subList(i, Math.min(i + chunk, orderIds.size()));
			List<NormalOrders> list =
					normalOrdersMapper.selectList(new LambdaQueryWrapper<NormalOrders>().in(NormalOrders::getOrderId, sub));
			for (NormalOrders o : list) {
				m.put(o.getOrderId(), o);
			}
		}
		return m;
	}

	private Map<Long, ItemRowMeta> loadItemMeta(long companyId, List<Long> itemIds) {
		Map<Long, ItemRowMeta> out = new HashMap<>();
		final int chunk = 500;
		for (int i = 0; i < itemIds.size(); i += chunk) {
			List<Long> sub = itemIds.subList(i, Math.min(i + chunk, itemIds.size()));
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("companyId", companyId);
			p.addValue("itemIds", sub);
			jdbc.query(
					SQL_ITEM_META,
					p,
					rs -> {
						long iid = rs.getLong("item_id");
						Long bid = rs.getObject("brand_attr_id") == null ? null : rs.getLong("brand_attr_id");
						String bn = rs.getString("brand_name");
						String ic = rs.getString("item_cat_raw");
						String mc = rs.getString("main_cat_name");
						out.put(
								iid,
								new ItemRowMeta(
										iid,
										bid,
										bn != null ? bn : "",
										ic != null ? ic : "",
										mc != null ? mc : ""));
					});
		}
		return out;
	}

	private static Map<String, String> formatDataRow(String orderIdStr, BucketAgg b) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderIdCell(orderIdStr));
		row.put("barnd", b.barnd == null ? "" : b.barnd);
		row.put("main_category", b.mainCategory == null ? "" : b.mainCategory);
		row.put("create_time", formatEpoch(b.createTime));
		row.put("delivery_time", formatEpoch(b.deliveryTime));
		row.put("item_fee", centsToYuanTruthy(b.itemFeeSum));
		row.put("discount_fee", centsToYuanTruthy(b.discountFeeSum));
		row.put("total_fee", centsToYuanTruthy(b.totalFeeSum));
		return row;
	}

	private static Map<String, String> formatFreightRow(String orderIdStr, NormalOrders ord) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		Integer freight = ord != null ? ord.getFreightFee() : null;
		Integer ct = ord != null ? ord.getCreateTime() : null;
		row.put("order_id", formatOrderIdCell(orderIdStr));
		row.put("barnd", "");
		row.put("main_category", "运费");
		row.put("create_time", formatEpoch(ct));
		row.put("delivery_time", "");
		row.put("item_fee", "0");
		row.put("discount_fee", "0");
		row.put("total_fee", centsToYuanTruthy(freight == null ? 0 : freight));
		return row;
	}

	private static String formatOrderIdCell(String orderIdStr) {
		if (ORDER_ID_NUMERIC.matcher(orderIdStr).matches()) {
			return "\t" + orderIdStr;
		}
		return orderIdStr;
	}

	private static String formatEpoch(Integer epoch) {
		if (epoch == null || epoch == 0) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI).format(CSV_TIME);
	}

	private static String centsToYuanTruthy(int cents) {
		if (cents == 0) {
			return "0";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long toLong(Object val) {
		if (val instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(val).trim());
	}

	private record ItemRowMeta(
			long itemId, Long brandAttrId, String brandName, String itemCategoryRaw, String mainCategoryName) {

		static ItemRowMeta empty(long itemId) {
			return new ItemRowMeta(itemId, null, "", "", "");
		}

		String bucketKey(Integer deliveryTime) {
			String a = brandAttrId == null ? "" : String.valueOf(brandAttrId.longValue());
			String c = itemCategoryRaw == null ? "" : itemCategoryRaw;
			String d = deliveryTime == null ? "" : String.valueOf(deliveryTime);
			return a + "_" + c + "_" + d;
		}
	}

	private static final class BucketAgg {
		private String barnd;
		private String mainCategory;
		private Integer createTime;
		private Integer deliveryTime;
		private int itemFeeSum;
		private int discountFeeSum;
		private int totalFeeSum;

		static BucketAgg first(NormalOrdersItems row, ItemRowMeta meta) {
			BucketAgg b = new BucketAgg();
			b.barnd = meta.brandName();
			b.mainCategory = meta.mainCategoryName();
			b.createTime = row.getCreateTime();
			b.deliveryTime = row.getDeliveryTime();
			b.itemFeeSum = nzInt(row.getItemFee());
			b.discountFeeSum = nzInt(row.getDiscountFee());
			b.totalFeeSum = nzInt(row.getTotalFee());
			return b;
		}

		void add(NormalOrdersItems row) {
			itemFeeSum += nzInt(row.getItemFee());
			discountFeeSum += nzInt(row.getDiscountFee());
			totalFeeSum += nzInt(row.getTotalFee());
		}
	}

	private static int nzInt(Integer v) {
		return v == null ? 0 : v;
	}
}
