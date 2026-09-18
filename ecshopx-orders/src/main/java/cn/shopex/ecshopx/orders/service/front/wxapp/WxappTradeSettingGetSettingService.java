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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.companys.service.setting.RechargeSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.TradeBasicSettingRedisService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappTradeSettingGetSettingService {

	private static final String KEY_IS_OPEN = "is_open";
	private static final String KEY_RECHARGE_STATUS = "recharge_status";
	private static final String KEY_IS_RECHARGE_STATUS = "is_recharge_status";

	private final TradeBasicSettingRedisService tradeBasicSettingRedisService;
	private final RechargeSettingRedisService rechargeSettingRedisService;

	public WxappTradeSettingGetSettingService(
			TradeBasicSettingRedisService tradeBasicSettingRedisService,
			RechargeSettingRedisService rechargeSettingRedisService) {
		this.tradeBasicSettingRedisService = tradeBasicSettingRedisService;
		this.rechargeSettingRedisService = rechargeSettingRedisService;
	}

	public Map<String, Object> getSetting(long companyId) {
		Object basic = tradeBasicSettingRedisService.getSetting(companyId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();

		if (basic instanceof Map<?, ?> basicMap) {
			for (Map.Entry<?, ?> e : basicMap.entrySet()) {
				data.put(String.valueOf(e.getKey()), e.getValue());
			}
			boolean isOpen =
					basicMap.containsKey(KEY_IS_OPEN)
							&& "true".equalsIgnoreCase(String.valueOf(basicMap.get(KEY_IS_OPEN)).trim());
			data.put(KEY_IS_OPEN, isOpen ? Boolean.TRUE : Boolean.FALSE);
		} else if (basic instanceof List<?>) {
			// No keys from basic; omit is_open
		} else {
			throw new IllegalStateException("trade basic setting malformed");
		}

		Map<String, Object> recharge = rechargeSettingRedisService.handle(companyId, Collections.emptyMap());
		Object rawStatus = recharge.get(KEY_RECHARGE_STATUS);
		data.put(KEY_IS_RECHARGE_STATUS, normalizeRechargeStatusBoolean(rawStatus));
		return data;
	}

	private static Boolean normalizeRechargeStatusBoolean(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			return Boolean.parseBoolean(s.trim());
		}
		return Boolean.FALSE;
	}
}
