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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UgcCommentLikeRedisService {

	private final StringRedisTemplate redis;

	public UgcCommentLikeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void addCommentLikesToRedis(long userId, long commentId, long commentAuthorUserId) {
		String field = likeField(userId, commentId);
		redis.opsForHash().put("ugc_comment_like", field, "1");

		long nextComment = checkAndCountfromRedis(getCommentLikesFromRedis(commentId), 1);
		redis.opsForHash().put("ugc_comment_like_count", String.valueOf(commentId), String.valueOf(nextComment));

		long nextUser = checkAndCountfromRedis(getUserCommentLikesFromRedis(commentAuthorUserId), 1);
		redis.opsForHash().put("ugc_user_comment_like_count", String.valueOf(commentAuthorUserId), String.valueOf(nextUser));
	}

	public void reduceCommentLikesToRedis(long userId, long commentId, long commentAuthorUserId) {
		String field = likeField(userId, commentId);
		redis.opsForHash().put("ugc_comment_like", field, "0");

		long nextComment = checkAndCountfromRedis(getCommentLikesFromRedis(commentId), -1);
		redis.opsForHash().put("ugc_comment_like_count", String.valueOf(commentId), String.valueOf(nextComment));

		long nextUser = checkAndCountfromRedis(getUserCommentLikesFromRedis(commentAuthorUserId), -1);
		redis.opsForHash().put("ugc_user_comment_like_count", String.valueOf(commentAuthorUserId), String.valueOf(nextUser));
	}

	private static String likeField(long userId, long commentId) {
		return userId + "::" + commentId;
	}

	private long getCommentLikesFromRedis(long commentId) {
		Object raw = redis.opsForHash().get("ugc_comment_like_count", String.valueOf(commentId));
		return parseLongPositive(stringOrEmpty(raw));
	}

	private long getUserCommentLikesFromRedis(long userId) {
		Object raw = redis.opsForHash().get("ugc_user_comment_like_count", String.valueOf(userId));
		return parseLongPositive(stringOrEmpty(raw));
	}

	private static String stringOrEmpty(Object raw) {
		return raw != null ? raw.toString() : null;
	}

	private static long checkAndCountfromRedis(long count, long delta) {
		if (count > 0) {
			return count + delta;
		}
		return delta;
	}

	private static long parseLongPositive(String raw) {
		if (raw == null || raw.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
