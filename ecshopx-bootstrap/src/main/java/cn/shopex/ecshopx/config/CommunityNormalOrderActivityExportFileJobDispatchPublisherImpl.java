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

import cn.shopex.ecshopx.common.dispatch.CommunityNormalOrderActivityExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.community.dispatch.CommunityNormalOrderActivityExportFileJobPayloadSupport;
import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import cn.shopex.ecshopx.community.service.export.CommunityNormalOrderActivityXlsxExportService;
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
public class CommunityNormalOrderActivityExportFileJobDispatchPublisherImpl
		implements CommunityNormalOrderActivityExportFileJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final Environment environment;

	public CommunityNormalOrderActivityExportFileJobDispatchPublisherImpl(
			DispatchFacade dispatchFacade, Environment environment) {
		this.dispatchFacade = dispatchFacade;
		this.environment = environment;
	}

	@Override
	public void publish(
			long companyId,
			long operatorId,
			boolean datapassAllowed,
			CommunityActivityAdminListQuery query,
			Long optionalActivityId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", CommunityNormalOrderActivityXlsxExportService.EXPORT_TYPE);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("datapass_allowed", datapassAllowed);
		payload.put("optional_activity_id", optionalActivityId);
		payload.put(
				"filter",
				new LinkedHashMap<>(CommunityNormalOrderActivityExportFileJobPayloadSupport.toFilterMap(query)));
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
		dispatchFacade.dispatchJob(EspierDispatchJobNames.EXPORT_FILE_JOB, payload, options);
	}
}
