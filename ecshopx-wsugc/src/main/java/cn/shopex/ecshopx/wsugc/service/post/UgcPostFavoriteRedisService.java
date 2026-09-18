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
public class UgcPostFavoriteRedisService {

	private final StringRedisTemplate redis;

	public UgcPostFavoriteRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void addPostFavoritesToRedis(long userId, long postId, long postUserId) {
		String field = favoriteField(userId, postId);
		redis.opsForHash().put("ugc_post_favorite", field, "1");

		Object postCountRaw = redis.opsForHash().get("ugc_post_favorite_count", String.valueOf(postId));
		long nextPost = checkAndCountfromRedis(parseLongPositive(stringOrEmpty(postCountRaw)), 1);
		redis.opsForHash().put("ugc_post_favorite_count", String.valueOf(postId), String.valueOf(nextPost));

		Object userCountRaw = redis.opsForHash().get("ugc_user_post_favorite_count", String.valueOf(postUserId));
		long nextUser = checkAndCountfromRedis(parseLongPositive(stringOrEmpty(userCountRaw)), 1);
		redis.opsForHash().put("ugc_user_post_favorite_count", String.valueOf(postUserId), String.valueOf(nextUser));
	}

	public void reducePostFavoritesToRedis(long userId, long postId, long postUserId) {
		String field = favoriteField(userId, postId);
		redis.opsForHash().put("ugc_post_favorite", field, "0");

		Object postCountRaw = redis.opsForHash().get("ugc_post_favorite_count", String.valueOf(postId));
		long nextPost = checkAndCountfromRedis(parseLongPositive(stringOrEmpty(postCountRaw)), -1);
		redis.opsForHash().put("ugc_post_favorite_count", String.valueOf(postId), String.valueOf(nextPost));

		Object userCountRaw = redis.opsForHash().get("ugc_user_post_favorite_count", String.valueOf(postUserId));
		long nextUser = checkAndCountfromRedis(parseLongPositive(stringOrEmpty(userCountRaw)), -1);
		redis.opsForHash().put("ugc_user_post_favorite_count", String.valueOf(postUserId), String.valueOf(nextUser));
	}

	public int getUserPostFavoriteStatus(long userId, long postId) {
		Object raw = redis.opsForHash().get("ugc_post_favorite", favoriteField(userId, postId));
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.toString().trim()) != 0 ? 1 : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String favoriteField(long userId, long postId) {
		return userId + "::" + postId;
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
