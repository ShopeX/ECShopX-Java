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
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationSettingMapper;
import cn.shopex.ecshopx.reservation.util.ReservationNumLimitCodec;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReservationSettingSaveService {

	private final ReservationSettingMapper reservationSettingMapper;

	public ReservationSettingSaveService(ReservationSettingMapper reservationSettingMapper) {
		this.reservationSettingMapper = reservationSettingMapper;
	}

	public Map<String, Object> saveSetting(long companyId, Map<String, Object> postData) {
		ReservationSetting entity =
				reservationSettingMapper.selectOne(
						Wrappers.lambdaQuery(ReservationSetting.class)
								.eq(ReservationSetting::getCompanyId, companyId));
		boolean insert = entity == null;
		if (insert) {
			entity = new ReservationSetting();
		}
		entity.setCompanyId(companyId);
		entity.setTimeInterval(
				Integer.parseInt(Objects.toString(postData.get("interval"), "").trim()));
		entity.setResourceName(Objects.toString(postData.get("resourceName"), ""));
		entity.setMaxLimitDay(toInt(postData.get("maxLimitDay")));
		entity.setMinLimitHour(toInt(postData.get("minLimitHour")));
		entity.setCancelMinute(toInt(postData.get("cancelMinute")));
		entity.setReservationCondition(toInt(postData.get("condition")));
		entity.setReservationMode(toInt(postData.get("reservationMode")));
		entity.setSmsDelay(String.valueOf(parseSmsDelayHours(postData.get("sms_delay"))));
		if (postData.containsKey("reservationNumLimit")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> lim = (Map<String, Object>) postData.get("reservationNumLimit");
			entity.setReservationNumLimit(ReservationNumLimitCodec.serialize(lim != null ? lim : Map.of()));
		}
		if (insert) {
			reservationSettingMapper.insert(entity);
		} else {
			reservationSettingMapper.updateById(entity);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", entity.getId());
		result.put("company_id", entity.getCompanyId());
		result.put("time_interval", entity.getTimeInterval());
		result.put("resource_name", entity.getResourceName());
		result.put("max_limit_day", entity.getMaxLimitDay());
		return result;
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(Objects.toString(o, "").trim());
	}

	private static int parseSmsDelayHours(Object raw) {
		if (raw == null) {
			throw new ResourceException("预约提醒通知时间不能为空");
		}
		String trimmed = String.valueOf(raw).trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("预约提醒通知时间不能为空");
		}
		int v;
		try {
			v = Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("预约提醒通知时间必须为整数");
		}
		if (v < 1) {
			throw new ResourceException("预约提醒通知时间必须不小于 1");
		}
		return v;
	}
}
