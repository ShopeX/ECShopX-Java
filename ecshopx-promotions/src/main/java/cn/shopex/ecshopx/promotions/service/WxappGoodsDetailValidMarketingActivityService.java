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

import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsDetailValidMarketingActivityService {

	private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MemberAccountService memberAccountService;
	private final OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader;
	private final ObjectMapper objectMapper;

	public WxappGoodsDetailValidMarketingActivityService(MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper, MemberAccountService memberAccountService,
			OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader, ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.memberAccountService = memberAccountService;
		this.operatorCartCompanyProductModelReader = operatorCartCompanyProductModelReader;
		this.objectMapper = objectMapper;
	}

	@Nullable
	public List<Map<String, Object>> listValidMarketingActivityForGoodsDetail(long companyId, long userId, long distributorIdAsShopId,
			long goodsId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivity::getCompanyId, companyId)
				.eq(MarketingActivity::getCheckStatus, "agree")
				.le(MarketingActivity::getReleaseTime, now)
				.ge(MarketingActivity::getEndTime, now);
		List<MarketingActivity> activityList = marketingActivityMapper.selectList(w);
		if (activityList == null || activityList.isEmpty()) {
			return null;
		}
		Set<Long> marketingIds = activityList.stream().map(MarketingActivity::getMarketingId).collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, List<Map<String, Object>>> relByMarketing = loadRelItems(companyId, marketingIds, goodsId, now);
		Long userGrade = resolveUserGrade(userId, companyId);
		String productModel = operatorCartCompanyProductModelReader.getProductModel(companyId);
		List<Map<String, Object>> resultList = new ArrayList<>();
		for (MarketingActivity value : activityList) {
			if ("member_preference".equals(value.getMarketingType())) {
				continue;
			}
			int useBound = value.getUseBound() != null ? value.getUseBound() : 0;
			if (useBound > 0 && !relByMarketing.containsKey(value.getMarketingId())) {
				continue;
			}
			List<String> shopIds = parseShopIds(value.getShopIds());
			if (distributorIdAsShopId > 0 && !shopIds.isEmpty() && !shopIds.contains("all") && !shopIds.contains(String.valueOf(distributorIdAsShopId))) {
				continue;
			}
			long sourceId = value.getSourceId() != null ? value.getSourceId() : 0L;
			if ("platform".equals(productModel) && distributorIdAsShopId >= 0 && distributorIdAsShopId != sourceId) {
				continue;
			}
			List<?> validGrade = decodeJsonList(value.getValidGrade());
			if (validGrade != null && !validGrade.isEmpty() && userGrade != null && !gradeListContains(validGrade, userGrade)) {
				continue;
			}
			Map<String, Object> row = activityToRow(value, now, userId, relByMarketing.get(value.getMarketingId()));
			resultList.add(row);
		}
		return resultList.isEmpty() ? null : resultList;
	}

	private Map<Long, List<Map<String, Object>>> loadRelItems(long companyId, Set<Long> marketingIds, long goodsId, int now) {
		if (marketingIds.isEmpty() || goodsId <= 0) {
			return Map.of();
		}
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId)
				.in(MarketingActivityItems::getMarketingId, marketingIds)
				.eq(MarketingActivityItems::getGoodsId, goodsId)
				.ge(MarketingActivityItems::getEndTime, now);
		List<MarketingActivityItems> rows = marketingActivityItemsMapper.selectList(w);
		Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
		for (MarketingActivityItems r : rows) {
			Long mid = r.getMarketingId();
			if (mid == null) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", r.getItemId());
			m.put("goods_id", r.getGoodsId());
			m.put("marketing_id", mid);
			out.computeIfAbsent(mid, k -> new ArrayList<>()).add(m);
		}
		return out;
	}

	private Map<String, Object> activityToRow(MarketingActivity value, int now, long userId, List<Map<String, Object>> items) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("marketing_id", value.getMarketingId());
		row.put("marketing_type", value.getMarketingType());
		row.put("use_bound", value.getUseBound());
		row.put("shop_ids", parseShopIds(value.getShopIds()));
		row.put("valid_grade", decodeJsonList(value.getValidGrade()));
		row.put("items", items != null ? items : List.of());
		row.put("marketing_name", value.getMarketingName());
		try {
			row.put("condition_value", objectMapper.readValue(
					StringUtils.hasText(value.getConditionValue()) ? value.getConditionValue() : "{}",
					new TypeReference<Map<String, Object>>() {}));
		} catch (Exception e) {
			row.put("condition_value", Map.of());
		}
		row.put("condition_rules", List.of());
		row.put("start_date", formatEpoch(value.getStartTime()));
		row.put("end_date", formatEpoch(value.getEndTime()));
		if (now >= nz(value.getEndTime())) {
			row.put("status", "end");
		} else if (now >= nz(value.getStartTime()) && now < nz(value.getEndTime())) {
			row.put("status", "ongoing");
			row.put("last_seconds", Math.max(0, nz(value.getEndTime()) - now));
		} else {
			row.put("status", "waiting");
		}
		if (userId > 0) {
			row.put("usedCount", 0);
		}
		return row;
	}

	private static int nz(Integer v) {
		return v != null ? v : 0;
	}

	private static String formatEpoch(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return DT_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static List<String> parseShopIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return List.of("all");
		}
		return Arrays.stream(raw.split(",")).map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList());
	}

	private List<?> decodeJsonList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private static boolean gradeListContains(List<?> validGrade, long userGrade) {
		for (Object o : validGrade) {
			if (o instanceof Number n && n.longValue() == userGrade) {
				return true;
			}
			if (o != null) {
				try {
					if (Long.parseLong(o.toString().trim()) == userGrade) {
						return true;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return false;
	}

	private Long resolveUserGrade(long userId, long companyId) {
		if (userId <= 0) {
			return null;
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n) {
			return n.longValue();
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			try {
				return Long.parseLong(g.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
