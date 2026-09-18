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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.mapper.AdapayTradeDivFeeBatchMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayTradeDivFeeBatchService {

	private final AdapayTradeDivFeeBatchMapper adapayTradeDivFeeBatchMapper;

	public AdapayTradeDivFeeBatchService(AdapayTradeDivFeeBatchMapper adapayTradeDivFeeBatchMapper) {
		this.adapayTradeDivFeeBatchMapper = adapayTradeDivFeeBatchMapper;
	}

	public Map<String, Long> sumDivFeeByTradeIds(long companyId, List<String> tradeIds, String outputOperatorType) {
		List<Map<String, Object>> rows =
				adapayTradeDivFeeBatchMapper.sumDivFeeByTradeIds(companyId, tradeIds, outputOperatorType);
		Map<String, Long> out = new HashMap<>();
		if (rows == null) {
			return out;
		}
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			Object tid = row.get("tradeId");
			Object sum = row.get("sumDivFee");
			if (tid == null) {
				continue;
			}
			long v = 0L;
			if (sum instanceof Number n) {
				v = n.longValue();
			} else if (sum != null) {
				try {
					v = Long.parseLong(sum.toString());
				} catch (NumberFormatException ignored) {
					v = 0L;
				}
			}
			out.put(tid.toString(), v);
		}
		return out;
	}
}
