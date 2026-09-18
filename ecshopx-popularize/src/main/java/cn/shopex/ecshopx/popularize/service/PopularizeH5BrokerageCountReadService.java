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

import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.domain.TaskBrokerageCount;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import cn.shopex.ecshopx.popularize.mapper.TaskBrokerageCountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeH5BrokerageCountReadService {

	private static final Logger log = LoggerFactory.getLogger(PopularizeH5BrokerageCountReadService.class);

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;

	private final TaskBrokerageCountMapper taskBrokerageCountMapper;

	private final BrokerageMapper brokerageMapper;

	private final PopularizeSettingSaveService popularizeSettingSaveService;

	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;

	public PopularizeH5BrokerageCountReadService(
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper,
			TaskBrokerageCountMapper taskBrokerageCountMapper,
			BrokerageMapper brokerageMapper,
			PopularizeSettingSaveService popularizeSettingSaveService,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService) {
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
		this.taskBrokerageCountMapper = taskBrokerageCountMapper;
		this.brokerageMapper = brokerageMapper;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
	}

	public Map<String, Object> getPromoterBrokerageInfo(long companyId, long promoterUserId) {
		List<Long> one = List.of(promoterUserId);
		Map<Long, Map<String, String>> contacts =
				membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(one, 1);
		String promoterMobile = null;
		if (contacts.containsKey(promoterUserId)) {
			String raw = contacts.get(promoterUserId).getOrDefault("mobile", "");
			if (raw != null) {
				String trimmed = raw.trim();
				if (!trimmed.isEmpty()) {
					promoterMobile = trimmed;
				}
			}
		}

		Map<String, Object> stats = loadPromoterStatistics(companyId, promoterUserId);
		int itemTotalPrice = (int) longOrZero(stats.get("itemTotalPrice"));
		int rebateTotal = (int) longOrZero(stats.get("rebateTotal"));
		int cashWithdrawalRebate = (int) longOrZero(stats.get("cashWithdrawalRebate"));
		int freezeCashWithdrawalRebate = (int) longOrZero(stats.get("freezeCashWithdrawalRebate"));
		int noCloseRebate = (int) longOrZero(stats.get("noCloseRebate"));
		int pointTotal = (int) longOrZero(stats.get("pointTotal"));
		long hasbeenLong =
				(long) rebateTotal
						- (long) cashWithdrawalRebate
						- (long) freezeCashWithdrawalRebate
						- (long) noCloseRebate;
		int hasbeenCashWithdrawalRebate = (int) hasbeenLong;

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("promoter_mobile", promoterMobile);
		out.put("itemTotalPrice", Integer.valueOf(itemTotalPrice));
		out.put("rebateTotal", Integer.valueOf(rebateTotal));
		out.put("cashWithdrawalRebate", Integer.valueOf(cashWithdrawalRebate));
		out.put("freezeCashWithdrawalRebate", Integer.valueOf(freezeCashWithdrawalRebate));
		out.put("hasbeenCashWithdrawalRebate", Integer.valueOf(hasbeenCashWithdrawalRebate));
		out.put("noCloseRebate", Integer.valueOf(noCloseRebate));
		out.put("pointTotal", Integer.valueOf(pointTotal));
		return out;
	}

	public Map<String, Object> brokerageCount(
			long companyId, long userId, String isSalesmanPageRaw, String distributorIdRaw) {
		if (PopularizeH5BrokerageCountReadService.looksLikeSalesmanPageFlag(isSalesmanPageRaw)) {
			return brokerageCountSalesmanBranch(companyId, userId, distributorIdRaw);
		}
		return brokerageCountPromoterBranch(companyId, userId);
	}

	public Map<String, Object> brokeragePointCount(long companyId, long userId) {
		Map<String, Object> stats = loadPromoterStatistics(companyId, userId);
		int pointTotal = (int) longOrZero(stats.get("pointTotal"));

		LambdaQueryWrapper<Brokerage> wrapper =
				new LambdaQueryWrapper<Brokerage>()
						.select(Brokerage::getSource, Brokerage::getIsClose, Brokerage::getRebatePoint)
						.eq(Brokerage::getCompanyId, companyId)
						.eq(Brokerage::getUserId, userId)
						.eq(Brokerage::getCommissionType, "point");
		List<Brokerage> rows = brokerageMapper.selectList(wrapper);

		BigInteger orderNoCloseAcc = BigInteger.ZERO;
		BigInteger orderCloseAcc = BigInteger.ZERO;
		BigInteger orderTeamNoCloseAcc = BigInteger.ZERO;
		BigInteger orderTeamCloseAcc = BigInteger.ZERO;

		for (Brokerage item : rows) {
			String source = item.getSource();
			boolean closed = Boolean.TRUE.equals(item.getIsClose());
			BigInteger delta = parseRebatePointBigInteger(item.getRebatePoint());
			if ("order".equals(source)) {
				if (closed) {
					orderCloseAcc = orderCloseAcc.add(delta);
				} else {
					orderNoCloseAcc = orderNoCloseAcc.add(delta);
				}
			} else if ("order_team".equals(source)) {
				if (closed) {
					orderTeamCloseAcc = orderTeamCloseAcc.add(delta);
				} else {
					orderTeamNoCloseAcc = orderTeamNoCloseAcc.add(delta);
				}
			}
		}

		int orderNoCloseRebateInt = orderNoCloseAcc.intValue();
		int orderCloseRebateInt = orderCloseAcc.intValue();
		int orderTeamNoCloseRebateInt = orderTeamNoCloseAcc.intValue();
		int orderTeamCloseRebateInt = orderTeamCloseAcc.intValue();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("grand_point_total", bcaddInt(orderCloseRebateInt, orderTeamCloseRebateInt));
		out.put("point_total", Integer.valueOf(pointTotal));
		out.put("rebate_point", Integer.valueOf(0));
		out.put("order_no_close_rebate", Integer.valueOf(orderNoCloseRebateInt));
		out.put("order_close_rebate", Integer.valueOf(orderCloseRebateInt));
		out.put("order_total", bcaddInt(orderNoCloseRebateInt, orderCloseRebateInt));
		out.put("order_team_no_close_rebate", Integer.valueOf(orderTeamNoCloseRebateInt));
		out.put("order_team_close_rebate", Integer.valueOf(orderTeamCloseRebateInt));
		out.put("order_team_total", bcaddInt(orderTeamNoCloseRebateInt, orderTeamCloseRebateInt));
		return out;
	}

	private Map<String, Object> brokerageCountSalesmanBranch(long companyId, long userId, String distributorIdRaw) {
		long distScalar = parseDistributorIdScalar(distributorIdRaw);
		List<Long> dIds = loadSalespersonShopIds(companyId, userId);

		LinkedHashMap<String, Object> filterCount = new LinkedHashMap<>();
		filterCount.put("company_id", Long.valueOf(companyId));
		filterCount.put("user_id", Long.valueOf(userId));
		if (!dIds.isEmpty()) {
			filterCount.put("dIds", dIds);
		}
		if (distScalar > 0L) {
			filterCount.put("distributor_id", distScalar);
			filterCount.remove("dIds");
		}

		Map<String, Object> aggregateRow =
				salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountAggregate(filterCount);
		LinkedHashMap<String, Object> countDataShop = new LinkedHashMap<>(aggregateRow);

		LinkedHashMap<String, Object> filterWithdraw = new LinkedHashMap<>();
		if (distScalar > 0L) {
			filterWithdraw.put("distributor_id", distScalar);
		} else if (!dIds.isEmpty()) {
			filterWithdraw.put("distributor_id", dIds);
		}

		long sumWithdrawalMoney = sumWithdrawalMoneyFen(companyId, userId, filterWithdraw);

		log.debug(
				"brokerageCount salesman filters: filter_count={}, filter_withdrow={}",
				summarizeFilterShape(filterCount),
				summarizeFilterShape(filterWithdraw));

		int rebateClose = intOrZero(countDataShop.get("rebate_sum_close"));
		countDataShop.put("sum_withdrawal_money", (int) sumWithdrawalMoney);
		countDataShop.put("cashWithdrawalRebate", (int) (rebateClose - sumWithdrawalMoney));
		countDataShop.put("payedRebate", (int) sumWithdrawalMoney);
		countDataShop.put("orderRebate", intOrZero(countDataShop.get("rebate_sum")));
		countDataShop.put("noCloseRebate", intOrZero(countDataShop.get("rebate_sum_noclose")));
		countDataShop.put("orderCloseRebate", rebateClose);
		countDataShop.put("rebateTotal", intOrZero(countDataShop.get("rebate_sum")));
		return countDataShop;
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
			long v;
			if (sid instanceof Number n) {
				v = n.longValue();
			} else {
				try {
					v = Long.parseLong(String.valueOf(sid).trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private long sumWithdrawalMoneyFen(long companyId, long userId, Map<String, Object> filterWithdraw) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT money FROM distribution_cash_withdrawal ");
		sql.append("WHERE company_id = :companyId AND user_id = :userId ");
		sql.append("AND status IN ('apply','process','success') ");
		MapSqlParameterSource p =
				new MapSqlParameterSource()
						.addValue("companyId", companyId)
						.addValue("userId", String.valueOf(userId));
		Object distVal = filterWithdraw.get("distributor_id");
		if (distVal instanceof List<?> list && !list.isEmpty()) {
			List<String> ids = new ArrayList<>();
			for (Object el : list) {
				if (el == null) {
					continue;
				}
				ids.add(String.valueOf(el instanceof Number n ? n.longValue() : el));
			}
			if (!ids.isEmpty()) {
				sql.append("AND distributor_id IN (:distributorIds) ");
				p.addValue("distributorIds", ids);
			}
		} else if (distVal instanceof Number n && n.longValue() > 0L) {
			sql.append("AND distributor_id = :distributorIdStr ");
			p.addValue("distributorIdStr", String.valueOf(n.longValue()));
		}
		sql.append("ORDER BY created DESC");
		List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(sql.toString(), p);
		long sum = 0L;
		for (Map<String, Object> row : records) {
			sum += moneyToFenLong(row.get("money"));
		}
		return sum;
	}

	private static long moneyToFenLong(Object money) {
		if (money == null) {
			return 0L;
		}
		if (money instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(money).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Map<String, Object> brokerageCountPromoterBranch(long companyId, long userId) {
		Map<String, Object> stats = loadPromoterStatistics(companyId, userId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("payedRebate", (int) longOrZero(stats.get("payedRebate")));
		data.put("itemTotalPrice", (int) longOrZero(stats.get("itemTotalPrice")));
		data.put("cashWithdrawalRebate", (int) longOrZero(stats.get("cashWithdrawalRebate")));
		data.put("noCloseRebate", (int) longOrZero(stats.get("noCloseRebate")));
		data.put("rebateTotal", (int) longOrZero(stats.get("rebateTotal")));
		data.put("freezeCashWithdrawalRebate", (int) longOrZero(stats.get("freezeCashWithdrawalRebate")));

		QueryWrapper<TaskBrokerageCount> tw = new QueryWrapper<>();
		tw.select("COALESCE(SUM(rebate_money),0) AS s");
		tw.eq("company_id", companyId).eq("user_id", userId);
		List<Map<String, Object>> taskRows = taskBrokerageCountMapper.selectMaps(tw);
		data.put("taskBrokerageItemTotalFee", (int) longFromSumMap(firstRow(taskRows), "s"));

		int orderNoCloseRebate = (int) sumBrokerageRebate(companyId, userId, "order", false);
		int orderCloseRebate = (int) sumBrokerageRebate(companyId, userId, "order", true);
		data.put("orderNoCloseRebate", orderNoCloseRebate);
		data.put("orderCloseRebate", orderCloseRebate);
		data.put("orderRebate", orderNoCloseRebate + orderCloseRebate);

		int orderTeamNoCloseRebate = (int) sumBrokerageRebate(companyId, userId, "order_team", false);
		int orderTeamCloseRebate = (int) sumBrokerageRebate(companyId, userId, "order_team", true);
		data.put("orderTeamNoCloseRebate", orderTeamNoCloseRebate);
		data.put("orderTeamCloseRebate", orderTeamCloseRebate);
		data.put("orderTeamRebate", orderTeamNoCloseRebate + orderTeamCloseRebate);

		Map<String, Object> cfg = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
		data.put("limit_time", cfg != null ? cfg.get("limit_time") : null);
		return data;
	}

	private Map<String, Object> loadPromoterStatistics(long companyId, long userId) {
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

	private long sumBrokerageRebate(long companyId, long userId, String source, boolean isClose) {
		QueryWrapper<Brokerage> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(rebate),0) AS s");
		q.eq("company_id", companyId)
				.eq("user_id", userId)
				.eq("commission_type", "money")
				.eq("source", source)
				.eq("is_close", isClose);
		List<Map<String, Object>> rows = brokerageMapper.selectMaps(q);
		return longFromSumMap(firstRow(rows), "s");
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

	private static int intOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	public static boolean looksLikeSalesmanPageFlag(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equalsIgnoreCase(t);
	}

	private static long parseDistributorIdScalar(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equalsIgnoreCase(t)) {
			return 0L;
		}
		if (!DIGITS_ONLY.matcher(t).matches()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String summarizeFilterShape(Map<String, Object> m) {
		if (m == null || m.isEmpty()) {
			return "{}";
		}
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			Object v = e.getValue();
			if (v instanceof List<?> l) {
				sb.append(e.getKey()).append("=list(size=").append(l.size()).append(')');
			} else {
				sb.append(e.getKey()).append("=present");
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private static BigInteger parseRebatePointBigInteger(String raw) {
		if (raw == null) {
			return BigInteger.ZERO;
		}
		String trimmed = raw.trim();
		if (!StringUtils.hasText(trimmed)) {
			return BigInteger.ZERO;
		}
		try {
			return new BigInteger(trimmed);
		} catch (NumberFormatException e) {
			return BigInteger.ZERO;
		}
	}

	private static String bcaddInt(int a, int b) {
		return Long.toString((long) a + (long) b);
	}
}
