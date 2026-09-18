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

package cn.shopex.ecshopx.datacube.service.deliverystaff;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDeliveryStaffDataExportParamBuilder {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final DistributorMapper distributorMapper;

	public AdminDeliveryStaffDataExportParamBuilder(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	public AdminDeliveryStaffDataExportFilter build(
			Map<String, Object> user,
			String deliveryStaffName,
			String deliveryStaffMobile,
			String merchantIdQuery,
			String distributorIdQuery,
			String startRaw,
			String endRaw,
			Integer year,
			String month,
			String day) {
		AdminDeliveryStaffDataExportFilter f = new AdminDeliveryStaffDataExportFilter();
		long companyId = toLong(user.get("company_id"));
		f.setCompanyId(companyId);
		f.setOperatorId(readOptionalOperatorId(user));
		Object ot = user.get("operator_type");
		f.setOperatorType(ot != null ? ot.toString() : "");

		long jwtMerchantId = toLong(user.get("merchant_id"));

		if (StringUtils.hasText(deliveryStaffName)) {
			f.setUsername(deliveryStaffName.trim());
		}
		if (StringUtils.hasText(deliveryStaffMobile)) {
			f.setMobilePlain(deliveryStaffMobile.trim());
		}

		long distributorId = toLong(user.get("distributor_id"));
		if (StringUtils.hasText(distributorIdQuery)) {
			try {
				distributorId = Long.parseLong(distributorIdQuery.trim());
			} catch (NumberFormatException e) {
				distributorId = 0L;
			}
		}
		if (distributorId > 0) {
			f.setDistributorId(distributorId);
			f.setMatchDistributorIds(List.of(distributorId));
		}

		if (StringUtils.hasText(merchantIdQuery)) {
			try {
				f.setMerchantIdForOperators(Long.parseLong(merchantIdQuery.trim()));
			} catch (NumberFormatException ignored) {
				// skip invalid
			}
		}

		if (jwtMerchantId > 0 && "merchant".equals(f.getOperatorType())) {
			List<Distributor> dists = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getMerchantId, jwtMerchantId)
					.eq(Distributor::getCompanyId, companyId)
					.select(Distributor::getDistributorId));
			if (dists != null && !dists.isEmpty()) {
				List<Long> ids = new ArrayList<>();
				for (Distributor d : dists) {
					if (d.getDistributorId() != null && d.getDistributorId() > 0) {
						ids.add(d.getDistributorId());
					}
				}
				if (!ids.isEmpty()) {
					f.setMatchDistributorIds(ids);
					f.setMerchantIdForOperators(null);
				}
			}
		}

		f.setStartEpoch(parseEpochOrDefaultStart(startRaw));
		f.setEndEpoch(parseEpochOrDefaultEnd(endRaw));
		applyTimeWindowOverrides(f, year, month, day);
		return f;
	}

	/**
	 * Applies optional time range overrides from {@code year}, {@code month}, or {@code day}.
	 * Start/end are stored as epoch seconds in the Shanghai zone ({@code Asia/Shanghai}): day start at 00:00:00,
	 * day end at 23:59:59. Year, month, and day are three independent rules; if more than one is set,
	 * a later rule replaces the window produced by an earlier one.
	 */
	private void applyTimeWindowOverrides(
			AdminDeliveryStaffDataExportFilter f, Integer year, String month, String day) {
		if (year != null && year != 0) {
			LocalDate first = LocalDate.of(year, 1, 1);
			LocalDate last = LocalDate.of(year, 12, 31);
			f.setStartEpoch(zonedStartOfDay(first));
			f.setEndEpoch(zonedEndOfDay(last));
		}
		if (StringUtils.hasText(month)) {
			String m = month.trim();
			YearMonth ym;
			try {
				ym = YearMonth.parse(m, DateTimeFormatter.ofPattern("yyyy-MM"));
			} catch (DateTimeParseException e) {
				throw new BadRequestException("非法日期参数");
			}
			LocalDate first = ym.atDay(1);
			LocalDate last = ym.atEndOfMonth();
			f.setStartEpoch(zonedStartOfDay(first));
			f.setEndEpoch(zonedEndOfDay(last));
		}
		if (StringUtils.hasText(day)) {
			LocalDate d;
			try {
				d = LocalDate.parse(day.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException e) {
				throw new BadRequestException("非法日期参数");
			}
			f.setStartEpoch(zonedStartOfDay(d));
			f.setEndEpoch(zonedEndOfDay(d));
		}
	}

	private static long zonedStartOfDay(LocalDate date) {
		return date.atStartOfDay(SHANGHAI).toEpochSecond();
	}

	private static long zonedEndOfDay(LocalDate date) {
		return date.atTime(LocalTime.of(23, 59, 59)).atZone(SHANGHAI).toEpochSecond();
	}

	private static long parseEpochOrDefaultStart(String raw) {
		Long v = tryParseEpoch(raw);
		if (v != null) {
			return v;
		}
		ZonedDateTime z = LocalDate.now(SHANGHAI).atStartOfDay(SHANGHAI);
		return z.toEpochSecond();
	}

	private static long parseEpochOrDefaultEnd(String raw) {
		Long v = tryParseEpoch(raw);
		if (v != null) {
			return v;
		}
		ZonedDateTime z = LocalDate.now(SHANGHAI).atTime(LocalTime.of(23, 59, 59)).atZone(SHANGHAI);
		return z.toEpochSecond();
	}

	private static Long tryParseEpoch(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long readOptionalOperatorId(Map<String, Object> ud) {
		Object v = ud.get("operator_id");
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
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
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
}
