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
import cn.shopex.ecshopx.wsugc.domain.CommentLike;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.CommentLikeMapper;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcCommentLikeService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcCommentLikeService.class);

	private final CommentMapper commentMapper;
	private final CommentLikeMapper commentLikeMapper;
	private final MessageMapper messageMapper;
	private final UgcCommentLikeRedisService ugcCommentLikeRedisService;
	private final MemberAccountService memberAccountService;

	public FrontUgcCommentLikeService(
			CommentMapper commentMapper,
			CommentLikeMapper commentLikeMapper,
			MessageMapper messageMapper,
			UgcCommentLikeRedisService ugcCommentLikeRedisService,
			MemberAccountService memberAccountService) {
		this.commentMapper = commentMapper;
		this.commentLikeMapper = commentLikeMapper;
		this.messageMapper = messageMapper;
		this.ugcCommentLikeRedisService = ugcCommentLikeRedisService;
		this.memberAccountService = memberAccountService;
	}

	public boolean isCommentLikedByUser(long userId, long postId, long commentId) {
		if (userId <= 0L) {
			return false;
		}
		LambdaQueryWrapper<CommentLike> q = new LambdaQueryWrapper<CommentLike>()
				.eq(CommentLike::getUserId, userId)
				.eq(CommentLike::getPostId, postId)
				.eq(CommentLike::getCommentId, commentId)
				.eq(CommentLike::getDisabled, false);
		Long cnt = commentLikeMapper.selectCount(q);
		return cnt != null && cnt >= 1L;
	}

	public Map<String, Object> like(long userId, long postId, long commentId) {
		Comment comment = commentMapper.selectById(commentId);
		if (comment == null) {
			throw new ResourceException("关联数据不存在");
		}

		long commentAuthorUserId = comment.getUserId() != null ? comment.getUserId() : 0L;
		long commentCompanyId = comment.getCompanyId() != null ? comment.getCompanyId() : 0L;
		String commentContent = comment.getContent() != null ? comment.getContent() : "";

		LambdaQueryWrapper<CommentLike> pair = new LambdaQueryWrapper<CommentLike>()
				.eq(CommentLike::getUserId, userId)
				.eq(CommentLike::getPostId, postId)
				.eq(CommentLike::getCommentId, commentId);
		CommentLike existing = commentLikeMapper.selectOne(pair);

		String action = "like";
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			CommentLike entity = new CommentLike();
			entity.setUserId(userId);
			entity.setPostId(postId);
			entity.setCommentId(commentId);
			entity.setDisabled(false);
			entity.setCreated(now);
			entity.setUpdated(now);
			int rows = commentLikeMapper.insert(entity);
			if (rows <= 0) {
				throw new ResourceException("点赞失败！");
			}
			try {
				Message m = new Message();
				m.setType("like");
				m.setSubType("likeComment");
				m.setSource(1);
				m.setPostId(postId);
				m.setCommentId(commentId);
				m.setCompanyId(commentCompanyId);
				m.setFromUserId(userId);
				m.setToUserId(commentAuthorUserId);
				m.setTitle("赞了您的评论");
				m.setContent(commentContent);
				m.setFromNickname(wechatNickname(userId, commentCompanyId));
				m.setToNickname(wechatNickname(commentAuthorUserId, commentCompanyId));
				m.setCreated(now);
				m.setUpdated(now);
				m.setHasRead(false);
				messageMapper.insert(m);
			} catch (Exception e) {
				log.debug("发送 评论点赞 消息失败: {}", e.getMessage(), e);
			}
		} else if (!Boolean.TRUE.equals(existing.getDisabled())) {
			action = "unlike";
			CommentLike fresh = commentLikeMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(true);
			fresh.setUpdated(now);
			int updateRows = commentLikeMapper.updateById(fresh);
			if (updateRows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			CommentLike fresh = commentLikeMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(false);
			fresh.setUpdated(now);
			int updateRows = commentLikeMapper.updateById(fresh);
			if (updateRows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		if ("like".equals(action)) {
			ugcCommentLikeRedisService.addCommentLikesToRedis(userId, commentId, commentAuthorUserId);
		} else {
			ugcCommentLikeRedisService.reduceCommentLikesToRedis(userId, commentId, commentAuthorUserId);
		}

		LambdaQueryWrapper<CommentLike> active = new LambdaQueryWrapper<CommentLike>()
				.eq(CommentLike::getPostId, postId)
				.eq(CommentLike::getCommentId, commentId)
				.eq(CommentLike::getDisabled, false);
		long count = commentLikeMapper.selectCount(active);

		int likesVal = (int) Math.min(count, Integer.MAX_VALUE);
		LambdaUpdateWrapper<Comment> uw = new LambdaUpdateWrapper<>();
		uw.eq(Comment::getCommentId, commentId).set(Comment::getLikes, String.valueOf(likesVal));
		int commentUpdated = commentMapper.update(null, uw);
		if (commentUpdated == 0) {
			log.debug("回写 wsugc_comment.likes 未影响行: commentId={}", commentId);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("action", action);
		out.put("likes", count);
		return out;
	}

	private String wechatNickname(long uid, long cid) {
		Map<String, Object> wx = memberAccountService.getWechatUserInfo(Map.of(
				"user_id", uid,
				"company_id", cid));
		Object n = wx.get("nickname");
		return n != null ? n.toString() : "";
	}
}
