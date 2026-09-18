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

package cn.shopex.ecshopx.hfpay.service.statistics;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HfpayCompanyStatisticsFacadeService {

	private final HfpayDistributorTransactionListService distributorTransactionListService;
	private final HfpayCompanyDayTotalStatisticsService companyDayTotalStatisticsService;

	public HfpayCompanyStatisticsFacadeService(
			HfpayDistributorTransactionListService distributorTransactionListService,
			HfpayCompanyDayTotalStatisticsService companyDayTotalStatisticsService) {
		this.distributorTransactionListService = distributorTransactionListService;
		this.companyDayTotalStatisticsService = companyDayTotalStatisticsService;
	}

	public Map<String, Object> getCompanyStatistics(
			long companyId,
			String startDateTime,
			String endDateTime,
			Integer distributorId,
			int page,
			int pageSize) {
		Map<String, Object> tradeRecordResult =
				distributorTransactionListService.transactionList(
						companyId, startDateTime, endDateTime, distributorId, page, pageSize);
		Map<String, Object> totleResult = companyDayTotalStatisticsService.countType2(companyId);

		Map<String, Object> listWrapper = new LinkedHashMap<>();
		listWrapper.put("total_count", tradeRecordResult.get("total_count"));
		listWrapper.put("data", tradeRecordResult.get("list"));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("totle", totleResult);
		body.put("list", listWrapper);
		return body;
	}
}
