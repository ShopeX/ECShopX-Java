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

package cn.shopex.ecshopx.community.service.front;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.community.service.CommunityOrderListRowEnricher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderListPageResult;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityFrontOrderListService {

	private static final List<String> STATS_FILTER_REMOVED_KEYS =
			List.of(
					"order_status",
					"ziti_status",
					"delivery_status",
					"is_rate",
					"order_status|in",
					"auto_cancel_time|gt");

	private final CommunityFrontOrderListFilterBuilder communityFrontOrderListFilterBuilder;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final AdminNormalOrderListService adminNormalOrderListService;
	private final CommunityOrderListRowEnricher communityOrderListRowEnricher;
	private final AftersalesRefundService aftersalesRefundService;
	private final AftersalesDetailAggregateService aftersalesDetailAggregateService;

	public CommunityFrontOrderListService(
			CommunityFrontOrderListFilterBuilder communityFrontOrderListFilterBuilder,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			AdminNormalOrderListService adminNormalOrderListService,
			CommunityOrderListRowEnricher communityOrderListRowEnricher,
			AftersalesRefundService aftersalesRefundService,
			AftersalesDetailAggregateService aftersalesDetailAggregateService) {
		this.communityFrontOrderListFilterBuilder = communityFrontOrderListFilterBuilder;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.adminNormalOrderListService = adminNormalOrderListService;
		this.communityOrderListRowEnricher = communityOrderListRowEnricher;
		this.aftersalesRefundService = aftersalesRefundService;
		this.aftersalesDetailAggregateService = aftersalesDetailAggregateService;
	}

	public Map<String, Object> loadOrderList(
			long companyId, long userId, long chiefIdFromJwt, HttpServletRequest request) {
		int pageNo = intParam(request, "page", 1);
		int pageSize = intParam(request, "pageSize", 20);
		boolean chiefBranch = isSellerLoose(request) && chiefIdFromJwt > 0L;

		if (chiefBranch) {
			return loadChiefBranch(companyId, chiefIdFromJwt, request, pageNo, pageSize);
		}
		return loadMemberBranch(companyId, userId, request, pageNo, pageSize);
	}

	private Map<String, Object> loadChiefBranch(
			long companyId, long chiefId, HttpServletRequest request, int pageNo, int pageSize) {
		LambdaQueryWrapper<CommunityOrderRelActivity> rw =
				new LambdaQueryWrapper<CommunityOrderRelActivity>()
						.eq(CommunityOrderRelActivity::getCompanyId, companyId)
						.eq(CommunityOrderRelActivity::getChiefId, chiefId);
		String activityIdParam = request.getParameter("activity_id");
		if (StringUtils.hasText(activityIdParam)) {
			long aid = longLoose(activityIdParam.trim());
			if (aid > 0L) {
				rw.eq(CommunityOrderRelActivity::getActivityId, aid);
			}
		}
		List<CommunityOrderRelActivity> rels = communityOrderRelActivityMapper.selectList(rw);
		if (rels.isEmpty()) {
			return emptyChiefResponse(pageNo, pageSize);
		}

		List<Long> orderIds =
				rels.stream()
						.map(CommunityOrderRelActivity::getOrderId)
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();
		if (orderIds.isEmpty()) {
			return emptyChiefResponse(pageNo, pageSize);
		}

		Map<String, Object> filter =
				communityFrontOrderListFilterBuilder.build(companyId, 0L, request, true);
		filter.put("order_id|in", orderIds);

		AdminNormalOrderListPageResult pageResult =
				adminNormalOrderListService.pageOrders(filter, pageNo, pageSize, false);
		List<Map<String, Object>> rows = pageResult.list();
		communityOrderListRowEnricher.enrichForAdminAndFront(companyId, rows);
		applyAftersalesBlock(companyId, rows);

		Map<String, Object> statsFilter = copyFilterForChiefStatistics(filter);
		List<NormalOrders> allMatch =
				adminNormalOrderListService.listAllMatching(companyId, statsFilter, false);
		long totalFeeSum = 0L;
		int cancelCount = 0;
		long appliedTotalRefundFeeSum = 0L;
		for (NormalOrders o : allMatch) {
			totalFeeSum += parseMoneyToLong(o.getTotalFee());
			if (o.getOrderId() != null) {
				appliedTotalRefundFeeSum += aftersalesRefundService.getTotalRefundFee(companyId, o.getOrderId());
			}
			if ("CANCEL".equals(o.getOrderStatus())) {
				cancelCount++;
			}
		}

		Map<String, Object> statistics = new LinkedHashMap<>();
		statistics.put("totalFee", totalFeeSum);
		statistics.put("appliedTotalNum", cancelCount);
		statistics.put("appliedTotalRefundFee", appliedTotalRefundFeeSum);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put(
				"pager",
				Map.of("count", pageResult.total(), "page_no", (long) pageNo, "page_size", (long) pageSize));
		out.put("statistics", statistics);
		return out;
	}

	private static Map<String, Object> emptyChiefResponse(int pageNo, int pageSize) {
		Map<String, Object> statistics = new LinkedHashMap<>();
		statistics.put("totalFee", 0L);
		statistics.put("appliedTotalNum", 0);
		statistics.put("appliedTotalRefundFee", 0L);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("pager", Map.of("count", 0L, "page_no", (long) pageNo, "page_size", (long) pageSize));
		out.put("statistics", statistics);
		return out;
	}

	private Map<String, Object> copyFilterForChiefStatistics(Map<String, Object> filter) {
		Map<String, Object> copy = new LinkedHashMap<>(filter);
		for (String k : STATS_FILTER_REMOVED_KEYS) {
			copy.remove(k);
		}
		return copy;
	}

	private Map<String, Object> loadMemberBranch(
			long companyId, long userId, HttpServletRequest request, int pageNo, int pageSize) {
		Map<String, Object> filter =
				communityFrontOrderListFilterBuilder.build(companyId, userId, request, false);
		AdminNormalOrderListPageResult pageResult =
				adminNormalOrderListService.pageOrders(filter, pageNo, pageSize, false);
		List<Map<String, Object>> rows = pageResult.list();
		communityOrderListRowEnricher.enrichForAdminAndFront(companyId, rows);
		applyAftersalesBlock(companyId, rows);

		Map<String, Object> statistics = new LinkedHashMap<>();
		statistics.put("totalFee", 0L);
		statistics.put("appliedTotalNum", 0);
		statistics.put("appliedTotalRefundFee", 0L);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put(
				"pager",
				Map.of("count", pageResult.total(), "page_no", (long) pageNo, "page_size", (long) pageSize));
		out.put("statistics", statistics);
		return out;
	}

	private void applyAftersalesBlock(long companyId, List<Map<String, Object>> rows) {
		long nowSec = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> order : rows) {
			order.put("can_apply_aftersales", 0);
			int totalLeftAftersalesNum = 0;
			String orderIdStr = String.valueOf(order.get("order_id"));
			long orderIdLong = longLoose(orderIdStr);

			Object itemsObj = order.get("items");
			if (!(itemsObj instanceof List<?> itemList)) {
				long refundFee = aftersalesRefundService.getTotalRefundFee(companyId, orderIdLong);
				order.put("appliedTotalRefundFee", refundFee);
				continue;
			}

			String receiptType = String.valueOf(order.get("receipt_type"));
			String zitiStatus = String.valueOf(order.get("ziti_status"));
			long orderAutoCloseAftersales = longVal(order.get("order_auto_close_aftersales_time"));
			boolean anyLineWithinAftersalesWindow = false;

			for (Object o : itemList) {
				if (!(o instanceof Map<?, ?> rawItem)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> item = (Map<String, Object>) rawItem;

				long subOrderId = longVal(item.get("id"));
				long lineCompanyId = longVal(item.get("company_id"));
				if (lineCompanyId <= 0L) {
					lineCompanyId = companyId;
				}
				String lineOrderId = item.get("order_id") != null ? String.valueOf(item.get("order_id")) : orderIdStr;

				int appliedNum =
						aftersalesDetailAggregateService.getAppliedNum(lineCompanyId, lineOrderId, subOrderId);

				if ("ziti".equals(receiptType) && "DONE".equals(zitiStatus)) {
					item.put("delivery_item_num", intVal(item.get("num")));
				}

				int deliveryItemNum = intVal(item.get("delivery_item_num"));
				int cancelItemNum = intVal(item.get("cancel_item_num"));
				int leftAftersales = deliveryItemNum + cancelItemNum - appliedNum;
				item.put("left_aftersales_num", leftAftersales);
				item.put("show_aftersales", appliedNum > cancelItemNum ? 1 : 0);

				totalLeftAftersalesNum += leftAftersales;

				if (leftAftersales > 0) {
					long lineClose = longVal(item.get("auto_close_aftersales_time"));
					if (lineClose <= 0L) {
						lineClose = orderAutoCloseAftersales;
					}
					if (lineClose <= 0L || lineClose >= nowSec) {
						anyLineWithinAftersalesWindow = true;
					}
				}
			}

			if (totalLeftAftersalesNum > 0 && anyLineWithinAftersalesWindow) {
				order.put("can_apply_aftersales", 1);
			}

			long refundFee = aftersalesRefundService.getTotalRefundFee(companyId, orderIdLong);
			order.put("appliedTotalRefundFee", refundFee);
		}
	}

	private static boolean isSellerLoose(HttpServletRequest request) {
		String raw = request.getParameter("is_seller");
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if ("1".equals(s)) {
			return true;
		}
		try {
			return Integer.parseInt(s) == 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static int intParam(HttpServletRequest request, String name, int defaultVal) {
		String s = request.getParameter(name);
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			int v = Integer.parseInt(s.trim());
			return v > 0 ? v : defaultVal;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long parseMoneyToLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	private static long longLoose(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
