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

package cn.shopex.ecshopx.orders.service.notify;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import cn.shopex.ecshopx.common.port.orders.TradePaySuccessTemplatedSmsPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeFinishSmsNotifyBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeFinishSmsNotifyBusService.class);
	private static final DateTimeFormatter PAY_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;
	private final TradePaySuccessTemplatedSmsPort tradePaySuccessTemplatedSmsPort;

	public TradeFinishSmsNotifyBusService(
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort,
			TradePaySuccessTemplatedSmsPort tradePaySuccessTemplatedSmsPort) {
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
		this.tradePaySuccessTemplatedSmsPort = tradePaySuccessTemplatedSmsPort;
	}

	public void handleTradeFinishRow(Map<String, Object> snakeCaseTradeRow) {
		if (snakeCaseTradeRow == null || snakeCaseTradeRow.isEmpty()) {
			return;
		}
		Object rawPayType = snakeCaseTradeRow.get("pay_type");
		String payType = rawPayType == null ? "" : String.valueOf(rawPayType).trim();
		if ("point".equalsIgnoreCase(payType) || "deposit".equalsIgnoreCase(payType)) {
			return;
		}
		Long companyId = parseCompanyId(snakeCaseTradeRow.get("company_id"));
		if (companyId == null) {
			log.debug("TradeFinishSmsNotify: skip, invalid or missing company_id");
			return;
		}
		long cid = companyId;
		Optional<String> passport =
				companyPassportUidByCompanyIdPort.findPassportUid(cid);
		if (passport.isEmpty() || !StringUtils.hasText(passport.get())) {
			log.debug("TradeFinishSmsNotify: skip, blank passport uid for companyId={}", cid);
			return;
		}
		Object mobileRaw = snakeCaseTradeRow.get("mobile");
		String mobile = mobileRaw == null ? "" : String.valueOf(mobileRaw).trim();
		if (!StringUtils.hasText(mobile)) {
			log.debug("TradeFinishSmsNotify: skip, blank mobile");
			return;
		}
		String payTime = formatPayTime(snakeCaseTradeRow.get("time_start"));
		int payFeeCents = parsePayFeeCents(snakeCaseTradeRow.get("pay_fee"));
		String payMoney =
				BigDecimal.valueOf(payFeeCents)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString();
		Map<String, String> vars = new LinkedHashMap<>(2);
		vars.put("pay_time", payTime);
		vars.put("pay_money", payMoney);
		try {
			tradePaySuccessTemplatedSmsPort.sendTradePaySuccess(cid, mobile, vars);
		} catch (Throwable t) {
			log.debug("TradeFinishSmsNotify: send failed: {}", t.toString());
		}
	}

	private static Long parseCompanyId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePayFeeCents(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String formatPayTime(Object timeStart) {
		if (timeStart == null) {
			return "";
		}
		String s = String.valueOf(timeStart).trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		if (s.chars().allMatch(Character::isDigit)) {
			try {
				long sec = Long.parseLong(s);
				return PAY_TIME.format(Instant.ofEpochSecond(sec));
			} catch (NumberFormatException e) {
				return s;
			}
		}
		return s;
	}
}
