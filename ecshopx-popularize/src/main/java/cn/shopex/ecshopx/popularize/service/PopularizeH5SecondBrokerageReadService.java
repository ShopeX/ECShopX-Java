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

package cn.shopex.ecshopx.popularize.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PopularizeH5SecondBrokerageReadService {

	private final PopularizeH5BrokerageListReadService popularizeH5BrokerageListReadService;

	private final PopularizeH5BrokerageCountReadService popularizeH5BrokerageCountReadService;

	public PopularizeH5SecondBrokerageReadService(
			PopularizeH5BrokerageListReadService popularizeH5BrokerageListReadService,
			PopularizeH5BrokerageCountReadService popularizeH5BrokerageCountReadService) {
		this.popularizeH5BrokerageListReadService = popularizeH5BrokerageListReadService;
		this.popularizeH5BrokerageCountReadService = popularizeH5BrokerageCountReadService;
	}

	public Object getSecondBrokerageList(
			long companyId,
			long closeOwnerUserId,
			long noCloseOwnerUserId,
			String brokerageSource,
			int page,
			int pageSize,
			String closeType) {
		Map<String, Object> closeData =
				popularizeH5BrokerageListReadService.loadDefaultBrokerageListSegment(
						companyId, closeOwnerUserId, brokerageSource, true, page, pageSize);
		Map<String, Object> noCloseData =
				popularizeH5BrokerageListReadService.loadDefaultBrokerageListSegment(
						companyId, noCloseOwnerUserId, brokerageSource, false, page, pageSize);

		if (Objects.equals("close", closeType)) {
			return closeData;
		}
		if (Objects.equals("noClose", closeType)) {
			return noCloseData;
		}

		Map<String, Object> info =
				popularizeH5BrokerageCountReadService.getPromoterBrokerageInfo(companyId, closeOwnerUserId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("close", closeData);
		data.put("noClose", noCloseData);
		data.put("info", info);
		return data;
	}
}
