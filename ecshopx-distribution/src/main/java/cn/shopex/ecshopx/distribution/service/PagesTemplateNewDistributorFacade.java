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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.port.theme.NewDistributorPagesTemplatePort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Coordinates default storefront page setup for a newly created shop within a company.
 */
@Service
public class PagesTemplateNewDistributorFacade {

	private final NewDistributorPagesTemplatePort newDistributorPagesTemplatePort;

	public PagesTemplateNewDistributorFacade(NewDistributorPagesTemplatePort newDistributorPagesTemplatePort) {
		this.newDistributorPagesTemplatePort = newDistributorPagesTemplatePort;
	}

	/**
	 * Applies template defaults from a persisted distributor row snapshot (API row shape after insert).
	 */
	public void newDistributor(Map<String, Object> row) {
		long companyId = extractLong(row.get("company_id"));
		long distributorId = extractLong(row.get("distributor_id"));
		newDistributorPagesTemplatePort.bindDefaultTemplates(companyId, distributorId, row);
	}

	/**
	 * Applies default storefront templates for a new distributor using the persisted row snapshot.
	 */
	public void applyDefaultTemplatesForNewDistributor(Map<String, Object> distributorRow) {
		newDistributor(distributorRow);
	}

	/**
	 * Applies template defaults for the new distributor using company and shop ids only (region defaults to HQ).
	 */
	public void newDistributor(long companyId, long distributorId) {
		Map<String, Object> minimal = new LinkedHashMap<>();
		minimal.put("company_id", companyId);
		minimal.put("distributor_id", distributorId);
		newDistributorPagesTemplatePort.bindDefaultTemplates(companyId, distributorId, minimal);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
