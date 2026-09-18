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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminGoodsDataParamBuilder {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	public AdminGoodsDataFilter build(HttpServletRequest request, Map<String, Object> user) {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		LocalDate start = resolveDate(request.getParameter("start"), yesterday);
		LocalDate end = resolveDate(request.getParameter("end"), yesterday);
		validateDateRange(start, end);

		long companyId = longFromUser(user.get("company_id"));
		String operatorType = stringFromUser(user.get("operator_type"));
		long operatorId = longFromUser(user.get("operator_id"));

		boolean orderRestrict = StringUtils.hasText(request.getParameter("order_class"));
		String orderClassValue = orderRestrict ? request.getParameter("order_class").trim() : "";

		List<Long> actIds = parseActIds(request);

		Long merchantIdOrNull = null;
		if (Objects.equals("merchant", operatorType)) {
			Object m = user.get("merchant_id");
			if (m != null) {
				long mid = longFromUser(m);
				if (mid > 0L) {
					merchantIdOrNull = mid;
				}
			}
		}

		return new AdminGoodsDataFilter(
				companyId,
				start.format(ISO_DATE),
				end.format(ISO_DATE),
				orderRestrict,
				orderClassValue,
				List.copyOf(actIds),
				merchantIdOrNull,
				operatorId);
	}

	private static List<Long> parseActIds(HttpServletRequest request) {
		List<Long> out = new ArrayList<>();
		if (!request.getParameterMap().containsKey("act_id")) {
			return out;
		}
		String[] vals = request.getParameterValues("act_id");
		if (vals == null) {
			return out;
		}
		for (String v : vals) {
			if (!StringUtils.hasText(v)) {
				continue;
			}
			for (String part : v.split(",")) {
				String p = part.trim();
				if (p.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(p));
				} catch (NumberFormatException ignored) {
					// drop invalid segment
				}
			}
		}
		return out;
	}

	static LocalDate resolveDate(String raw, LocalDate defaultWhenBlank) {
		if (!StringUtils.hasText(raw)) {
			return defaultWhenBlank;
		}
		try {
			return LocalDate.parse(raw.trim(), ISO_DATE);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("日期格式无效");
		}
	}

	static void validateDateRange(LocalDate start, LocalDate end) {
		if (start.isAfter(end)) {
			throw new ResourceException("结束日期要大于等于开始日期");
		}
		LocalDate yesterday = LocalDate.now().minusDays(1);
		if (end.isAfter(yesterday)) {
			throw new ResourceException("结束日期必须小于今天");
		}
		long spanDays = ChronoUnit.DAYS.between(start, end);
		if (spanDays > 30L) {
			throw new ResourceException("最多查询30天内数据");
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

	private static String stringFromUser(Object o) {
		return o == null ? "" : o.toString();
	}
}
