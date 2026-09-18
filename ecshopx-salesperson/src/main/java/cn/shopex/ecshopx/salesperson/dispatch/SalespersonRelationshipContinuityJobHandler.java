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

package cn.shopex.ecshopx.salesperson.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterWxappActionEventPort;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SalespersonRelationshipContinuityJobHandler implements DispatchHandler {

	private final MarketingCenterWxappActionEventPort marketingCenterWxappActionEventPort;

	public SalespersonRelationshipContinuityJobHandler(
			MarketingCenterWxappActionEventPort marketingCenterWxappActionEventPort) {
		this.marketingCenterWxappActionEventPort = marketingCenterWxappActionEventPort;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload, "company_id");
		if (companyId <= 0L) {
			return;
		}
		marketingCenterWxappActionEventPort.sendEvent(companyId, payload);
	}

	private static long extractLong(Map<String, Object> payload, String key) {
		return toLong(payload.get(key));
	}

	private static long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
