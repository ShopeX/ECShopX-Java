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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionEditPushMarketingCenterProcessor {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public DistributionEditPushMarketingCenterProcessor(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	/**
	 * Forwards distribution snapshot fields from the payload {@code entities} map to marketing center OpenAPI method
	 * {@code basics.distribution.edit.proccess}, mapping keys so the request body aligns with the entities snapshot
	 * shape expected by that method.
	 */
	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Object rawEntities = payload.get("entities");
		if (!(rawEntities instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) rawEntities;
		long companyId = readLong(entities.get("company_id"));
		long distributorId = readLong(entities.get("distributor_id"));
		if (companyId <= 0L || distributorId <= 0L) {
			return;
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("distributor_id", distributorId);
		if (entities.containsKey("distributor_bn")) {
			Object bn = entities.get("distributor_bn");
			if (bn != null && StringUtils.hasText(bn.toString())) {
				params.put("distributor_bn", bn.toString());
			}
		}
		marketingCenterOpenApiSignedFormClient.basicsDistributionEditProccess(companyId, params);
	}

	private static long readLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw != null && StringUtils.hasText(raw.toString())) {
			try {
				return Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
