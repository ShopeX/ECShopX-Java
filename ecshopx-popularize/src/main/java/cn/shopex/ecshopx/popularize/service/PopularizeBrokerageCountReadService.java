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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.dto.PromoterBrokerageCompanySumRow;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeBrokerageCountReadService {

	private static final Logger log = LoggerFactory.getLogger(PopularizeBrokerageCountReadService.class);

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;

	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;

	public PopularizeBrokerageCountReadService(
			PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper,
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService) {
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
	}

	public Map<String, Object> brokerageCount(long companyId, String userIdRaw, String distributorIdRaw) {
		boolean distributorPresent = false;
		long distributorId = 0L;
		String dTrim = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		if (StringUtils.hasText(dTrim)) {
			if (!DIGITS_ONLY.matcher(dTrim).matches()) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			try {
				distributorId = Long.parseLong(dTrim);
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			if (distributorId > 0L) {
				distributorPresent = true;
			}
		}

		boolean userIdPresent = false;
		long userId = 0L;
		String uTrim = userIdRaw == null ? "" : userIdRaw.trim();
		if (StringUtils.hasText(uTrim)) {
			if (!DIGITS_ONLY.matcher(uTrim).matches()) {
				throw new BadRequestException("user_id 格式错误");
			}
			try {
				userId = Long.parseLong(uTrim);
			} catch (NumberFormatException e) {
				throw new BadRequestException("user_id 格式错误");
			}
			if (userId > 0L) {
				userIdPresent = true;
			}
		}

		Map<String, Object> countDataShop;
		if (distributorPresent) {
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("dIds", List.of(distributorId));
			if (userIdPresent) {
				filter.put("user_id", userId);
			}
			countDataShop = salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountAggregate(filter);
			log.debug("brokerageCount distributor_id={}", distributorId);
		} else {
			countDataShop = null;
		}

		Map<String, Object> countData;
		if (userIdPresent) {
			countData = loadPromoterCount(companyId, userId);
		} else {
			countData = loadCompanyCount(companyId);
		}

		if (!distributorPresent) {
			LinkedHashMap<String, Object> shop = new LinkedHashMap<>();
			shop.put("rebate_sum_noclose", longOrZero(countData.get("noCloseRebate")));
			shop.put("price_sum", longOrZero(countData.get("itemTotalPrice")));
			shop.put("rebate_sum", longOrZero(countData.get("rebateTotal")));
			countDataShop = shop;
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("payedRebate", longOrZero(countData.get("payedRebate")));
		data.put("itemTotalPrice", longOrZero(countData.get("itemTotalPrice")));
		data.put("cashWithdrawalRebate", longOrZero(countData.get("cashWithdrawalRebate")));
		data.put("noCloseRebate", longOrZero(countData.get("noCloseRebate")));
		data.put("rebateTotal", longOrZero(countData.get("rebateTotal")));
		data.put("freezeCashWithdrawalRebate", longOrZero(countData.get("freezeCashWithdrawalRebate")));
		data.put("pointTotal", longOrZero(countData.get("pointTotal")));
		data.put("countDataShop", countDataShop);
		return data;
	}

	public Map<Long, Map<String, Object>> batchPromoterBrokerageCountsByUserIds(
			long companyId, Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		List<Long> distinctIds =
				userIds.stream()
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.toList();
		if (distinctIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		LambdaQueryWrapper<PromoterBrokerageStatistics> q =
				new LambdaQueryWrapper<PromoterBrokerageStatistics>()
						.eq(PromoterBrokerageStatistics::getCompanyId, companyId)
						.in(PromoterBrokerageStatistics::getUserId, distinctIds);
		List<PromoterBrokerageStatistics> rows = promoterBrokerageStatisticsMapper.selectList(q);
		Map<Long, PromoterBrokerageStatistics> byUser =
				rows.stream()
						.filter(Objects::nonNull)
						.filter(r -> r.getUserId() != null)
						.collect(Collectors.toMap(PromoterBrokerageStatistics::getUserId, r -> r, (a, b) -> a));

		LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Long uid : distinctIds) {
			out.put(uid, promoterStatisticsRowToMap(byUser.get(uid)));
		}
		return out;
	}

	private Map<String, Object> loadPromoterCount(long companyId, long userId) {
		LambdaQueryWrapper<PromoterBrokerageStatistics> q =
				new LambdaQueryWrapper<PromoterBrokerageStatistics>()
						.eq(PromoterBrokerageStatistics::getCompanyId, companyId)
						.eq(PromoterBrokerageStatistics::getUserId, userId);
		PromoterBrokerageStatistics row = promoterBrokerageStatisticsMapper.selectOne(q);
		return promoterStatisticsRowToMap(row);
	}

	private static Map<String, Object> promoterStatisticsRowToMap(PromoterBrokerageStatistics row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (row == null) {
			m.put("itemTotalPrice", 0L);
			m.put("rebateTotal", 0L);
			m.put("noCloseRebate", 0L);
			m.put("cashWithdrawalRebate", 0L);
			m.put("freezeCashWithdrawalRebate", 0L);
			m.put("rechargeRebate", 0L);
			m.put("payedRebate", 0L);
			m.put("rechargePoint", 0L);
			m.put("cashWithdrawalPoint", 0L);
			m.put("noClosePoint", 0L);
			m.put("pointTotal", 0L);
			return m;
		}
		m.put("itemTotalPrice", row.getItemTotalPrice() != null ? row.getItemTotalPrice() : 0L);
		m.put("rebateTotal", row.getRebateTotal() != null ? row.getRebateTotal() : 0L);
		m.put("noCloseRebate", row.getNoCloseRebate() != null ? row.getNoCloseRebate() : 0L);
		m.put("cashWithdrawalRebate", row.getCashWithdrawalRebate() != null ? row.getCashWithdrawalRebate() : 0L);
		m.put(
				"freezeCashWithdrawalRebate",
				row.getFreezeCashWithdrawalRebate() != null ? row.getFreezeCashWithdrawalRebate() : 0L);
		m.put("rechargeRebate", row.getRechargeRebate() != null ? row.getRechargeRebate() : 0L);
		m.put("payedRebate", row.getPayedRebate() != null ? row.getPayedRebate() : 0L);
		m.put("rechargePoint", row.getRechargePoint() != null ? row.getRechargePoint() : 0L);
		m.put(
				"cashWithdrawalPoint",
				row.getCashWithdrawalPoint() != null ? row.getCashWithdrawalPoint() : 0L);
		m.put("noClosePoint", row.getNoClosePoint() != null ? row.getNoClosePoint() : 0L);
		m.put("pointTotal", row.getPointTotal() != null ? row.getPointTotal() : 0L);
		return m;
	}

	private Map<String, Object> loadCompanyCount(long companyId) {
		PromoterBrokerageCompanySumRow row = promoterBrokerageStatisticsMapper.selectCompanySum(companyId);
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (row == null) {
			m.put("itemTotalPrice", 0L);
			m.put("rebateTotal", 0L);
			m.put("noCloseRebate", 0L);
			m.put("cashWithdrawalRebate", 0L);
			m.put("freezeCashWithdrawalRebate", 0L);
			m.put("rechargeRebate", 0L);
			m.put("payedRebate", 0L);
			m.put("pointTotal", 0L);
			return m;
		}
		m.put("itemTotalPrice", row.getItemTotalPrice() != null ? row.getItemTotalPrice() : 0L);
		m.put("rebateTotal", row.getRebateTotal() != null ? row.getRebateTotal() : 0L);
		m.put("noCloseRebate", row.getNoCloseRebate() != null ? row.getNoCloseRebate() : 0L);
		m.put("cashWithdrawalRebate", row.getCashWithdrawalRebate() != null ? row.getCashWithdrawalRebate() : 0L);
		m.put(
				"freezeCashWithdrawalRebate",
				row.getFreezeCashWithdrawalRebate() != null ? row.getFreezeCashWithdrawalRebate() : 0L);
		m.put("rechargeRebate", row.getRechargeRebate() != null ? row.getRechargeRebate() : 0L);
		m.put("payedRebate", row.getPayedRebate() != null ? row.getPayedRebate() : 0L);
		m.put("pointTotal", row.getPointTotal() != null ? row.getPointTotal() : 0L);
		return m;
	}

	private static long longOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
