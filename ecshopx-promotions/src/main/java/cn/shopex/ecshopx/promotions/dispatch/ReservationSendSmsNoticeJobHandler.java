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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReservationSendSmsNoticeJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(ReservationSendSmsNoticeJobHandler.class);

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SmsSendTestService smsSendTestService;

	public ReservationSendSmsNoticeJobHandler(SmsSendTestService smsSendTestService) {
		this.smsSendTestService = smsSendTestService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractCompanyId(payload.get("company_id"));
		String mobile = payload.get("mobile") == null ? "" : String.valueOf(payload.get("mobile"));
		Object rawTs = payload.get("to_shop_time");
		long epochSec =
				rawTs instanceof Number ? ((Number) rawTs).longValue() : Long.parseLong(String.valueOf(rawTs).trim());
		String dateStr = DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epochSec));
		Map<String, String> vars = new LinkedHashMap<>();
		vars.put("date", dateStr);
		vars.put("shop_name", stringOrEmpty(payload.get("shop_name")));
		vars.put("rights_name", stringOrEmpty(payload.get("rights_name")));
		vars.put("shop_address", stringOrEmpty(payload.get("shop_address")));
		vars.put("telephone", stringOrEmpty(payload.get("telephone")));
		Object userName = payload.get("user_name");
		if (userName != null && !userName.toString().isEmpty()) {
			vars.put("user_name", String.valueOf(userName));
		}
		String templateKey = templateNameFromPayload(payload.get("sms_template_name"));
		try {
			smsSendTestService.sendTemplatedNoticeSms(companyId, mobile, templateKey, vars);
		} catch (RuntimeException e) {
			log.debug(
					"{} failed companyId={} mobileTail={} err={}",
					templateKey,
					companyId,
					maskTail(mobile),
					e.getMessage());
		}
	}

	private static String templateNameFromPayload(Object raw) {
		if (raw == null || raw.toString().isBlank()) {
			return "reservation_notice";
		}
		return raw.toString().trim();
	}

	private static long extractCompanyId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static String maskTail(String mobile) {
		if (mobile == null || mobile.length() < 4) {
			return "****";
		}
		return "****" + mobile.substring(mobile.length() - 4);
	}
}
