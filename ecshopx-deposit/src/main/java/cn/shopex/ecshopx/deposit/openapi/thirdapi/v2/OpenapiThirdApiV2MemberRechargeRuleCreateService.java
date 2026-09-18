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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeV2FailException;
import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.service.RechargeRuleCreateService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleMergedFieldParser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberRechargeRuleCreateService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final RechargeRuleCreateService rechargeRuleCreateService;

	public OpenapiThirdApiV2MemberRechargeRuleCreateService(
			RechargeRuleCreateService rechargeRuleCreateService) {
		this.rechargeRuleCreateService = rechargeRuleCreateService;
	}

	public Map<String, Object> executeOpenapiCreateRechargeRule(
			long companyId,
			String fixedMoneyRaw,
			String ruleTypeRaw,
			String ruleDataRaw) {
		if (fixedMoneyRaw == null || !StringUtils.hasText(fixedMoneyRaw.trim())) {
			throw missingParams("需付金额必填");
		}

		if (ruleTypeRaw == null || !StringUtils.hasText(ruleTypeRaw.trim())) {
			throw missingParams("请填写正确的充值类型");
		}
		String ruleType = ruleTypeRaw.trim();
		if (!"money".equals(ruleType) && !"point".equals(ruleType)) {
			throw missingParams("请填写正确的充值类型");
		}

		if (ruleDataRaw == null || !StringUtils.hasText(ruleDataRaw.trim())) {
			throw missingParams("请填写正确的赠送金额");
		}
		try {
			RechargeRuleMergedFieldParser.parseNonNegativeIntegerString(
					ruleDataRaw.trim(), "请填写正确的赠送金额");
		} catch (ResourceException e) {
			throw missingParams("请填写正确的赠送金额");
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("fixed_money", fixedMoneyRaw.trim());
		merged.put("rule_type", ruleType);
		merged.put("rule_data", ruleDataRaw.trim());

		RechargeRule entity;
		try {
			entity = rechargeRuleCreateService.create(companyId, merged);
		} catch (ResourceException e) {
			throw rechargeError(e.getMessage());
		}
		return formatOpenApiResponse(entity);
	}

	public static Map<String, Object> formatOpenApiResponse(RechargeRule entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("rechargerule_id", entity.getId());
		String fenStr = entity.getMoney();
		row.put(
				"money",
				new BigDecimal(fenStr.trim())
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString());
		row.put("rule_type", entity.getRuleType());
		row.put("rule_data", Integer.parseInt(entity.getRuleData().trim()));
		row.put("create_time", formatEpochSeconds(entity.getCreateTime()));
		return row;
	}

	private static String formatEpochSeconds(String createTimeStr) {
		long epoch = Long.parseLong(createTimeStr.trim());
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}

	private static OpenapiMemberRechargeV2FailException missingParams(String message) {
		return new OpenapiMemberRechargeV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberRechargeV2FailException rechargeError(String message) {
		return new OpenapiMemberRechargeV2FailException(OpenapiErrorCode.MEMBER_RECHARGE_ERROR, message);
	}
}
