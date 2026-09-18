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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.distribution.service.distributor.DistributorCouponListAppendService;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.dto.WxappGetCardListParams;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardWxappGetCardListService {

	private final DiscountCardWxappDefaultItemIdLookupService defaultItemIdLookupService;
	private final DiscountCardKaquanListByItemIdQueryService kaquanListByItemIdQueryService;
	private final DiscountCardAppendDistributorIdForListService appendDistributorIdForListService;
	private final DistributorCouponListAppendService distributorCouponListAppendService;
	private final UserDiscountMapper userDiscountMapper;

	public DiscountCardWxappGetCardListService(
			DiscountCardWxappDefaultItemIdLookupService defaultItemIdLookupService,
			DiscountCardKaquanListByItemIdQueryService kaquanListByItemIdQueryService,
			DiscountCardAppendDistributorIdForListService appendDistributorIdForListService,
			DistributorCouponListAppendService distributorCouponListAppendService,
			UserDiscountMapper userDiscountMapper) {
		this.defaultItemIdLookupService = defaultItemIdLookupService;
		this.kaquanListByItemIdQueryService = kaquanListByItemIdQueryService;
		this.appendDistributorIdForListService = appendDistributorIdForListService;
		this.distributorCouponListAppendService = distributorCouponListAppendService;
		this.userDiscountMapper = userDiscountMapper;
	}

	public Map<String, Object> build(WxappGetCardListParams params) {
		Map<String, Object> filter = new LinkedHashMap<>();
		if (StringUtils.hasText(params.getCardType())) {
			filter.put("card_type", params.getCardType().trim());
		} else {
			filter.put("card_type", List.of("cash", "discount", "new_gift", "money"));
		}
		if (StringUtils.hasText(params.getEndDate())) {
			filter.put("end_date", System.currentTimeMillis() / 1000L);
		}
		String dist = params.getDistributorId();
		if (dist != null && !dist.isBlank() && !"undefined".equals(dist)) {
			if (!"all".equals(dist)) {
				filter.put("distributor_id", dist);
			}
		}
		filter.put("receive", "1");
		filter.put("coupon_type", StringUtils.hasText(params.getWorkUserId()) ? "guide" : "mall");
		filter.put("company_id", params.getCompanyId());

		if (params.getItemId() != null) {
			filter.put("item_id", List.of(params.getItemId()));
			Optional<Long> defOpt =
					defaultItemIdLookupService.findDefaultItemIdOrNull(params.getCompanyId(), params.getItemId());
			if (defOpt.isPresent()) {
				filter.put("default_item_id", List.of(defOpt.get()));
			}
		}

		if (StringUtils.hasText(params.getCardIdCsv())) {
			List<Long> cardIds = parseCardIdCsv(params.getCardIdCsv());
			if (!cardIds.isEmpty()) {
				filter.put("card_id", cardIds);
				filter.remove("receive");
			}
		}

		if (params.getUserId() != null) {
			filter.put("user_id", params.getUserId());
		}
		if (params.getGradeId() != null) {
			filter.put("grade_id", params.getGradeId());
		}

		PageResult<Map<String, Object>> page =
				kaquanListByItemIdQueryService.query(filter, params.getPageNo(), params.getPageSize());
		List<Map<String, Object>> list = new ArrayList<>(page.getList());

		Map<String, Object> defaultButton = new LinkedHashMap<>();
		defaultButton.put("label", "get");
		defaultButton.put("text", "领取");
		for (Map<String, Object> row : list) {
			row.put("button_status", new LinkedHashMap<>(defaultButton));
		}

		Long uid = params.getUserId();
		long companyId = params.getCompanyId();
		long now = System.currentTimeMillis() / 1000L;
		if (uid != null && !list.isEmpty()) {
			List<Long> cardIdList = new ArrayList<>();
			for (Map<String, Object> row : list) {
				Object cid = row.get("card_id");
				if (cid instanceof Number n) {
					cardIdList.add(n.longValue());
				}
			}
			Map<Long, Integer> userGetMap = toNumMap(
					userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, uid, cardIdList));
			for (Map<String, Object> row : list) {
				Object cid = row.get("card_id");
				if (!(cid instanceof Number n)) {
					continue;
				}
				long cardId = n.longValue();
				int userGetNum = userGetMap.getOrDefault(cardId, 0);
				row.put("user_get_num", userGetNum);
				Object gl = row.get("get_limit");
				int getLimit = gl instanceof Number ? ((Number) gl).intValue() : 0;
				if (getLimit > 0 && getLimit <= userGetNum) {
					row.put("ifget", 1);
					Map<String, Object> taken = new LinkedHashMap<>();
					taken.put("label", "get_invalid");
					taken.put("text", "已领取");
					row.put("button_status", taken);
				}
				Object endDate = row.get("end_date");
				int ed = endDate instanceof Number ? ((Number) endDate).intValue() : 0;
				if (ed > 0 && ed <= now) {
					row.put("gameOver", 1);
				}
				Object qty = row.get("quantity");
				int quantity = qty instanceof Number ? ((Number) qty).intValue() : 0;
				Object gn = row.get("get_num");
				int getNum = gn instanceof Number ? ((Number) gn).intValue() : 0;
				if (quantity <= getNum) {
					row.put("numNull", 1);
				}
			}
		}

		appendDistributorIdForListService.apply(companyId, list);
		distributorCouponListAppendService.appendDistributorList(companyId, list);
		distributorCouponListAppendService.appendDistributorInfo(companyId, list);

		if (StringUtils.hasText(params.getWorkUserId()) && !list.isEmpty()) {
			List<Long> cardIdList = new ArrayList<>();
			for (Map<String, Object> row : list) {
				Object cid = row.get("card_id");
				if (cid instanceof Number n) {
					cardIdList.add(n.longValue());
				}
			}
			if (!cardIdList.isEmpty()) {
				Map<Long, Integer> spMap = toNumMap(userDiscountMapper.countIssuedGroupByCardIdForSalesperson(
						companyId, params.getWorkUserId().trim(), cardIdList));
				for (Map<String, Object> row : list) {
					Object cid = row.get("card_id");
					if (cid instanceof Number n) {
						row.put("salesperson_get_num", spMap.getOrDefault(n.longValue(), 0));
					}
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		String totalStr = String.valueOf(page.getTotal());
		data.put("total_count", totalStr);
		data.put("pagers", Map.of("total", totalStr));
		data.put("list", list);
		return data;
	}

	private static List<Long> parseCardIdCsv(String csv) {
		List<Long> out = new ArrayList<>();
		for (String p : csv.split(",")) {
			String t = p.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static Map<Long, Integer> toNumMap(List<UserDiscountCardAggRow> rows) {
		Map<Long, Integer> m = new LinkedHashMap<>();
		if (rows == null) {
			return m;
		}
		for (UserDiscountCardAggRow r : rows) {
			if (r.getCardId() != null && r.getNum() != null) {
				m.put(r.getCardId(), r.getNum().intValue());
			}
		}
		return m;
	}
}
