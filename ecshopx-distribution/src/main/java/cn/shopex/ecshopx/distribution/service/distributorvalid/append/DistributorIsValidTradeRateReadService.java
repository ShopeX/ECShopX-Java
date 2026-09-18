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

package cn.shopex.ecshopx.distribution.service.distributorvalid.append;

import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidTradeRateReadService {

	private final TradeRateMapper tradeRateMapper;

	public DistributorIsValidTradeRateReadService(TradeRateMapper tradeRateMapper) {
		this.tradeRateMapper = tradeRateMapper;
	}

	public void appendScoreList(long companyId, Map<String, Object> result) {
		Object did = result.get("distributor_id");
		if (!(did instanceof Number n)) {
			return;
		}
		long distributorId = n.longValue();
		LinkedHashMap<String, Object> score = new LinkedHashMap<>();
		score.put("avg_star", "5.0");
		score.put("default", Integer.valueOf(1));
		BigDecimal avg = tradeRateMapper.selectAvgStarByCompanyAndDistributor(companyId, distributorId);
		if (avg != null && Double.isFinite(avg.doubleValue())) {
			score.put("avg_star", String.format(Locale.US, "%.1f", avg));
			score.put("default", Integer.valueOf(0));
		}
		result.put("scoreList", score);
	}
}
