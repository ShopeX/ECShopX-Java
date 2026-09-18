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

package cn.shopex.ecshopx.wechat.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SubscribeReplyQueryService {

	private final SubscribeReplyRedisService subscribeReplyRedisService;
	private final MessageReplySettingContentService messageReplySettingContentService;

	public SubscribeReplyQueryService(
			SubscribeReplyRedisService subscribeReplyRedisService,
			MessageReplySettingContentService messageReplySettingContentService) {
		this.subscribeReplyRedisService = subscribeReplyRedisService;
		this.messageReplySettingContentService = messageReplySettingContentService;
	}

	public Map<String, Object> getSubscribeReply(String authorizerAppId) {
		Map<String, Object> data = subscribeReplyRedisService.getSubscribeReplyContent(authorizerAppId);
		if (data == null) {
			return defaultSubscribeReplyBody();
		}
		Object rc = data.get("reply_content");
		if (rc == null) {
			rc = Collections.emptyList();
		}
		if (!hasMeaningfulReplyContent(rc)) {
			return defaultSubscribeReplyBody();
		}
		String replyTypeStr =
				data.get("reply_type") == null ? "" : String.valueOf(data.get("reply_type")).trim();
		Object transformed = messageReplySettingContentService.replySettingContent(
				replyTypeStr, data.get("reply_content"), authorizerAppId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("reply_type", data.get("reply_type") != null ? data.get("reply_type") : replyTypeStr);
		out.put("reply_content", transformed);
		return out;
	}

	private static LinkedHashMap<String, Object> defaultSubscribeReplyBody() {
		LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
		defaults.put("reply_content", "");
		defaults.put("reply_type", "news");
		return defaults;
	}

	private static boolean hasMeaningfulReplyContent(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Boolean b) {
			return b == Boolean.TRUE;
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}
}
