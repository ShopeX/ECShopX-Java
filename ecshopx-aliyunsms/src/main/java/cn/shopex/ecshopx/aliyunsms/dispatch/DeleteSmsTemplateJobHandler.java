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

import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsDeleteSmsTemplateClient;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeleteSmsTemplateJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(DeleteSmsTemplateJobHandler.class);

	private final AliyunsmsDeleteSmsTemplateClient aliyunsmsDeleteSmsTemplateClient;

	public DeleteSmsTemplateJobHandler(AliyunsmsDeleteSmsTemplateClient aliyunsmsDeleteSmsTemplateClient) {
		this.aliyunsmsDeleteSmsTemplateClient = aliyunsmsDeleteSmsTemplateClient;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		String templateCode = stringify(payload.get("template_code"));
		if (templateCode == null || templateCode.isBlank()) {
			log.warn("deleteSmsTemplate job skipped: empty templateCode companyId={}", companyId);
			return;
		}
		aliyunsmsDeleteSmsTemplateClient.deleteSmsTemplate(companyId, templateCode);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
