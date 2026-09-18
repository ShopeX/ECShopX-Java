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

package cn.shopex.ecshopx.distribution.service.export;

import cn.shopex.ecshopx.common.dispatch.DistributorWhiteListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.dispatch.DistributorWhiteListExportFileJobPayloadSupport;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorWhiteListExportService {

	private static final Logger log = LoggerFactory.getLogger(DistributorWhiteListExportService.class);

	private final OperatorsQueryService operatorsQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorWhiteListExportFileJobDispatchPublisher distributorWhiteListExportFileJobDispatchPublisher;

	public DistributorWhiteListExportService(
			OperatorsQueryService operatorsQueryService,
			DistributorListQueryService distributorListQueryService,
			DistributorWhiteListExportFileJobDispatchPublisher distributorWhiteListExportFileJobDispatchPublisher) {
		this.operatorsQueryService = operatorsQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorWhiteListExportFileJobDispatchPublisher = distributorWhiteListExportFileJobDispatchPublisher;
	}

	public void exportWhiteList(
			long companyId,
			long operatorId,
			long merchantId,
			String operatorType,
			Long jwtDistributorId,
			Map<String, Object> mergedRequest,
			String datapassBlockHeader) {
		try {
			DistributorWhiteListExportFilter exportFilter =
					buildExportFilter(companyId, operatorType, jwtDistributorId, mergedRequest);
			long supplierId = resolveSupplierId(companyId, operatorId);
			LinkedHashMap<String, Object> filterMap =
					DistributorWhiteListExportFileJobPayloadSupport.filterToMap(exportFilter);
			distributorWhiteListExportFileJobDispatchPublisher.publish(
					companyId, operatorId, merchantId, supplierId, filterMap, datapassBlockHeader);
		} catch (Exception ex) {
			log.warn("whitelist export job dispatch failed", ex);
		}
	}

	private long resolveSupplierId(long companyId, long operatorId) {
		if (operatorId <= 0L) {
			return 0L;
		}
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_id", operatorId);
		filter.put("operator_type", "supplier");
		Map<String, Object> supplier = operatorsQueryService.getInfo(filter);
		return (supplier != null && !supplier.isEmpty()) ? operatorId : 0L;
	}

	public DistributorWhiteListExportFilter buildExportFilter(
			long companyId,
			String operatorType,
			Long jwtDistributorId,
			Map<String, Object> merged) {
		boolean shopNotFound = false;
		List<Long> distributorIds = null;

		String ot = operatorType == null ? "" : operatorType.trim();
		if (StringUtils.hasText(ot)
				&& "distributor".equalsIgnoreCase(ot)
				&& jwtDistributorId != null
				&& jwtDistributorId > 0L) {
			distributorIds = List.of(jwtDistributorId);
		}

		boolean mobileFilterActive = merged != null && merged.containsKey("search_mobile");
		String mobileValue = "";
		if (mobileFilterActive) {
			mobileValue = Objects.toString(merged.get("search_mobile"), "");
		}

		String usernamePrefix = null;
		if (merged != null) {
			Object un = merged.get("username");
			if (un instanceof String s && StringUtils.hasText(s)) {
				usernamePrefix = s.trim();
			}
		}

		Long shopResolvedDistributorId = null;
		if (merged != null) {
			Object sc = merged.get("shop_code");
			if (ValuePresence.hasEffectiveValue(sc) && sc instanceof String s) {
				String code = s.trim();
				Map<String, Long> mapped =
						distributorListQueryService.mapShopCodeToDistributorId(companyId, List.of(code));
				Long id = mapped.get(code);
				if (id == null) {
					shopNotFound = true;
				} else {
					shopResolvedDistributorId = id;
				}
			}
		}

		List<Long> requestDistributorIds = null;
		if (merged != null && merged.containsKey("distributor_id")) {
			Object rawDist = merged.get("distributor_id");
			if (ValuePresence.hasEffectiveValue(rawDist)) {
				requestDistributorIds = tryParseDistributorIds(rawDist);
			}
		}

		if (requestDistributorIds != null && !requestDistributorIds.isEmpty()) {
			distributorIds = requestDistributorIds;
			shopNotFound = false;
		} else if (shopResolvedDistributorId != null) {
			distributorIds = List.of(shopResolvedDistributorId);
		}

		return new DistributorWhiteListExportFilter(
				companyId, distributorIds, mobileFilterActive, mobileValue, usernamePrefix, shopNotFound);
	}

	/**
	 * Builds the same filter shape as {@link #buildExportFilter} from query-string parameters only
	 * (no request body merge at the controller).
	 */
	public DistributorWhiteListExportFilter buildExportFilterForWhiteListGet(
			long companyId, String operatorType, Long jwtDistributorId, Map<String, Object> queryOnlyMerged) {
		return buildExportFilter(companyId, operatorType, jwtDistributorId, queryOnlyMerged);
	}

	private List<Long> tryParseDistributorIds(Object rawDist) {
		try {
			if (rawDist instanceof List<?> list) {
				if (list.isEmpty()) {
					return List.of();
				}
				List<Long> out = new ArrayList<>(list.size());
				for (Object o : list) {
					if (o == null) {
						continue;
					}
					if (o instanceof Number n) {
						out.add(n.longValue());
					} else if (o instanceof String s) {
						out.add(Long.parseLong(s.trim()));
					} else {
						return null;
					}
				}
				return out;
			}
			if (rawDist instanceof Number n) {
				return List.of(n.longValue());
			}
			if (rawDist instanceof String s) {
				if (!StringUtils.hasText(s)) {
					return null;
				}
				return List.of(Long.parseLong(s.trim()));
			}
		} catch (NumberFormatException ex) {
			log.warn("ignored invalid distributor_id in whitelist export request");
			return null;
		}
		return null;
	}
}
