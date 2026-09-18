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

package cn.shopex.ecshopx.hfpay.dispatch;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HfpayEnterapplyInitAfterDistributionEditExecutionService {

	private final EnterapplyInitLedger ledger;

	public HfpayEnterapplyInitAfterDistributionEditExecutionService(EnterapplyInitLedger ledger) {
		this.ledger = ledger;
	}

	public void executeFromDispatchPayload(Map<String, Object> payload) {
		Object raw = payload != null ? payload.get("entities") : null;
		if (!(raw instanceof Map<?, ?>)) {
			throw new BadRequestException("entities must be a map");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) raw;
		ledger.afterDistributionEditEnterapplyInitFromEntities(entities);
	}
}
