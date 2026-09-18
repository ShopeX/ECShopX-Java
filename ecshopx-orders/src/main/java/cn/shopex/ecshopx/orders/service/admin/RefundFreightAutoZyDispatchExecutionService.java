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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.mapper.DistributionDistributorRefundFreightBulkMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RefundFreightAutoZyDispatchExecutionService {

	private final DistributionDistributorRefundFreightBulkMapper distributionDistributorRefundFreightBulkMapper;

	public RefundFreightAutoZyDispatchExecutionService(
			DistributionDistributorRefundFreightBulkMapper distributionDistributorRefundFreightBulkMapper) {
		this.distributionDistributorRefundFreightBulkMapper = distributionDistributorRefundFreightBulkMapper;
	}

	public void executeFromDispatchPayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Object rawEntities = payload.get("entities");
		if (!(rawEntities instanceof Map<?, ?> rawMap)) {
			return;
		}
		if (!looseEqualsOne(rawMap.get("is_refund_freight"))) {
			return;
		}
		Object rawCompany = rawMap.get("company_id");
		if (!(rawCompany instanceof Number companyNum)) {
			return;
		}
		long companyId = companyNum.longValue();
		try {
			distributionDistributorRefundFreightBulkMapper.updateIsRefundFreightByCompanyAndDistributionType(
					companyId, 0, 1);
		} catch (Exception ignored) {
		}
	}

	private static boolean looseEqualsOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(v).trim());
	}
}
