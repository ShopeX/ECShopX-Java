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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RechargeSendSmsNoticeJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(RechargeSendSmsNoticeJobHandler.class);

	private static final DateTimeFormatter RECHARGE_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final SmsSendTestService smsSendTestService;
	private final StringRedisTemplate redis;

	public RechargeSendSmsNoticeJobHandler(
			SmsSendTestService smsSendTestService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.smsSendTestService = smsSendTestService;
		this.redis = redis;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong("companyId", payload);
		String mobile = extractMobile(payload);
		long totalFeeFen = extractLong("totalFee", payload);
		long balanceFen = readDepositTotalFen(companyId);
		Map<String, String> vars = buildDepositRechargeTemplateVars(totalFeeFen, balanceFen);
		try {
			smsSendTestService.sendTemplatedNoticeSms(companyId, mobile, "deposit_recharge", vars);
		} catch (RuntimeException e) {
			log.debug(
					"deposit_recharge failed companyId={} mobileTail={} err={}",
					companyId,
					maskTail(mobile),
					e.getMessage());
		}
	}

	private static long extractLong(String key, Map<String, Object> payload) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String extractMobile(Map<String, Object> payload) {
		Object raw = payload.get("mobile");
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private long readDepositTotalFen(long companyId) {
		Object raw = redis.opsForHash().get("shopDepositTotal", String.valueOf(companyId));
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, String> buildDepositRechargeTemplateVars(long rechargeTotalFen, long depositTotalFen) {
		Map<String, String> vars = new LinkedHashMap<>();
		vars.put("recharge_money", fenToYuanDisplay(rechargeTotalFen));
		vars.put("deposit_money", fenToYuanDisplay(depositTotalFen));
		vars.put("recharge_date", RECHARGE_DATE.format(Instant.now()));
		return vars;
	}

	private static String fenToYuanDisplay(long fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String maskTail(String mobile) {
		if (mobile == null || mobile.length() < 4) {
			return "****";
		}
		return "****" + mobile.substring(mobile.length() - 4);
	}
}
