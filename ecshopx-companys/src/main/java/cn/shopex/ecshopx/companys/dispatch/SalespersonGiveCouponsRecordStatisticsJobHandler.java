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

package cn.shopex.ecshopx.companys.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.companys.service.CompanysStatisticsService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Bus-registered job handler that runs the give-coupons record statistics block when a matching message is consumed.
 */
@Component
public class SalespersonGiveCouponsRecordStatisticsJobHandler implements DispatchHandler {

	private final CompanysStatisticsService companysStatisticsService;

	public SalespersonGiveCouponsRecordStatisticsJobHandler(CompanysStatisticsService companysStatisticsService) {
		this.companysStatisticsService = companysStatisticsService;
	}

	/**
	 * @param payload keyed values carried with the dispatch message
	 */
	@Override
	public void handle(Map<String, Object> payload) {
		companysStatisticsService.runGiveCouponsBlockForYesterday();
	}
}
