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

package cn.shopex.ecshopx.aliyunsms.dispatch;

import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsCreateSmsTemplateClient;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AddSmsTemplateJobHandler implements DispatchHandler {

	private final AliyunsmsCreateSmsTemplateClient aliyunsmsCreateSmsTemplateClient;

	public AddSmsTemplateJobHandler(AliyunsmsCreateSmsTemplateClient aliyunsmsCreateSmsTemplateClient) {
		this.aliyunsmsCreateSmsTemplateClient = aliyunsmsCreateSmsTemplateClient;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		int templateType = extractInt(payload.get("template_type"));
		String templateName = stringify(payload.get("template_name"));
		String remark = stringify(payload.get("remark"));
		String templateContent = stringify(payload.get("template_content"));
		int sceneId = extractInt(payload.get("scene_id"));
		String relatedSignName = stringify(payload.get("related_sign_name"));
		String templateCode =
				aliyunsmsCreateSmsTemplateClient.createSmsTemplate(
						companyId, templateType, templateName, remark, templateContent, sceneId, relatedSignName);
		payload.put("template_code", templateCode);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
