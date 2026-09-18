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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MessageReplySettingContentService {

	private final WechatOfficialAccountPermanentMaterialClient permanentMaterialClient;

	public MessageReplySettingContentService(WechatOfficialAccountPermanentMaterialClient permanentMaterialClient) {
		this.permanentMaterialClient = permanentMaterialClient;
	}

	public Object replySettingContent(String replyType, Object content, String authorizerAppId) {
		if (content == null) {
			return "";
		}
		if (content instanceof Boolean b && Boolean.FALSE.equals(b)) {
			return "";
		}
		if (content instanceof String s && "0".equals(s)) {
			return "";
		}
		if (content instanceof Number n && n.doubleValue() == 0.0) {
			return "";
		}
		if (content instanceof String s && !StringUtils.hasText(s)) {
			return "";
		}
		if (content instanceof Collection<?> c && c.isEmpty()) {
			return "";
		}
		if (content instanceof Map<?, ?> m && m.isEmpty()) {
			return "";
		}

		if (!StringUtils.hasText(authorizerAppId)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}

		String rt = replyType == null ? "" : replyType;
		switch (rt) {
			case "text":
			case "image":
				return content instanceof String str ? str : String.valueOf(content);
			case "voice":
			case "news": {
				String mediaId = content instanceof String ? (String) content : String.valueOf(content);
				Object newsData =
						permanentMaterialClient.fetchPermanentMaterialForReply(authorizerAppId.trim(), mediaId);
				LinkedHashMap<String, Object> box = new LinkedHashMap<>();
				box.put("content", newsData);
				box.put("media_id", mediaId);
				return box;
			}
			default:
				return Collections.emptyList();
		}
	}
}
