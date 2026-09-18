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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentListDisplaySupport;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentListService;
import cn.shopex.ecshopx.wsugc.service.follower.FrontUgcFollowerListService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostDetailEmbedService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcMessageDetailService {

	private static final List<String> FROM_USER_INFO_KEYS =
			List.of("username", "avatar", "headimgurl", "nickname", "user_id");

	private final MessageMapper messageMapper;
	private final MemberAccountService memberAccountService;
	private final FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService;
	private final FrontUgcCommentListService frontUgcCommentListService;
	private final FrontUgcFollowerListService frontUgcFollowerListService;

	public FrontUgcMessageDetailService(
			MessageMapper messageMapper,
			MemberAccountService memberAccountService,
			FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService,
			FrontUgcCommentListService frontUgcCommentListService,
			FrontUgcFollowerListService frontUgcFollowerListService) {
		this.messageMapper = messageMapper;
		this.memberAccountService = memberAccountService;
		this.frontUgcPostDetailEmbedService = frontUgcPostDetailEmbedService;
		this.frontUgcCommentListService = frontUgcCommentListService;
		this.frontUgcFollowerListService = frontUgcFollowerListService;
	}

	public Map<String, Object> buildMessageDetail(long companyId, String messageIdRaw, String langTag) {
		if (messageIdRaw == null || !StringUtils.hasText(messageIdRaw.trim())) {
			return null;
		}
		String trimmed = messageIdRaw.trim();
		final long messageId;
		try {
			messageId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}

		Message row =
				messageMapper.selectOne(
						new LambdaQueryWrapper<Message>()
								.eq(Message::getCompanyId, companyId)
								.eq(Message::getMessageId, messageId));
		if (row == null) {
			return null;
		}

		if (messageId == 0L) {
			return new LinkedHashMap<>(new TreeMap<>(messageToRawColumnMap(row)));
		}

		Map<String, Object> formatted = formatListRow(row, null, langTag);
		return new LinkedHashMap<>(new TreeMap<>(formatted));
	}

	public Map<String, Object> formatListRow(Message row, String filterTopType, String langTag) {
		LinkedHashMap<String, Object> v = new LinkedHashMap<>(messageToFormattedBaseMap(row));
		enrichFromUserInfo(v, row);
		enrichPostAndComment(v, row, langTag);
		applyFilterTopTypeEnrichments(v, row, filterTopType);
		return v;
	}

	private void enrichFromUserInfo(LinkedHashMap<String, Object> v, Message row) {
		long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
		Long fid = row.getFromUserId();
		if (fid != null && fid != 0L) {
			Map<String, Object> filter = Map.of("user_id", fid, "company_id", companyId);
			Map<String, Object> wx = memberAccountService.getWechatUserInfo(filter);
			Map<String, Object> member = memberAccountService.getMemberInfo(fid, companyId);
			if (member != null && !member.isEmpty()) {
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
				if (wx != null) {
					merged.putAll(wx);
				}
				merged.putAll(member);
				LinkedHashMap<String, Object> fromUserInfo = new LinkedHashMap<>();
				for (String k : FROM_USER_INFO_KEYS) {
					if (merged.containsKey(k)) {
						fromUserInfo.put(k, merged.get(k));
					}
				}
				v.put("from_userInfo", fromUserInfo);
			} else {
				LinkedHashMap<String, Object> fromUserInfo = new LinkedHashMap<>();
				if (wx != null) {
					fromUserInfo.putAll(wx);
				}
				v.put("from_userInfo", fromUserInfo);
			}
		}
	}

	private void enrichPostAndComment(LinkedHashMap<String, Object> v, Message row, String langTag) {
		long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
		Long pid = row.getPostId();
		if (pid != null && pid != 0L) {
			Map<String, Object> postInfo =
					frontUgcPostDetailEmbedService.buildPostInfoForMessage(companyId, pid, langTag);
			if (postInfo != null) {
				v.put("postInfo", postInfo);
			}
		}

		Long cid = row.getCommentId();
		if (cid != null && cid != 0L) {
			Map<String, Object> commentInfo =
					frontUgcCommentListService.buildCommentDetailForMessageEmbed(companyId, cid, langTag);
			if (commentInfo != null) {
				v.put("commentInfo", commentInfo);
			}
		}
	}

	private void applyFilterTopTypeEnrichments(
			LinkedHashMap<String, Object> v, Message row, String filterTopType) {
		if (filterTopType == null) {
			return;
		}
		if ("followerUser".equals(filterTopType)) {
			long fromUserId = row.getFromUserId() != null ? row.getFromUserId() : 0L;
			long toUserId = row.getToUserId() != null ? row.getToUserId() : 0L;
			v.put(
					"mutal_follow",
					frontUgcFollowerListService.computeMutualFollowForMessage(fromUserId, toUserId));
			return;
		}
		if (!"system".equals(filterTopType)) {
			return;
		}
		String subType = row.getSubType();
		if ("refusePost".equals(subType)) {
			v.put("title", "笔记暂不被推荐,快来修改");
			if (isEmptyStringContent(row.getContent())) {
				v.put("content", "您的笔记包含违规内容");
			}
		} else if ("refuseComment".equals(subType)) {
			v.put("title", "评论暂不被推荐");
			if (isEmptyStringContent(row.getContent())) {
				v.put("content", "您的评论包含违规内容");
			}
		}
	}

	private static boolean isEmptyStringContent(String content) {
		return content == null || content.isEmpty();
	}

	private static Map<String, Object> messageToRawColumnMap(Message m) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("message_id", m.getMessageId() == null ? "0" : String.valueOf(m.getMessageId()));
		row.put("title", m.getTitle());
		row.put("content", m.getContent());
		row.put("created", m.getCreated() == null ? 0 : m.getCreated());
		row.put("updated", m.getUpdated() == null ? 0 : m.getUpdated());
		row.put(
				"from_user_id",
				m.getFromUserId() == null ? "0" : String.valueOf(m.getFromUserId()));
		row.put("from_nickname", m.getFromNickname());
		row.put("to_user_id", m.getToUserId() == null ? "0" : String.valueOf(m.getToUserId()));
		row.put("to_nickname", m.getToNickname());
		row.put("company_id", m.getCompanyId());
		row.put("hasread", m.getHasRead() != null && m.getHasRead());
		row.put("source", m.getSource());
		row.put("type", m.getType());
		row.put("sub_type", m.getSubType());
		row.put("comment_id", m.getCommentId());
		row.put("post_id", m.getPostId());
		return row;
	}

	private static LinkedHashMap<String, Object> messageToFormattedBaseMap(Message m) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
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
		row.put("sub_type", m.getSubType());
		row.put("source", m.getSource());
		row.put("post_id", m.getPostId());
		row.put("comment_id", m.getCommentId());
		row.put("company_id", m.getCompanyId());
		int createdSec = m.getCreated() == null ? 0 : m.getCreated();
		row.put("created", createdSec);
		row.put("updated", m.getUpdated() == null ? 0 : m.getUpdated());
		row.put("hasread", m.getHasRead() != null && m.getHasRead());
		row.put("created_text", FrontUgcCommentListDisplaySupport.formatDateYmd(createdSec));
		row.put(
				"created_moment",
				FrontUgcCommentListDisplaySupport.formatCommentStyleMoment(createdSec));
		return row;
	}
}
