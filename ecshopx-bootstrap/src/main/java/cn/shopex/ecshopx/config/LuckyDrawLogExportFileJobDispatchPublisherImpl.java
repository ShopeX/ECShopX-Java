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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.LuckyDrawLogExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.promotions.dispatch.LuckyDrawLogExportFileJobTypes;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class LuckyDrawLogExportFileJobDispatchPublisherImpl implements LuckyDrawLogExportFileJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final Environment environment;

	public LuckyDrawLogExportFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade, Environment environment) {
		this.dispatchFacade = dispatchFacade;
		this.environment = environment;
	}

	@Override
	public void enqueue(
			long companyId,
			long operatorId,
			long merchantId,
			long supplierId,
			String activityIdRaw,
			String datapassBlockHeader) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", LuckyDrawLogExportFileJobTypes.TYPE_EXPORT_LUCKDRAW_LOG);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("merchant_id", merchantId);
		payload.put("supplier_id", supplierId);
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("activity_id", activityIdRaw);
		filter.put("datapass_block", datapassBlockHeader);
		payload.put("filter", filter);
		DispatchOptions options;
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			options =
					new DispatchOptions(
							DispatchMode.SYNC,
							DispatchDriverType.SYNC,
							null,
							null,
							RetryPolicy.platformDefault());
		} else {
			options =
					new DispatchOptions(
							DispatchMode.ASYNC,
							DispatchDriverType.REDIS,
							"slow",
							null,
							RetryPolicy.platformDefault());
		}
		dispatchFacade.dispatchJob(EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG, payload, options);
	}
}
