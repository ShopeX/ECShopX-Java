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
public class HfpayEnterapplyInitAfterDistributionAddExecutionService {

	private final EnterapplyInitLedger ledger;

	public HfpayEnterapplyInitAfterDistributionAddExecutionService(EnterapplyInitLedger ledger) {
		this.ledger = ledger;
	}

	public void executeFromDispatchPayload(Map<String, Object> payload) {
		Object raw = payload != null ? payload.get("entities") : null;
		if (!(raw instanceof Map<?, ?>)) {
			throw new BadRequestException("entities must be a map");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) raw;
		String cardId = requireNonBlankAlias(entities, "card_id", "cardId", "entities.card_id");
		String companyId = requireNonBlankAlias(entities, "company_id", "companyId", "entities.company_id");
		ledger.record(cardId, companyId);
	}

	private static String requireNonBlankAlias(
			Map<String, Object> entities, String snakeKey, String camelKey, String label) {
		Object value = entities.get(snakeKey);
		if (value == null) {
			value = entities.get(camelKey);
		}
		if (value == null) {
			throw new BadRequestException(label + " is required");
		}
		String text = String.valueOf(value).trim();
		if (text.isEmpty()) {
			throw new BadRequestException(label + " must be non-blank");
		}
		return text;
	}
}
