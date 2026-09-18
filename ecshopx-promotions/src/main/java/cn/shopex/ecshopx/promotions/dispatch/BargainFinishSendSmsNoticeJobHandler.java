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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BargainFinishSendSmsNoticeJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(BargainFinishSendSmsNoticeJobHandler.class);

	private final MemberAccountService memberAccountService;
	private final SmsSendTestService smsSendTestService;

	public BargainFinishSendSmsNoticeJobHandler(
			MemberAccountService memberAccountService, SmsSendTestService smsSendTestService) {
		this.memberAccountService = memberAccountService;
		this.smsSendTestService = smsSendTestService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"), "company_id");
		long bargainOwnerUserId = extractLong(payload.get("bargain_owner_user_id"), "bargain_owner_user_id");
		Object rawEnd = payload.get("promotion_end_time_sec");
		Long promotionEndTimeSec = null;
		if (rawEnd instanceof Number n) {
			promotionEndTimeSec = n.longValue();
		} else if (rawEnd != null && !rawEnd.toString().isBlank()) {
			try {
				promotionEndTimeSec = Long.parseLong(rawEnd.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("promotion_end_time_sec is invalid");
			}
		}
		Locale localeHint = Locale.forLanguageTag(stringOrEmpty(payload.get("locale_language_tag")));
		if (promotionEndTimeSec == null) {
			log.debug(
					"bargainFinish_notice skipped: promotionEndTimeSec is null (companyId={}, bargainOwnerUserId={}, locale={})",
					companyId,
					bargainOwnerUserId,
					localeHint);
			return;
		}
		String mobile =
				Objects.toString(
								memberAccountService.resolveMemberMobileForH5Context(
										bargainOwnerUserId, companyId),
								"")
						.trim();
		if (mobile.isEmpty()) {
			log.debug(
					"bargainFinish_notice skipped: empty mobile (companyId={}, bargainOwnerUserId={})",
					companyId,
					bargainOwnerUserId);
			return;
		}
		Map<String, String> vars = new LinkedHashMap<>();
		vars.put("item_name", stringOrEmpty(payload.get("item_name")));
		int cents = extractPriceCents(payload.get("price"));
		BigDecimal payYuan =
				BigDecimal.valueOf((long) cents)
						.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
		vars.put("pay_money", payYuan.toPlainString());
		vars.put(
				"end_time",
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
						.withZone(ZoneId.systemDefault())
						.format(Instant.ofEpochSecond(promotionEndTimeSec.longValue())));
		try {
			smsSendTestService.sendTemplatedNoticeSms(companyId, mobile, "bargainFinish_notice", vars);
		} catch (Exception e) {
			log.debug("短信发送失败: bargainFinish_notice =>{}", e.getMessage());
		}
	}

	private static long extractLong(Object raw, String field) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null || raw.toString().isBlank()) {
			throw new BadRequestException(field + " is required");
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(field + " is invalid");
		}
	}

	private static int extractPriceCents(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("price is invalid");
		}
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
