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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.dispatch.DistributorItemsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorItemsExportOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorItemsExportOrchestratorImpl implements DistributorItemsExportOrchestrator {

	private static final int DISTRIBUTOR_ITEMS_EXPORT_PAGE_SIZE = 500;

	private final DistributorItemsExportFilterBuilder distributorItemsExportFilterBuilder;
	private final DistributorItemsExportFileJobDispatchPublisher distributorItemsExportFileJobDispatchPublisher;
	private final LangueProperties langueProperties;

	public DistributorItemsExportOrchestratorImpl(
			DistributorItemsExportFilterBuilder distributorItemsExportFilterBuilder,
			DistributorItemsExportFileJobDispatchPublisher distributorItemsExportFileJobDispatchPublisher,
			LangueProperties langueProperties) {
		this.distributorItemsExportFilterBuilder = distributorItemsExportFilterBuilder;
		this.distributorItemsExportFileJobDispatchPublisher = distributorItemsExportFileJobDispatchPublisher;
		this.langueProperties = langueProperties;
	}

	@Override
	public void submitExport(HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> merged) {
		long companyId = longVal(operatorJwt.get("company_id"));
		Map<String, Object> filterBase = distributorItemsExportFilterBuilder.build(companyId, merged);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		long operatorId = toLongOperatorId(operatorJwt.get("operator_id"));
		distributorItemsExportFileJobDispatchPublisher.publish(
				companyId,
				operatorId,
				acceptLanguage,
				new LinkedHashMap<>(filterBase),
				DISTRIBUTOR_ITEMS_EXPORT_PAGE_SIZE);
	}

	private static long toLongOperatorId(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
