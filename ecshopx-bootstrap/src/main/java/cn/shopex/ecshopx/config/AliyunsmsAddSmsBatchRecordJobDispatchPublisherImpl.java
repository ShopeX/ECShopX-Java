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

import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class AliyunsmsAddSmsBatchRecordJobDispatchPublisherImpl implements AliyunsmsAddSmsBatchRecordJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public AliyunsmsAddSmsBatchRecordJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(
			long companyId,
			int taskId,
			List<String> mobilesPlain,
			int sceneId,
			String templateCode,
			String templateType,
			String smsContent,
			int status,
			String bizId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("task_id", taskId);
		payload.put("mobile", new ArrayList<>(mobilesPlain == null ? List.of() : mobilesPlain));
		payload.put("scene_id", sceneId);
		payload.put("template_code", templateCode);
		payload.put("template_type", templateType);
		payload.put("sms_content", smsContent);
		payload.put("status", status);
		payload.put("biz_id", bizId);
		dispatchFacade.dispatchJob(
				AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));
	}
}
