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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderListPromoterUserIdLookupPort;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListFilterBuilder;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListPostProcessor;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListQueryExecutor;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListTypeRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderListService {

	private final AdminOrderListFilterBuilder filterBuilder;
	private final AdminOrderListTypeRegistry typeRegistry;
	private final AdminOrderListQueryExecutor queryExecutor;
	private final AdminOrderListPostProcessor postProcessor;
	private final AdminOrderListPromoterUserIdLookupPort promoterUserIdLookupPort;

	public AdminOrderListService(
			AdminOrderListFilterBuilder filterBuilder,
			AdminOrderListTypeRegistry typeRegistry,
			AdminOrderListQueryExecutor queryExecutor,
			AdminOrderListPostProcessor postProcessor,
			AdminOrderListPromoterUserIdLookupPort promoterUserIdLookupPort) {
		this.filterBuilder = filterBuilder;
		this.typeRegistry = typeRegistry;
		this.queryExecutor = queryExecutor;
		this.postProcessor = postProcessor;
		this.promoterUserIdLookupPort = promoterUserIdLookupPort;
	}

	public Map<String, Object> getOrderList(long companyId, Map<String, Object> jwt, HttpServletRequest request) {
		int pageNo =
				Math.max(
						1,
						parseIntDefault(request.getParameter("page"), 1));
		int limit =
				Math.max(
						1,
						parseIntDefault(request.getParameter("pageSize"), 20));

		String orderTypeRaw = request.getParameter("order_type");
		if (orderTypeRaw == null) {
			orderTypeRaw = "";
		}
		String orderClassParam = request.getParameter("order_class");
		if (orderClassParam == null) {
			orderClassParam = "";
		}
		String orderClassForRegistry = orderClassParam;
		if ("point".equals(orderClassParam) || "deposit".equals(orderClassParam)) {
			orderClassForRegistry = "";
		}

		AdminOrderListFilterBuilder.BuiltFilter built = filterBuilder.build(companyId, jwt, request);
		Map<String, Object> filter = built.filter();

		applySalespersonNameFilter(companyId, request, orderTypeRaw, filter);

		boolean createTimeAsc = false;
		if (!orderTypeRaw.isEmpty()) {
			String ob = request.getParameter("order_by");
			createTimeAsc = ob != null && "asc".equalsIgnoreCase(ob.trim());
		}

		AdminOrderListExecutorKind kind;
		if (orderTypeRaw.isEmpty()) {
			kind = AdminOrderListExecutorKind.ASSOCIATION;
		} else {
			Optional<AdminOrderListExecutorKind> k =
					typeRegistry.resolveOrderListDispatch(orderTypeRaw, orderClassForRegistry, filter);
			if (k.isEmpty()) {
				throw new ResourceException("无此类型订单！");
			}
			kind = k.get();
		}

		AdminOrderListQueryExecutor.PageResult page =
				queryExecutor.queryPage(kind, filter, pageNo, limit, createTimeAsc);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", page.list());
		Map<String, Object> pager = new LinkedHashMap<>();
		pager.put("count", page.total());
		pager.put("page_no", pageNo);
		pager.put("page_size", limit);
		data.put("pager", pager);
		data.put("filter", filter);
		data.put("datapass_block", parseDatapassBlockInt(request));

		String orderTypeForPost = orderTypeRaw.isEmpty() ? "" : orderTypeRaw;
		postProcessor.afterQuery(companyId, orderTypeForPost, data);

		return data;
	}

	private void applySalespersonNameFilter(
			long companyId, HttpServletRequest request, String orderTypeRaw, Map<String, Object> filter) {
		String spn = request.getParameter("salespersonname");
		if (!StringUtils.hasText(spn) || !StringUtils.hasText(spn.trim())) {
			return;
		}
		List<Long> ids = promoterUserIdLookupPort.listUserIdsBySalespersonNamePlaintext(companyId, spn.trim());
		if (ids.isEmpty()) {
			return;
		}
		if (!orderTypeRaw.isEmpty()) {
			filter.put("salesman_id|in", ids);
		} else {
			filter.put("promoter_user_id", ids);
		}
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

	private static int parseDatapassBlockInt(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return n.intValue();
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return parseDatapassValue(t);
			}
		}
		return parseDatapassValue(request.getParameter("x-datapass-block"));
	}

	private static int parseDatapassValue(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		String t = raw.trim();
		if ("0".equals(t) || "false".equalsIgnoreCase(t)) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 1;
		}
	}
}
