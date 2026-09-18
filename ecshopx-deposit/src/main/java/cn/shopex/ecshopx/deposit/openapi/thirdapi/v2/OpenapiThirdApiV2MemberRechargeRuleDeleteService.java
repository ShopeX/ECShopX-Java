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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeV2FailException;
import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.mapper.RechargeRuleMapper;
import cn.shopex.ecshopx.deposit.service.RechargeRuleDeleteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberRechargeRuleDeleteService {

	private final RechargeRuleMapper rechargeRuleMapper;
	private final RechargeRuleDeleteService rechargeRuleDeleteService;

	public OpenapiThirdApiV2MemberRechargeRuleDeleteService(
			RechargeRuleMapper rechargeRuleMapper,
			RechargeRuleDeleteService rechargeRuleDeleteService) {
		this.rechargeRuleMapper = rechargeRuleMapper;
		this.rechargeRuleDeleteService = rechargeRuleDeleteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiDeleteRechargeRule(long companyId, String rechargeruleIdRaw) {
		long ruleId = parseRechargeRuleId(rechargeruleIdRaw);

		String cid = String.valueOf(companyId);
		RechargeRule existing = rechargeRuleMapper.selectOne(
				new LambdaQueryWrapper<RechargeRule>()
						.eq(RechargeRule::getId, (int) ruleId)
						.eq(RechargeRule::getCompanyId, cid)
						.last("LIMIT 1"));
		if (existing == null) {
			throw notFound();
		}

		rechargeRuleDeleteService.deleteById(companyId, String.valueOf(ruleId));

		Map<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return data;
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

	private static OpenapiMemberRechargeV2FailException notFound() {
		return new OpenapiMemberRechargeV2FailException(
				OpenapiErrorCode.MEMBER_RECHARGE_NOT_FOUND, "未查询到该储值规则");
	}
}
