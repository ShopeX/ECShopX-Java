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

package cn.shopex.ecshopx.distribution.service.distributorvalid;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.service.distributorvalid.append.DistributorIsValidMarketingActivityReadService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.append.DistributorIsValidSalesCountReadService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.append.DistributorIsValidTradeRateReadService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidAppendFieldsService {

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorIsValidTradeRateReadService distributorIsValidTradeRateReadService;
	private final DistributorIsValidMarketingActivityReadService distributorIsValidMarketingActivityReadService;
	private final DistributorIsValidSalesCountReadService distributorIsValidSalesCountReadService;

	public DistributorIsValidAppendFieldsService(
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorIsValidTradeRateReadService distributorIsValidTradeRateReadService,
			DistributorIsValidMarketingActivityReadService distributorIsValidMarketingActivityReadService,
			DistributorIsValidSalesCountReadService distributorIsValidSalesCountReadService) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorIsValidTradeRateReadService = distributorIsValidTradeRateReadService;
		this.distributorIsValidMarketingActivityReadService = distributorIsValidMarketingActivityReadService;
		this.distributorIsValidSalesCountReadService = distributorIsValidSalesCountReadService;
	}

	public Map<String, Object> applyTailFields(long companyId, Map<String, Object> result, DistributorIsValidQuery query) {
		if (result == null || result.isEmpty()) {
			return result;
		}
		if (!"true".equals(String.valueOf(result.get("is_valid")))) {
			return result;
		}
		result.put("status", Boolean.FALSE);
		result.put("old_valid", Boolean.FALSE);
		Object mobile = result.get("mobile");
		if (mobile != null) {
			String m = String.valueOf(mobile);
			result.put("phone", sensitiveFieldEncryptor.decrypt(m));
		} else {
			result.put("phone", "");
		}
		result.put("store_address", result.get("address") == null ? "" : String.valueOf(result.get("address")));
		result.put("store_name", result.get("name") == null ? "" : String.valueOf(result.get("name")));
		if (DistributorIsValidQuery.showFlagTruthy(query.showScoreRaw())) {
			distributorIsValidTradeRateReadService.appendScoreList(companyId, result);
		}
		if (DistributorIsValidQuery.showFlagTruthy(query.showMarketingRaw())) {
			distributorIsValidMarketingActivityReadService.appendMarketingActivityList(companyId, result);
		}
		if (DistributorIsValidQuery.showFlagTruthy(query.showSalesCountRaw())) {
			distributorIsValidSalesCountReadService.appendSalesCount(companyId, result);
		}
		return result;
	}
}
