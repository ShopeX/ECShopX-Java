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

import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.mapper.RechargeRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

@Service
public class RechargeRuleDeleteService {

	private final RechargeRuleMapper rechargeRuleMapper;

	public RechargeRuleDeleteService(RechargeRuleMapper rechargeRuleMapper) {
		this.rechargeRuleMapper = rechargeRuleMapper;
	}

	public void deleteById(long companyId, String id) {
		String cid = String.valueOf(companyId);
		QueryWrapper<RechargeRule> wrapper =
				Wrappers.<RechargeRule>query().eq("company_id", cid).eq("id", id);
		rechargeRuleMapper.delete(wrapper);
	}
}
