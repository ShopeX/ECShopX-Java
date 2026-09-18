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

package cn.shopex.ecshopx.wechat.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WoaMpTemplateMessageClient;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SendTemplateMessageJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(SendTemplateMessageJobHandler.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WoaMpTemplateMessageClient woaMpTemplateMessageClient;

	public SendTemplateMessageJobHandler(
			WechatAuthQueryService wechatAuthQueryService, WoaMpTemplateMessageClient woaMpTemplateMessageClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.woaMpTemplateMessageClient = woaMpTemplateMessageClient;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			long companyId = extractCompanyId(payload);
			String templateId = extractString(payload, "template_id");
			String touser = extractString(payload, "touser");
			@SuppressWarnings("unchecked")
			Map<String, Object> msgData = (Map<String, Object>) payload.get("msg_data");

			String authorizerAppid =
					wechatAuthQueryService.getAuthorizerAppidForWoaQuery(String.valueOf(companyId));
			if (authorizerAppid == null || authorizerAppid.isBlank()) {
				return;
			}
			woaMpTemplateMessageClient.sendTemplateMessage(authorizerAppid.trim(), templateId, touser, msgData);
		} catch (RuntimeException e) {
			log.debug("SendTemplateMessageJob failed: {}", e.toString());
		}
	}

	private static long extractCompanyId(Map<String, Object> payload) {
		Object raw = payload.get("company_id");
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String extractString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		return raw == null ? "" : String.valueOf(raw);
	}
}
