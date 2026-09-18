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

import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountListFilterParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountNewCardListService {

	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardUserCardIdsByGoodsService discountCardUserCardIdsByGoodsService;
	private final OperatorCartCompanyProductModelReader productModelReader;

	public UserDiscountNewCardListService(UserDiscountMapper userDiscountMapper,
			DiscountCardUserCardIdsByGoodsService discountCardUserCardIdsByGoodsService,
			OperatorCartCompanyProductModelReader productModelReader) {
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardUserCardIdsByGoodsService = discountCardUserCardIdsByGoodsService;
		this.productModelReader = productModelReader;
	}

	public Map<String, Object> loadPage(Map<String, Object> filter, int pageNo, int pageSize) {
		Map<String, Object> goodsWork = new HashMap<>(filter);
		List<Long> filterUserDiscountIds = null;
		Object itemRaw = filter.get("item_id");
		if (itemRaw instanceof List<?> il && !il.isEmpty()) {
			List<Long> ids = discountCardUserCardIdsByGoodsService.resolveUserDiscountIds(goodsWork);
			if (ids.isEmpty()) {
				return new HashMap<>(Map.of("total_count", 0, "list", List.of()));
			}
			filterUserDiscountIds = ids;
		}
		int ps = pageSize;
		if (ps > 50) {
			ps = 50;
		}
		if (ps <= 0) {
			ps = 20;
		}
		int pn = pageNo < 1 ? 1 : pageNo;
		long offset = (long) (pn - 1) * ps;
		UserDiscountListFilterParams f = buildFilterParams(filter, filterUserDiscountIds);
		long total = userDiscountMapper.countNewUserCardListDistinct(f);
		if (total <= 0) {
			return new HashMap<>(Map.of("total_count", 0, "list", List.of()));
		}
		List<Map<String, Object>> rows = userDiscountMapper.selectNewUserCardListPage(f, offset, ps);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			list.add(new HashMap<>(row));
		}
		Map<String, Object> out = new HashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private UserDiscountListFilterParams buildFilterParams(Map<String, Object> filter, List<Long> filterUserDiscountIds) {
		UserDiscountListFilterParams f = new UserDiscountListFilterParams();
		f.setCompanyId(toLong(filter.get("company_id")));
		f.setUserId(toLong(filter.get("user_id")));
		Object cardTypes = filter.get("card_type");
		if (cardTypes instanceof List<?> list && !list.isEmpty()) {
			List<String> normalized = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					String s = String.valueOf(o).trim();
					if (!s.isEmpty()) {
						normalized.add(s);
					}
				}
			}
			f.setCardTypes(normalized.isEmpty() ? List.of("discount", "cash") : normalized);
		} else {
			f.setCardTypes(List.of("discount", "cash"));
		}
		Object usePlatform = filter.get("use_platform");
		if (usePlatform != null && !String.valueOf(usePlatform).isBlank()) {
			f.setUsePlatform(String.valueOf(usePlatform).trim());
		} else {
			f.setUsePlatform("mall");
		}
		Object useScenes = filter.get("use_scenes");
		if (useScenes != null && !String.valueOf(useScenes).isBlank()) {
			f.setUseScenes(String.valueOf(useScenes).trim());
		}
		Object statusObj = filter.get("status");
		if (statusObj instanceof List<?> statusList && !statusList.isEmpty()) {
			List<Integer> statuses = new ArrayList<>();
			for (Object raw : statusList) {
				Integer parsed = parseIntOrNull(raw);
				if (parsed != null) {
					statuses.add(parsed);
				}
			}
			f.setStatuses(statuses.isEmpty() ? List.of(1, 4) : statuses);
		} else {
			f.setStatuses(List.of(1, 4));
		}
		f.setNowEpoch(toIntOrDefault(filter.get("now_epoch"), (int) Instant.now().getEpochSecond()));
		if (filter.containsKey("valid_only")) {
			f.setValidOnly(Boolean.TRUE.equals(filter.get("valid_only")));
		}
		Object code = filter.get("code");
		if (code != null && !String.valueOf(code).isBlank()) {
			f.setCode(String.valueOf(code).trim());
		}
		Object cardId = filter.get("card_id");
		Long cardIdLong = parseOptionalLong(cardId);
		if (cardIdLong != null) {
			f.setCardId(cardIdLong);
		}
		Object lc = filter.get("least_cost|lte");
		if (lc != null) {
			if (lc instanceof Number n) {
				f.setLeastCostLte(n.intValue());
			} else {
				try {
					f.setLeastCostLte(Integer.parseInt(String.valueOf(lc).trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		f.setDistributorId(toLong(filter.get("distributor_id")));
		f.setProductModel(productModelReader.getProductModel(f.getCompanyId()));
		if (filterUserDiscountIds != null) {
			f.setFilterUserDiscountIds(filterUserDiscountIds);
		}
		return f;
	}

	private static Integer parseIntOrNull(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return null;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int toIntOrDefault(Object raw, int defaultVal) {
		Integer parsed = parseIntOrNull(raw);
		return parsed != null ? parsed : defaultVal;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			return Long.parseLong(s.trim());
		}
		return 0L;
	}

	private static Long parseOptionalLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
