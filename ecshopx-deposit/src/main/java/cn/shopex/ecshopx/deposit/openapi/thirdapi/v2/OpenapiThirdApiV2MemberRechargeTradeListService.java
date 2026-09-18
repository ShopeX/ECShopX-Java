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

package cn.shopex.ecshopx.deposit.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberRechargeTradeListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final DateTimeFormatter PARSE_DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final DepositTradeMapper depositTradeMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberRechargeTradeListService(
			DepositTradeMapper depositTradeMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.depositTradeMapper = depositTradeMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> executeOpenapiGetRechargeTradeList(
			long companyId,
			int page,
			int pageSize,
			String mobileRaw,
			String tradeIdRaw,
			String shopIdRaw,
			String dateBeginRaw,
			String dateEndRaw) {
		LambdaQueryWrapper<DepositTrade> baseFilter = new LambdaQueryWrapper<>();
		baseFilter.eq(DepositTrade::getCompanyId, String.valueOf(companyId));
		baseFilter.eq(DepositTrade::getTradeStatus, "SUCCESS");

		if (phpTruthy(mobileRaw)) {
			baseFilter.eq(DepositTrade::getMobile, sensitiveFieldEncryptor.encrypt(mobileRaw.trim()));
		}
		if (phpTruthy(tradeIdRaw)) {
			baseFilter.eq(DepositTrade::getDepositTradeId, tradeIdRaw.trim());
		}
		if (phpTruthy(shopIdRaw)) {
			baseFilter.eq(DepositTrade::getShopId, shopIdRaw.trim());
		}
		if (phpTruthy(dateBeginRaw)) {
			baseFilter.ge(DepositTrade::getTimeStart, phpStrtotimeSeconds(dateBeginRaw));
		}
		if (phpTruthy(dateEndRaw)) {
			baseFilter.le(DepositTrade::getTimeStart, phpStrtotimeSeconds(dateEndRaw));
		}
		baseFilter.orderByDesc(DepositTrade::getTimeStart);

		long totalCount = depositTradeMapper.selectCount(baseFilter);

		Page<DepositTrade> pageRequest = new Page<>(page, pageSize, false);
		depositTradeMapper.selectPage(pageRequest, baseFilter);
		List<DepositTrade> entities = pageRequest.getRecords();

		List<Map<String, Object>> list =
				entities.stream().map(this::formatOpenApiListRow).toList();

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private Map<String, Object> formatOpenApiListRow(DepositTrade e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_id", e.getDepositTradeId());
		row.put("trade_type", e.getTradeType());
		row.put("money", formatMoneyYuan(e.getMoney()));
		String encMobile = e.getMobile();
		row.put("mobile", encMobile == null ? null : sensitiveFieldEncryptor.decrypt(encMobile));
		row.put("member_card_code", e.getMemberCardCode());
		row.put("shop_id", e.getShopId());
		row.put("shop_name", e.getShopName());
		row.put("open_id", e.getOpenId());
		row.put("transaction_id", nullOrRaw(e.getTransactionId()));
		row.put("recharge_rule_id", nullOrRaw(e.getRechargeRuleId()));
		row.put("bank_type", nullOrRaw(e.getBankType()));
		row.put("authorizer_appid", e.getAuthorizerAppid());
		row.put("detail", e.getDetail());
		row.put("time_start", formatPhpUnixDatetime(e.getTimeStart()));
		row.put("time_expire", formatPhpUnixDatetime(e.getTimeExpire()));
		return row;
	}

	private static String formatMoneyYuan(String money) {
		if (money == null || money.isBlank()) {
			return new BigDecimal(0).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
					.toPlainString();
		}
		return new BigDecimal(money.trim())
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String formatPhpUnixDatetime(String unixStr) {
		long epoch = 0L;
		if (unixStr != null && !unixStr.isBlank()) {
			try {
				epoch = Long.parseLong(unixStr.trim());
			} catch (NumberFormatException ignored) {
				epoch = 0L;
			}
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}

	private static Object nullOrRaw(String v) {
		if (v == null || v.isBlank()) {
			return null;
		}
		return v.trim();
	}

	private static boolean phpTruthy(String raw) {
		if (raw == null || raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}

	private static String phpStrtotimeSeconds(String raw) {
		if (raw == null || raw.isBlank()) {
			return "0";
		}
		try {
			LocalDateTime dt = LocalDateTime.parse(raw.trim(), PARSE_DATETIME_FMT);
			long epoch = dt.atZone(ZoneId.systemDefault()).toEpochSecond();
			return String.valueOf(epoch);
		} catch (DateTimeParseException ignored) {
			return "0";
		}
	}
}
