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

package cn.shopex.ecshopx.distribution.support;

import cn.shopex.ecshopx.distribution.domain.DistributeLogs;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class DistributeLogsColumnNamesDataMapper {

	private static final String MSG_DATE_FORMAT_YEAR = "distribution.repositories.date_format_year";
	private static final String MSG_HOUR_SUFFIX = "distribution.repositories.hour_suffix";

	private final MessageSource messageSource;
	private final ZoneId planCloseZoneId;

	public DistributeLogsColumnNamesDataMapper(
			MessageSource messageSource,
			@Value("${distribution.plan-close-timezone:Asia/Shanghai}") String planCloseZoneId) {
		this.messageSource = messageSource;
		this.planCloseZoneId = ZoneId.of(planCloseZoneId);
	}

	public Map<String, Object> toRow(DistributeLogs entity, Locale locale) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("distributor_id", entity.getDistributorId());
		m.put("order_id", entity.getOrderId());
		m.put("item_id", entity.getItemId());
		m.put("mobile", entity.getMobile());
		m.put("item_name", entity.getItemName());
		m.put("company_id", entity.getCompanyId());
		m.put("distributor_mobile", entity.getDistributorMobile());
		m.put("user_id", entity.getUserId());
		m.put("pic", entity.getPic());
		m.put("num", entity.getNum());
		m.put("rebate", entity.getRebate());
		m.put("total_rebate", entity.getTotalRebate());
		m.put("is_close", entity.getIsClose());
		m.put("plan_close_time", entity.getPlanCloseTime());
		m.put("plan_close_date", formatPlanCloseDate(entity.getPlanCloseTime(), locale));
		m.put("shop_id", entity.getShopId());
		m.put("create_time", entity.getCreateTime());
		m.put("update_time", entity.getUpdateTime());
		return m;
	}

	private String formatPlanCloseDate(Integer planCloseTime, Locale locale) {
		long ts;
		if (planCloseTime == null) {
			ts = 0L;
		} else {
			ts = planCloseTime.longValue();
		}
		ZonedDateTime zdt = Instant.ofEpochSecond(ts).atZone(planCloseZoneId);
		String datePattern = messageSource.getMessage(MSG_DATE_FORMAT_YEAR, null, locale);
		String hourSuffix = messageSource.getMessage(MSG_HOUR_SUFFIX, null, locale);
		DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern(datePattern);
		String datePart = zdt.format(dateFmt);
		int hour = zdt.getHour();
		return datePart + (hour + 1) + hourSuffix;
	}
}
