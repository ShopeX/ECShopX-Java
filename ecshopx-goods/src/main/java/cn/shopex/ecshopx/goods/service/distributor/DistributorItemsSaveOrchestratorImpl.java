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
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.DistributorItemsSaveOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsSaveRequestValidator;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.dto.SaveDistributorItemsResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorItemsSaveOrchestratorImpl implements DistributorItemsSaveOrchestrator {

	private final DistributorListQueryService distributorListQueryService;
	private final DistributorInfoResolveService distributorInfoResolveService;
	private final DistributorItemsPagedAddRunner pagedAddRunner;
	private final LangueProperties langueProperties;

	public DistributorItemsSaveOrchestratorImpl(
			DistributorListQueryService distributorListQueryService,
			DistributorInfoResolveService distributorInfoResolveService,
			DistributorItemsPagedAddRunner pagedAddRunner,
			LangueProperties langueProperties) {
		this.distributorListQueryService = distributorListQueryService;
		this.distributorInfoResolveService = distributorInfoResolveService;
		this.pagedAddRunner = pagedAddRunner;
		this.langueProperties = langueProperties;
	}

	@Override
	public SaveDistributorItemsResult save(
			HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> merged) {
		long companyId = toLong(operatorJwt.get("company_id"));
		String requestLang = RequestLangTag.current(langueProperties);
		List<Long> distributorIds = resolveDistributorIds(merged, companyId);
		boolean isQueue =
				!(distributorIds.size() < 5
						&& DistributorItemsSaveRequestValidator.itemIdsCountForQueueThreshold(merged) < 5);
		boolean isCanSale = DistributorItemsSaveRequestValidator.parseIsCanSale(merged);
		List<Long> defaultItemFilter = DistributorItemsSaveRequestValidator.parseDefaultItemIdList(merged);
		int pageSize = 100;
		Object lastRes = true;
		for (Long distributorId : distributorIds) {
			if (distributorId == null || distributorId <= 0) {
				continue;
			}
			var info = distributorInfoResolveService.resolveStoreDetail(companyId, distributorId, requestLang);
			if (info.isEmpty()) {
				continue;
			}
			Object isv = info.get().get("is_valid");
			if (isv != null && "delete".equalsIgnoreCase(String.valueOf(isv).trim())) {
				continue;
			}
			var params =
					new DistributorItemsPagedAddRunner.AddJobParams(
							companyId, distributorId, defaultItemFilter, isCanSale, 1, pageSize);
			if (isQueue) {
				pagedAddRunner.enqueueAsyncFirstPage(params);
				lastRes = true;
			} else {
				lastRes = pagedAddRunner.runAllPagesSync(params);
			}
		}
		return new SaveDistributorItemsResult(true, lastRes);
	}

	private List<Long> resolveDistributorIds(Map<String, Object> merged, long companyId) {
		List<Long> parsed = DistributorItemsSaveRequestValidator.parseDistributorIdList(merged);
		if (parsed == null) {
			return new ArrayList<>(distributorListQueryService.listNonDeletedDistributorIdsForCompany(companyId));
		}
		return parsed;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
