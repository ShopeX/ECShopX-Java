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

package cn.shopex.ecshopx.wsugc.service.comment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.content.UgcWxContentTextCheckService;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostOpenIdResolveService;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcCommentCreateService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcCommentCreateService.class);

	private final PostMapper postMapper;
	private final CommentMapper commentMapper;
	private final MessageMapper messageMapper;
	private final UgcPostOpenIdResolveService ugcPostOpenIdResolveService;
	private final UgcWxContentTextCheckService ugcWxContentTextCheckService;
	private final UgcPostPointService ugcPostPointService;
	private final UgcCommentRedisCounterService ugcCommentRedisCounterService;
	private final MemberAccountService memberAccountService;

	public FrontUgcCommentCreateService(
			PostMapper postMapper,
			CommentMapper commentMapper,
			MessageMapper messageMapper,
			UgcPostOpenIdResolveService ugcPostOpenIdResolveService,
			UgcWxContentTextCheckService ugcWxContentTextCheckService,
			UgcPostPointService ugcPostPointService,
			UgcCommentRedisCounterService ugcCommentRedisCounterService,
			MemberAccountService memberAccountService) {
		this.postMapper = postMapper;
		this.commentMapper = commentMapper;
		this.messageMapper = messageMapper;
		this.ugcPostOpenIdResolveService = ugcPostOpenIdResolveService;
		this.ugcWxContentTextCheckService = ugcWxContentTextCheckService;
		this.ugcPostPointService = ugcPostPointService;
		this.ugcCommentRedisCounterService = ugcCommentRedisCounterService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> create(Map<String, Object> merged, long userId, long companyId, String clientIp) {
		long postId = parsePositiveLong(merged.get("post_id"), 0L);
		if (postId <= 0) {
			throw new ResourceException("笔记id不能为空！");
		}

		Object contentRaw = merged.get("content");
		if (!(contentRaw instanceof String)) {
			throw new ResourceException("评论内容不能为空！");
		}
		String content = (String) contentRaw;
		byte[] utf8 = content.getBytes(StandardCharsets.UTF_8);
		if (utf8.length < 1) {
			throw new ResourceException("评论内容不能为空！");
		}
		if (utf8.length > 15000) {
			throw new ResourceException("评论内容不超过500个汉字！");
		}

		long parentCommentId = parsePositiveLong(merged.get("parent_comment_id"), 0L);
		long replyCommentId = parsePositiveLong(merged.get("reply_comment_id"), 0L);
		if (parentCommentId <= 0) {
			parentCommentId = 0L;
		}
		if (replyCommentId <= 0) {
			replyCommentId = 0L;
		}

		Post post = postMapper.selectById(postId);
		if (post == null) {
			throw new ResourceException("关联数据不存在");
		}

		String openId = ugcPostOpenIdResolveService.resolveOpenId(userId, companyId);
		int contentStatus = 0;
		if (StringUtils.hasText(content)) {
			contentStatus = ugcWxContentTextCheckService.checkTextStatus(companyId, content, openId);
		}

		int rowStatus;
		String msg;
		if (contentStatus == 1) {
			rowStatus = 1;
			msg = "发表评论成功";
		} else if (contentStatus == 4) {
			rowStatus = 4;
			msg = "评论违规";
		} else {
			rowStatus = 0;
			msg = "评论等待管理员审核";
		}

		long replyUserId;
		boolean replyComment;
		if (replyCommentId > 0L) {
			Comment target = commentMapper.selectById(replyCommentId);
			if (target == null) {
				throw new ResourceException("回复的评论不存在");
			}
			if (!Objects.equals(target.getPostId(), postId)) {
				throw new ResourceException("回复的评论不存在");
			}
			replyUserId = nullToZero(target.getUserId());
			replyComment = true;
		} else if (parentCommentId > 0L) {
			Comment parent = commentMapper.selectById(parentCommentId);
			if (parent == null) {
				throw new ResourceException("父评论不存在");
			}
			if (!Objects.equals(parent.getPostId(), postId)) {
				throw new ResourceException("父评论不存在");
			}
			replyUserId = nullToZero(parent.getUserId());
			replyComment = true;
		} else {
			replyUserId = nullToZero(post.getUserId());
			replyComment = false;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		String ip = clientIp != null ? clientIp : "";

		Comment entity = new Comment();
		entity.setPostId(postId);
		entity.setUserId(userId);
		entity.setParentCommentId(parentCommentId);
		entity.setReplyCommentId(replyCommentId);
		entity.setReplyUserId(replyUserId);
		entity.setContent(content);
		entity.setIp(ip);
		entity.setPOrder(0);
		entity.setStatus(rowStatus);
		entity.setEnable(1);
		entity.setDisabled(false);
		entity.setCompanyId(companyId);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setAiVerifyTime(0L);
		entity.setManualVerifyTime(0L);

		commentMapper.insert(entity);
		long newId = entity.getCommentId() != null ? entity.getCommentId() : 0L;

		if (rowStatus == 1) {
			try {
				if (!replyComment) {
					ugcPostPointService.addUgcPoint(postId, userId, companyId, 22, "");
				} else {
					log.debug("addUgcPoint 送积分失败: 回复评论而不是笔记不给积分");
				}
			} catch (Exception e) {
				log.debug("addUgcPoint 送积分失败: {}", e.getMessage());
			}
		}

		if (rowStatus == 1) {
			try {
				sendReplyMessage(
						userId,
						companyId,
						postId,
						newId,
						replyComment,
						replyUserId,
						post,
						content);
			} catch (Exception e) {
				log.debug("发送评论消息 失败: {}", e.getMessage());
			}
		} else {
			log.debug("发送评论消息失败: 没有审核通过");
		}

		ugcCommentRedisCounterService.addCommentsToRedis(newId, postId, userId);

		Comment persisted = newId > 0L ? commentMapper.selectById(newId) : entity;
		Map<String, Object> out = commentToSnakeMap(persisted != null ? persisted : entity);
		out.put("message", msg);
		return out;
	}

	private void sendReplyMessage(
			long fromUserId,
			long companyId,
			long postId,
			long commentId,
			boolean replyComment,
			long toUserId,
			Post post,
			String commentContent) {
		long postCompanyId = post.getCompanyId() != null ? post.getCompanyId() : companyId;
		String fromNick = wechatNickname(fromUserId, companyId);
		String toNick = wechatNickname(toUserId, postCompanyId);

		Message m = new Message();
		m.setType("reply");
		m.setSubType(replyComment ? "replyComment" : "replyPost");
		m.setSource(1);
		m.setPostId(postId);
		m.setCommentId(commentId);
		m.setCompanyId(companyId);
		m.setFromUserId(fromUserId);
		m.setFromNickname(fromNick != null ? fromNick : "");
		m.setToUserId(toUserId);
		m.setToNickname(toNick != null ? toNick : "");
		m.setTitle(replyComment ? "回复了您" : "评论了您的笔记");
		m.setContent(commentContent);
		int now = (int) (System.currentTimeMillis() / 1000L);
		m.setCreated(now);
		m.setUpdated(now);
		m.setHasRead(false);
		messageMapper.insert(m);
	}

	private String wechatNickname(long uid, long cid) {
		Map<String, Object> wx = memberAccountService.getWechatUserInfo(Map.of(
				"user_id", uid,
				"company_id", cid));
		Object n = wx.get("nickname");
		return n != null ? n.toString() : "";
	}

	private static Map<String, Object> commentToSnakeMap(Comment c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", longToDecString(c.getCommentId()));
		m.put("post_id", c.getPostId());
		m.put("user_id", longToDecString(c.getUserId()));
		m.put("parent_comment_id", nullToZeroLong(c.getParentCommentId()));
		m.put("reply_comment_id", nullToZeroLong(c.getReplyCommentId()));
		m.put("reply_user_id", longToDecString(c.getReplyUserId()));
		m.put("content", c.getContent());
		m.put("likes", likesToDecString(c.getLikes()));
		m.put("ip", c.getIp());
		m.put("province", c.getProvince());
		m.put("city", c.getCity());
		m.put("district", c.getDistrict());
		m.put("p_order", intToDecString(c.getPOrder()));
		m.put("status", c.getStatus());
		m.put("enable", intToDecString(c.getEnable()));
		m.put("disabled", booleanTo01(c.getDisabled()));
		m.put("company_id", longToDecString(c.getCompanyId()));
		m.put("created", c.getCreated());
		m.put("updated", c.getUpdated());
		m.put("ai_verify_time", verifyTimeJson(c.getAiVerifyTime()));
		m.put("manual_verify_time", verifyTimeJson(c.getManualVerifyTime()));
		m.put("ai_refuse_reason", c.getAiRefuseReason());
		m.put("manual_refuse_reason", c.getManualRefuseReason());
		return m;
	}

	private static String longToDecString(Long v) {
		return v != null ? String.valueOf(v) : "0";
	}

	private static long nullToZeroLong(Long v) {
		return v != null ? v : 0L;
	}

	private static String likesToDecString(String likes) {
		if (likes == null || likes.isEmpty()) {
			return "0";
		}
		return likes.trim().isEmpty() ? "0" : likes.trim();
	}

	private static String intToDecString(Integer v) {
		return v != null ? String.valueOf(v) : "0";
	}

	private static int booleanTo01(Boolean disabled) {
		return Boolean.TRUE.equals(disabled) ? 1 : 0;
	}

	private static Object verifyTimeJson(Long v) {
		if (v == null || v.longValue() == 0L) {
			return null;
		}
		return v;
	}

	private static long nullToZero(Long v) {
		return v != null ? v : 0L;
	}

	private static long parsePositiveLong(Object raw, long defaultZero) {
		if (raw == null) {
			return defaultZero;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : defaultZero;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return defaultZero;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : defaultZero;
		} catch (NumberFormatException e) {
			return defaultZero;
		}
	}
}
