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

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KeywordAutoreplyQueryService {

	private final KeywordAutoreplyRedisService keywordAutoreplyRedisService;
	private final MessageReplySettingContentService messageReplySettingContentService;

	public KeywordAutoreplyQueryService(
			KeywordAutoreplyRedisService keywordAutoreplyRedisService,
			MessageReplySettingContentService messageReplySettingContentService) {
		this.keywordAutoreplyRedisService = keywordAutoreplyRedisService;
		this.messageReplySettingContentService = messageReplySettingContentService;
	}

	public List<Map<String, Object>> getKeywordReplyList(String authorizerAppId) {
		List<Map<String, Object>> list = keywordAutoreplyRedisService.listKeywordAutoreplyRules(authorizerAppId);
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> row = list.get(i);
			String replyTypeStr =
					row.get("reply_type") == null ? "" : String.valueOf(row.get("reply_type")).trim();
			Object rawContent = row.get("reply_content");
			Object transformed =
					messageReplySettingContentService.replySettingContent(replyTypeStr, rawContent, authorizerAppId);
			row.put("reply_content", transformed);
		}
		return list;
	}
}
