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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.util.ReservationNumLimitCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationWxappLimitCheckService {

	private static final List<String> LIMIT_STATUSES = List.of("success", "to_the_shop", "not_to_shop");

	private final ReservationSettingQueryService reservationSettingQueryService;

	private final ReservationRecordMapper reservationRecordMapper;

	private final ObjectMapper objectMapper;

	public ReservationWxappLimitCheckService(
			ReservationSettingQueryService reservationSettingQueryService,
			ReservationRecordMapper reservationRecordMapper,
			ObjectMapper objectMapper) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.reservationRecordMapper = reservationRecordMapper;
		this.objectMapper = objectMapper;
	}

	public void checkLimit(Map<String, Object> postData) {
		long companyId = longVal(postData.get("company_id"));
		ReservationSetting setting =
				reservationSettingQueryService.findByCompanyId(companyId).orElse(null);
		if (setting == null) {
			return;
		}
		String rawLimit = setting.getReservationNumLimit();
		Map<String, Object> limitArr = ReservationNumLimitCodec.parse(rawLimit, objectMapper);
		if (limitArr == null || limitArr.isEmpty()) {
			return;
		}
		Object typeObj = limitArr.get("limit_type");
		if (typeObj == null) {
			return;
		}
		String limitType = Objects.toString(typeObj, "").trim();
		if (!StringUtils.hasText(limitType)) {
			return;
		}

		String dateDay = Objects.toString(postData.get("date_day"), "");
		long toShopDate = startOfDayEpochSeconds(dateDay);
		long userId = longVal(postData.get("user_id"));
		Long labelId = labelIdOrNull(postData.get("label_id"));

		switch (limitType) {
			case "limit_days" -> checkLimitDays(limitArr, toShopDate, userId, labelId, companyId);
			case "limit_nums" -> checkLimitNums(limitArr, toShopDate, userId, labelId, companyId);
			default -> {
				// unknown type: ignore (same as empty config)
			}
		}
	}

	private void checkLimitDays(
			Map<String, Object> limitArr,
			long toShopDate,
			long userId,
			Long labelId,
			long companyId) {
		int limitDays = intVal(limitArr.get("limit_days"));
		if (limitDays <= 0) {
			return;
		}
		long windowSec = 24L * 3600L * limitDays;
		long limitBegin = toShopDate - windowSec;
		long limitEnd = toShopDate + windowSec;
		List<ReservationRecord> rows =
				reservationRecordMapper.selectForLimitDaysWindow(
						companyId, userId, labelId, limitBegin, limitEnd, LIMIT_STATUSES);
		if (rows == null || rows.isEmpty()) {
			return;
		}
		boolean blocked = false;
		for (ReservationRecord r : rows) {
			if (r.getAgreementDate() == null) {
				continue;
			}
			long agg = r.getAgreementDate().longValue();
			if (Math.abs(agg - toShopDate) < windowSec) {
				blocked = true;
				break;
			}
		}
		if (!blocked) {
			return;
		}
		ReservationRecord last = rows.get(rows.size() - 1);
		int noticeEpoch = last.getAgreementDate() != null ? last.getAgreementDate() : (int) toShopDate;
		String noticeYmd =
				LocalDate.ofInstant(Instant.ofEpochSecond(noticeEpoch), ZoneId.systemDefault())
						.format(DateTimeFormatter.ISO_LOCAL_DATE);
		throw new ResourceException(
				"您已经预约了" + noticeYmd + "的课程,我们建议2次课程间隔至少" + limitDays + "天");
	}

	private void checkLimitNums(
			Map<String, Object> limitArr,
			long toShopDate,
			long userId,
			Long labelId,
			long companyId) {
		int limitNums = intVal(limitArr.get("limit_nums"));
		if (limitNums <= 0) {
			return;
		}
		long count =
				reservationRecordMapper.countForLimitNumsSameDay(
						companyId, userId, labelId, (int) toShopDate, LIMIT_STATUSES);
		if (limitNums <= count) {
			throw new ResourceException("该项目一天的预约次数已达上限，不能再预约");
		}
	}

	private static int intVal(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long labelIdOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L ? null : n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long startOfDayEpochSeconds(String ymd) {
		LocalDate d = LocalDate.parse(ymd.trim());
		return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
	}
}
