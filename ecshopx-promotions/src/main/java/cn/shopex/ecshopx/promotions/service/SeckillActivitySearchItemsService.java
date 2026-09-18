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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.port.SeckillSearchItemsGoodsListPort;
import cn.shopex.ecshopx.promotions.util.SeckillShopIdCsvParser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivitySearchItemsService {

	private static final String SEARCH_ITEMS_LIST_ERR = "获取商品列表出错.";

	private final SeckillSearchItemsGoodsListPort seckillSearchItemsGoodsListPort;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final MessageSource messageSource;

	public SeckillActivitySearchItemsService(
			SeckillSearchItemsGoodsListPort seckillSearchItemsGoodsListPort,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			SeckillActivityCreateService seckillActivityCreateService,
			MessageSource messageSource) {
		this.seckillSearchItemsGoodsListPort = seckillSearchItemsGoodsListPort;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.messageSource = messageSource;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> searchItems(
			HttpServletRequest request,
			long companyId,
			Map<String, Object> operatorJwtMap,
			Locale locale) {
		validateSearchItemsPaging(request);
		long startEpoch =
				seckillActivityCreateService.requireFlexibleEpochSeconds(request.getParameter("activity_start_time"), locale);
		long endEpoch =
				seckillActivityCreateService.requireFlexibleEpochSeconds(request.getParameter("activity_end_time"), locale);
		long releaseEpoch =
				seckillActivityCreateService.requireFlexibleEpochSeconds(request.getParameter("activity_release_time"), locale);

		int activityRelease = (int) releaseEpoch;
		int activityStart = (int) startEpoch;
		int activityEnd = (int) endEpoch;

		if (activityRelease > activityStart) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.release_time_after_start", null, locale));
		}
		if (activityStart >= activityEnd) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.start_must_before_end", null, locale));
		}

		Map<String, Object> result = seckillSearchItemsGoodsListPort.listForSeckillSearch(request);

		if (Boolean.TRUE.equals(result.get("_seckillFlatEarly"))) {
			LinkedHashMap<String, Object> flat = new LinkedHashMap<>();
			flat.put("list", List.of());
			flat.put("total_count", 0);
			return flat;
		}

		List<Map<String, Object>> listRows = (List<Map<String, Object>>) result.get("list");
		if (listRows == null) {
			listRows = List.of();
		}

		List<Map<String, Object>> validItems = new ArrayList<>();
		List<Map<String, Object>> invalidItems = new ArrayList<>();

		List<Long> shopIds =
				SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(
						Objects.toString(request.getParameter("distributor_id"), "").trim());
		long sourceId = readSourceIdFromOperatorJwtMap(operatorJwtMap);
		String seckillType = Objects.toString(request.getParameter("seckill_type"), "normal").trim();
		if (!StringUtils.hasText(seckillType)) {
			seckillType = "normal";
		}
		String marketingTypeRaw = Objects.toString(request.getParameter("marketing_type"), "").trim();
		boolean useMarketing = StringUtils.hasText(marketingTypeRaw);
		long activityId = parseNonNegativeLongQuery(request.getParameter("activity_id"));

		for (Map<String, Object> row : listRows) {
			Long itemId = readItemIdFromRow(row);
			if (itemId == null || itemId <= 0L) {
				validItems.add(row);
				continue;
			}
			LinkedHashMap<String, Object> guard = new LinkedHashMap<>();
			guard.put("company_id", companyId);
			guard.put("start_time", activityStart);
			guard.put("end_time", activityEnd);
			guard.put("item_ids", List.of(itemId));
			guard.put("use_bound", 1);
			guard.put("shop_ids", shopIds);
			guard.put("source_id", sourceId);
			guard.put("seckill_type", seckillType);
			if (activityId > 0L) {
				if (useMarketing) {
					guard.put("marketing_id", activityId);
				} else {
					guard.put("exclude_seckill_id", activityId);
				}
			}
			try {
				if (useMarketing) {
					guard.put("marketing_type", marketingTypeRaw);
					marketingActivityCrossPromotionGuardService.checkSeckillSearchItemMarketing(guard);
				} else {
					marketingActivityCrossPromotionGuardService.checkSeckillSearchItemNonMarketing(guard);
				}
				validItems.add(row);
			} catch (ResourceException ex) {
				invalidItems.add(row);
			}
		}

		LinkedHashMap<String, Object> listObj = new LinkedHashMap<>();
		listObj.put("validItems", validItems);
		listObj.put("invalidItems", invalidItems);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("list", listObj);
		return data;
	}

	private static long readSourceIdFromOperatorJwtMap(Map<String, Object> jwt) {
		Object v = jwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long readItemIdFromRow(Map<String, Object> row) {
		Object o = row.get("item_id");
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long l = n.longValue();
			return l > 0L ? l : null;
		}
		try {
			long l = Long.parseLong(o.toString().trim());
			return l > 0L ? l : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseNonNegativeLongQuery(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * 先校验 {@code page}/{@code pageSize}，再校验活动时间等字段。
	 */
	private static void validateSearchItemsPaging(HttpServletRequest request) {
		String ps = request.getParameter("page");
		String pz = request.getParameter("pageSize");
		LinkedHashMap<String, List<String>> requiredErrors = new LinkedHashMap<>();
		if (!StringUtils.hasText(ps)) {
			requiredErrors.put("page", List.of("validation.required"));
		}
		if (!StringUtils.hasText(pz)) {
			requiredErrors.put("pageSize", List.of("validation.required"));
		}
		if (!requiredErrors.isEmpty()) {
			throw new ResourceException(SEARCH_ITEMS_LIST_ERR, requiredErrors);
		}
		int p;
		try {
			p = Integer.parseInt(ps.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(SEARCH_ITEMS_LIST_ERR, Map.of("page", List.of("validation.integer")));
		}
		try {
			Integer.parseInt(pz.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(SEARCH_ITEMS_LIST_ERR, Map.of("pageSize", List.of("validation.integer")));
		}
		if (p < 1) {
			throw new ResourceException(SEARCH_ITEMS_LIST_ERR, Map.of("page", List.of("validation.min.numeric")));
		}
	}
}
