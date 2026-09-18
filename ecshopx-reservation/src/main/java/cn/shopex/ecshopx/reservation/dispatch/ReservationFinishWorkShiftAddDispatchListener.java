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

package cn.shopex.ecshopx.reservation.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import cn.shopex.ecshopx.reservation.service.WorkShiftCreateService;
import cn.shopex.ecshopx.reservation.service.WorkShiftListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReservationFinishWorkShiftAddDispatchListener implements DispatchListener {

	private static final Logger log = LoggerFactory.getLogger(ReservationFinishWorkShiftAddDispatchListener.class);

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final WorkShiftMapper workShiftMapper;
	private final WorkShiftListService workShiftListService;
	private final WorkShiftCreateService workShiftCreateService;

	public ReservationFinishWorkShiftAddDispatchListener(
			WorkShiftMapper workShiftMapper,
			WorkShiftListService workShiftListService,
			WorkShiftCreateService workShiftCreateService) {
		this.workShiftMapper = workShiftMapper;
		this.workShiftListService = workShiftListService;
		this.workShiftCreateService = workShiftCreateService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Object rawEntities = payload.get("entities");
		if (!(rawEntities instanceof Map<?, ?>)) {
			throw new BadRequestException("entities is required");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) rawEntities;
		try {
			Object postRaw = entities.get("postdata");
			Object resultRaw = entities.get("result");
			Object settingRaw = entities.get("setting_data");
			if (!(postRaw instanceof Map<?, ?>) || !(resultRaw instanceof Map<?, ?>)) {
				throw new BadRequestException("postdata and result are required");
			}
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : ((Map<?, ?>) postRaw).entrySet()) {
				merged.put(String.valueOf(e.getKey()), e.getValue());
			}
			for (Map.Entry<?, ?> e : ((Map<?, ?>) resultRaw).entrySet()) {
				merged.put(String.valueOf(e.getKey()), e.getValue());
			}
			if (settingRaw instanceof Map<?, ?> sd) {
				for (Map.Entry<?, ?> e : sd.entrySet()) {
					merged.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			handleWorkShift(merged);
		} catch (BadRequestException ex) {
			throw ex;
		} catch (Exception ex) {
			log.debug("预约成功后增加排班出错 {}", ex.getMessage());
		}
	}

	private void handleWorkShift(Map<String, Object> params) {
		String dateDay = dateDayFrom(params);
		if (dateDay.isEmpty()) {
			return;
		}
		long companyId = companyIdFrom(params);
		long shopId = shopIdFrom(params);
		long resourceLevelId = resourceLevelIdFrom(params);
		long workDateEpoch = parseAgreementDateEpoch(dateDay);
		LambdaQueryWrapper<WorkShift> w =
				Wrappers.<WorkShift>lambdaQuery()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getShopId, shopId)
						.eq(WorkShift::getResourceLevelId, resourceLevelId)
						.eq(WorkShift::getWorkDate, workDateEpoch);
		List<WorkShift> levelRows = workShiftMapper.selectList(w);
		if (levelRows != null && !levelRows.isEmpty()) {
			return;
		}
		Map<String, Object> defaultRoot = workShiftListService.getDefaultWorkShiftRootForApi(companyId, shopId);
		if (defaultRoot == null || defaultRoot.isEmpty()) {
			return;
		}
		LocalDate d = LocalDate.parse(dateDay, ISO_DATE);
		String weekday =
				d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ENGLISH);
		Object dayEntry = defaultRoot.get(weekday);
		if (!(dayEntry instanceof Map<?, ?> dayMap)) {
			return;
		}
		Object typeIdObj = dayMap.get("typeId");
		if (typeIdObj == null) {
			typeIdObj = dayMap.get("type_id");
		}
		if (typeIdObj == null) {
			return;
		}
		String shiftTypeIdRaw = typeIdObj.toString().trim();
		workShiftCreateService.createWorkShift(companyId, shopId, shiftTypeIdRaw, workDateEpoch, resourceLevelId);
	}

	private static String dateDayFrom(Map<String, Object> params) {
		Object v = params.get("date_day");
		if (v == null) {
			v = params.get("dateDay");
		}
		return v != null ? v.toString().trim() : "";
	}

	private static long companyIdFrom(Map<String, Object> params) {
		Object v = params.get("company_id");
		if (v == null) {
			v = params.get("companyId");
		}
		return longVal(v);
	}

	private static long shopIdFrom(Map<String, Object> params) {
		Object v = params.get("shop_id");
		if (v == null) {
			v = params.get("shopId");
		}
		return longVal(v);
	}

	private static long resourceLevelIdFrom(Map<String, Object> params) {
		Object v = params.get("resource_level_id");
		if (v == null) {
			v = params.get("resourceLevelId");
		}
		return longVal(v);
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(Objects.toString(v, "0").trim());
	}

	private static int parseAgreementDateEpoch(String ymd) {
		LocalDate d = LocalDate.parse(ymd, ISO_DATE);
		return (int) d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
	}
}
