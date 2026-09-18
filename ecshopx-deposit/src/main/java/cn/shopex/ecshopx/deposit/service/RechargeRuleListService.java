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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RechargeRuleListService {

	private final RechargeRuleMapper rechargeRuleMapper;

	public RechargeRuleListService(RechargeRuleMapper rechargeRuleMapper) {
		this.rechargeRuleMapper = rechargeRuleMapper;
	}

	public Map<String, Object> getRechargeRuleListPage(String companyId, int pageSize, int page) {
		LambdaQueryWrapper<RechargeRule> w = new LambdaQueryWrapper<>();
		w.eq(RechargeRule::getCompanyId, companyId);
		w.orderByDesc(RechargeRule::getCreateTime);
		Page<RechargeRule> p = new Page<>(page, pageSize);
		rechargeRuleMapper.selectPage(p, w);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", p.getTotal());
		out.put("list", p.getRecords());
		return out;
	}

	public Map<String, Object> getRechargeRuleListPageForWxapp(String companyId, int pageSize, int page) {
		Map<String, Object> raw = getRechargeRuleListPage(companyId, pageSize, page);
		Object tcObj = raw.get("total_count");
		long totalCount;
		if (tcObj == null) {
			totalCount = 0L;
		} else if (tcObj instanceof Number) {
			totalCount = ((Number) tcObj).longValue();
		} else {
			totalCount = Long.parseLong(String.valueOf(tcObj).trim());
		}
		if (totalCount <= 0) {
			return raw;
		}
		@SuppressWarnings("unchecked")
		List<RechargeRule> records = (List<RechargeRule>) raw.get("list");
		List<Map<String, Object>> mappedList = new ArrayList<>();
		if (records != null) {
			for (RechargeRule entity : records) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("id", entity.getId());
				row.put("companyId", entity.getCompanyId());
				String fenStr = entity.getMoney();
				if (fenStr == null || fenStr.isBlank()) {
					row.put("money", null);
				} else {
					row.put(
							"money",
							new BigDecimal(fenStr.trim())
									.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
									.toPlainString());
				}
				row.put("ruleType", entity.getRuleType());
				row.put("ruleData", entity.getRuleData());
				row.put("createTime", entity.getCreateTime());
				mappedList.add(row);
			}
		}
		raw.put("list", mappedList);
		return raw;
	}
}
