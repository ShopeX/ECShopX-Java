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

package cn.shopex.ecshopx.wsugc.service.message;

import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentListDisplaySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcMessageDashboardService {

	private static final List<String> MESSAGE_TYPE_KEYS =
			List.of("system", "reply", "like", "favoritePost", "followerUser");

	private final MessageMapper messageMapper;

	public FrontUgcMessageDashboardService(MessageMapper messageMapper) {
		this.messageMapper = messageMapper;
	}

	public List<Map<String, Object>> buildMessageDashboard(long companyId, long toUserId) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (String typeKey : MESSAGE_TYPE_KEYS) {
			int unread =
					messageMapper
							.selectCount(
									new LambdaQueryWrapper<Message>()
											.eq(Message::getCompanyId, companyId)
											.eq(Message::getToUserId, toUserId)
											.eq(Message::getType, typeKey)
											.eq(Message::getHasRead, false))
							.intValue();

			int total =
					messageMapper
							.selectCount(
									new LambdaQueryWrapper<Message>()
											.eq(Message::getCompanyId, companyId)
											.eq(Message::getToUserId, toUserId)
											.eq(Message::getType, typeKey))
							.intValue();

			List<Message> latest =
					messageMapper.selectList(
							new LambdaQueryWrapper<Message>()
									.eq(Message::getCompanyId, companyId)
									.eq(Message::getToUserId, toUserId)
									.eq(Message::getType, typeKey)
									.orderByDesc(Message::getCreated)
									.last("LIMIT 1"));

			List<Map<String, Object>> list;
			if (latest == null || latest.isEmpty()) {
				list = Collections.emptyList();
			} else {
				list = List.of(toMessageRowMap(latest.get(0)));
			}

			Map<String, Object> recent = new LinkedHashMap<>();
			recent.put("total_count", total);
			recent.put("list", list);

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("type", typeKey);
			row.put("unread_nums", unread);
			row.put("recent_message", recent);
			result.add(row);
		}
		return result;
	}

	private static Map<String, Object> toMessageRowMap(Message m) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("message_id", String.valueOf(m.getMessageId()));
		row.put("from_nickname", m.getFromNickname());
		row.put(
				"from_user_id",
				m.getFromUserId() == null ? "0" : String.valueOf(m.getFromUserId()));
		row.put("to_nickname", m.getToNickname());
		row.put("to_user_id", m.getToUserId() == null ? "0" : String.valueOf(m.getToUserId()));
		row.put("title", m.getTitle());
		row.put("content", m.getContent());
		row.put("type", m.getType());
		int createdSec = m.getCreated() == null ? 0 : m.getCreated();
		row.put("created", createdSec);
		row.put("created_text", FrontUgcCommentListDisplaySupport.formatDateMd(createdSec));
		row.put(
				"created_moment",
				FrontUgcCommentListDisplaySupport.formatCommentStyleMoment(createdSec));
		return row;
	}
}
