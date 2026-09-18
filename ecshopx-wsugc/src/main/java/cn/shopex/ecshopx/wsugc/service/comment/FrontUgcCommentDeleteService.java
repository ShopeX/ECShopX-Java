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
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcCommentDeleteService {

	private final CommentMapper commentMapper;
	private final UgcCommentRedisCounterService ugcCommentRedisCounterService;

	public FrontUgcCommentDeleteService(
			CommentMapper commentMapper,
			UgcCommentRedisCounterService ugcCommentRedisCounterService) {
		this.commentMapper = commentMapper;
		this.ugcCommentRedisCounterService = ugcCommentRedisCounterService;
	}

	public Map<String, Object> deleteByMember(long userId, long commentId) {
		LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
		q.eq(Comment::getUserId, userId)
				.eq(Comment::getCommentId, commentId)
				.eq(Comment::getDisabled, false);
		Comment found = commentMapper.selectOne(q);
		if (found == null) {
			throw new ResourceException("评论不存在或已删除");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Comment> u = new LambdaUpdateWrapper<>();
		u.eq(Comment::getCommentId, commentId)
				.eq(Comment::getUserId, userId)
				.eq(Comment::getDisabled, false)
				.set(Comment::getDisabled, true)
				.set(Comment::getUpdated, now);
		int rows = commentMapper.update(null, u);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Comment reloaded = commentMapper.selectById(commentId);
		if (reloaded == null) {
			throw new ResourceException("评论不存在或已删除");
		}

		long postId = reloaded.getPostId() != null ? reloaded.getPostId() : 0L;
		long authorUserId = reloaded.getUserId() != null ? reloaded.getUserId() : userId;
		ugcCommentRedisCounterService.reduceCommentsToRedis(commentId, postId, authorUserId);

		return commentToSnakeMap(reloaded);
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
}
