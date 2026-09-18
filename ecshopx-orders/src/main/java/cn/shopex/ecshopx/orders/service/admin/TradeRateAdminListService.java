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

import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import cn.shopex.ecshopx.orders.service.admin.dto.TradeRateAdminListQuery;
import cn.shopex.ecshopx.orders.service.admin.dto.TradeRateListRow;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TradeRateAdminListService {

	private final ShopMenuService shopMenuService;
	private final TradeRateMapper tradeRateMapper;
	private final MemberAccountService memberAccountService;

	public TradeRateAdminListService(
			ShopMenuService shopMenuService,
			TradeRateMapper tradeRateMapper,
			MemberAccountService memberAccountService) {
		this.shopMenuService = shopMenuService;
		this.tradeRateMapper = tradeRateMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> getTradeRateList(
			long companyId, Map<String, Object> operatorJwt, HttpServletRequest request) {
		TradeRateAdminListQuery q = buildQuery(request);

		String pm = shopMenuService.resolveProductModelKeyForCompany(Long.valueOf(companyId));
		Long distributorId = null;
		if ("platform".equals(pm) && operatorJwt != null) {
			distributorId = EspierAdminJwtControllerSupport.parseLongOrNull(operatorJwt.get("distributor_id"));
		}

		boolean useJoin = "platform".equals(pm) && distributorId != null;

		long totalCount;
		List<TradeRateListRow> rows;
		int page = resolvePage(request);
		int pageSize = resolvePageSize(request);
		long offset = (long) (page - 1) * pageSize;
		long limit = (long) pageSize;

		if (useJoin) {
			totalCount =
					tradeRateMapper.countTradeRateListAdminJoin(companyId, distributorId.longValue(), q);
			rows = tradeRateMapper.selectTradeRateListAdminPageJoin(
					companyId, distributorId.longValue(), q, offset, limit);
		} else {
			totalCount = tradeRateMapper.countTradeRateListAdminSimple(companyId, q);
			rows = tradeRateMapper.selectTradeRateListAdminPageSimple(companyId, q, offset, limit);
		}

		String orderTypeFilter = q.getOrderType();
		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (TradeRateListRow row : rows) {
			list.add(enrichRow(row, companyId, orderTypeFilter));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	private static TradeRateAdminListQuery buildQuery(HttpServletRequest request) {
		TradeRateAdminListQuery q = new TradeRateAdminListQuery();

		String orderId = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("order_id"));
		if (orderId != null) {
			q.setOrderId(orderId);
		}

		String itemIdRaw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("item_id"));
		if (itemIdRaw != null) {
			try {
				q.setItemId(Long.parseLong(itemIdRaw));
			} catch (NumberFormatException ignored) {
			}
		}

		String orderType = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("order_type"));
		if (orderType != null) {
			q.setOrderType(orderType);
		}

		applyRateStatus(request, q);

		String beginRaw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("time_start_begin"));
		if (beginRaw != null) {
			try {
				int gte = Integer.parseInt(beginRaw);
				q.setCreatedGte(gte);
				String endRaw =
						EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("time_start_end"));
				if (endRaw != null) {
					try {
						q.setCreatedLte(Integer.parseInt(endRaw));
					} catch (NumberFormatException ignored) {
					}
				}
			} catch (NumberFormatException ignored) {
			}
		}

		return q;
	}

	private static void applyRateStatus(HttpServletRequest request, TradeRateAdminListQuery q) {
		if (!request.getParameterMap().containsKey("rate_status")) {
			return;
		}
		String s = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("rate_status"));
		if (s == null) {
			return;
		}
		q.setIsReply(s);
	}

	private static int resolvePage(HttpServletRequest request) {
		String raw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("page"));
		if (raw == null) {
			return 1;
		}
		try {
			int p = Integer.parseInt(raw);
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int resolvePageSize(HttpServletRequest request) {
		String raw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("pageSize"));
		if (raw == null) {
			return 20;
		}
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private Map<String, Object> enrichRow(TradeRateListRow row, long companyId, String orderTypeFilter) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rate_id", row.getRateId());
		m.put("company_id", row.getCompanyId());
		m.put("item_id", row.getItemId());
		m.put("goods_id", row.getGoodsId());
		m.put("order_id", row.getOrderId());
		m.put("user_id", row.getUserId());
		m.put("rate_pic", row.getRatePic());
		m.put("rate_pic_num", row.getRatePicNum());
		m.put("content", row.getContent());
		m.put("content_len", row.getContentLen());
		m.put("is_reply", Boolean.TRUE.equals(row.getIsReply()));
		m.put("disabled", Boolean.TRUE.equals(row.getDisabled()));
		m.put("anonymous", Boolean.TRUE.equals(row.getAnonymous()));
		m.put("star", row.getStar());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("unionid", row.getUnionid());
		m.put("item_spec_desc", row.getItemSpecDesc());
		m.put("order_type", row.getOrderType());
		if (row.getDistributorId() != null) {
			m.put("distributor_id", row.getDistributorId());
		}
		m.put("username", resolveUsername(row.getUserId(), companyId));
		m.put("item_name", resolveItemName(row.getItemId(), companyId, orderTypeFilter));
		return m;
	}

	private String resolveUsername(Long userId, long companyId) {
		if (userId == null || userId <= 0) {
			return "匿名";
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		String u = EspierAdminJwtControllerSupport.optionalTrimmedString(member.get("username"));
		return u != null ? u : "匿名";
	}

	private String resolveItemName(Long itemId, long companyId, String orderTypeFilter) {
		if (itemId == null || itemId <= 0) {
			return "";
		}
		String name;
		if ("pointsmall".equals(orderTypeFilter)) {
			name = tradeRateMapper.selectPointsmallItemNameByItemIdAndCompanyId(itemId, companyId);
		} else {
			name = tradeRateMapper.selectGoodsItemNameByItemIdAndCompanyId(itemId, companyId);
		}
		return name != null ? name : "";
	}
}
