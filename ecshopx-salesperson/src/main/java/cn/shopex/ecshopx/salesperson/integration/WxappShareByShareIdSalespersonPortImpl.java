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

package cn.shopex.ecshopx.salesperson.integration;

import cn.shopex.ecshopx.common.wechat.port.WxappShareByShareIdSalespersonPort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappShareByShareIdSalespersonPortImpl implements WxappShareByShareIdSalespersonPort {

	private static final Logger log = LoggerFactory.getLogger(WxappShareByShareIdSalespersonPortImpl.class);

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public WxappShareByShareIdSalespersonPortImpl(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	@Override
	public Map<String, Object> getSalespersonAndDistributorByWorkUserid(long companyId, String workUserid) {
		if (!StringUtils.hasText(workUserid == null ? null : workUserid.trim())) {
			return new LinkedHashMap<>();
		}
		String trimmed = workUserid.trim();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("work_userid", trimmed);
		Map<String, Object> infodata =
				marketingCenterOpenApiSignedFormClient.postReturningParsedData(companyId, "basics.salesperson.info", payload);
		if (infodata == null || infodata.isEmpty()) {
			return new LinkedHashMap<>();
		}
		String storeBn = extractStoreBn(infodata);
		Map<String, Object> distributorInfo = distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, storeBn);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(infodata);
		if (distributorInfo != null && !distributorInfo.isEmpty()) {
			out.put("distributorInfo", distributorInfo);
			log.info("wxapp share enrich: companyId={} workUserid={} distributor row merged", companyId, trimmed);
		}
		return out;
	}

	private static String extractStoreBn(Map<String, Object> infodata) {
		Object outer = infodata.get("infodata");
		if (outer instanceof Map<?, ?> innerMap) {
			Object bn = innerMap.get("store_bn");
			return bn == null ? "" : String.valueOf(bn).trim();
		}
		return "";
	}
}
