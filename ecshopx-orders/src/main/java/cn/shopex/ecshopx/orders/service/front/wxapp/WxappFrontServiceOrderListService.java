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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper;
import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper.SourceNameRow;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListQueryExecutor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappFrontServiceOrderListService {

	private static final DateTimeFormatter CREATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final AdminOrderListQueryExecutor adminOrderListQueryExecutor;
	private final AdminOrderListSourcesLookupMapper adminOrderListSourcesLookupMapper;

	public WxappFrontServiceOrderListService(
			AdminOrderListQueryExecutor adminOrderListQueryExecutor,
			AdminOrderListSourcesLookupMapper adminOrderListSourcesLookupMapper) {
		this.adminOrderListQueryExecutor = adminOrderListQueryExecutor;
		this.adminOrderListSourcesLookupMapper = adminOrderListSourcesLookupMapper;
	}

	public Map<String, Object> getOrderList(
			AdminOrderListExecutorKind kind,
			Map<String, Object> filter,
			int page,
			int limit,
			String from,
			boolean needTotal) {
		Map<String, Object> f = new LinkedHashMap<>(filter);
		f.remove("invoice_list");
		f.remove("is_distribution");
		f.putIfAbsent("supplier_id", 0);
		f.put("wxapp_order_list_from", listContextFrom(from));
		AdminOrderListQueryExecutor.PageResult pr =
				adminOrderListQueryExecutor.queryPage(kind, f, page, limit, false);
		attachSourceNames(pr);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", pr.list());
		LinkedHashMap<String, Object> pager = new LinkedHashMap<>();
		pager.put("count", pr.total());
		pager.put("page_no", page);
		pager.put("page_size", limit);
		out.put("pager", pager);
		return out;
	}

	private void attachSourceNames(AdminOrderListQueryExecutor.PageResult pr) {
		if (pr.list() == null || pr.list().isEmpty()) {
			return;
		}
		List<Long> sourceIds =
				pr.list().stream()
						.map(r -> longVal(r.get("source_id")))
						.filter(id -> id > 0L)
						.distinct()
						.toList();
		if (sourceIds.isEmpty()) {
			for (Map<String, Object> row : pr.list()) {
				row.put("source_name", "-");
				row.put("create_date", formatCreateDate(intVal(row.get("create_time"))));
			}
			return;
		}
		Map<Long, String> names = new LinkedHashMap<>();
		for (SourceNameRow sn : adminOrderListSourcesLookupMapper.selectSourceNamesByIds(sourceIds)) {
			if (sn.sourceId() != null) {
				names.put(sn.sourceId(), sn.sourceName() != null ? sn.sourceName() : "-");
			}
		}
		for (Map<String, Object> row : pr.list()) {
			long sid = longVal(row.get("source_id"));
			row.put("source_name", sid > 0 && names.containsKey(sid) ? names.get(sid) : "-");
			row.put("create_date", formatCreateDate(intVal(row.get("create_time"))));
		}
	}

	private static String formatCreateDate(int createTime) {
		if (createTime <= 0) {
			return "";
		}
		return Instant.ofEpochSecond(createTime).atZone(ZoneId.systemDefault()).toLocalDateTime().format(CREATE_FMT);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String listContextFrom(String from) {
		if (from == null || !StringUtils.hasText(from.trim())) {
			return "front_list";
		}
		return from.trim();
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
