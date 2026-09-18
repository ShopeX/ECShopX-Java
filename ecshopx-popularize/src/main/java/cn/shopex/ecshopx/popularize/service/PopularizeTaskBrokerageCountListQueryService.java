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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.popularize.domain.TaskBrokerage;
import cn.shopex.ecshopx.popularize.domain.TaskBrokerageCount;
import cn.shopex.ecshopx.popularize.mapper.TaskBrokerageCountMapper;
import cn.shopex.ecshopx.popularize.mapper.TaskBrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeTaskBrokerageCountListQueryService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final Map<String, String> ORDER_STATUS_MSG = Map.ofEntries(
			Map.entry("NOTPAY", "未支付"),
			Map.entry("CANCEL", "已取消"),
			Map.entry("CANCEL_WAIT_PROCESS", "取消待处理"),
			Map.entry("DONE", "已完成"),
			Map.entry("PAYED", "已支付"),
			Map.entry("REFUND_SUCCESS", "已退款"),
			Map.entry("WAIT_BUYER_CONFIRM", "待收货"),
			Map.entry("REVIEW_PASS", "审核通过待出库"),
			Map.entry("WAIT_GROUPS_SUCCESS", "等待成团"),
			Map.entry("REFUND_PROCESS", "退款处理中"));

	private static final Map<String, String> STATUS_MSG_AGG = Map.of(
			"wait", "待统计",
			"finish", "已完成",
			"close", "关闭");

	private final TaskBrokerageCountMapper taskBrokerageCountMapper;
	private final TaskBrokerageMapper taskBrokerageMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public PopularizeTaskBrokerageCountListQueryService(
			TaskBrokerageCountMapper taskBrokerageCountMapper,
			TaskBrokerageMapper taskBrokerageMapper,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.taskBrokerageCountMapper = taskBrokerageCountMapper;
		this.taskBrokerageMapper = taskBrokerageMapper;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getTaskBrokerageCountList(
			Map<String, Object> filter, String fields, int page, int pageSize) {
		LambdaQueryWrapper<TaskBrokerageCount> wrapper = buildCountWrapper(filter);
		long totalCount = taskBrokerageCountMapper.selectCount(wrapper);
		List<Map<String, Object>> pageRows = new ArrayList<>();
		if (totalCount > 0) {
			Page<TaskBrokerageCount> pg = new Page<>(page, pageSize, false);
			Page<TaskBrokerageCount> result = taskBrokerageCountMapper.selectPage(pg, wrapper);
			for (TaskBrokerageCount e : result.getRecords()) {
				pageRows.add(countEntityToRow(e));
			}
		}

		Set<Long> pageUserIds = new LinkedHashSet<>();
		for (Map<String, Object> row : pageRows) {
			Object uid = row.get("user_id");
			if (uid instanceof Number n) {
				pageUserIds.add(n.longValue());
			}
		}
		Map<Long, String> memberMobiles = loadDecryptedMobilesByUserIds(
				longFrom(filter.get("company_id")), pageUserIds);

		Set<Long> itemIds = new LinkedHashSet<>();
		Set<Long> userIds = new LinkedHashSet<>();
		for (Map<String, Object> row : pageRows) {
			Object iid = row.get("item_id");
			Object uid = row.get("user_id");
			if (iid instanceof Number n) {
				itemIds.add(n.longValue());
			}
			if (uid instanceof Number n) {
				userIds.add(n.longValue());
			}
		}

		long companyId = longFrom(filter.get("company_id"));
		List<Map<String, Object>> taskDetailRows;
		if (itemIds.isEmpty() || userIds.isEmpty()) {
			taskDetailRows = Collections.emptyList();
		} else {
			List<TaskBrokerage> tasks = taskBrokerageMapper.selectList(
					new LambdaQueryWrapper<TaskBrokerage>()
							.eq(TaskBrokerage::getCompanyId, companyId)
							.in(TaskBrokerage::getItemId, itemIds)
							.in(TaskBrokerage::getUserId, userIds));
			taskDetailRows = new ArrayList<>();
			for (TaskBrokerage t : tasks) {
				taskDetailRows.add(taskEntityToRow(t));
			}
		}

		List<String> orderIds = new ArrayList<>();
		for (Map<String, Object> t : taskDetailRows) {
			Object oid = t.get("order_id");
			if (oid != null && StringUtils.hasText(String.valueOf(oid))) {
				orderIds.add(String.valueOf(oid).trim());
			}
		}
		Map<String, Map<String, Object>> ordersById = loadOrdersByOrderIds(orderIds);
		Set<String> indexAftersales = loadAftersalesClosedOrderIndex(orderIds, companyId);

		for (Map<String, Object> t : taskDetailRows) {
			String oid = String.valueOf(t.get("order_id")).trim();
			Map<String, Object> order = ordersById.get(oid);
			if (order != null) {
				t.put("mobile", order.get("mobile"));
				t.put("order_status", order.get("order_status"));
				String os = order.get("order_status") == null ? "" : String.valueOf(order.get("order_status"));
				t.put("order_status_msg", ORDER_STATUS_MSG.getOrDefault(os, ""));
				Object tf = order.get("total_fee");
				t.put("total_fee", yuanFromOrderTotalFee(tf));
				t.put("order_auto_close_aftersales_time", order.get("order_auto_close_aftersales_time"));
			}
		}

		long nowTime = Instant.now().getEpochSecond();
		for (Map<String, Object> row : pageRows) {
			row.put("total_fee", 0L);
			row.put("finish_num", 0);
			row.put("wait_num", 0);
			row.put("close_num", 0);
			row.put("orders", new ArrayList<Map<String, Object>>());

			long rCompany = longFrom(row.get("company_id"));
			long rItem = longFrom(row.get("item_id"));
			long rUser = longFrom(row.get("user_id"));

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> ordersOut = (List<Map<String, Object>>) row.get("orders");

			for (Map<String, Object> value : taskDetailRows) {
				if (longFrom(value.get("company_id")) != rCompany
						|| longFrom(value.get("item_id")) != rItem
						|| longFrom(value.get("user_id")) != rUser) {
					continue;
				}
				Map<String, Object> orderInfo = new LinkedHashMap<>();
				String orderIdStr = value.get("order_id") == null ? "" : String.valueOf(value.get("order_id")).trim();
				orderInfo.put("order_id", orderIdStr);
				orderInfo.put("mobile", value.get("mobile"));
				orderInfo.put("total_fee", value.get("total_fee"));
				orderInfo.put("order_status", value.get("order_status"));
				orderInfo.put("order_status_msg", value.get("order_status_msg"));
				String st = value.get("status") == null ? "" : String.valueOf(value.get("status"));
				orderInfo.put("status", st);
				orderInfo.put("status_msg", STATUS_MSG_AGG.getOrDefault(st, ""));

				if ("wait".equals(st)) {
					row.put("wait_num", intFrom(row.get("wait_num")) + 1);
				} else if ("close".equals(st)) {
					row.put("close_num", intFrom(row.get("close_num")) + 1);
				} else {
					if (indexAftersales.contains(orderIdStr)) {
						orderInfo.put("status_msg", "已关闭");
						orderInfo.put("status", "close");
						row.put("close_num", intFrom(row.get("close_num")) + 1);
					} else {
						long closeTs = longFrom(value.get("order_auto_close_aftersales_time"));
						if (nowTime < closeTs) {
							orderInfo.put("status_msg", "待统计");
							orderInfo.put("status", "wait");
							row.put("wait_num", intFrom(row.get("wait_num")) + 1);
						} else {
							BigDecimal yuan = toBigDecimal(value.get("total_fee"));
							long addFen = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
							row.put("total_fee", longFrom(row.get("total_fee")) + addFen);
							row.put("finish_num", intFrom(row.get("finish_num")) + 1);
						}
					}
				}
				ordersOut.add(orderInfo);
			}

			Map<String, Object> rebateConf = parseJsonObject(stringFrom(row.get("rebate_conf")));
			row.put("rebate_conf", rebateConf);
			row.put("status", 0);
			row.put("promoter_mobile", memberMobiles.getOrDefault(rUser, ""));

			Object filterThreshold = firstRebateTaskFilter(rebateConf);
			String rebateType = stringFrom(row.get("rebate_type"));
			if ("total_money".equals(rebateType)) {
				if (truthyFilter(filterThreshold)) {
					BigDecimal doneYuan = BigDecimal.valueOf(longFrom(row.get("total_fee")))
							.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
					if (doneYuan.compareTo(toBigDecimal(filterThreshold)) >= 0) {
						row.put("status", 1);
					} else {
						int f = (int) toBigDecimal(filterThreshold).longValue();
						long fen = longFrom(row.get("total_fee"));
						int doneInt = (int) (fen / 100);
						int gap = f - doneInt;
						row.put("limit_desc", "还差 " + gap + " 元达标");
					}
				}
			} else if ("total_num".equals(rebateType)) {
				if (truthyFilter(filterThreshold)) {
					int finishNum = intFrom(row.get("finish_num"));
					int f = (int) toBigDecimal(filterThreshold).longValue();
					if (finishNum >= f) {
						row.put("status", 1);
					} else {
						row.put("limit_desc", "还差 " + (f - finishNum) + " 件达标");
					}
				}
			}
			row.put("rebate_money", computeRebateMoneyFen(row, rebateConf));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", pageRows);
		return out;
	}

	/**
	 * Export path: reload task brokerage rows for current count page and join orders only (no aftersales).
	 */
	public Map<String, Object> taskBrokerageListForExport(List<Map<String, Object>> countRows, long companyId) {
		if (countRows == null || countRows.isEmpty()) {
			return Map.of("total_count", 0L, "list", Collections.emptyList());
		}
		Set<Long> itemIds = new LinkedHashSet<>();
		Set<Long> userIds = new LinkedHashSet<>();
		for (Map<String, Object> row : countRows) {
			Object iid = row.get("item_id");
			Object uid = row.get("user_id");
			if (iid instanceof Number n) {
				itemIds.add(n.longValue());
			}
			if (uid instanceof Number n) {
				userIds.add(n.longValue());
			}
		}
		if (itemIds.isEmpty() || userIds.isEmpty()) {
			return Map.of("total_count", 0L, "list", Collections.emptyList());
		}
		List<TaskBrokerage> tasks = taskBrokerageMapper.selectList(
				new LambdaQueryWrapper<TaskBrokerage>()
						.eq(TaskBrokerage::getCompanyId, companyId)
						.in(TaskBrokerage::getItemId, itemIds)
						.in(TaskBrokerage::getUserId, userIds));
		List<Map<String, Object>> taskDetailRows = new ArrayList<>();
		for (TaskBrokerage t : tasks) {
			taskDetailRows.add(taskEntityToRow(t));
		}
		List<String> orderIds = new ArrayList<>();
		for (Map<String, Object> t : taskDetailRows) {
			Object oid = t.get("order_id");
			if (oid != null && StringUtils.hasText(String.valueOf(oid))) {
				orderIds.add(String.valueOf(oid).trim());
			}
		}
		Map<String, Map<String, Object>> ordersById = loadOrdersByOrderIds(orderIds);
		for (Map<String, Object> t : taskDetailRows) {
			Object oidObj = t.get("order_id");
			if (oidObj == null || !StringUtils.hasText(String.valueOf(oidObj).trim())) {
				continue;
			}
			String oid = String.valueOf(oidObj).trim();
			Map<String, Object> order = ordersById.get(oid);
			if (order != null) {
				t.put("mobile", order.get("mobile"));
				t.put("order_status", order.get("order_status"));
				String os = order.get("order_status") == null ? "" : String.valueOf(order.get("order_status"));
				t.put("order_status_msg", ORDER_STATUS_MSG.getOrDefault(os, ""));
				Object tf = order.get("total_fee");
				t.put("total_fee", yuanFromOrderTotalFee(tf));
			}
		}
		Map<String, Object> wrap = new LinkedHashMap<>();
		wrap.put("total_count", (long) taskDetailRows.size());
		wrap.put("list", taskDetailRows);
		return wrap;
	}

	private LambdaQueryWrapper<TaskBrokerageCount> buildCountWrapper(Map<String, Object> filter) {
		LambdaQueryWrapper<TaskBrokerageCount> w = new LambdaQueryWrapper<>();
		applyTaskBrokerageCountListFilter(w, filter);
		w.orderByDesc(TaskBrokerageCount::getCreated);
		return w;
	}

	private void applyTaskBrokerageCountListFilter(
			LambdaQueryWrapper<TaskBrokerageCount> w, Map<String, Object> filter) {
		w.eq(TaskBrokerageCount::getCompanyId, longFrom(filter.get("company_id")));
		Object userId = filter.get("user_id");
		if (userId != null) {
			w.eq(TaskBrokerageCount::getUserId, longFrom(userId));
		}
		Object itemName = filter.get("item_name|contains");
		if (itemName != null && StringUtils.hasText(String.valueOf(itemName).trim())) {
			w.like(TaskBrokerageCount::getItemName, "%" + String.valueOf(itemName).trim() + "%");
		}
		Object gte = filter.get("updated|gte");
		if (gte != null) {
			w.ge(TaskBrokerageCount::getUpdated, (int) longFrom(gte));
		}
		Object lte = filter.get("updated|lte");
		if (lte != null) {
			w.le(TaskBrokerageCount::getUpdated, (int) longFrom(lte));
		}
		Object planDate = filter.get("plan_date");
		if (planDate != null && StringUtils.hasText(String.valueOf(planDate).trim())) {
			w.eq(TaskBrokerageCount::getPlanDate, String.valueOf(planDate).trim());
		}
	}

	public long sumRebateMoney(Map<String, Object> filter) {
		LambdaQueryWrapper<TaskBrokerageCount> w = new LambdaQueryWrapper<>();
		applyTaskBrokerageCountListFilter(w, filter);
		Long v = taskBrokerageCountMapper.sumRebateMoney(w);
		return v == null ? 0L : v.longValue();
	}

	/**
	 * Builds the filter map for task brokerage count list and export (single source of truth).
	 */
	public Map<String, Object> buildCountListFilter(
			long companyId,
			Long resolvedUserId,
			String itemName,
			String timeStart,
			String timeEnd,
			String planDate) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if (resolvedUserId != null) {
			filter.put("user_id", resolvedUserId);
		}
		if (nonEmptyFilterString(itemName)) {
			filter.put("item_name|contains", itemName.trim());
		}
		if (nonEmptyFilterObject(timeStart) && nonEmptyFilterObject(timeEnd)) {
			int[] range = parseUpdatedRange(timeStart.trim(), timeEnd.trim());
			filter.put("updated|gte", range[0]);
			filter.put("updated|lte", range[1]);
		}
		if (nonEmptyFilterString(planDate)) {
			try {
				filter.put("plan_date", endOfMonthYmd(planDate.trim()));
			} catch (Exception e) {
				// invalid plan_date: do not add filter key
			}
		}
		return filter;
	}

	public static boolean nonEmptyFilterObject(Object raw) {
		if (raw == null) {
			return false;
		}
		if (Boolean.FALSE.equals(raw)) {
			return false;
		}
		if (raw instanceof Number n && n.intValue() == 0) {
			return false;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t) || "0.0".equals(t)) {
				return false;
			}
		}
		return true;
	}

	public static boolean nonEmptyFilterString(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (!StringUtils.hasText(t)) {
			return false;
		}
		return !"0".equals(t);
	}

	private Map<String, Map<String, Object>> loadOrdersByOrderIds(List<String> orderIds) {
		if (orderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<String> distinct = new ArrayList<>(new LinkedHashSet<>(orderIds));
		Map<String, Map<String, Object>> out = new HashMap<>();
		int chunk = 400;
		for (int i = 0; i < distinct.size(); i += chunk) {
			List<String> part = distinct.subList(i, Math.min(i + chunk, distinct.size()));
			MapSqlParameterSource p = new MapSqlParameterSource("orderIds", part);
			String sql =
					"SELECT order_id, mobile, order_status, total_fee, order_auto_close_aftersales_time "
							+ "FROM orders_normal_orders WHERE order_id IN (:orderIds)";
			List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, p);
			for (Map<String, Object> r : rows) {
				String oid = String.valueOf(r.get("order_id")).trim();
				out.put(oid, new LinkedHashMap<>(r));
			}
		}
		return out;
	}

	private Set<String> loadAftersalesClosedOrderIndex(List<String> orderIds, long companyId) {
		if (orderIds.isEmpty()) {
			return Collections.emptySet();
		}
		List<String> distinct = new ArrayList<>(new LinkedHashSet<>(orderIds));
		Set<String> result = new HashSet<>();
		int chunk = 400;
		for (int i = 0; i < distinct.size(); i += chunk) {
			List<String> part = distinct.subList(i, Math.min(i + chunk, distinct.size()));
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("orderIds", part);
			p.addValue("companyId", companyId);
			String sql =
					"SELECT aftersales_bn, order_id FROM aftersales WHERE order_id IN (:orderIds) AND company_id = :companyId";
			List<Map<String, Object>> asRows = namedParameterJdbcTemplate.queryForList(sql, p);
			if (asRows.isEmpty()) {
				continue;
			}
			Map<Object, String> bnToOrderId = new HashMap<>();
			List<Object> bnList = new ArrayList<>();
			for (Map<String, Object> r : asRows) {
				Object bn = r.get("aftersales_bn");
				Object oid = r.get("order_id");
				if (bn == null) {
					continue;
				}
				bnList.add(bn);
				bnToOrderId.put(bn, oid == null ? "" : String.valueOf(oid).trim());
			}
			if (bnList.isEmpty()) {
				continue;
			}
			MapSqlParameterSource p2 = new MapSqlParameterSource();
			p2.addValue("afterBnList", bnList);
			p2.addValue("companyId", companyId);
			String sql2 =
					"SELECT aftersales_bn FROM aftersales_detail WHERE company_id = :companyId "
							+ "AND aftersales_bn IN (:afterBnList) AND aftersales_status = 2 AND progress = 4";
			List<Map<String, Object>> det = namedParameterJdbcTemplate.queryForList(sql2, p2);
			for (Map<String, Object> d : det) {
				Object bn = d.get("aftersales_bn");
				String oid = bnToOrderId.get(bn);
				if (oid != null && !oid.isEmpty()) {
					result.add(oid);
				}
			}
		}
		return result;
	}

	private Map<Long, String> loadDecryptedMobilesByUserIds(long companyId, Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Members> mems = membersMapper.selectList(
				new LambdaQueryWrapper<Members>()
						.eq(Members::getCompanyId, companyId)
						.in(Members::getUserId, userIds));
		Map<Long, String> out = new HashMap<>();
		for (Members m : mems) {
			if (m.getUserId() == null) {
				continue;
			}
			String enc = m.getMobile();
			String plain = enc == null ? "" : sensitiveFieldEncryptor.decrypt(enc);
			out.put(m.getUserId(), plain == null ? "" : plain);
		}
		return out;
	}

	private static Map<String, Object> countEntityToRow(TaskBrokerageCount e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("rebate_type", e.getRebateType());
		m.put("item_id", e.getItemId());
		m.put("item_bn", e.getItemBn());
		m.put("user_id", e.getUserId());
		m.put("company_id", e.getCompanyId());
		m.put("total_fee", e.getTotalFee());
		m.put("item_name", e.getItemName());
		m.put("item_spec_desc", e.getItemSpecDesc());
		m.put("rebate_conf", e.getRebateConf());
		m.put("finish_num", e.getFinishNum());
		m.put("wait_num", e.getWaitNum());
		m.put("close_num", e.getCloseNum());
		m.put("plan_date", e.getPlanDate());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("rebate_money", e.getRebateMoney());
		return m;
	}

	private static Map<String, Object> taskEntityToRow(TaskBrokerage t) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", t.getId());
		m.put("item_id", t.getItemId());
		m.put("user_id", t.getUserId());
		m.put("order_id", t.getOrderId());
		m.put("company_id", t.getCompanyId());
		m.put("item_name", t.getItemName());
		m.put("item_spec_desc", t.getItemSpecDesc());
		m.put("status", t.getStatus());
		m.put("plan_date", t.getPlanDate());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		return m;
	}

	private Map<String, Object> parseJsonObject(String json) {
		if (json == null || !StringUtils.hasText(json)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> m = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			return m != null ? new LinkedHashMap<>(m) : new LinkedHashMap<>();
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private static Object firstRebateTaskFilter(Map<String, Object> rebateConf) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> tasks = (List<Map<String, Object>>) rebateConf.get("rebate_task");
		if (tasks == null || tasks.isEmpty()) {
			return null;
		}
		return tasks.get(0).get("filter");
	}

	private static boolean truthyFilter(Object filter) {
		if (filter == null) {
			return false;
		}
		if (filter instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (filter instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		String s = String.valueOf(filter).trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private long computeRebateMoneyFen(Map<String, Object> row, Map<String, Object> rebateConf) {
		if (rebateConf == null || rebateConf.isEmpty()) {
			return 0L;
		}
		Object typeObj = rebateConf.get("rebate_task_type");
		String rebateTaskType = typeObj == null ? "" : String.valueOf(typeObj);
		BigDecimal currentFilter = BigDecimal.ZERO;
		String rebateType = stringFrom(row.get("rebate_type"));
		if ("total_money".equals(rebateType)) {
			currentFilter = BigDecimal.valueOf(longFrom(row.get("total_fee")))
					.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
		} else if ("total_num".equals(rebateType)) {
			currentFilter = BigDecimal.valueOf(intFrom(row.get("finish_num")));
		}
		long rebateMoney = 0L;
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> tasks = (List<Map<String, Object>>) rebateConf.get("rebate_task");
		if (tasks == null) {
			return 0L;
		}
		for (Map<String, Object> rebateTask : tasks) {
			Object f = rebateTask.get("filter");
			if (!truthyFilter(f)) {
				continue;
			}
			BigDecimal fBd = toBigDecimal(f);
			if (currentFilter.compareTo(fBd) < 0) {
				continue;
			}
			if ("money".equals(rebateTaskType)) {
				Object money = rebateTask.get("money");
				long m = money instanceof Number n ? n.longValue() : parseLongSafe(money);
				rebateMoney = m > 0 ? m * 100 : 0;
			} else {
				Object tf = rebateTask.get("total_fee");
				Object ratio = rebateTask.get("ratio");
				BigDecimal tfBd = toBigDecimal(tf);
				BigDecimal rBd = toBigDecimal(ratio);
				if (rBd.compareTo(BigDecimal.ZERO) > 0) {
					rebateMoney = tfBd.multiply(rBd).setScale(0, RoundingMode.HALF_UP).longValue();
				} else {
					rebateMoney = 0L;
				}
			}
		}
		return rebateMoney;
	}

	private static long parseLongSafe(Object o) {
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static BigDecimal yuanFromOrderTotalFee(Object totalFeeDb) {
		if (totalFeeDb == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal fen = toBigDecimal(totalFeeDb);
		return fen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal toBigDecimal(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(o).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intFrom(Object o) {
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

	private static String stringFrom(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	public static String formatYenFromFen(Object fenObj) {
		long fen = longFrom(fenObj);
		BigDecimal yuan = BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return "¥" + yuan.stripTrailingZeros().toPlainString();
	}

	public static String endOfMonthYmd(String planDateInput) {
		String t = planDateInput.trim();
		try {
			LocalDate d = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
			return d.withDayOfMonth(d.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (Exception e) {
			YearMonth ym = YearMonth.parse(t, DateTimeFormatter.ofPattern("yyyy-MM"));
			return ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
	}

	public static int[] parseUpdatedRange(String timeStart, String timeEnd) {
		long start = dayStartEpochSeconds(timeStart.trim());
		long end = dayEndEpochSeconds(timeEnd.trim());
		return new int[] {(int) start, (int) end};
	}

	private static long dayStartEpochSeconds(String raw) {
		ZonedDateTime zdt = parseFlexibleDate(raw).atStartOfDay(SHANGHAI);
		return zdt.toEpochSecond();
	}

	private static long dayEndEpochSeconds(String raw) {
		ZonedDateTime zdt = parseFlexibleDate(raw).atTime(23, 59, 59).atZone(SHANGHAI);
		return zdt.toEpochSecond();
	}

	private static LocalDate parseFlexibleDate(String raw) {
		String t = raw.trim();
		try {
			long epoch = Long.parseLong(t);
			return Instant.ofEpochSecond(epoch).atZone(SHANGHAI).toLocalDate();
		} catch (NumberFormatException e) {
			return LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
		}
	}
}
