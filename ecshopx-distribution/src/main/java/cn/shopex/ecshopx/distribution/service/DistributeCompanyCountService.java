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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.popularize.dto.PromoterBrokerageCompanySumRow;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributeCompanyCountService {

	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;

	public DistributeCompanyCountService(PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper) {
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
	}

	public Map<String, Object> getCount(long companyId) {
		PromoterBrokerageCompanySumRow row = promoterBrokerageStatisticsMapper.selectCompanySum(companyId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (row == null) {
			putZeros(out);
			return out;
		}
		out.put("itemTotalPrice", row.getItemTotalPrice() != null ? row.getItemTotalPrice() : 0L);
		out.put("rebateTotal", row.getRebateTotal() != null ? row.getRebateTotal() : 0L);
		out.put("noCloseRebate", row.getNoCloseRebate() != null ? row.getNoCloseRebate() : 0L);
		out.put(
				"cashWithdrawalRebate",
				row.getCashWithdrawalRebate() != null ? row.getCashWithdrawalRebate() : 0L);
		out.put(
				"freezeCashWithdrawalRebate",
				row.getFreezeCashWithdrawalRebate() != null ? row.getFreezeCashWithdrawalRebate() : 0L);
		out.put("rechargeRebate", row.getRechargeRebate() != null ? row.getRechargeRebate() : 0L);
		out.put("payedRebate", row.getPayedRebate() != null ? row.getPayedRebate() : 0L);
		return out;
	}

	private static void putZeros(LinkedHashMap<String, Object> out) {
		out.put("itemTotalPrice", 0L);
		out.put("rebateTotal", 0L);
		out.put("noCloseRebate", 0L);
		out.put("cashWithdrawalRebate", 0L);
		out.put("freezeCashWithdrawalRebate", 0L);
		out.put("rechargeRebate", 0L);
		out.put("payedRebate", 0L);
	}
}
