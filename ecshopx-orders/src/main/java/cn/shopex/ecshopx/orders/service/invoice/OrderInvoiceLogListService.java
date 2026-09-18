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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceLogListService {

	private static final int MAX_PAGE_SIZE = 200;

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;

	public OrderInvoiceLogListService(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceLogMapper orderInvoiceLogMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
	}

	public Map<String, Object> getInvoiceLogList(long companyId, HttpServletRequest request) {
		String rawInvoiceId = request.getParameter("invoice_id");
		String v = rawInvoiceId == null ? null : rawInvoiceId.trim();
		if (v == null || v.isEmpty()) {
			return emptyPayload();
		}
		final long invoicePk;
		try {
			invoicePk = Long.parseLong(v);
		} catch (NumberFormatException e) {
			return emptyPayload();
		}

		OrderInvoice inv = orderInvoiceMapper.selectOne(
				new LambdaQueryWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoicePk)
						.eq(OrderInvoice::getCompanyId, companyId)
						.last("LIMIT 1"));
		if (inv == null) {
			return emptyPayload();
		}

		String pageStr = request.getParameter("page");
		String pageSizeStr = request.getParameter("page_size");
		int page = parseIntDefault(pageStr, 1);
		int pageSize = parseIntDefault(pageSizeStr, 20);
		if (page < 1) {
			page = 1;
		}
		if (pageSize < 1) {
			pageSize = 20;
		}
		pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

		LambdaQueryWrapper<OrderInvoiceLog> logBase =
				new LambdaQueryWrapper<OrderInvoiceLog>().eq(OrderInvoiceLog::getInvoiceId, invoicePk);
		long totalCount = orderInvoiceLogMapper.selectCount(logBase);

		List<Map<String, Object>> listRows;
		if (totalCount == 0L) {
			listRows = Collections.emptyList();
		} else {
			Page<OrderInvoiceLog> p = new Page<>(page, pageSize);
			p.setSearchCount(false);
			logBase.orderByDesc(OrderInvoiceLog::getId);
			orderInvoiceLogMapper.selectPage(p, logBase);
			List<OrderInvoiceLog> records = p.getRecords();
			listRows = new ArrayList<>(records.size());
			for (OrderInvoiceLog row : records) {
				listRows.add(toRowMap(row));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", listRows);
		out.put("total_count", totalCount);
		return out;
	}

	private Map<String, Object> toRowMap(OrderInvoiceLog row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("invoice_id", row.getInvoiceId());
		m.put("operator_type", row.getOperatorType());
		m.put("user_id", row.getUserId());
		m.put("operator_id", row.getOperatorId());
		m.put("operator_content", operatorContentForResponse(row.getOperatorContent()));
		m.put("create_time", row.getCreateTime());
		m.put("update_time", row.getUpdateTime());
		return m;
	}

	private static String operatorContentForResponse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw;
	}

	private static Map<String, Object> emptyPayload() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", Collections.emptyList());
		out.put("total_count", 0L);
		return out;
	}

	private static int parseIntDefault(String s, int dflt) {
		if (s == null || s.isBlank()) {
			return dflt;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}
}
