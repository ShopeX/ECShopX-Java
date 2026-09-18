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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import cn.shopex.ecshopx.orders.domain.dto.MonitorSourcePaidOrderAggRow;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MonitorsStatsService {

	private static final String REDIS_PREFIX = "datecube_tracklog";

	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private static final RowMapper<Map<String, Object>> REL_ROW_MAPPER =
			new RowMapper<>() {
				@Override
				public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					row.put("monitor_id", rs.getObject("monitor_id"));
					row.put("source_id", rs.getObject("source_id"));
					row.put("company_id", rs.getObject("company_id"));
					return row;
				}
			};

	private final JdbcTemplate jdbcTemplate;
	private final SourcesMapper sourcesMapper;
	private final StringRedisTemplate datacubeRedis;
	private final NormalOrdersMapper normalOrdersMapper;

	public MonitorsStatsService(
			JdbcTemplate jdbcTemplate,
			SourcesMapper sourcesMapper,
			@Qualifier("datacubeStringRedisTemplate") StringRedisTemplate datacubeRedis,
			NormalOrdersMapper normalOrdersMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.sourcesMapper = sourcesMapper;
		this.datacubeRedis = datacubeRedis;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public Map<String, Object> buildStats(
			long companyId,
			String monitorIdParam,
			String dateType,
			String beginDate,
			String endDate) {
		String trimmedMonitor = monitorIdParam != null ? monitorIdParam.trim() : "";
		Object rawMonitorId = rawMonitorId(trimmedMonitor);

		DateRange range = resolveDateRange(dateType, beginDate, endDate);
		List<String> ymdFields = buildYmdFieldList(range.start(), range.stop());
		long startEpoch = range.start().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		long endEpoch = range.stop().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();

		List<Map<String, Object>> relRows;
		try {
			relRows =
					jdbcTemplate.query(
							"SELECT monitor_id, source_id, company_id FROM datacube_relsources WHERE company_id = ? AND"
									+ " monitor_id = ? ORDER BY source_id ASC",
							REL_ROW_MAPPER,
							companyId,
							rawMonitorId);
		} catch (DataAccessException e) {
			throw new ResourceException(truncateDriverMessage(e));
		}
		if (relRows == null) {
			relRows = new ArrayList<>();
		}

		Map<String, Object> statsTotal = new LinkedHashMap<>();
		statsTotal.put("total_pv", 0L);
		statsTotal.put("total_uv", 0L);
		statsTotal.put("total_visitor", 0L);
		statsTotal.put("total_member_visitor", 0L);
		statsTotal.put("total_pay_num", 0L);
		statsTotal.put("total_pay_amount", BigDecimal.ZERO);

		String companyStr = String.valueOf(companyId);
		String monitorStr = trimmedMonitor;

		for (Map<String, Object> v : relRows) {
			long sourceId = longFromJdbcObject(v.get("source_id"));
			Sources src = sourcesMapper.selectById(sourceId);
			if (src == null) {
				throw new ResourceException("source_id=" + sourceId + "的来源不存在");
			}
			v.put("source_name", src.getSourceName() != null ? src.getSourceName() : "");

			Object rowCompany = v.get("company_id");
			Object rowMonitor = v.get("monitor_id");
			Object rowSource = v.get("source_id");
			String rowCompanyStr = String.valueOf(rowCompany);
			String rowMonitorStr = String.valueOf(rowMonitor);
			String rowSourceStr = String.valueOf(rowSource);

			long registerNum = sumHmget(registerListKey(rowCompanyStr, rowMonitorStr, rowSourceStr), ymdFields);
			long entriesNum = sumHmget(entriesListKey(rowCompanyStr, rowMonitorStr, rowSourceStr), ymdFields);

			v.put("register_num", registerNum);
			v.put("entries_num", entriesNum);

			long totalPv = sumHmget(pageViewKey(rowCompanyStr, rowMonitorStr, rowSourceStr), ymdFields);
			v.put("total_pv", totalPv);
			statsTotal.put("total_pv", (Long) statsTotal.get("total_pv") + totalPv);

			long totalUv = sumHmget(uniqueVisitorKey(rowCompanyStr, rowMonitorStr, rowSourceStr), ymdFields);
			v.put("total_uv", totalUv);
			statsTotal.put("total_uv", (Long) statsTotal.get("total_uv") + totalUv);

			long totalMemberVisitor =
					sumHmget(memberVisitorKey(rowCompanyStr, rowMonitorStr, rowSourceStr), ymdFields);
			v.put("total_member_visitor", totalMemberVisitor);
			statsTotal.put(
					"total_member_visitor",
					(Long) statsTotal.get("total_member_visitor") + totalMemberVisitor);

			long totalVisitor = totalUv - totalMemberVisitor;
			v.put("total_visitor", totalVisitor);
			statsTotal.put("total_visitor", (Long) statsTotal.get("total_visitor") + totalVisitor);

			long rowCompanyId = longFromJdbcObject(rowCompany);
			MonitorSourcePaidOrderAggRow paid =
					normalOrdersMapper.selectPaidAggsByMonitorSource(
							rowCompanyId, rowMonitor, rowSource, startEpoch, endEpoch);
			long payNum = paid != null && paid.getPayNum() != null ? paid.getPayNum() : 0L;
			BigDecimal payAmount =
					paid != null && paid.getPayAmount() != null ? paid.getPayAmount() : BigDecimal.ZERO;

			v.put("total_pay_num", payNum);
			v.put("total_pay_amount", payAmount);
			statsTotal.put("total_pay_num", (Long) statsTotal.get("total_pay_num") + payNum);
			statsTotal.put(
					"total_pay_amount",
					((BigDecimal) statsTotal.get("total_pay_amount")).add(payAmount));

			v.put("conversion_rate", formatConversionRate(totalUv, payNum));
		}

		long totalRegisterNum = sumHmget(registerTotalKey(companyStr, monitorStr), ymdFields);
		long totalEntriesNum = sumHmget(entriesTotalKey(companyStr, monitorStr), ymdFields);
		statsTotal.put("total_register_num", totalRegisterNum);
		statsTotal.put("total_entries_num", totalEntriesNum);

		long aggUv = (Long) statsTotal.get("total_uv");
		long aggPayNum = (Long) statsTotal.get("total_pay_num");
		statsTotal.put("total_conversion_rate", formatConversionRate(aggUv, aggPayNum));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("stats_total", statsTotal);
		result.put("stats_list", relRows);
		return result;
	}

	private static Object rawMonitorId(String trimmed) {
		if (!StringUtils.hasText(trimmed)) {
			return trimmed;
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return trimmed;
		}
	}

	private record DateRange(LocalDate start, LocalDate stop) {}

	private DateRange resolveDateRange(String dateType, String beginDate, String endDate) {
		ZoneId z = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(z);
		return switch (dateType) {
			case "today" -> new DateRange(today, today);
			case "yesterday" -> {
				LocalDate y = today.minusDays(1);
				yield new DateRange(y, y);
			}
			case "before7days" -> new DateRange(today.minusDays(7), today.minusDays(1));
			case "before30days" -> new DateRange(today.minusDays(30), today.minusDays(1));
			case "beforemonth" -> {
				LocalDate firstThisMonth = today.withDayOfMonth(1);
				yield new DateRange(firstThisMonth.minusMonths(1), firstThisMonth.minusDays(1));
			}
			case "custom" -> {
				LocalDate start = parseFlexibleDate(beginDate, "begin_date");
				LocalDate stop = parseFlexibleDate(endDate, "end_date");
				yield new DateRange(start, stop);
			}
			default -> throw new IllegalStateException("unexpected date_type: " + dateType);
		};
	}

	private LocalDate parseFlexibleDate(String raw, String fieldKey) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ResourceException("获取监控页面的来源统计出错: " + fieldKey.replace('_', ' ') + " 不能为空");
		}
		String t = raw.trim();
		List<DateTimeFormatter> formatters = new ArrayList<>();
		formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
		formatters.add(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		formatters.add(DateTimeFormatter.ofPattern("yyyy-M-d"));
		for (DateTimeFormatter f : formatters) {
			try {
				return LocalDate.parse(t, f);
			} catch (DateTimeParseException ignored) {
			}
		}
		try {
			if (t.length() == 8 && t.chars().allMatch(Character::isDigit)) {
				return LocalDate.parse(t, YMD);
			}
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return ldt.toLocalDate();
		} catch (DateTimeParseException ignored) {
		}
		throw new ResourceException("获取监控页面的来源统计出错: " + fieldKey.replace('_', ' ') + " 日期格式无效");
	}

	private static List<String> buildYmdFieldList(LocalDate start, LocalDate stop) {
		List<String> fields = new ArrayList<>();
		for (LocalDate d = start; !d.isAfter(stop); d = d.plusDays(1)) {
			fields.add(d.format(YMD));
		}
		return fields;
	}

	private long sumHmget(String redisKey, List<String> fields) {
		if (fields.isEmpty()) {
			return 0L;
		}
		List<Object> vals = datacubeRedis.opsForHash().multiGet(redisKey, new ArrayList<>(fields));
		if (vals == null) {
			return 0L;
		}
		long sum = 0L;
		for (Object v : vals) {
			sum += parseLongLoose(v);
		}
		return sum;
	}

	private static long parseLongLoose(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return new BigDecimal(s).setScale(0, RoundingMode.DOWN).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String pageViewKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":viewnum:" + companyId + "|" + monitorId + ":page_view:" + sourceId;
	}

	private static String uniqueVisitorKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":viewnum:" + companyId + "|" + monitorId + ":unique_visitor:" + sourceId;
	}

	private static String memberVisitorKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":viewnum:" + companyId + "|" + monitorId + ":member_visitor:" + sourceId;
	}

	private static String registerListKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":registernum:" + companyId + "|" + monitorId + ":list:" + sourceId;
	}

	private static String entriesListKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":entriesnum:" + companyId + "|" + monitorId + ":list:" + sourceId;
	}

	private static String registerTotalKey(String companyId, String monitorId) {
		return REDIS_PREFIX + ":registernum:" + companyId + "|" + monitorId + ":total";
	}

	private static String entriesTotalKey(String companyId, String monitorId) {
		return REDIS_PREFIX + ":entriesnum:" + companyId + "|" + monitorId + ":total";
	}

	private static String formatConversionRate(long totalUv, long payNum) {
		if (totalUv > 0) {
			double pct = payNum * 100.0 / totalUv;
			return String.format(Locale.US, "%.2f", pct) + "\uFF05";
		}
		return "0%";
	}

	private static long longFromJdbcObject(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String truncateDriverMessage(DataAccessException e) {
		String m = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
		if (m == null) {
			return "数据库访问失败";
		}
		return m.length() > 2000 ? m.substring(0, 2000) : m;
	}
}
