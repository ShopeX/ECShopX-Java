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

package cn.shopex.ecshopx.datacube.service.distributordata;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.DistributorData;
import cn.shopex.ecshopx.datacube.mapper.DistributorDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDistributorDataReadService {

	private static final int FIXED_PAGE_NO = 1;

	private static final int FIXED_PAGE_SIZE = 90;

	private static final String DISTRIBUTOR_ALL = "all";

	private final DistributorDataMapper distributorDataMapper;

	private final ObjectMapper objectMapper;

	public AdminDistributorDataReadService(
			DistributorDataMapper distributorDataMapper, ObjectMapper objectMapper) {
		this.distributorDataMapper = distributorDataMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorDataResult(HttpServletRequest request, Map<String, Object> user) {
		LocalDate start = resolveStartDate(request);
		LocalDate end = resolveEndDate(request);
		validateDateRange(start, end);

		String operatorType = stringFromUser(user.get("operator_type"));
		Object resolved = resolveDistributorSelection(request, user, operatorType);
		assertShopSelectedIfRequired(operatorType, resolved);
		resolved = applyStaffMultiStoreOverride(user, operatorType, resolved);

		long companyId = longFromUser(user.get("company_id"));
		long merchantIdForFilter = longFromUserNullable(user.get("merchant_id"));

		return getDistributorDataList(companyId, resolved, start, end, operatorType, merchantIdForFilter, user);
	}

	private void assertShopSelectedIfRequired(String operatorType, Object resolved) {
		if ("distributor".equals(operatorType)) {
			return;
		}
		if (resolved == null) {
			throw new ResourceException("店铺必须选择");
		}
	}

	private Object resolveDistributorSelection(
			HttpServletRequest request, Map<String, Object> user, String operatorType) {
		if ("distributor".equals(operatorType)) {
			if (!user.containsKey("distributor_id") || user.get("distributor_id") == null) {
				throw new ResourceException("店铺必须选择");
			}
			return longFromUser(user.get("distributor_id"));
		}
		if (!request.getParameterMap().containsKey("distributor_id")) {
			return null;
		}
		String raw = request.getParameter("distributor_id");
		if (raw != null && DISTRIBUTOR_ALL.equals(raw.trim())) {
			return DISTRIBUTOR_ALL;
		}
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		Long parsed = longOrNull(raw);
		if (parsed == null) {
			throw new ResourceException("参数错误");
		}
		return parsed;
	}

	private Object applyStaffMultiStoreOverride(
			Map<String, Object> user, String operatorType, Object resolved) {
		if (!"staff".equals(operatorType)) {
			return resolved;
		}
		if (!isLooseZero(resolved)) {
			return resolved;
		}
		List<Long> fromJwt = parseLongIdsFromInput(user.get("distributor_ids"));
		if (fromJwt.isEmpty()) {
			return resolved;
		}
		return fromJwt;
	}

	private static boolean isLooseZero(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (o instanceof String s) {
			Long v = longOrNull(s);
			return v == null || v == 0L;
		}
		return false;
	}

	private Map<String, Object> getDistributorDataList(
			long companyId,
			Object distributorSelection,
			LocalDate start,
			LocalDate end,
			String operatorType,
			long merchantIdScalar,
			Map<String, Object> user) {
		if (distributorSelection instanceof List<?> list && !list.isEmpty()) {
			List<Long> distributorIds = new ArrayList<>();
			for (Object o : list) {
				Long v = longOrNull(o);
				if (v != null) {
					distributorIds.add(v);
				}
			}
			if (distributorIds.isEmpty()) {
				throw new ResourceException("参数错误");
			}
			List<Map<String, Object>> rows =
					distributorDataMapper.selectAggregatedByDistributorIds(companyId, distributorIds, start, end);
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("list", normalizeAggregatedRows(rows));
			return inner;
		}
		if (DISTRIBUTOR_ALL.equals(distributorSelection)) {
			if (!"merchant".equals(operatorType)) {
				throw new ResourceException("参数错误");
			}
			List<Long> merchantIds = List.of(merchantIdScalar);
			List<Map<String, Object>> rows =
					distributorDataMapper.selectAggregatedByMerchantIds(companyId, merchantIds, start, end);
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("list", normalizeAggregatedRows(rows));
			return inner;
		}
		if (!(distributorSelection instanceof Long distributorId)) {
			throw new ResourceException("参数错误");
		}
		LambdaQueryWrapper<DistributorData> w = new LambdaQueryWrapper<>();
		w.eq(DistributorData::getCompanyId, companyId);
		w.ge(DistributorData::getCountDate, start).le(DistributorData::getCountDate, end);
		w.eq(DistributorData::getDistributorId, distributorId);
		if ("merchant".equals(operatorType)) {
			w.eq(DistributorData::getMerchantId, longFromUserNullable(user.get("merchant_id")));
		}
		w.orderByAsc(DistributorData::getCountDate);

		Page<DistributorData> page = new Page<>(FIXED_PAGE_NO, FIXED_PAGE_SIZE, false);
		long total = distributorDataMapper.selectCount(w);
		distributorDataMapper.selectPage(page, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorData row : page.getRecords()) {
			list.add(toDistributorRow(row));
		}
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("total_count", (int) total);
		inner.put("list", list);
		return inner;
	}

	private List<Map<String, Object>> normalizeAggregatedRows(List<Map<String, Object>> rows) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			out.add(normalizeAggregatedRow(row));
		}
		return out;
	}

	private Map<String, Object> normalizeAggregatedRow(Map<String, Object> row) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if ("count_date".equalsIgnoreCase(k) && v != null) {
				m.put("count_date", formatCountDateValue(v));
			} else {
				m.put(k, v);
			}
		}
		return m;
	}

	private static String formatCountDateValue(Object v) {
		if (v instanceof LocalDate d) {
			return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		if (v instanceof java.sql.Date d) {
			return d.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		if (v instanceof java.util.Date d) {
			return new java.sql.Date(d.getTime()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		return v.toString();
	}

	private static Map<String, Object> toDistributorRow(DistributorData e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put(
				"count_date",
				e.getCountDate() != null ? e.getCountDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("member_count", nullToZeroInt(e.getMemberCount()));
		m.put("aftersales_count", nullToZeroInt(e.getAftersalesCount()));
		m.put("refunded_count", nullToZeroInt(e.getRefundedCount()));
		m.put("amount_payed_count", nullToZeroLong(e.getAmountPayedCount()));
		m.put("amount_point_payed_count", nullToZeroLong(e.getAmountPointPayedCount()));
		m.put("order_count", nullToZeroInt(e.getOrderCount()));
		m.put("order_point_count", nullToZeroInt(e.getOrderPointCount()));
		m.put("order_payed_count", nullToZeroInt(e.getOrderPayedCount()));
		m.put("order_point_payed_count", nullToZeroInt(e.getOrderPointPayedCount()));
		m.put("gmv_count", nullToZeroLong(e.getGmvCount()));
		m.put("gmv_point_count", nullToZeroLong(e.getGmvPointCount()));
		m.put("merchant_id", e.getMerchantId() != null ? e.getMerchantId() : 0L);
		return m;
	}

	private static int nullToZeroInt(Integer v) {
		return v != null ? v : 0;
	}

	private static long nullToZeroLong(Long v) {
		return v != null ? v : 0L;
	}

	private LocalDate resolveStartDate(HttpServletRequest request) {
		String raw = request.getParameter("start");
		if (!StringUtils.hasText(raw)) {
			return LocalDate.now().minusDays(7);
		}
		return parseIsoDate(raw);
	}

	private LocalDate resolveEndDate(HttpServletRequest request) {
		String raw = request.getParameter("end");
		if (!StringUtils.hasText(raw)) {
			return LocalDate.now().minusDays(1);
		}
		return parseIsoDate(raw);
	}

	private static LocalDate parseIsoDate(String raw) {
		try {
			return LocalDate.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("日期格式无效");
		}
	}

	private void validateDateRange(LocalDate start, LocalDate end) {
		if (start.isAfter(end)) {
			throw new ResourceException("结束日期要大于等于开始日期");
		}
		LocalDate yesterday = LocalDate.now().minusDays(1);
		if (end.isAfter(yesterday)) {
			throw new ResourceException("结束日期必须小于当前日期");
		}
		long spanDays = ChronoUnit.DAYS.between(start, end);
		if (spanDays > 90) {
			throw new ResourceException("最多查询90天内数据");
		}
	}

	private List<Long> parseLongIdsFromInput(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				Long v = longOrNull(o);
				if (v != null) {
					out.add(v);
				}
			}
			return out;
		}
		if (raw instanceof String s && s.trim().startsWith("[")) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node.isArray()) {
					List<Long> out = new ArrayList<>();
					for (JsonNode n : node) {
						if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							Long v = longOrNull(n.asText());
							if (v != null) {
								out.add(v);
							}
						}
					}
					return out;
				}
			} catch (Exception ignored) {
				return List.of();
			}
		}
		Long single = longOrNull(raw);
		if (single != null) {
			return List.of(single);
		}
		return List.of();
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longFromUser(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static long longFromUserNullable(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringFromUser(Object o) {
		return o != null ? o.toString() : "";
	}
}
