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
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Records the last accepted distribution-add enterapply identifiers. Production use is a
 * lightweight bookkeeping hook; tests assert on {@link #lastRecorded()}.
 */
@Component
public class EnterapplyInitLedger {

	public record CardCompanyRef(String cardId, String companyId) {}

	private final AtomicReference<CardCompanyRef> lastRecorded = new AtomicReference<>();

	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;
	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;

	public EnterapplyInitLedger(
			HfpayEnterapplyReadService hfpayEnterapplyReadService, HfpayEnterapplyMapper hfpayEnterapplyMapper) {
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
	}

	public void record(String cardId, String companyId) {
		lastRecorded.set(new CardCompanyRef(cardId, companyId));
	}

	public Optional<CardCompanyRef> lastRecorded() {
		return Optional.ofNullable(lastRecorded.get());
	}

	public void afterDistributionEditEnterapplyInitFromEntities(Map<String, Object> entities) {
		long companyId = requireLongId(entities, "company_id");
		long distributorId = requireLongId(entities, "distributor_id");
		Object rawOpen = entities.get("is_open");
		if (!(rawOpen instanceof String) || !"true".equals(rawOpen)) {
			return;
		}
		if (hfpayEnterapplyReadService.getEnterapply(companyId, distributorId) != null) {
			return;
		}
		HfpayEnterapply row = new HfpayEnterapply();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setApplyType("1");
		row.setStatus("1");
		hfpayEnterapplyMapper.insert(row);
	}

	private static long requireLongId(Map<String, Object> entities, String field) {
		Object raw = entities != null ? entities.get(field) : null;
		if (raw == null) {
			throw new BadRequestException(field + " is required");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			throw new BadRequestException(field + " is required");
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			throw new BadRequestException(field + " must be a valid number");
		}
	}
}
