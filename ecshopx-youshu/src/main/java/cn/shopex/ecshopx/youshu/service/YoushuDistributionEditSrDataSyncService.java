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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class YoushuDistributionEditSrDataSyncService {

	public void syncAfterDistributionEdit(Map<String, Object> entities) {
		long companyId = requireLong(entities.get("company_id"), "company_id");
		long distributorId = requireLong(entities.get("distributor_id"), "distributor_id");
		if (companyId <= 0 || distributorId <= 0) {
			return;
		}
	}

	private static long requireLong(Object raw, String fieldLabel) {
		if (raw == null) {
			throw new BadRequestException(fieldLabel + " is required");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(fieldLabel + " must be a number");
		}
	}
}
