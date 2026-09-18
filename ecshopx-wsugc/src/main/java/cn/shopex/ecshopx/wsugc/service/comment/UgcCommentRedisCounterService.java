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
public class UgcCommentRedisCounterService {

	private final StringRedisTemplate redis;

	public UgcCommentRedisCounterService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void addCommentsToRedis(long commentId, long postId, long userId) {
		String field = commentId + "::" + postId;
		redis.opsForHash().put("ugc_comment", field, "1");

		String postCountRaw = (String) redis.opsForHash().get("ugc_comment_count", String.valueOf(postId));
		long postCount = parseLongPositive(postCountRaw);
		long nextPostCount = applyCountDelta(postCount, 1);
		redis.opsForHash().put("ugc_comment_count", String.valueOf(postId), String.valueOf(nextPostCount));

		String userCountRaw = (String) redis.opsForHash().get("ugc_user_comment_count", String.valueOf(userId));
		long userCount = parseLongPositive(userCountRaw);
		long nextUserCount = applyCountDelta(userCount, 1);
		redis.opsForHash().put("ugc_user_comment_count", String.valueOf(userId), String.valueOf(nextUserCount));
	}

	public void reduceCommentsToRedis(long commentId, long postId, long userId) {
		String field = commentId + "::" + postId;
		redis.opsForHash().put("ugc_comment", field, "0");

		String postCountRaw = (String) redis.opsForHash().get("ugc_comment_count", String.valueOf(postId));
		long postCount = parseLongPositive(postCountRaw);
		long nextPostCount = applyCountDelta(postCount, -1);
		redis.opsForHash().put("ugc_comment_count", String.valueOf(postId), String.valueOf(nextPostCount));

		String userCountRaw = (String) redis.opsForHash().get("ugc_user_comment_count", String.valueOf(userId));
		long userCount = parseLongPositive(userCountRaw);
		long nextUserCount = applyCountDelta(userCount, -1);
		redis.opsForHash().put("ugc_user_comment_count", String.valueOf(userId), String.valueOf(nextUserCount));
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

	private static long applyCountDelta(long stored, long delta) {
		if (stored > 0) {
			return stored + delta;
		}
		return delta;
	}
}
