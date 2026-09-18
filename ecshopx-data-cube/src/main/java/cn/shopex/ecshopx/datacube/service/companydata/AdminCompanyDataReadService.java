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

package cn.shopex.ecshopx.datacube.service.companydata;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.CompanyData;
import cn.shopex.ecshopx.datacube.domain.MerchantData;
import cn.shopex.ecshopx.datacube.mapper.CompanyDataMapper;
import cn.shopex.ecshopx.datacube.mapper.MerchantDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminCompanyDataReadService {

	private static final int FIXED_PAGE_NO = 1;

	private static final int FIXED_PAGE_SIZE = 90;

	private final CompanyDataMapper companyDataMapper;

	private final MerchantDataMapper merchantDataMapper;

	public AdminCompanyDataReadService(CompanyDataMapper companyDataMapper, MerchantDataMapper merchantDataMapper) {
		this.companyDataMapper = companyDataMapper;
		this.merchantDataMapper = merchantDataMapper;
	}

	public Map<String, Object> getCompanyDataResult(HttpServletRequest request, Map<String, Object> user) {
		LocalDate start = resolveStartDate(request);
		LocalDate end = resolveEndDate(request);
		validateDateRange(start, end);

		long companyId = longFromUser(user.get("company_id"));
		String operatorType = stringFromUser(user.get("operator_type"));
		if (Objects.equals("merchant", operatorType)) {
			return queryMerchantData(user, companyId, start, end);
		}
		return queryCompanyData(request, companyId, start, end);
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

	private Map<String, Object> queryMerchantData(Map<String, Object> user, long companyId, LocalDate start, LocalDate end) {
		LambdaQueryWrapper<MerchantData> w = new LambdaQueryWrapper<>();
		w.eq(MerchantData::getCompanyId, companyId);
		w.ge(MerchantData::getCountDate, start).le(MerchantData::getCountDate, end);
		long merchantId = longFromUserNullable(user.get("merchant_id"));
		if (merchantId != 0L) {
			w.eq(MerchantData::getMerchantId, merchantId);
		}
		w.orderByAsc(MerchantData::getCountDate);

		Page<MerchantData> page = new Page<>(FIXED_PAGE_NO, FIXED_PAGE_SIZE, false);
		long total = merchantDataMapper.selectCount(w);
		merchantDataMapper.selectPage(page, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (MerchantData row : page.getRecords()) {
			list.add(toMerchantRow(row));
		}
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("total_count", (int) total);
		inner.put("list", list);
		return inner;
	}

	private Map<String, Object> queryCompanyData(HttpServletRequest request, long companyId, LocalDate start, LocalDate end) {
		boolean orderClassKeyPresent = request.getParameterMap().containsKey("order_class");
		boolean actIdKeyPresent = request.getParameterMap().containsKey("act_id");

		String orderClass = "";
		if (orderClassKeyPresent) {
			String p = request.getParameter("order_class");
			orderClass = p != null ? p : "";
		}
		long actId = 0L;
		if (actIdKeyPresent) {
			actId = parseActIdLong(request.getParameter("act_id"));
		}

		boolean useClassActBranch = StringUtils.hasText(orderClass) && !"0".equals(orderClass);

		LambdaQueryWrapper<CompanyData> w = new LambdaQueryWrapper<>();
		w.eq(CompanyData::getCompanyId, companyId);
		w.ge(CompanyData::getCountDate, start).le(CompanyData::getCountDate, end);
		if (useClassActBranch) {
			w.eq(CompanyData::getOrderClass, orderClass);
			w.eq(CompanyData::getActId, actId);
		} else {
			w.isNull(CompanyData::getOrderClass);
			w.eq(CompanyData::getActId, 0L);
		}
		w.orderByAsc(CompanyData::getCountDate);

		Page<CompanyData> page = new Page<>(FIXED_PAGE_NO, FIXED_PAGE_SIZE, false);
		long total = companyDataMapper.selectCount(w);
		companyDataMapper.selectPage(page, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (CompanyData row : page.getRecords()) {
			list.add(toCompanyRow(row));
		}
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("total_count", (int) total);
		inner.put("list", list);
		return inner;
	}

	private static long parseActIdLong(String raw) {
		if (raw == null || raw.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> toCompanyRow(CompanyData e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put(
				"count_date",
				e.getCountDate() != null ? e.getCountDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
		m.put("company_id", e.getCompanyId());
		m.put("member_count", nullToZeroInt(e.getMemberCount()));
		m.put("aftersales_count", nullToZeroInt(e.getAftersalesCount()));
		m.put("refunded_count", nullToZeroInt(e.getRefundedCount()));
		m.put("amount_payed_count", nullToZeroLong(e.getAmountPayedCount()));
		m.put("order_count", nullToZeroInt(e.getOrderCount()));
		m.put("order_payed_count", nullToZeroInt(e.getOrderPayedCount()));
		m.put("gmv_count", nullToZeroLong(e.getGmvCount()));
		m.put("order_class", e.getOrderClass());
		m.put("act_id", e.getActId() != null ? e.getActId() : 0L);
		return m;
	}

	private static Map<String, Object> toMerchantRow(MerchantData e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put(
				"count_date",
				e.getCountDate() != null ? e.getCountDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
		m.put("company_id", e.getCompanyId());
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
