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

package cn.shopex.ecshopx.distribution.service.marketingcenter;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOperatorSalespersonInfoPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("marketingCenterOperatorSalespersonInfoHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.marketing-center.operator-salesperson-info",
		name = "http-enabled",
		havingValue = "true")
public class MarketingCenterOperatorSalespersonInfoHttpService implements MarketingCenterOperatorSalespersonInfoPort {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private final DistributorListQueryService distributorListQueryService;

	public MarketingCenterOperatorSalespersonInfoHttpService(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			DistributorListQueryService distributorListQueryService) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.distributorListQueryService = distributorListQueryService;
	}

	@Override
	public Map<String, Object> fetchSalespersonInfoByWorkUserid(long companyId, String workUserid, boolean isAppDetail) {
		Map<String, Object> dataPayload = new LinkedHashMap<>();
		dataPayload.put("work_userid", workUserid == null ? "" : workUserid);
		Map<String, Object> infodata =
				marketingCenterOpenApiSignedFormClient.postReturningParsedData(companyId, "basics.salesperson.info", dataPayload);
		if (infodata == null || infodata.isEmpty()) {
			return new LinkedHashMap<>();
		}

		LinkedHashMap<String, LinkedHashMap<String, Object>> distributorCodes = new LinkedHashMap<>();
		Object storesRaw = infodata.get("stores");
		List<?> storesList = storesRaw instanceof List<?> l ? l : List.of();
		for (Object o : storesList) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : raw.entrySet()) {
				if (e.getKey() instanceof String k) {
					row.put(k, e.getValue());
				}
			}
			Object bn = row.get("store_bn");
			String key = String.valueOf(bn != null ? bn : "").trim();
			if (!key.isEmpty()) {
				distributorCodes.put(key, row);
			}
		}

		List<String> shopCodeFilter = new ArrayList<>(distributorCodes.keySet());
		List<Distributor> distributors =
				distributorListQueryService.listValidByCompanyAndShopCodesOrderedByCreatedDesc(companyId, shopCodeFilter);

		List<Map<String, Object>> store = new ArrayList<>();
		List<Map<String, Object>> syntheticSeeds = new ArrayList<>();
		boolean allowSyntheticSeed = distributorCodes.isEmpty();
		for (Distributor value : distributors) {
			String shopCode = value.getShopCode() != null ? value.getShopCode().trim() : "";
			String displayName = pickStoreDisplayName(value);
			Map<String, Object> storeRow = new LinkedHashMap<>();
			storeRow.put("distributor_id", value.getDistributorId());
			storeRow.put("name", displayName);
			storeRow.put("shop_code", shopCode);
			storeRow.put("logo", value.getLogo());
			storeRow.put("is_center", Boolean.FALSE);
			store.add(storeRow);

			LinkedHashMap<String, Object> matched =
					StringUtils.hasText(shopCode) ? distributorCodes.get(shopCode) : null;
			if (matched != null && value.getDistributorId() != null) {
				matched.put("distributor_id", value.getDistributorId());
			}
			if (allowSyntheticSeed) {
				Map<String, Object> seed = new LinkedHashMap<>();
				seed.put("distributor_id", value.getDistributorId());
				seed.put("store_name", displayName);
				syntheticSeeds.add(seed);
				allowSyntheticSeed = false;
			}
		}

		List<Map<String, Object>> distributorIdsOut;
		if (!distributorCodes.isEmpty()) {
			distributorIdsOut = new ArrayList<>(distributorCodes.values());
		} else {
			distributorIdsOut = new ArrayList<>(syntheticSeeds);
		}

		Object groupsRaw = infodata.get("groups");
		List<Object> shopAreaIds;
		if (groupsRaw instanceof List<?> gl) {
			shopAreaIds = new ArrayList<>(gl);
		} else {
			shopAreaIds = Collections.emptyList();
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("company_id", companyId);
		result.put("work_userid", infodata.get("work_userid") != null ? String.valueOf(infodata.get("work_userid")) : "");
		result.put("mobile", infodata.get("mobile"));
		result.put("special_identity", infodata.get("special_identity"));
		result.put("head_portrait", infodata.get("salesperson_avatar"));
		result.put("username", infodata.get("salesperson_name"));
		result.put("distributor_ids", distributorIdsOut);
		result.put("shop_area_ids", shopAreaIds);
		result.put("logintype", "salesperson_workwechat");
		if (isAppDetail) {
			result.put("distributors", store);
		}
		return result;
	}

	private static String pickStoreDisplayName(Distributor value) {
		if (value.getName() != null && StringUtils.hasText(value.getName().trim())) {
			return value.getName().trim();
		}
		return "";
	}
}
