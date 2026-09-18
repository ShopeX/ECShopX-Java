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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.mapper.RechargeRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RechargeRuleCreateService {

	private final RechargeRuleMapper rechargeRuleMapper;

	public RechargeRuleCreateService(RechargeRuleMapper rechargeRuleMapper) {
		this.rechargeRuleMapper = rechargeRuleMapper;
	}

	public RechargeRule create(long companyId, Map<String, Object> merged) {
		long fen = RechargeRuleMergedFieldParser.parseFixedMoneyToFen(merged.get("fixed_money"));
		if (fen < 1 || fen > 10_000_000L) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}

		String ruleType = RechargeRuleMergedFieldParser.requireRuleType(merged);
		String ruleDataStr = RechargeRuleMergedFieldParser.normalizeRuleDataForType(ruleType, merged.get("rule_data"));

		String cid = String.valueOf(companyId);
		String moneyKey = String.valueOf(fen);
		long dup = rechargeRuleMapper.selectCount(
				new LambdaQueryWrapper<RechargeRule>()
						.eq(RechargeRule::getCompanyId, cid)
						.eq(RechargeRule::getMoney, moneyKey));
		if (dup > 0) {
			throw new ResourceException("当前面额数已存在，不能重复添加", 400);
		}

		long total = rechargeRuleMapper.selectCount(
				new LambdaQueryWrapper<RechargeRule>().eq(RechargeRule::getCompanyId, cid));
		if (total >= 14) {
			throw new ResourceException("最多添加14个面额", 400);
		}

		RechargeRule row = new RechargeRule();
		row.setCompanyId(cid);
		row.setMoney(moneyKey);
		row.setRuleType(ruleType);
		row.setRuleData(ruleDataStr);
		row.setCreateTime(String.valueOf(Instant.now().getEpochSecond()));
		rechargeRuleMapper.insert(row);
		return row;
	}
}
