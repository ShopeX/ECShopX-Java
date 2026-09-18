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
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationEveryDayTimePeriodParamValidator {

	public EveryDayTimePeriodQuery validate(Map<String, String> queryLike) {
		StringBuilder err = new StringBuilder();
		String shopRaw = queryLike == null ? null : queryLike.get("shopId");
		if (!StringUtils.hasText(shopRaw == null ? "" : shopRaw.trim())) {
			err.append("门店必填，");
		}
		String dateRaw = queryLike == null ? null : queryLike.get("dateDay");
		if (!StringUtils.hasText(dateRaw == null ? "" : dateRaw.trim())) {
			err.append("日期必填，");
		}
		if (!err.isEmpty()) {
			throw new ResourceException(err.toString());
		}
		return new EveryDayTimePeriodQuery(shopRaw.trim(), dateRaw.trim());
	}

	public record EveryDayTimePeriodQuery(String shopIdRaw, String dateDayRaw) {}
}
