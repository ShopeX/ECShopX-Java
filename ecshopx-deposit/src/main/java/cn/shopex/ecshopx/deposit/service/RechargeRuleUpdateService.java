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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RechargeRuleUpdateService {

	private final RechargeRuleMapper rechargeRuleMapper;

	public RechargeRuleUpdateService(RechargeRuleMapper rechargeRuleMapper) {
		this.rechargeRuleMapper = rechargeRuleMapper;
	}

	public void update(long companyId, Map<String, Object> merged) {
		int id = parseRuleId(merged.get("id"));

		long fen = RechargeRuleMergedFieldParser.parseFixedMoneyToFen(merged.get("fixed_money"));
		if (fen < 1 || fen > 10_000_000L) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}

		String ruleType = RechargeRuleMergedFieldParser.requireRuleType(merged);
		String ruleDataStr = RechargeRuleMergedFieldParser.normalizeRuleDataForType(ruleType, merged.get("rule_data"));

		String cid = String.valueOf(companyId);
		RechargeRule existing = rechargeRuleMapper.selectOne(
				new LambdaQueryWrapper<RechargeRule>()
						.eq(RechargeRule::getId, id)
						.eq(RechargeRule::getCompanyId, cid));
		if (existing == null) {
			throw new ResourceException("修改的充值规则不存在");
		}

		String moneyKey = String.valueOf(fen);
		long conflict = rechargeRuleMapper.selectCount(
				new LambdaQueryWrapper<RechargeRule>()
						.eq(RechargeRule::getCompanyId, cid)
						.eq(RechargeRule::getMoney, moneyKey)
						.ne(RechargeRule::getId, id));
		if (conflict > 0) {
			throw new ResourceException("当前面额数已存在，不能重复添加");
		}

		LambdaUpdateWrapper<RechargeRule> uw = new LambdaUpdateWrapper<RechargeRule>()
				.eq(RechargeRule::getId, id)
				.eq(RechargeRule::getCompanyId, cid)
				.set(RechargeRule::getMoney, moneyKey)
				.set(RechargeRule::getRuleType, ruleType)
				.set(RechargeRule::getRuleData, ruleDataStr);
		int rows = rechargeRuleMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("修改的充值规则不存在");
		}
	}

	private static int parseRuleId(Object raw) {
		if (raw == null) {
			throw new ResourceException("请选择要编辑的规则", 400);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("请选择要编辑的规则", 400);
			}
			return parseRuleIdFromString(t);
		}
		if (raw instanceof Number n) {
			if (n instanceof Double d) {
				double dv = d.doubleValue();
				if (!Double.isFinite(dv) || dv < 1 || dv != Math.rint(dv) || dv > Integer.MAX_VALUE) {
					throw new ResourceException("请选择要编辑的规则", 400);
				}
				return (int) dv;
			}
			if (n instanceof Float f) {
				float fv = f.floatValue();
				if (!Float.isFinite(fv) || fv < 1 || fv != Math.rint(fv) || fv > Integer.MAX_VALUE) {
					throw new ResourceException("请选择要编辑的规则", 400);
				}
				return (int) fv;
			}
			long v = n.longValue();
			if (v < 1 || v > Integer.MAX_VALUE) {
				throw new ResourceException("请选择要编辑的规则", 400);
			}
			return (int) v;
		}
		String t = raw.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("请选择要编辑的规则", 400);
		}
		return parseRuleIdFromString(t);
	}

	private static int parseRuleIdFromString(String t) {
		if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
			throw new ResourceException("请选择要编辑的规则", 400);
		}
		try {
			long v = Long.parseLong(t);
			if (v < 1 || v > Integer.MAX_VALUE) {
				throw new ResourceException("请选择要编辑的规则", 400);
			}
			return (int) v;
		} catch (NumberFormatException e) {
			throw new ResourceException("请选择要编辑的规则", 400);
		}
	}
}
