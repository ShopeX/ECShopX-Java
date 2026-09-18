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

import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsModifySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class AliyunsmsModifySmsTemplateJobDispatchPublisherImpl implements AliyunsmsModifySmsTemplateJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public AliyunsmsModifySmsTemplateJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(
			long companyId,
			long templateId,
			int templateType,
			String templateName,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName,
			String templateCode) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("id", templateId);
		payload.put("status", 0);
		payload.put("template_name", templateName);
		payload.put("template_type", templateType);
		payload.put("remark", remark);
		payload.put("template_content", templateContent);
		payload.put("scene_id", sceneId);
		payload.put("related_sign_name", relatedSignName);
		payload.put("template_code", templateCode);
		dispatchFacade.dispatchJob(
				AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB,
				payload,
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					null,
					null,
					RetryPolicy.platformDefault()));
	}
}
