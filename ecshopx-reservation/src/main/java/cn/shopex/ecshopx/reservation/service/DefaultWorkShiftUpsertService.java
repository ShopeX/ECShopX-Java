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
import cn.shopex.ecshopx.reservation.domain.DefaultWorkShift;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.DefaultWorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.util.WorkShiftDataCodec;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultWorkShiftUpsertService {

	private static final Map<String, String> WEEKDAY_CN =
			Map.ofEntries(
					Map.entry("monday", "周一"),
					Map.entry("tuesday", "周二"),
					Map.entry("wednesday", "周三"),
					Map.entry("thursday", "周四"),
					Map.entry("friday", "周五"),
					Map.entry("saturday", "周六"),
					Map.entry("sunday", "周日"));

	private final ReservationShopDetailPort reservationShopDetailPort;
	private final WorkShiftTypeMapper workShiftTypeMapper;
	private final DefaultWorkShiftMapper defaultWorkShiftMapper;

	public DefaultWorkShiftUpsertService(
			ReservationShopDetailPort reservationShopDetailPort,
			WorkShiftTypeMapper workShiftTypeMapper,
			DefaultWorkShiftMapper defaultWorkShiftMapper) {
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.workShiftTypeMapper = workShiftTypeMapper;
		this.defaultWorkShiftMapper = defaultWorkShiftMapper;
	}

	public Map<String, Object> upsertDefaultWorkShift(
			long companyId, long shopId, Map<String, Map<String, String>> workShiftData) {
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

		for (Map.Entry<String, Map<String, String>> e : workShiftData.entrySet()) {
			String weekKey = e.getKey();
			Map<String, String> inner = e.getValue();
			String typeIdRaw = inner.get("typeId");
			if (typeIdRaw == null) {
				typeIdRaw = "";
			} else {
				typeIdRaw = typeIdRaw.trim();
			}

			String shiftBeginStr;
			String shiftEndStr;
			if ("-1".equals(typeIdRaw)) {
				shiftBeginStr = "00:00";
				shiftEndStr = "23:59";
			} else {
				long typeIdLong = Long.parseLong(typeIdRaw);
				WorkShiftType row = workShiftTypeMapper.selectById(typeIdLong);
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

			if (!"-1".equals(typeIdRaw)
					&& (storeOpenBegin > shiftBegin || storeOpenEnd < shiftEnd)) {
				String cn =
						WEEKDAY_CN.getOrDefault(
								weekKey.toLowerCase(Locale.ROOT), weekKey);
				throw new ResourceException(
						cn + "的排班时间超出门店开店时间:" + storeOpenTimeRaw);
			}
		}

		String serialized = WorkShiftDataCodec.serializeWorkShiftData(workShiftData);

		DefaultWorkShift existing =
				defaultWorkShiftMapper.selectOne(
						Wrappers.<DefaultWorkShift>lambdaQuery()
								.eq(DefaultWorkShift::getCompanyId, companyId)
								.eq(DefaultWorkShift::getShopId, shopId));
		if (existing == null) {
			DefaultWorkShift n = new DefaultWorkShift();
			n.setCompanyId(companyId);
			n.setShopId(shopId);
			n.setWorkShiftData(serialized);
			defaultWorkShiftMapper.insert(n);
		} else {
			defaultWorkShiftMapper.update(
					null,
					Wrappers.<DefaultWorkShift>lambdaUpdate()
							.eq(DefaultWorkShift::getCompanyId, companyId)
							.eq(DefaultWorkShift::getShopId, shopId)
							.set(DefaultWorkShift::getWorkShiftData, serialized));
		}

		Map<String, Object> status = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> e : workShiftData.entrySet()) {
			Map<String, Object> innerObj = new LinkedHashMap<>();
			Map<String, String> inner = e.getValue();
			if (inner != null) {
				for (Map.Entry<String, String> ie : inner.entrySet()) {
					innerObj.put(ie.getKey(), ie.getValue());
				}
			}
			status.put(e.getKey(), innerObj);
		}
		return status;
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
