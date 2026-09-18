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
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.support.DistributorIdParamParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeBrokerageListReadService {

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private static final DateTimeFormatter PLAN_CLOSE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final BrokerageMapper brokerageMapper;
	private final SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService;
	private final ObjectMapper objectMapper;

	public PopularizeBrokerageListReadService(
			BrokerageMapper brokerageMapper,
			SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService,
			ObjectMapper objectMapper) {
		this.brokerageMapper = brokerageMapper;
		this.salesmanBrokerageLogsQueryService = salesmanBrokerageLogsQueryService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getBrokerageList(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String userIdRaw,
			String isCloseRaw,
			String distributorIdRaw) {
		int page = parseRequiredPositiveInt(pageRaw, "分页参数错误");
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		int pageSize = parseRequiredPositiveInt(pageSizeRaw, "每页最多查询50条数据");
		if (pageSize < 1 || pageSize > 50) {
			throw new BadRequestException("每页最多查询50条数据");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		if (userIdRaw != null && StringUtils.hasText(userIdRaw.trim())) {
			String u = userIdRaw.trim();
			if (!DIGITS_ONLY.matcher(u).matches()) {
				throw new BadRequestException("user_id 格式错误");
			}
			try {
				long uid = Long.parseLong(u);
				if (uid <= 0L) {
					throw new BadRequestException("user_id 格式错误");
				}
				filter.put("user_id", uid);
			} catch (NumberFormatException e) {
				throw new BadRequestException("user_id 格式错误");
			}
		}

		if (isCloseRaw != null && StringUtils.hasText(isCloseRaw.trim())) {
			filter.put("is_close", Boolean.valueOf("true".equals(isCloseRaw.trim())));
		}

		if (distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim())) {
			List<Long> ids = DistributorIdParamParser.parseDistributorIdListParam(distributorIdRaw.trim(), "distributor_id");
			if (ids != null && !ids.isEmpty()) {
				filter.put("dIds", ids);
			}
		}

		boolean salesman = filter.containsKey("dIds") && filter.get("dIds") instanceof List<?> dList && !dList.isEmpty();
		if (salesman) {
			return getBrokerageListForSalesman(companyId, filter, page, pageSize);
		}

		return getBrokerageListDefault(companyId, filter, page, pageSize);
	}

	private Map<String, Object> getBrokerageListForSalesman(
			long companyId, Map<String, Object> filter, int page, int pageSize) {
		long total = salesmanBrokerageLogsQueryService.countByFilter(filter);
		List<Map<String, Object>> list = salesmanBrokerageLogsQueryService.selectPageByFilter(filter, pageSize, page);
		List<Map<String, Object>> rawSp = salesmanBrokerageLogsQueryService.enrichListWithSalesperson(companyId, list);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", list);
		data.put("isSalesmanPage", Integer.valueOf(1));
		Object value = (rawSp == null || rawSp.isEmpty()) ? null : rawSp;
		data.put("data_alesperson", value);
		return data;
	}

	private Map<String, Object> getBrokerageListDefault(
			long companyId, Map<String, Object> filter, int page, int pageSize) {
		LambdaQueryWrapper<Brokerage> w =
				new LambdaQueryWrapper<Brokerage>().eq(Brokerage::getCompanyId, companyId).orderByDesc(Brokerage::getCreated);
		if (filter.containsKey("user_id")) {
			w.eq(Brokerage::getUserId, (Long) filter.get("user_id"));
		}
		if (filter.containsKey("is_close")) {
			w.eq(Brokerage::getIsClose, (Boolean) filter.get("is_close"));
		}
		Page<Brokerage> pg = new Page<>(page, pageSize);
		Page<Brokerage> result = brokerageMapper.selectPage(pg, w);
		long total = result.getTotal();
		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (Brokerage e : result.getRecords()) {
			rowMaps.add(toDefaultListRow(e));
		}
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", rowMaps);
		return data;
	}

	private int parseRequiredPositiveInt(String raw, String onFailureMessage) {
		if (raw == null) {
			throw new BadRequestException(onFailureMessage);
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException(onFailureMessage);
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(onFailureMessage);
		}
	}

	private Map<String, Object> toDefaultListRow(Brokerage e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("brokerage_type", e.getBrokerageType());
		m.put("order_id", e.getOrderId());
		m.put("user_id", e.getUserId());
		m.put("buy_user_id", e.getBuyUserId());
		m.put("source", e.getSource());
		m.put("order_type", e.getOrderType());
		m.put("company_id", e.getCompanyId());
		m.put("price", e.getPrice());
		m.put("is_close", e.getIsClose());
		Integer planCloseTime = e.getPlanCloseTime();
		m.put("plan_close_time", planCloseTime);
		if (planCloseTime == null) {
			m.put("plan_close_date", null);
		} else {
			Instant inst = Instant.ofEpochSecond(planCloseTime.intValue());
			m.put("plan_close_date", PLAN_CLOSE_FMT.format(inst));
		}
		String commissionType = e.getCommissionType();
		m.put("commission_type", commissionType);
		String type = commissionType;
		Object rebateVal = e.getRebate();
		Object rebatePointVal = e.getRebatePoint();
		if ("point".equals(type)) {
			m.put("rebate", Integer.valueOf(0));
			m.put("rebate_point", normalizeRebatePointOut(rebatePointVal));
		} else {
			m.put("rebate", rebateVal);
			m.put("rebate_point", Integer.valueOf(0));
		}
		String d = e.getDetail();
		if (d == null || d.trim().isEmpty()) {
			m.put("detail", null);
		} else {
			try {
				m.put("detail", objectMapper.readValue(d.trim(), Object.class));
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("detail JSON 格式错误");
			}
		}
		m.put("created", e.getCreated());
		return m;
	}

	private static Object normalizeRebatePointOut(Object rebatePointVal) {
		if (rebatePointVal == null) {
			return null;
		}
		if (rebatePointVal instanceof Number n) {
			return n;
		}
		String s = String.valueOf(rebatePointVal).trim();
		if (!StringUtils.hasText(s)) {
			return rebatePointVal;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return rebatePointVal;
		}
	}
}
