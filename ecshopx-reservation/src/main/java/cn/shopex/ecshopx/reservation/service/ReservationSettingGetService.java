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

import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.util.ReservationNumLimitCodec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReservationSettingGetService {

	private static final Set<String> LIMIT_TYPES = Set.of("not_open", "limit_days", "limit_nums");

	private final ReservationSettingQueryService reservationSettingQueryService;

	public ReservationSettingGetService(ReservationSettingQueryService reservationSettingQueryService) {
		this.reservationSettingQueryService = reservationSettingQueryService;
	}

	public Object buildResponse(long companyId) {
		Optional<ReservationSetting> optional = reservationSettingQueryService.findByCompanyId(companyId);
		if (optional.isEmpty()) {
			return Collections.emptyList();
		}
		ReservationSetting row = optional.get();
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("id", row.getId());
		result.put("companyId", row.getCompanyId());
		result.put("timeInterval", row.getTimeInterval());
		result.put("resourceName", row.getResourceName());
		result.put("maxLimitDay", row.getMaxLimitDay());
		result.put("minLimitHour", row.getMinLimitHour());
		result.put("reservationCondition", row.getReservationCondition());
		result.put("reservationMode", row.getReservationMode());
		result.put("cancelMinute", row.getCancelMinute());
		result.put("reservationNumLimit", row.getReservationNumLimit());
		result.put("smsDelay", row.getSmsDelay());
		result.put("created", row.getCreated());
		result.put("updated", row.getUpdated());

		String rawLimit = row.getReservationNumLimit();
		if (rawLimit != null && !rawLimit.isBlank()) {
			Map<String, Object> limit = ReservationNumLimitCodec.parse(rawLimit, null);
			if (!limit.isEmpty()) {
				Object ltObj = limit.get("limit_type");
				if (ltObj != null) {
					String typeStr = ltObj.toString();
					if (LIMIT_TYPES.contains(typeStr)) {
						result.put("limitType", typeStr);
						result.put("limit", normalizeLimitValue(limit.get(typeStr)));
					}
				}
			}
		}

		return result;
	}

	private static Object normalizeLimitValue(Object limVal) {
		if (limVal == null) {
			return null;
		}
		if (limVal instanceof Number n) {
			return n;
		}
		try {
			return Long.parseLong(limVal.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
