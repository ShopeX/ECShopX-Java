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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.domain.TaskBrokerageCount;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.mapper.TaskBrokerageCountMapper;
import cn.shopex.ecshopx.popularize.support.PopularizeQueryFlagTruthy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterFrontIndexCountService {

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private final BrokerageMapper brokerageMapper;
	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;
	private final TaskBrokerageCountMapper taskBrokerageCountMapper;
	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;
	private final PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final PromoterMapper promoterMapper;

	public PromoterFrontIndexCountService(
			BrokerageMapper brokerageMapper,
			PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper,
			TaskBrokerageCountMapper taskBrokerageCountMapper,
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			PromoterMapper promoterMapper) {
		this.brokerageMapper = brokerageMapper;
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
		this.taskBrokerageCountMapper = taskBrokerageCountMapper;
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.promoterFrontPromoterChildrenListService = promoterFrontPromoterChildrenListService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.promoterMapper = promoterMapper;
	}

	public LinkedHashMap<String, Object> indexCount(
			long companyId,
			long userId,
			String isSalesmanPageRaw,
			String distributorIdRaw) {
		Promoter promoterRow =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (promoterRow == null) {
			throw new ResourceException("当前用户不是推广员");
		}

		LambdaQueryWrapper<Brokerage> wOrder =
				new LambdaQueryWrapper<Brokerage>()
						.eq(Brokerage::getCompanyId, companyId)
						.eq(Brokerage::getUserId, userId)
						.eq(Brokerage::getSource, "order")
						.gt(Brokerage::getPrice, 0);
		int promoterOrderCount = toBoundedInt(brokerageMapper.selectCount(wOrder));

		LambdaQueryWrapper<Brokerage> wTeam =
				new LambdaQueryWrapper<Brokerage>()
						.eq(Brokerage::getCompanyId, companyId)
						.eq(Brokerage::getUserId, userId)
						.eq(Brokerage::getSource, "order_team")
						.gt(Brokerage::getPrice, 0);
		int promoterGradeOrderCount = toBoundedInt(brokerageMapper.selectCount(wTeam));

		PromoterBrokerageStatistics row =
				promoterBrokerageStatisticsMapper.selectOne(
						new LambdaQueryWrapper<PromoterBrokerageStatistics>()
								.eq(PromoterBrokerageStatistics::getCompanyId, companyId)
								.eq(PromoterBrokerageStatistics::getUserId, userId));

		int itemTotalPrice = statLongToInt(row == null ? null : row.getItemTotalPrice());
		int cashWithdrawalRebate = statLongToInt(row == null ? null : row.getCashWithdrawalRebate());
		int noCloseRebate = statLongToInt(row == null ? null : row.getNoCloseRebate());
		int rebateTotal = statLongToInt(row == null ? null : row.getRebateTotal());
		int freezeCashWithdrawalRebate =
				statLongToInt(row == null ? null : row.getFreezeCashWithdrawalRebate());
		int pointTotal = statLongToInt(row == null ? null : row.getPointTotal());

		QueryWrapper<TaskBrokerageCount> tw = new QueryWrapper<>();
		tw.select("COALESCE(SUM(rebate_money),0) AS s");
		tw.eq("company_id", companyId).eq("user_id", userId);
		List<Map<String, Object>> taskRows = taskBrokerageCountMapper.selectMaps(tw);
		int taskBrokerageItemTotalFee = intOrZero(longFromSumMap(firstRow(taskRows), "s"));

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("promoter_order_count", promoterOrderCount);
		data.put("promoter_grade_order_count", promoterGradeOrderCount);
		data.put("itemTotalPrice", itemTotalPrice);
		data.put("cashWithdrawalRebate", cashWithdrawalRebate);
		data.put("noCloseRebate", noCloseRebate);
		data.put("rebateTotal", rebateTotal);
		data.put("freezeCashWithdrawalRebate", freezeCashWithdrawalRebate);
		data.put("taskBrokerageItemTotalFee", taskBrokerageItemTotalFee);
		data.put("taskBrokerageItemTotalPoint", Integer.valueOf(0));

		int isbuyPromoter = promoterFrontPromoterChildrenListService.relationChildrenCountByUserId(companyId, userId, 1);
		int notbuyPromoter = promoterFrontPromoterChildrenListService.relationChildrenCountByUserId(companyId, userId, 0);
		data.put("isbuy_promoter", isbuyPromoter);
		data.put("notbuy_promoter", notbuyPromoter);
		data.put("pointTotal", pointTotal);

		if (!PopularizeQueryFlagTruthy.isTruthyForH5QueryFlag(isSalesmanPageRaw)) {
			return data;
		}

		List<Long> dIds = loadSalespersonShopIds(companyId, userId);
		LinkedHashMap<String, Object> filterCount = new LinkedHashMap<>();
		filterCount.put("company_id", Long.valueOf(companyId));
		filterCount.put("user_id", Long.valueOf(userId));
		if (!dIds.isEmpty()) {
			filterCount.put("dIds", dIds);
		}
		applyDistributorIdToFilterCount(filterCount, distributorIdRaw);

		Map<String, Object> aggregateRow =
				salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountAggregate(filterCount);
		data.put("countDataShop", new LinkedHashMap<>(aggregateRow));
		data.put("rebateTotal", rebateTotalFromSalesmanAggregate(aggregateRow.get("rebate_sum")));

		return data;
	}

	private static void applyDistributorIdToFilterCount(
			LinkedHashMap<String, Object> filterCount, String distributorIdRaw) {
		if (!PopularizeQueryFlagTruthy.isTruthyForH5QueryFlag(distributorIdRaw)) {
			return;
		}
		String t = distributorIdRaw.strip();
		if (!DIGITS_ONLY.matcher(t).matches()) {
			throw new BadRequestException("distributor_id 格式不正确");
		}
		long v = Long.parseLong(t);
		if (v <= 0L) {
			throw new BadRequestException("distributor_id 格式不正确");
		}
		filterCount.remove("dIds");
		filterCount.put("distributor_id", Long.valueOf(v));
	}

	private static int toBoundedInt(long c) {
		if (c > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (c < 0L) {
			return 0;
		}
		return (int) c;
	}

	private static int statLongToInt(Long v) {
		if (v == null) {
			return 0;
		}
		return toBoundedInt(v.longValue());
	}

	private List<Long> loadSalespersonShopIds(long companyId, long userId) {
		String sql =
				"SELECT shop_id FROM shop_salesperson WHERE company_id = :companyId AND user_id = :userId LIMIT 1000";
		MapSqlParameterSource p =
				new MapSqlParameterSource()
						.addValue("companyId", companyId)
						.addValue("userId", (int) userId);
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, p);
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object sid = row.get("shop_id");
			if (sid == null) {
				continue;
			}
			long val;
			if (sid instanceof Number n) {
				val = n.longValue();
			} else {
				try {
					val = Long.parseLong(String.valueOf(sid).trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (val > 0L) {
				out.add(val);
			}
		}
		return out;
	}

	private static Map<String, Object> firstRow(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private static long longFromSumMap(Map<String, Object> row, String key) {
		if (row == null) {
			return 0L;
		}
		Object v = row.get(key);
		if (v == null) {
			for (Map.Entry<String, Object> e : row.entrySet()) {
				if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
					v = e.getValue();
					break;
				}
			}
		}
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static Object rebateTotalFromSalesmanAggregate(Object rebateSum) {
		if (rebateSum == null) {
			return null;
		}
		if (rebateSum instanceof Number n) {
			return Integer.valueOf(n.intValue());
		}
		String s = String.valueOf(rebateSum).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Integer.valueOf((int) Long.parseLong(s));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
