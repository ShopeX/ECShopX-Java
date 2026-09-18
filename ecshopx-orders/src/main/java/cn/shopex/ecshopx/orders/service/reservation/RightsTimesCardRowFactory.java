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

package cn.shopex.ecshopx.orders.service.reservation;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.orders.domain.Rights;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RightsTimesCardRowFactory {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RightsTimesCardRowFactory(SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> toTimesCardRow(Rights r, int nowEpochSec) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rights_id", r.getRightsId());
		m.put("user_id", r.getUserId());
		m.put("company_id", r.getCompanyId());
		boolean canRes = Boolean.TRUE.equals(r.getCanReservation());
		m.put("can_reservation", canRes ? 1 : 0);
		m.put("rights_name", r.getRightsName());
		m.put("rights_subname", r.getRightsSubname());
		m.put("rights_from", r.getRightsFrom());
		String mobileCipher = r.getMobile();
		m.put("mobile", sensitiveFieldEncryptor.decrypt(mobileCipher != null ? mobileCipher : ""));
		long totalNum = r.getTotalNum() != null ? r.getTotalNum() : 0L;
		long totalConsumNum = r.getTotalConsumNum() != null ? r.getTotalConsumNum() : 0L;
		m.put("total_num", totalNum);
		m.put("total_consum_num", totalConsumNum);
		m.put("start_time", r.getStartTime());
		m.put("end_time", r.getEndTime());
		m.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : "");
		m.put("label_infos", parseLabelInfos(r.getLabelInfos()));
		m.put("status", r.getStatus());
		Integer isNotLimitNum = r.getIsNotLimitNum();
		m.put("is_not_limit_num", isNotLimitNum);

		boolean isValid = r.getEndTime() == null || r.getEndTime() >= nowEpochSec;
		if (isValid && isNotLimitNum != null && isNotLimitNum == 2) {
			if (totalNum > totalConsumNum) {
				isValid = true;
			} else {
				isValid = false;
			}
		}
		m.put("is_valid", isValid);

		long totalSurplusNum;
		if (isNotLimitNum != null && isNotLimitNum == 1) {
			totalSurplusNum = -1L;
		} else {
			totalSurplusNum = totalNum - totalConsumNum;
		}
		m.put("total_surplus_num", totalSurplusNum);

		return m;
	}

	public Map<String, Object> toWxappRightsDetailRow(Rights r, int nowEpochSec) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rights_id", r.getRightsId());
		m.put("user_id", r.getUserId());
		m.put("company_id", r.getCompanyId());
		m.put("can_reservation", Boolean.TRUE.equals(r.getCanReservation()));
		m.put("rights_name", r.getRightsName());
		m.put("rights_subname", r.getRightsSubname());
		long totalNum = r.getTotalNum() != null ? r.getTotalNum() : 0L;
		long totalConsumNum = r.getTotalConsumNum() != null ? r.getTotalConsumNum() : 0L;
		m.put("total_num", totalNum);
		m.put("total_consum_num", totalConsumNum);
		m.put("is_not_limit_num", r.getIsNotLimitNum());
		m.put("status", r.getStatus());
		m.put("start_time", r.getStartTime());
		m.put("end_time", r.getEndTime());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		m.put("order_id", r.getOrderId());
		m.put("rights_from", r.getRightsFrom());
		m.put("operator_desc", r.getOperatorDesc());
		m.put("label_infos", parseLabelInfos(r.getLabelInfos()));

		Object limitRaw = r.getIsNotLimitNum();
		Integer et = r.getEndTime();

		boolean isValid = true;
		if (et != null && et != 0 && et < nowEpochSec) {
			isValid = false;
		}
		if (isValid && isNotLimitNumLooseEquals(limitRaw, 2)) {
			if (totalNum > totalConsumNum) {
				isValid = true;
			} else {
				isValid = false;
			}
			m.put("total_surplus_num", totalNum - totalConsumNum);
		}
		if (isNotLimitNumLooseEquals(limitRaw, 1)) {
			m.put("total_surplus_num", -1L);
		}
		m.put("is_valid", isValid);

		return m;
	}

	/**
	 * Loose equality for stored limit-type values: integer {@code n} matches decimal string "{@code n}".
	 * Accepts {@link Number} or trimmed digit {@link String}; invalid or non-numeric strings yield false.
	 */
	private static boolean isNotLimitNumLooseEquals(Object rawIsNotLimitNum, int expected) {
		if (rawIsNotLimitNum == null) {
			return false;
		}
		if (rawIsNotLimitNum instanceof Number n) {
			return n.intValue() == expected;
		}
		if (rawIsNotLimitNum instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			try {
				return Integer.parseInt(t) == expected;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static Object parseLabelInfos(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return JSON.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return null;
		}
	}
}
