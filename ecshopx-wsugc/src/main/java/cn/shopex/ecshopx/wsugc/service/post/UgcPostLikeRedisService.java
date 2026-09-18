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

package cn.shopex.ecshopx.wsugc.service.post;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UgcPostLikeRedisService {

	private final StringRedisTemplate redis;

	public UgcPostLikeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void addPostLikesToRedis(long userId, long postId, long postUserId) {
		String field = likeField(userId, postId);
		redis.opsForHash().put("ugc_post_like", field, "1");

		long nextPost = checkAndCountfromRedis(getPostLikesFromRedis(postId), 1);
		redis.opsForHash().put("ugc_post_like_count", String.valueOf(postId), String.valueOf(nextPost));

		long nextUser = checkAndCountfromRedis(getUserPostLikesFromRedis(postUserId), 1);
		redis.opsForHash().put("ugc_user_post_like_count", String.valueOf(postUserId), String.valueOf(nextUser));
	}

	public void reducePostLikesToRedis(long userId, long postId, long postUserId) {
		String field = likeField(userId, postId);
		redis.opsForHash().put("ugc_post_like", field, "0");

		long nextPost = checkAndCountfromRedis(getPostLikesFromRedis(postId), -1);
		redis.opsForHash().put("ugc_post_like_count", String.valueOf(postId), String.valueOf(nextPost));

		long nextUser = checkAndCountfromRedis(getUserPostLikesFromRedis(postUserId), -1);
		redis.opsForHash().put("ugc_user_post_like_count", String.valueOf(postUserId), String.valueOf(nextUser));
	}

	public int getUserPostLikeStatus(long userId, long postId) {
		Object raw = redis.opsForHash().get("ugc_post_like", likeField(userId, postId));
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.toString().trim()) != 0 ? 1 : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String likeField(long userId, long postId) {
		return userId + "::" + postId;
	}

	private long getPostLikesFromRedis(long postId) {
		Object raw = redis.opsForHash().get("ugc_post_like_count", String.valueOf(postId));
		return parseLongPositive(stringOrEmpty(raw));
	}

	private long getUserPostLikesFromRedis(long postUserId) {
		Object raw = redis.opsForHash().get("ugc_user_post_like_count", String.valueOf(postUserId));
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
