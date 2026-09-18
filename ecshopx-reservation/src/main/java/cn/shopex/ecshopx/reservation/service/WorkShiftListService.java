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
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.DefaultWorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import cn.shopex.ecshopx.reservation.util.WorkShiftDataCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkShiftListService {

	private static final long ONE_DAY_SEC = 86400L;

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
	private static final DateTimeFormatter WEEKDAY_EN =
			DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

	private static final List<String> WEEK_KEY_ORDER =
			List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

	private final ResourceLevelListService resourceLevelListService;
	private final WorkShiftMapper workShiftMapper;
	private final DefaultWorkShiftMapper defaultWorkShiftMapper;
	private final WorkShiftTypeMapper workShiftTypeMapper;
	private final ObjectMapper objectMapper;

	public WorkShiftListService(
			ResourceLevelListService resourceLevelListService,
			WorkShiftMapper workShiftMapper,
			DefaultWorkShiftMapper defaultWorkShiftMapper,
			WorkShiftTypeMapper workShiftTypeMapper,
			ObjectMapper objectMapper) {
		this.resourceLevelListService = resourceLevelListService;
		this.workShiftMapper = workShiftMapper;
		this.defaultWorkShiftMapper = defaultWorkShiftMapper;
		this.workShiftTypeMapper = workShiftTypeMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getListWorkShift(
			long companyId, String shopId, long beginEpochSec, long endEpochSec) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("weekDay", buildWeekDay(beginEpochSec, endEpochSec));

		Map<String, Object> resourceBlock =
				resourceLevelListService.listResourceLevels(companyId, shopId, true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> resourceList =
				(List<Map<String, Object>>) resourceBlock.get("list");
		if (resourceList == null || resourceList.isEmpty()) {
			return result;
		}

		long shopIdLong;
		try {
			shopIdLong = Long.parseLong(shopId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺id格式无效");
		}
		ZoneId zone = ZoneId.systemDefault();
		List<Map<String, Object>> resourceLevelOut = new ArrayList<>();
		for (Map<String, Object> listRow : resourceList) {
			long resourceLevelId = toLong(listRow.get("resourceLevelId"));
			Map<String, Object> weekMaps =
					buildWorkShiftWeekMaps(companyId, shopIdLong, resourceLevelId, beginEpochSec, endEpochSec, zone);
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(listRow);
			merged.putAll(weekMaps);
			resourceLevelOut.add(merged);
		}
		result.put("resourceLevel", resourceLevelOut);
		return result;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getDefaultWorkShiftRootForApi(long companyId, long shopId) {
		return loadDefaultWorkShiftRoot(companyId, shopId);
	}

	private Map<String, Object> buildWorkShiftWeekMaps(
			long companyId,
			long shopId,
			long resourceLevelId,
			long beginEpochSec,
			long endEpochSec,
			ZoneId zone) {
		LambdaQueryWrapper<WorkShift> w =
				Wrappers.<WorkShift>lambdaQuery()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getShopId, shopId)
						.eq(WorkShift::getResourceLevelId, resourceLevelId)
						.ge(WorkShift::getWorkDate, beginEpochSec)
						.le(WorkShift::getWorkDate, endEpochSec)
						.orderByDesc(WorkShift::getWorkDate);
		List<WorkShift> rows = workShiftMapper.selectList(w);
		LinkedHashMap<String, LinkedHashMap<String, Object>> byWeek = new LinkedHashMap<>();
		if (rows != null) {
			for (WorkShift ws : rows) {
				if (ws.getWorkDate() == null) {
					continue;
				}
				String weekKey = weekKeyEnglish(ws.getWorkDate(), zone);
				byWeek.put(weekKey, workShiftEntityToNormalizedMap(ws));
			}
		}

		Map<String, Object> defaultRoot = loadDefaultWorkShiftRoot(companyId, shopId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (defaultRoot != null && !defaultRoot.isEmpty()) {
			for (Map.Entry<String, Object> en : defaultRoot.entrySet()) {
				String weekday = en.getKey();
				if (!(en.getValue() instanceof Map<?, ?> innerRaw)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> inner = (Map<String, Object>) innerRaw;
				Object defaultTypeId = extractInnerTypeId(inner);
				LinkedHashMap<String, Object> data;
				if (byWeek.containsKey(weekday)) {
					data = new LinkedHashMap<>(byWeek.get(weekday));
				} else {
					data = new LinkedHashMap<>();
					data.put("id", 0L);
					data.put("companyId", companyId);
					data.put("shopId", shopId);
					data.put("resourceLevelId", resourceLevelId);
					data.put("shiftTypeId", coerceShiftTypeIdForPlaceholder(defaultTypeId));
				}
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>(data);
				merged.putAll(shiftTypeInfoMap(data.get("shiftTypeId")));
				out.put(weekday, merged);
			}
		} else if (!byWeek.isEmpty()) {
			for (Map.Entry<String, LinkedHashMap<String, Object>> en : byWeek.entrySet()) {
				String weekday = en.getKey();
				LinkedHashMap<String, Object> data = new LinkedHashMap<>(en.getValue());
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>(data);
				merged.putAll(shiftTypeInfoMap(data.get("shiftTypeId")));
				out.put(weekday, merged);
			}
		}
		return orderWeekKeys(out);
	}

	private static LinkedHashMap<String, Object> orderWeekKeys(Map<String, Object> raw) {
		LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
		for (String k : WEEK_KEY_ORDER) {
			if (raw.containsKey(k)) {
				ordered.put(k, raw.get(k));
			}
		}
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			if (!ordered.containsKey(e.getKey())) {
				ordered.put(e.getKey(), e.getValue());
			}
		}
		return ordered;
	}

	private Map<String, Object> loadDefaultWorkShiftRoot(long companyId, long shopId) {
		DefaultWorkShift def =
				defaultWorkShiftMapper.selectOne(
						Wrappers.<DefaultWorkShift>lambdaQuery()
								.eq(DefaultWorkShift::getCompanyId, companyId)
								.eq(DefaultWorkShift::getShopId, shopId));
		if (def == null || def.getWorkShiftData() == null || def.getWorkShiftData().isBlank()) {
			return Map.of();
		}
		try {
			return WorkShiftDataCodec.parseWorkShiftDataRoot(def.getWorkShiftData(), objectMapper);
		} catch (RuntimeException ex) {
			throw new ResourceException("默认排班数据解析失败");
		}
	}

	private static Object extractInnerTypeId(Map<String, Object> inner) {
		Object tid = inner.get("typeId");
		if (tid == null) {
			tid = inner.get("type_id");
		}
		return tid;
	}

	private static Object coerceShiftTypeIdForPlaceholder(Object raw) {
		if (raw == null) {
			return -1L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return -1L;
		}
		if ("-1".equals(s)) {
			return -1L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return raw;
		}
	}

	private LinkedHashMap<String, Object> shiftTypeInfoMap(Object shiftTypeIdField) {
		if (isRestShiftType(shiftTypeIdField)) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("typeName", "休息");
			m.put("beginTime", "00:00");
			m.put("endTime", "23:59");
			m.put("typeId", "-1");
			return m;
		}
		long typeId;
		try {
			typeId =
					shiftTypeIdField instanceof Number n
							? n.longValue()
							: Long.parseLong(shiftTypeIdField.toString().trim());
		} catch (NumberFormatException e) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("typeName", "休息");
			m.put("beginTime", "00:00");
			m.put("endTime", "23:59");
			m.put("typeId", "-1");
			return m;
		}
		if (typeId == -1L) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("typeName", "休息");
			m.put("beginTime", "00:00");
			m.put("endTime", "23:59");
			m.put("typeId", "-1");
			return m;
		}
		WorkShiftType row = workShiftTypeMapper.selectById(typeId);
		if (row == null) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("typeName", "休息");
			m.put("beginTime", "00:00");
			m.put("endTime", "23:59");
			m.put("typeId", "-1");
			return m;
		}
		return workShiftTypeToNormalizedMap(row);
	}

	private static LinkedHashMap<String, Object> workShiftTypeToNormalizedMap(WorkShiftType row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("typeId", row.getTypeId());
		m.put("companyId", row.getCompanyId());
		m.put("typeName", row.getTypeName());
		m.put("beginTime", row.getBeginTime());
		m.put("endTime", row.getEndTime());
		m.put("status", row.getStatus());
		if (row.getCreated() != null) {
			m.put("created", row.getCreated());
		}
		if (row.getUpdated() != null) {
			m.put("updated", row.getUpdated());
		}
		return m;
	}

	private static boolean isRestShiftType(Object shiftTypeIdField) {
		if (shiftTypeIdField == null) {
			return false;
		}
		if (shiftTypeIdField instanceof Number n) {
			return n.longValue() == -1L;
		}
		return "-1".equals(shiftTypeIdField.toString().trim());
	}

	private static LinkedHashMap<String, Object> workShiftEntityToNormalizedMap(WorkShift ws) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", ws.getId());
		m.put("companyId", ws.getCompanyId());
		m.put("shopId", ws.getShopId());
		m.put("resourceLevelId", ws.getResourceLevelId());
		m.put("workDate", ws.getWorkDate());
		m.put("shiftTypeId", ws.getShiftTypeId());
		if (ws.getCreated() != null) {
			m.put("created", ws.getCreated());
		}
		if (ws.getUpdated() != null) {
			m.put("updated", ws.getUpdated());
		}
		return m;
	}

	private static String weekKeyEnglish(long workDateEpoch, ZoneId z) {
		LocalDate d = Instant.ofEpochSecond(workDateEpoch).atZone(z).toLocalDate();
		return WEEKDAY_EN.format(d).toLowerCase(Locale.ENGLISH);
	}

	private static LinkedHashMap<String, Object> buildWeekDay(long beginEpochSec, long endEpochSec) {
		ZoneId z = ZoneId.systemDefault();
		LinkedHashMap<String, Object> weekDay = new LinkedHashMap<>();
		String[] cn = {"周一", "周二", "周三", "周四", "周五", "周六"};
		String[] keys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
		for (int i = 0; i < keys.length; i++) {
			long sec = beginEpochSec + i * ONE_DAY_SEC;
			LocalDate d = Instant.ofEpochSecond(sec).atZone(z).toLocalDate();
			LinkedHashMap<String, Object> cell = new LinkedHashMap<>();
			cell.put("ymd", d.format(YMD));
			cell.put("md", cn[i] + "(" + d.format(MD) + ")");
			weekDay.put(keys[i], cell);
		}
		LocalDate sun = Instant.ofEpochSecond(endEpochSec).atZone(z).toLocalDate();
		LinkedHashMap<String, Object> sunday = new LinkedHashMap<>();
		sunday.put("ymd", sun.format(YMD));
		sunday.put("md", "周日(" + sun.format(MD) + ")");
		weekDay.put("sunday", sunday);
		return weekDay;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
