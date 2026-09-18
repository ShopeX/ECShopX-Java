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

package cn.shopex.ecshopx.datacube.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.datacube.service.MerchantDataService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MerchantStatisticJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(MerchantStatisticJobHandler.class);

	private final MerchantDataService merchantDataService;

	public MerchantStatisticJobHandler(MerchantDataService merchantDataService) {
		this.merchantDataService = merchantDataService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			long companyId = longValue(payload.get("company_id"));
			long merchantId = longValue(payload.get("merchant_id"));
			LocalDate countDate = parseCountDate(payload.get("count_date"));
			merchantDataService.runStatistics(companyId, merchantId, countDate);
		} catch (Exception e) {
			log.debug("队列统计: 执行商户日统计时失败", e);
		}
	}

	private static LocalDate parseCountDate(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof LocalDate d) {
			return d;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return LocalDate.parse(s);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static long longValue(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}
}
