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

package cn.shopex.ecshopx.employeepurchase.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemListService;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 对齐 PHP {@code EmployeePurchaseActivityItemsExportService}。
 */
@Service
public class EmployeePurchaseActivityItemsCsvExportService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final String EXPORT_TYPE = "employee_purchase_activity_items";
	private static final int PAGE_SIZE = 100;

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("item_name", "商品标题");
		TITLE_HEADERS.put("goods_bn", "SPU编码");
		TITLE_HEADERS.put("item_bn", "SKU编码");
		TITLE_HEADERS.put("activity_price", "活动价格");
		TITLE_HEADERS.put("activity_store", "活动库存");
		TITLE_HEADERS.put("limit_num", "限购数量");
		TITLE_HEADERS.put("limit_fee", "限购金额");
		TITLE_HEADERS.put("shelf_status", "状态");
		TITLE_HEADERS.put("sort", "排序");
	}

	private final ActivitiesMapper activitiesMapper;
	private final EmployeePurchaseActivityItemListService activityItemListService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public EmployeePurchaseActivityItemsCsvExportService(
			ActivitiesMapper activitiesMapper,
			EmployeePurchaseActivityItemListService activityItemListService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.activitiesMapper = activitiesMapper;
		this.activityItemListService = activityItemListService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void validateExportable(ActivityItemsExportQuery query) {
		requireActivity(query);
		Map<String, Object> page = loadPage(query, 1, 1);
		if (toLong(page.get("total_count")) <= 0L) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
	}

	public void runExport(ActivityItemsExportQuery query) {
		requireActivity(query);
		List<Map<String, String>> csvRows = new ArrayList<>();
		int page = 1;
		long totalCount;
		do {
			Map<String, Object> result = loadPage(query, page, PAGE_SIZE);
			totalCount = toLong(result.get("total_count"));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list =
					result.get("list") instanceof List<?> raw ? (List<Map<String, Object>>) raw : List.of();
			for (Map<String, Object> goods : list) {
				csvRows.addAll(flattenGoodsRows(goods, query.itemIds()));
			}
			page++;
		} while ((long) (page - 1) * PAGE_SIZE < totalCount);

		if (csvRows.isEmpty()) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + "_activity_" + query.activityId() + "_items";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}
		exportLogCreateService.createFinishLog(
				query.companyId(),
				query.operatorId(),
				EXPORT_TYPE,
				uploaded.get("filename"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}

	private Map<String, Object> loadPage(ActivityItemsExportQuery query, int page, int pageSize) {
		return activityItemListService.getActivityItemList(
				query.companyId(),
				query.distributorId(),
				query.activityId(),
				page,
				pageSize,
				query.mainCatId(),
				query.category(),
				query.itemName(),
				query.itemBn(),
				query.shelfStatus());
	}

	private Activities requireActivity(ActivityItemsExportQuery query) {
		var wrapper =
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, query.companyId())
						.eq(Activities::getId, query.activityId());
		if (query.distributorScope() != null) {
			wrapper.eq(Activities::getDistributorId, query.distributorScope());
		}
		Activities activity = activitiesMapper.selectOne(wrapper.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		return activity;
	}

	@SuppressWarnings("unchecked")
	static List<Map<String, String>> flattenGoodsRows(Map<String, Object> goods, List<Long> itemIdsFilter) {
		List<Map<String, Object>> items;
		Object spec = goods.get("spec_items");
		if (spec instanceof List<?> list && !list.isEmpty()) {
			items = (List<Map<String, Object>>) list;
		} else {
			items = List.of(goods);
		}
		Set<Long> allow = toAllowSet(itemIdsFilter);
		List<Map<String, String>> rows = new ArrayList<>(items.size());
		for (Map<String, Object> item : items) {
			if (!allow.isEmpty() && !allow.contains(toLong(item.get("item_id")))) {
				continue;
			}
			rows.add(toCsvRow(item));
		}
		return rows;
	}

	private static Set<Long> toAllowSet(List<Long> itemIdsFilter) {
		if (itemIdsFilter == null || itemIdsFilter.isEmpty()) {
			return Set.of();
		}
		Set<Long> allow = new HashSet<>();
		for (Long id : itemIdsFilter) {
			if (id != null && id > 0L) {
				allow.add(id);
			}
		}
		return allow;
	}

	private static Map<String, String> toCsvRow(Map<String, Object> item) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("item_name", stringVal(item.get("item_name")));
		row.put("goods_bn", stringVal(item.get("goods_bn")));
		row.put("item_bn", stringVal(item.get("item_bn")));
		row.put("activity_price", fenToYuan(item.get("activity_price")));
		row.put("activity_store", Integer.toString(toInt(item.get("activity_store"))));
		row.put("limit_num", Integer.toString(toInt(item.get("limit_num"))));
		row.put("limit_fee", fenToYuan(item.get("limit_fee")));
		row.put("shelf_status", toInt(item.get("shelf_status"), 1) == 1 ? "上架" : "下架");
		row.put("sort", Integer.toString(toInt(item.get("sort"))));
		return row;
	}

	private static String fenToYuan(Object raw) {
		long fen = toLong(raw);
		return BigDecimal.valueOf(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString();
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return new BigDecimal(raw.toString().trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object raw) {
		return toInt(raw, 0);
	}

	private static int toInt(Object raw, int defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(raw.toString().trim()).intValue();
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
