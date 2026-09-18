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
import cn.shopex.ecshopx.reservation.domain.DefaultWorkShift;
import cn.shopex.ecshopx.reservation.domain.ResourceLevelRelService;
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.DefaultWorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelRelServiceMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import cn.shopex.ecshopx.reservation.port.ReservationRightsDetailPort;
import cn.shopex.ecshopx.reservation.util.WorkShiftDataCodec;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class ReservationCheckService {

	private static final List<String> SLOT_OCCUPANCY_STATUSES =
			List.of("system", "success", "not_to_shop", "to_the_shop");

	private static final List<String> RIGHTS_USAGE_STATUSES = List.of("success", "to_the_shop", "not_to_shop");

	private final ReservationRecordMapper reservationRecordMapper;

	private final ReservationRightsDetailPort reservationRightsDetailPort;

	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	private final WorkShiftMapper workShiftMapper;

	private final ResourceLevelMapper resourceLevelMapper;

	private final WorkShiftTypeMapper workShiftTypeMapper;

	private final DefaultWorkShiftMapper defaultWorkShiftMapper;

	private final ObjectMapper objectMapper;

	public ReservationCheckService(
			ReservationRecordMapper reservationRecordMapper,
			ReservationRightsDetailPort reservationRightsDetailPort,
			ResourceLevelRelServiceMapper resourceLevelRelServiceMapper,
			WorkShiftMapper workShiftMapper,
			ResourceLevelMapper resourceLevelMapper,
			WorkShiftTypeMapper workShiftTypeMapper,
			DefaultWorkShiftMapper defaultWorkShiftMapper,
			ObjectMapper objectMapper) {
		this.reservationRecordMapper = reservationRecordMapper;
		this.reservationRightsDetailPort = reservationRightsDetailPort;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
		this.workShiftMapper = workShiftMapper;
		this.resourceLevelMapper = resourceLevelMapper;
		this.workShiftTypeMapper = workShiftTypeMapper;
		this.defaultWorkShiftMapper = defaultWorkShiftMapper;
		this.objectMapper = objectMapper;
	}

	public void checkAndMaybeAssignResource(long companyId, Map<String, Object> paramsData) {
		String dateDay = Objects.toString(paramsData.get("date_day"), "");
		String beginTime = Objects.toString(paramsData.get("begin_time"), "");
		int agreementEpoch = parseAgreementDateEpoch(dateDay);
		long workDate = agreementEpoch;
		long toShop = toShopTimeUnix(dateDay, beginTime);
		long now = System.currentTimeMillis() / 1000L;
		if (now >= toShop) {
			throw new ResourceException("该时段已过期");
		}

		List<Long> availableIds = new ArrayList<>();
		boolean hasRightsAndLabel = paramsData.containsKey("rights_id") && paramsData.containsKey("label_id");
		if (hasRightsAndLabel) {
			long rightsId = toLongRequiredRights(paramsData.get("rights_id"));
			String labelId = Objects.toString(paramsData.get("label_id"), "");
			Map<String, Object> rightsDetail = reservationRightsDetailPort.getRightsDetail(rightsId, companyId);
			if (rightsDetail == null || rightsDetail.isEmpty()) {
				throw new ResourceException("您预约的项目已失效");
			}
			Long userId = toLongOrNull(paramsData.get("user_id"));
			long count = 0L;
			if (userId != null) {
				count = reservationRecordMapper.countRightsQuotaUsage(
						companyId,
						0L,
						rightsId,
						"",
						userId,
						RIGHTS_USAGE_STATUSES);
			}
			Object isNotLimit = rightsDetail.get("is_not_limit_num");
			int isNotLimitNum = isNotLimit instanceof Number n ? n.intValue() : 0;
			Object totalNumObj = rightsDetail.get("total_num");
			long totalNum = totalNumObj instanceof Number n ? n.longValue() : 0L;
			if (count > 0 && isNotLimitNum == 2 && count >= totalNum) {
				throw new ResourceException("该课程的预约次数已达上限");
			}

			List<ResourceLevelRelService> relList =
					resourceLevelRelServiceMapper.selectListForReservationCheck(companyId, longVal(paramsData, "shop_id"), labelId);
			if (relList == null || relList.isEmpty()) {
				throw new ResourceException("没有可预约的资源");
			}
			Set<Long> allLevelIds = new LinkedHashSet<>();
			for (ResourceLevelRelService rel : relList) {
				if (rel.getResourceLevelId() != null) {
					allLevelIds.add(rel.getResourceLevelId());
				}
			}
			long shopId = longVal(paramsData, "shop_id");
			Set<Long> shiftLevelIds = collectShiftLevelIds(companyId, shopId, workDate, allLevelIds, beginTime);
			if (shiftLevelIds.isEmpty()) {
				throw new ResourceException("没有可预约的资源");
			}
			availableIds.addAll(shiftLevelIds);
		}

		long resourceLevelIdArg = toLongOrZero(paramsData.get("resource_level_id"));
		if (resourceLevelIdArg > 0) {
			if (availableIds.isEmpty() || availableIds.contains(resourceLevelIdArg)) {
				availableIds = new ArrayList<>(List.of(resourceLevelIdArg));
			} else {
				throw new ResourceException("没有可预约的资源");
			}
		}

		if (availableIds.isEmpty()) {
			throw new ResourceException("没有可预约的资源");
		}

		List<Long> occupied = reservationRecordMapper.selectOccupiedResourceLevelIds(
				companyId,
				longVal(paramsData, "shop_id"),
				agreementEpoch,
				beginTime,
				availableIds,
				SLOT_OCCUPANCY_STATUSES);
		Set<Long> occ = new HashSet<>(occupied != null ? occupied : List.of());
		List<Long> diff = new ArrayList<>();
		for (Long id : availableIds) {
			if (!occ.contains(id)) {
				diff.add(id);
			}
		}
		if (diff.isEmpty()) {
			throw new ResourceException("没有可预约的资源");
		}

		List<Long> activeIds = resourceLevelMapper.selectActiveIdsByShopAndFilter(
				String.valueOf(companyId), String.valueOf(longVal(paramsData, "shop_id")), diff);
		if (activeIds == null || activeIds.isEmpty()) {
			throw new ResourceException("预约失败");
		}
		int pick = ThreadLocalRandom.current().nextInt(activeIds.size());
		Long chosen = activeIds.get(pick);
		paramsData.put("resource_level_id", chosen);
		String name = resolveResourceLevelName(companyId, longVal(paramsData, "shop_id"), chosen);
		paramsData.put("resource_level_name", name);
	}

	private String resolveResourceLevelName(long companyId, long shopId, long resourceLevelId) {
		var rl = resourceLevelMapper.selectById(resourceLevelId);
		return rl != null && rl.getName() != null ? rl.getName() : "";
	}

	private Set<Long> collectShiftLevelIds(
			long companyId, long shopId, long workDate, Set<Long> allLevelIds, String postBeginTime) {
		Set<Long> fromSql =
				new HashSet<>(workShiftMapper.selectIdsIntersectingReservationWindow(
						companyId, shopId, allLevelIds, workDate, postBeginTime, null));
		Set<Long> out = new LinkedHashSet<>(fromSql);
		for (Long levelId : allLevelIds) {
			if (out.contains(levelId)) {
				continue;
			}
			if (shiftPassesWithDefaultFallback(companyId, shopId, workDate, levelId, postBeginTime)) {
				out.add(levelId);
			}
		}
		return out;
	}

	private boolean shiftPassesWithDefaultFallback(
			long companyId, long shopId, long workDate, long resourceLevelId, String postBeginTime) {
		ZoneId z = ZoneId.systemDefault();
		String weekKey = weekKeyEnglish(workDate, z);
		List<WorkShift> rows = workShiftMapper.selectList(
				Wrappers.<WorkShift>lambdaQuery()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getShopId, shopId)
						.eq(WorkShift::getWorkDate, workDate)
						.eq(WorkShift::getResourceLevelId, resourceLevelId));
		Long shiftTypeId = null;
		if (rows != null && !rows.isEmpty()) {
			shiftTypeId = rows.get(rows.size() - 1).getShiftTypeId();
		}
		if (shiftTypeId != null && shiftTypeId == -1L) {
			return false;
		}
		if (shiftTypeId == null) {
			shiftTypeId = readDefaultShiftTypeId(companyId, shopId, weekKey);
		}
		if (shiftTypeId == null) {
			return false;
		}
		if (shiftTypeId == -1L) {
			return false;
		}
		WorkShiftType type = workShiftTypeMapper.selectById(shiftTypeId);
		if (type == null) {
			return false;
		}
		return shiftTimeCoversReservationBegin(type.getBeginTime(), type.getEndTime(), postBeginTime, z);
	}

	private Long readDefaultShiftTypeId(long companyId, long shopId, String weekKey) {
		DefaultWorkShift def = defaultWorkShiftMapper.selectOne(
				Wrappers.<DefaultWorkShift>lambdaQuery()
						.eq(DefaultWorkShift::getCompanyId, companyId)
						.eq(DefaultWorkShift::getShopId, shopId));
		if (def == null || def.getWorkShiftData() == null || def.getWorkShiftData().isBlank()) {
			return null;
		}
		try {
			Map<String, Object> root =
					WorkShiftDataCodec.parseWorkShiftDataRoot(def.getWorkShiftData(), objectMapper);
			Object day = root.get(weekKey);
			if (!(day instanceof Map<?, ?> m)) {
				return null;
			}
			Object tid = m.get("typeId");
			if (tid == null) {
				tid = m.get("type_id");
			}
			if (tid == null) {
				return null;
			}
			if (tid instanceof Number n) {
				return n.longValue();
			}
			String s = tid.toString().trim();
			if (s.isEmpty()) {
				return null;
			}
			return Long.parseLong(s);
		} catch (Exception e) {
			return null;
		}
	}

	private static String weekKeyEnglish(long workDateEpoch, ZoneId z) {
		LocalDate d = Instant.ofEpochSecond(workDateEpoch).atZone(z).toLocalDate();
		return DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH).format(d).toLowerCase(Locale.ENGLISH);
	}

	/**
	 * 以系统默认时区的「当天」将排班起止与预约开始时刻转为秒级时间戳，判断预约开始是否落在排班半开区间内。
	 */
	private static boolean shiftTimeCoversReservationBegin(String typeBegin, String typeEnd, String postBegin, ZoneId z) {
		if (typeBegin == null || typeEnd == null || postBegin == null) {
			return false;
		}
		LocalDate today = LocalDate.now(z);
		LocalTime tb = parseFlexibleTime(typeBegin.trim());
		LocalTime te = parseFlexibleTime(typeEnd.trim());
		LocalTime pb = parseFlexibleTime(postBegin.trim());
		long shiftStart = today.atTime(tb).atZone(z).toEpochSecond();
		long shiftEnd = today.atTime(te).atZone(z).toEpochSecond();
		long postStart = today.atTime(pb).atZone(z).toEpochSecond();
		return shiftStart <= postStart && shiftEnd > postStart;
	}

	private static LocalTime parseFlexibleTime(String s) {
		String[] patterns = {"H:mm", "HH:mm", "H:m", "HH:m"};
		for (String p : patterns) {
			try {
				return LocalTime.parse(s, DateTimeFormatter.ofPattern(p));
			} catch (Exception ignored) {
				// try next
			}
		}
		throw new ResourceException("时段必选");
	}

	private static int parseAgreementDateEpoch(String ymd) {
		try {
			LocalDate d = LocalDate.parse(ymd);
			return (int) d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (Exception e) {
			throw new ResourceException("日期必填");
		}
	}

	private static long toShopTimeUnix(String ymd, String beginTime) {
		LocalDate d = LocalDate.parse(ymd);
		LocalTime t = parseFlexibleTime(beginTime.trim());
		return d.atTime(t).atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	private static long longVal(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(Objects.toString(v, "0"));
	}

	private static long toLongRequiredRights(Object v) {
		if (v == null) {
			throw new ResourceException("代客预约时,预约项目必填");
		}
		try {
			long x = Long.parseLong(v.toString().trim());
			if (x <= 0) {
				throw new ResourceException("代客预约时,预约项目必填");
			}
			return x;
		} catch (NumberFormatException e) {
			throw new ResourceException("代客预约时,预约项目必填");
		}
	}

	private static Long toLongOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
