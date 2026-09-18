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

import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.common.exception.ResourceException;
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
public class AliyunsmsAddSmsTemplateJobDispatchPublisherImpl implements AliyunsmsAddSmsTemplateJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public AliyunsmsAddSmsTemplateJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public String publish(
			long companyId,
			int templateType,
			String templateName,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("template_type", templateType);
		payload.put("template_name", templateName);
		payload.put("remark", remark);
		payload.put("template_content", templateContent);
		payload.put("scene_id", sceneId);
		payload.put("related_sign_name", relatedSignName);
		dispatchFacade.dispatchJob(
				AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB,
				payload,
				new DispatchOptions(DispatchMode.SYNC, null, null, null, RetryPolicy.platformDefault()));
		Object raw = payload.get("template_code");
		if (raw == null) {
			throw new ResourceException("添加阿里云短信模板失败");
		}
		String code = String.valueOf(raw).trim();
		if (code.isEmpty()) {
			throw new ResourceException("添加阿里云短信模板失败");
		}
		return code;
	}
}
