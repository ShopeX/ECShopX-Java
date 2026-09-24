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

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountNewGetCardListFacadeService {

	private final UserDiscountValidItemsResolver userDiscountValidItemsResolver;
	private final UserDiscountNewCardListService userDiscountNewCardListService;
	private final UserDiscountCardValidityEvaluator userDiscountCardValidityEvaluator;
	private final UserDiscountNewGetCardListResponseFormatter responseFormatter;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public UserDiscountNewGetCardListFacadeService(UserDiscountValidItemsResolver userDiscountValidItemsResolver,
			UserDiscountNewCardListService userDiscountNewCardListService,
			UserDiscountCardValidityEvaluator userDiscountCardValidityEvaluator,
			UserDiscountNewGetCardListResponseFormatter responseFormatter,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.userDiscountValidItemsResolver = userDiscountValidItemsResolver;
		this.userDiscountNewCardListService = userDiscountNewCardListService;
		this.userDiscountCardValidityEvaluator = userDiscountCardValidityEvaluator;
		this.responseFormatter = responseFormatter;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> build(long companyId, long userId, UserDiscountNewGetCardListRequest req) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", userId);
		filter.put("now_epoch", (int) Instant.now().getEpochSecond());

		if ("picker".equalsIgnoreCase(trim(req.getPageType()))) {
			filter.put("card_type", List.of("discount", "cash"));
		}
		putIfText(filter, "use_platform", req.getUsePlatform());
		putIfText(filter, "use_scenes", req.getUseScenes());
		putIfText(filter, "code", req.getCode());
		Long cardId = req.parseCardIdOrNull();
		if (cardId != null) {
			filter.put("card_id", cardId);
		}
		Integer leastCost = req.parseAmountLteOrNull();
		if (leastCost != null) {
			filter.put("least_cost|lte", leastCost);
		}
		String distributorRaw = trim(req.getDistributorId());
		if (!distributorRaw.isEmpty() && !"0".equals(distributorRaw) && !"undefined".equalsIgnoreCase(distributorRaw)) {
			Long distributorId = parsePositiveLongOrNull(distributorRaw);
			if (distributorId != null) {
				filter.put("distributor_id", distributorId);
			}
		}

		boolean validRequired = req.isValidRequired();
		filter.put("valid_only", validRequired);
		if (validRequired) {
			Map<Long, Map<String, Object>> validItems = userDiscountValidItemsResolver.resolve(companyId, userId, req);
			if (!validItems.isEmpty()) {
				filter.put("item_id", new ArrayList<>(validItems.keySet()));
			}
			Map<String, Object> page = userDiscountNewCardListService.loadPage(filter, req.getPageNo(), req.getPageSize());
			List<Map<String, Object>> list = castCardList(page.get("list"));
			userDiscountCardValidityEvaluator.evaluate(companyId, userId, req, validItems, list);
			list = retainUsableCards(page, list);
			return newGetCardListBody(companyId, page, list);
		}

		Map<String, Object> page = userDiscountNewCardListService.loadPage(filter, req.getPageNo(), req.getPageSize());
		List<Map<String, Object>> list = castCardList(page.get("list"));
		userDiscountCardValidityEvaluator.evaluate(companyId, userId, req, Map.of(), list);
		return newGetCardListBody(companyId, page, list);
	}

	private Map<String, Object> newGetCardListBody(long companyId, Map<String, Object> page, List<Map<String, Object>> list) {
		Object totalRaw = page.get("total_count");
		long count = 0L;
		if (totalRaw instanceof Number n) {
			count = n.longValue();
		}
		CurrencyExchangeRate curRow = companyDefaultCurrencyService.getCur(companyId);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", responseFormatter.formatList(list));
		out.put("total_count", responseFormatter.formatTotalCount(count));
		out.put("cur", responseFormatter.formatCur(companyDefaultCurrencyService.toCurResponseMap(curRow)));
		return out;
	}

	private static List<Map<String, Object>> retainUsableCards(Map<String, Object> page, List<Map<String, Object>> list) {
		List<Map<String, Object>> usable = new ArrayList<>();
		int dropped = 0;
		for (Map<String, Object> card : list) {
			if (Boolean.TRUE.equals(card.get("valid"))) {
				usable.add(card);
			} else {
				dropped++;
			}
		}
		if (dropped > 0) {
			Object totalRaw = page.get("total_count");
			long total = 0L;
			if (totalRaw instanceof Number n) {
				total = n.longValue();
			} else if (totalRaw != null) {
				try {
					total = Long.parseLong(String.valueOf(totalRaw).trim());
				} catch (NumberFormatException ignored) {
				}
			}
			page.put("total_count", Math.max(0L, total - dropped));
		}
		return usable;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castCardList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> map) {
				out.add((Map<String, Object>) map);
			}
		}
		return out;
	}

	private static String trim(String raw) {
		return raw == null ? "" : raw.trim();
	}

	private static void putIfText(Map<String, Object> target, String key, String value) {
		if (ValuePresence.hasEffectiveValue(value)) {
			target.put(key, value.trim());
		}
	}

	private static Long parsePositiveLongOrNull(String raw) {
		try {
			long value = Long.parseLong(raw.trim());
			return value > 0L ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
