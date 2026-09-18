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

package cn.shopex.ecshopx.deposit.integration.openapi;

import cn.shopex.ecshopx.common.deposit.port.OpenapiMemberDetailDepositRechargeSumPort;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class OpenapiMemberDetailDepositRechargeSumPortImpl implements OpenapiMemberDetailDepositRechargeSumPort {

	private final DepositTradeMapper depositTradeMapper;

	public OpenapiMemberDetailDepositRechargeSumPortImpl(DepositTradeMapper depositTradeMapper) {
		this.depositTradeMapper = depositTradeMapper;
	}

	@Override
	public Map<Long, Long> sumRechargeSuccessFenByUserIds(List<Long> userIds) {
		if (CollectionUtils.isEmpty(userIds)) {
			return Map.of();
		}
		List<Map<String, Object>> rows = depositTradeMapper.sumRechargeSuccessFenByUserIds(userIds);
		Map<Long, Long> result = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Long userId = toLong(row.get("user_id"));
			Long moneySum = toLong(row.get("money_sum"));
			if (userId != null && moneySum != null) {
				result.put(userId, moneySum);
			}
		}
		return result;
	}

	private static Long toLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
