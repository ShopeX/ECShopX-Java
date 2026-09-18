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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkShiftCreateService {

	private final ReservationShopDetailPort reservationShopDetailPort;
	private final WorkShiftTypeMapper workShiftTypeMapper;
	private final WorkShiftMapper workShiftMapper;

	public WorkShiftCreateService(
			ReservationShopDetailPort reservationShopDetailPort,
			WorkShiftTypeMapper workShiftTypeMapper,
			WorkShiftMapper workShiftMapper) {
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.workShiftTypeMapper = workShiftTypeMapper;
		this.workShiftMapper = workShiftMapper;
	}

	public Map<String, Object> createWorkShift(
			long companyId,
			long shopId,
			String shiftTypeIdRaw,
			long workDateStartOfDayEpoch,
			long resourceLevelId) {
		Map<String, Object> storeData =
				reservationShopDetailPort.getShopsDetailForWorkShiftCreate(shopId, companyId);

		Object hourObj = storeData.get("hour");
		if (hourObj == null) {
			throw new BadRequestException("门店营业时段 hour 缺失或格式无效");
		}
		String storeOpenTimeRaw = hourObj.toString().trim();
		String[] segments = storeOpenTimeRaw.split("-", -1);
		if (segments.length < 2) {
			throw new BadRequestException("门店营业时段格式无效，需为「开始-结束」以短横线分隔");
		}
		String beginSegment = segments[0].trim();
		String endSegment = segments[1].trim();

		ZoneId zone = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zone);
		String todayYmd = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

		long storeOpenBegin = parseDateTimeConcatToEpoch(todayYmd, beginSegment, zone);
		long storeOpenEnd = parseDateTimeConcatToEpoch(todayYmd, endSegment, zone);

		String trimmedShiftType = shiftTypeIdRaw == null ? "" : shiftTypeIdRaw.trim();
		String shiftBeginStr;
		String shiftEndStr;
		long shiftTypeIdLong;
		if ("-1".equals(trimmedShiftType)) {
			shiftBeginStr = "00:00";
			shiftEndStr = "23:59";
			shiftTypeIdLong = -1L;
		} else {
			try {
				shiftTypeIdLong = Long.parseLong(trimmedShiftType);
			} catch (NumberFormatException ex) {
				throw new BadRequestException("shiftTypeId 格式无效");
			}
			WorkShiftType row = workShiftTypeMapper.selectById(shiftTypeIdLong);
			if (row == null) {
				shiftBeginStr = "00:00";
				shiftEndStr = "23:59";
			} else {
				shiftBeginStr = row.getBeginTime() != null ? row.getBeginTime().trim() : "00:00";
				shiftEndStr = row.getEndTime() != null ? row.getEndTime().trim() : "23:59";
			}
		}

		long shiftBegin = parseDateTimeConcatToEpoch(todayYmd, shiftBeginStr, zone);
		long shiftEnd = parseDateTimeConcatToEpoch(todayYmd, shiftEndStr, zone);

		if (!"-1".equals(trimmedShiftType)
				&& (storeOpenBegin > shiftBegin || storeOpenEnd < shiftEnd)) {
			throw new ResourceException("排班时间超出门店开店时间:" + storeOpenTimeRaw);
		}

		int now = (int) Instant.now().getEpochSecond();
		WorkShift entity = new WorkShift();
		entity.setCompanyId(companyId);
		entity.setShopId(shopId);
		entity.setResourceLevelId(resourceLevelId);
		entity.setWorkDate(workDateStartOfDayEpoch);
		entity.setShiftTypeId(shiftTypeIdLong);
		entity.setCreated(now);
		entity.setUpdated(now);

		workShiftMapper.insert(entity);
		if (entity.getId() == null) {
			throw new IllegalStateException("id not generated");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", companyId);
		out.put("shop_id", shopId);
		out.put("resource_level_id", resourceLevelId);
		out.put("work_date", workDateStartOfDayEpoch);
		out.put("shift_type_id", shiftTypeIdLong);
		return out;
	}

	private static long parseDateTimeConcatToEpoch(String dateYmd, String timePart, ZoneId zone) {
		String t = timePart == null ? "" : timePart.trim();
		try {
			LocalTime lt = LocalTime.parse(t, DateTimeFormatter.ISO_LOCAL_TIME);
			LocalDate d = LocalDate.parse(dateYmd, DateTimeFormatter.ISO_LOCAL_DATE);
			return d.atTime(lt).atZone(zone).toEpochSecond();
		} catch (DateTimeParseException ex) {
			throw new BadRequestException("营业时段或班次时间格式无效: " + t);
		}
	}
}
