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

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthNameMapService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceListService {

	private static final int MAX_PAGE_SIZE = 200;

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceItemMapper orderInvoiceItemMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final MembersMapper membersMapper;
	private final RegionauthNameMapService regionauthNameMapService;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;

	public OrderInvoiceListService(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceItemMapper orderInvoiceItemMapper,
			NormalOrdersMapper normalOrdersMapper,
			MembersMapper membersMapper,
			RegionauthNameMapService regionauthNameMapService,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceItemMapper = orderInvoiceItemMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.membersMapper = membersMapper;
		this.regionauthNameMapService = regionauthNameMapService;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
	}

	public Map<String, Object> getInvoiceList(long companyId, HttpServletRequest request) {
		int page = parseIntDefault(request.getParameter("page"), 1);
		int pageSize = parseIntDefault(request.getParameter("pageSize"), 20);
		if (page < 1) {
			page = 1;
		}
		if (pageSize < 1) {
			pageSize = 20;
		}
		pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

		LambdaQueryWrapper<OrderInvoice> base = buildFilterWrapper(companyId, request);
		return executeInvoiceListQuery(companyId, base, page, pageSize);
	}

	public Map<String, Object> getUserInvoiceList(long companyId, long userId, HttpServletRequest request) {
		int page = parseIntDefault(request.getParameter("page"), 1);
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseIntDefault(request.getParameter("page_size"), 10);
		if (pageSize < 1) {
			pageSize = 10;
		}
		pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

		LambdaQueryWrapper<OrderInvoice> base = buildWxappMemberInvoiceListWrapper(companyId, userId, request);
		return executeInvoiceListQuery(companyId, base, page, pageSize);
	}

	private Map<String, Object> executeInvoiceListQuery(
			long companyId, LambdaQueryWrapper<OrderInvoice> base, int page, int pageSize) {
		long totalCount = orderInvoiceMapper.selectCount(base);

		List<OrderInvoice> records;
		if (totalCount == 0L) {
			records = Collections.emptyList();
		} else {
			Page<OrderInvoice> p = new Page<>(page, pageSize);
			p.setSearchCount(false);
			orderInvoiceMapper.selectPage(p, base);
			records = p.getRecords();
		}

		List<Map<String, Object>> listRows = new ArrayList<>(records.size());
		for (OrderInvoice inv : records) {
			listRows.add(OrderInvoiceApiRowSupport.toColumnNamesData(inv));
		}

		if (!records.isEmpty()) {
			enrichListRows(companyId, records, listRows);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", listRows);
		out.put("total_count", totalCount);
		return out;
	}

	private void enrichListRows(
			long companyId, List<OrderInvoice> records, List<Map<String, Object>> listRows) {
		List<Long> invoiceIds = new ArrayList<>(records.size());
		for (OrderInvoice inv : records) {
			if (inv.getId() != null) {
				invoiceIds.add(inv.getId());
			}
		}

		OrderInvoice first = records.get(0);
		long itemQueryCompanyId = first.getCompanyId() == null ? 0L : first.getCompanyId();

		Map<Long, List<Map<String, Object>>> itemsByInvoiceId = new LinkedHashMap<>();
		if (!invoiceIds.isEmpty()) {
			List<OrderInvoiceItem> itemEntities =
					orderInvoiceItemMapper.selectList(
							new LambdaQueryWrapper<OrderInvoiceItem>()
									.in(OrderInvoiceItem::getInvoiceId, invoiceIds)
									.eq(OrderInvoiceItem::getCompanyId, itemQueryCompanyId)
									.orderByAsc(OrderInvoiceItem::getId));
			for (OrderInvoiceItem ent : itemEntities) {
				Long iid = ent.getInvoiceId();
				if (iid == null) {
					continue;
				}
				itemsByInvoiceId
						.computeIfAbsent(iid, k -> new ArrayList<>())
						.add(OrderInvoiceItemApiRowSupport.toRow(ent));
			}
		}

		Set<Long> orderPkIds = new LinkedHashSet<>();
		for (OrderInvoice inv : records) {
			collectParsedOrderIds(inv.getOrderId(), orderPkIds);
		}
		Map<Long, NormalOrders> orderById = new LinkedHashMap<>();
		if (!orderPkIds.isEmpty()) {
			for (NormalOrders no : normalOrdersMapper.selectBatchIds(orderPkIds)) {
				if (no != null && no.getOrderId() != null) {
					orderById.put(no.getOrderId(), no);
				}
			}
		}

		Set<Long> userIds = new LinkedHashSet<>();
		for (OrderInvoice inv : records) {
			if (inv.getUserId() != null) {
				userIds.add(inv.getUserId());
			}
		}
		Map<Long, Members> memberById = new LinkedHashMap<>();
		if (!userIds.isEmpty()) {
			for (Members m : membersMapper.selectBatchIds(userIds)) {
				if (m != null && m.getUserId() != null) {
					memberById.put(m.getUserId(), m);
				}
			}
		}

		Set<Long> regionauthIds = new LinkedHashSet<>();
		for (OrderInvoice inv : records) {
			if (inv.getRegionauthId() != null) {
				regionauthIds.add(inv.getRegionauthId());
			}
		}
		Map<Long, String> regionauthNameById =
				regionauthNameMapService.mapRegionauthIdToName(companyId, regionauthIds);

		Set<Long> distributorIds = new LinkedHashSet<>();
		for (OrderInvoice inv : records) {
			Long did = parseDistributorIdLong(inv.getOrderShopId());
			if (did != null) {
				distributorIds.add(did);
			}
		}
		Map<Long, String> distributorNameById = new LinkedHashMap<>();
		if (!distributorIds.isEmpty()) {
			List<Long> idList = new ArrayList<>(distributorIds);
			List<Map<String, Object>> distRows =
					distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(
							companyId, idList);
			for (Map<String, Object> row : distRows) {
				Long did = longFromCell(row.get("distributor_id"));
				if (did != null) {
					Object nameObj = row.get("name");
					distributorNameById.put(did, nameObj == null ? "" : String.valueOf(nameObj));
				}
			}
		}

		for (int i = 0; i < records.size(); i++) {
			OrderInvoice inv = records.get(i);
			Map<String, Object> row = listRows.get(i);
			Long invId = inv.getId();
			List<Map<String, Object>> items =
					invId == null
							? Collections.emptyList()
							: itemsByInvoiceId.getOrDefault(invId, Collections.emptyList());
			row.put("invoice_items", items);

			Long rid = inv.getRegionauthId();
			row.put(
					"regionauth_name",
					rid == null ? "" : regionauthNameById.getOrDefault(rid, ""));

			Members m = inv.getUserId() == null ? null : memberById.get(inv.getUserId());
			row.put("user_mobile", m == null || m.getMobile() == null ? "" : m.getMobile());
			row.put(
					"user_card_code",
					m == null || m.getUserCardCode() == null ? "" : m.getUserCardCode());

			row.put(
					"distributor_name",
					resolveDistributorNameForList(inv.getOrderShopId(), distributorNameById));

			Long mainPk = firstParsedOrderPk(inv.getOrderId());
			String holder = "";
			if (mainPk != null) {
				NormalOrders no = orderById.get(mainPk);
				if (no != null && no.getOrderHolder() != null) {
					holder = no.getOrderHolder();
				}
			}
			row.put("order_holder", holder);
		}
	}

	/**
	 * Matches list enrichment: distributor name from batch map when present; otherwise {@code 0} /
	 * empty shop id → 「自营」, else 「-」.
	 */
	private static String resolveDistributorNameForList(
			String orderShopId, Map<Long, String> distributorNameById) {
		Long did = parseDistributorIdLong(orderShopId);
		if (did != null && distributorNameById.containsKey(did)) {
			String n = distributorNameById.get(did);
			return n == null ? "" : n;
		}
		if (isSelfOperatedOrderShopId(orderShopId, did)) {
			return "自营";
		}
		return "-";
	}

	private static boolean isSelfOperatedOrderShopId(String rawOrderShopId, Long parsedDistributorId) {
		if (rawOrderShopId == null || rawOrderShopId.trim().isEmpty()) {
			return true;
		}
		return parsedDistributorId != null && parsedDistributorId == 0L;
	}

	private static Long parseDistributorIdLong(String orderShopId) {
		if (orderShopId == null) {
			return null;
		}
		String t = orderShopId.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long longFromCell(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void collectParsedOrderIds(String orderIdField, Set<Long> out) {
		if (orderIdField == null) {
			return;
		}
		for (String part : orderIdField.split(",")) {
			if (part == null) {
				continue;
			}
			String t = part.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
	}

	private static Long firstParsedOrderPk(String orderIdField) {
		if (orderIdField == null) {
			return null;
		}
		for (String part : orderIdField.split(",")) {
			if (part == null) {
				continue;
			}
			String t = part.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException ignored) {
				// try next segment
			}
		}
		return null;
	}

	private LambdaQueryWrapper<OrderInvoice> buildWxappMemberInvoiceListWrapper(
			long companyId, long userId, HttpServletRequest request) {
		LambdaQueryWrapper<OrderInvoice> w = new LambdaQueryWrapper<>();
		w.eq(OrderInvoice::getCompanyId, companyId);
		w.eq(OrderInvoice::getUserId, userId);

		if (request.getParameterMap().containsKey("regionauth_id")) {
			String raw = request.getParameter("regionauth_id");
			String v = raw == null ? "" : raw.trim();
			try {
				w.eq(OrderInvoice::getRegionauthId, Long.parseLong(v));
			} catch (NumberFormatException e) {
				w.apply("regionauth_id = {0}", v);
			}
		}

		w.orderByDesc(OrderInvoice::getId);
		return w;
	}

	private LambdaQueryWrapper<OrderInvoice> buildFilterWrapper(long companyId, HttpServletRequest request) {
		LambdaQueryWrapper<OrderInvoice> w = new LambdaQueryWrapper<>();
		w.eq(OrderInvoice::getCompanyId, companyId);

		if (isFilledQueryParam(request, "regionauth_id")) {
			String v = request.getParameter("regionauth_id").trim();
			try {
				w.eq(OrderInvoice::getRegionauthId, Long.parseLong(v));
			} catch (NumberFormatException e) {
				w.apply("regionauth_id = {0}", v);
			}
		}
		if (isFilledQueryParam(request, "order_id")) {
			String v = request.getParameter("order_id").trim();
			w.like(OrderInvoice::getOrderId, "%" + escapeLike(v) + "%");
		}
		if (isFilledQueryParam(request, "mobile")) {
			w.eq(OrderInvoice::getMobile, request.getParameter("mobile").trim());
		}
		if (isFilledQueryParam(request, "invoice_type_code")) {
			w.eq(OrderInvoice::getInvoiceTypeCode, request.getParameter("invoice_type_code").trim());
		}
		if (isFilledQueryParam(request, "invoice_apply_bn")) {
			w.eq(OrderInvoice::getInvoiceApplyBn, request.getParameter("invoice_apply_bn").trim());
		}
		if (isFilledQueryParam(request, "company_title")) {
			String v = request.getParameter("company_title").trim();
			w.like(OrderInvoice::getCompanyTitle, "%" + escapeLike(v) + "%");
		}
		if (isFilledQueryParam(request, "invoice_source")) {
			w.eq(OrderInvoice::getInvoiceSource, request.getParameter("invoice_source").trim());
		}
		if (isFilledQueryParam(request, "invoice_status")) {
			w.eq(OrderInvoice::getInvoiceStatus, request.getParameter("invoice_status").trim());
		}
		if (isFilledQueryParam(request, "start_time")) {
			String v = request.getParameter("start_time").trim();
			w.apply("create_time >= {0}", v);
		}
		if (isFilledQueryParam(request, "end_time")) {
			String v = request.getParameter("end_time").trim();
			w.apply("create_time <= {0}", v);
		}
		if (isFilledQueryParam(request, "distributor_id")) {
			w.eq(OrderInvoice::getOrderShopId, request.getParameter("distributor_id").trim());
		}
		if (isFilledQueryParam(request, "user_card_code")) {
			w.eq(OrderInvoice::getUserCardCode, request.getParameter("user_card_code").trim());
		}
		if (isFilledQueryParam(request, "email")) {
			w.eq(OrderInvoice::getEmail, request.getParameter("email").trim());
		}

		w.orderByDesc(OrderInvoice::getId);
		return w;
	}

	private static boolean isFilledQueryParam(HttpServletRequest request, String name) {
		String raw = request.getParameter(name);
		String v = raw == null ? null : raw.trim();
		return v != null && !v.isEmpty();
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

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
