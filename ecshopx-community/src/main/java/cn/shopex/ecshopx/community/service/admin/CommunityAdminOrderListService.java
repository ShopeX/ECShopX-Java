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

package cn.shopex.ecshopx.community.service.admin;

import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import cn.shopex.ecshopx.community.service.CommunityOrderListRowEnricher;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderListPageResult;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityAdminOrderListService {

	private final CommunityAdminOrderListFilterBuilder communityAdminOrderListFilterBuilder;
	private final AdminNormalOrderListService adminNormalOrderListService;
	private final CommunityActivityMapper communityActivityMapper;
	private final CommunityAdminOrderDatapassUiApplier communityAdminOrderDatapassUiApplier;
	private final CommunityOrderListRowEnricher communityOrderListRowEnricher;

	public CommunityAdminOrderListService(
			CommunityAdminOrderListFilterBuilder communityAdminOrderListFilterBuilder,
			AdminNormalOrderListService adminNormalOrderListService,
			CommunityActivityMapper communityActivityMapper,
			CommunityAdminOrderDatapassUiApplier communityAdminOrderDatapassUiApplier,
			CommunityOrderListRowEnricher communityOrderListRowEnricher) {
		this.communityAdminOrderListFilterBuilder = communityAdminOrderListFilterBuilder;
		this.adminNormalOrderListService = adminNormalOrderListService;
		this.communityActivityMapper = communityActivityMapper;
		this.communityAdminOrderDatapassUiApplier = communityAdminOrderDatapassUiApplier;
		this.communityOrderListRowEnricher = communityOrderListRowEnricher;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getOrderList(long companyId, Map<String, Object> jwtUser, HttpServletRequest request) {
		Map<String, Object> filter = communityAdminOrderListFilterBuilder.build(companyId, jwtUser, request);

		String actName = request.getParameter("activity_name");
		String actStat = request.getParameter("activity_status");
		if (StringUtils.hasText(actName) || StringUtils.hasText(actStat)) {
			LambdaQueryWrapper<CommunityActivity> aw = new LambdaQueryWrapper<>();
			aw.eq(CommunityActivity::getCompanyId, companyId);
			if (StringUtils.hasText(actName)) {
				aw.like(CommunityActivity::getActivityName, actName.trim());
			}
			if (StringUtils.hasText(actStat)) {
				aw.eq(CommunityActivity::getActivityStatus, actStat.trim());
			}
			List<CommunityActivity> acts = communityActivityMapper.selectList(aw);
			List<Long> ids = acts.stream().map(CommunityActivity::getActivityId).filter(Objects::nonNull).toList();
			List<Long> actIds = new ArrayList<>();
			actIds.add(0L);
			actIds.addAll(ids);
			filter.put("act_id", actIds);
		}

		int pageNo = intVal(filter.get("page"));
		int pageSize = intVal(filter.get("pageSize"));
		boolean asc = "asc".equalsIgnoreCase(str(filter.get("order_by")));

		AdminNormalOrderListPageResult pageResult =
				adminNormalOrderListService.pageOrders(filter, pageNo, pageSize, asc);
		List<Map<String, Object>> rows = pageResult.list();

		communityOrderListRowEnricher.enrichForAdminAndFront(companyId, rows);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("list", rows);
		result.put(
				"pager",
				Map.of("count", pageResult.total(), "page_no", (long) pageNo, "page_size", (long) pageSize));
		result.put("filter", buildFilterEcho(filter));
		boolean datapassBlocked = DatapassBlockResolver.isBlockedFromQueryParameter(request);
		int datapassBlock = datapassBlocked ? 1 : 0;
		result.put("datapass_block", datapassBlock);

		if (datapassBlock != 0) {
			for (Map<String, Object> row : rows) {
				communityAdminOrderDatapassUiApplier.applyBlockedListRow(row);
				Object appInfoObj = row.get("app_info");
				if (appInfoObj instanceof Map<?, ?> appRaw && !appRaw.isEmpty()) {
					Map<String, Object> appInfo = (Map<String, Object>) appRaw;
					Object buttonsObj = appInfo.get("buttons");
					List<Map<String, Object>> buttons = new ArrayList<>();
					Map<String, Object> markBtn = new LinkedHashMap<>();
					markBtn.put("type", "mark");
					markBtn.put("name", "备注");
					buttons.add(markBtn);
					if (buttonsObj instanceof List<?> rawList) {
						for (Object o : rawList) {
							if (o instanceof Map<?, ?> bm) {
								buttons.add(new LinkedHashMap<String, Object>((Map<String, Object>) bm));
							}
						}
					}
					appInfo.put("buttons", buttons);
				}
			}
		}

		return result;
	}

	/**
	 * Returns {@code $filter} without pagination / sort keys; always includes {@code order_type} = normal.
	 */
	private static Map<String, Object> buildFilterEcho(Map<String, Object> filter) {
		Map<String, Object> out = new LinkedHashMap<>();
		boolean placedOrderType = false;
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String k = e.getKey();
			if ("page".equals(k) || "pageSize".equals(k) || "order_by".equals(k)) {
				continue;
			}
			if ("order_class".equals(k) && !placedOrderType) {
				out.put("order_type", "normal");
				placedOrderType = true;
			}
			out.put(k, e.getValue());
		}
		if (!placedOrderType) {
			out.put("order_type", "normal");
		}
		return out;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
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
