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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappTradeRatePraiseStatusService {

	private final WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService;

	public WxappTradeRatePraiseStatusService(
			WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService) {
		this.wxappTradeRatePraiseCheckService = wxappTradeRatePraiseCheckService;
	}

	public Map<String, Object> ratePraiseStatus(
			long companyId, long userId, List<String> orderedRateKeys) {
		if (orderedRateKeys == null || orderedRateKeys.isEmpty()) {
			throw new ResourceException("参数错误");
		}
		Map<String, Object> list = new LinkedHashMap<>();
		for (String rateKey : orderedRateKeys) {
			boolean praise =
					wxappTradeRatePraiseCheckService.ratePraiseCheck(companyId, userId, rateKey);
			list.put(rateKey, Map.of("praise_status", praise));
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("list", list);
		return body;
	}
}
