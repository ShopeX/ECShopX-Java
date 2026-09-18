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
import cn.shopex.ecshopx.deposit.mapper.RechargeRuleMapper;
import cn.shopex.ecshopx.deposit.service.RechargeRuleMergedFieldParser;
import cn.shopex.ecshopx.deposit.service.RechargeRuleUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberRechargeRuleUpdateService {

	private final RechargeRuleUpdateService rechargeRuleUpdateService;
	private final RechargeRuleMapper rechargeRuleMapper;

	public OpenapiThirdApiV2MemberRechargeRuleUpdateService(
			RechargeRuleUpdateService rechargeRuleUpdateService,
			RechargeRuleMapper rechargeRuleMapper) {
		this.rechargeRuleUpdateService = rechargeRuleUpdateService;
		this.rechargeRuleMapper = rechargeRuleMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiUpdateRechargeRule(
			long companyId,
			String rechargeruleIdRaw,
			String fixedMoneyRaw,
			String ruleTypeRaw,
			String ruleDataRaw) {
		long ruleId = parseRechargeRuleId(rechargeruleIdRaw);

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
		merged.put("id", (int) ruleId);
		merged.put("fixed_money", fixedMoneyRaw.trim());
		merged.put("rule_type", ruleType);
		merged.put("rule_data", ruleDataRaw.trim());

		try {
			rechargeRuleUpdateService.update(companyId, merged);
		} catch (ResourceException e) {
			throw rechargeError(e.getMessage());
		}

		String cid = String.valueOf(companyId);
		RechargeRule entity = rechargeRuleMapper.selectOne(
				new LambdaQueryWrapper<RechargeRule>()
						.eq(RechargeRule::getId, (int) ruleId)
						.eq(RechargeRule::getCompanyId, cid)
						.last("LIMIT 1"));
		if (entity == null) {
			throw rechargeError("修改的充值规则不存在");
		}
		return OpenapiThirdApiV2MemberRechargeRuleCreateService.formatOpenApiResponse(entity);
	}

	private static long parseRechargeRuleId(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw missingParams("请填写正确的储值规则ID");
		}
		String trimmed = raw.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw missingParams("请填写正确的储值规则ID");
		}
		if (id < 1) {
			throw missingParams("请填写正确的储值规则ID");
		}
		return id;
	}

	private static OpenapiMemberRechargeV2FailException missingParams(String message) {
		return new OpenapiMemberRechargeV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberRechargeV2FailException rechargeError(String message) {
		return new OpenapiMemberRechargeV2FailException(OpenapiErrorCode.MEMBER_RECHARGE_ERROR, message);
	}
}
