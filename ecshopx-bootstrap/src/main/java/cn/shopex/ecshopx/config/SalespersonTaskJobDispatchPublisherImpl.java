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

import cn.shopex.ecshopx.common.dispatch.SalespersonDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.SalespersonTaskJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class SalespersonTaskJobDispatchPublisherImpl implements SalespersonTaskJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final Environment environment;

	public SalespersonTaskJobDispatchPublisherImpl(DispatchFacade dispatchFacade, Environment environment) {
		this.dispatchFacade = dispatchFacade;
		this.environment = environment;
	}

	@Override
	public void publish(
			long companyId,
			long subtaskId,
			String storeBn,
			String employeeNumber,
			long itemId,
			String unionId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("subtask_id", subtaskId);
		payload.put("store_bn", storeBn);
		payload.put("employee_number", employeeNumber);
		payload.put("user_id", unionId);
		if (itemId > 0L) {
			payload.put("item_id", itemId);
		}
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
		dispatchFacade.dispatchJob(SalespersonDispatchJobNames.SALESPERSON_TASK_JOB, payload, options);
	}
}
