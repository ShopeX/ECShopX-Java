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

package cn.shopex.ecshopx.wsugc.service.follower;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UgcFollowerRedisService {

	private final StringRedisTemplate redis;

	public UgcFollowerRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void addFollowersToRedis(long bloggerUserId, long followerUserId) {
		String field = followerField(bloggerUserId, followerUserId);
		redis.opsForHash().put("ugc_follower", field, "1");

		String bloggerCountRaw = (String) redis.opsForHash().get("ugc_follower_count", String.valueOf(bloggerUserId));
		long nextBlogger = checkAndCountfromRedis(parseLongPositive(bloggerCountRaw), 1);
		redis.opsForHash().put("ugc_follower_count", String.valueOf(bloggerUserId), String.valueOf(nextBlogger));

		String idolCountRaw = (String) redis.opsForHash().get("ugc_idol_count", String.valueOf(followerUserId));
		long nextIdol = checkAndCountfromRedis(parseLongPositive(idolCountRaw), 1);
		redis.opsForHash().put("ugc_idol_count", String.valueOf(followerUserId), String.valueOf(nextIdol));
	}

	public String getFollowerHashValue(long bloggerUserId, long followerUserId) {
		Object v = redis.opsForHash().get("ugc_follower", followerField(bloggerUserId, followerUserId));
		return v == null ? null : v.toString();
	}

	public void reduceFollowersToRedis(long bloggerUserId, long followerUserId) {
		String field = followerField(bloggerUserId, followerUserId);
		redis.opsForHash().put("ugc_follower", field, "0");

		String bloggerCountRaw = (String) redis.opsForHash().get("ugc_follower_count", String.valueOf(bloggerUserId));
		long nextBlogger = checkAndCountfromRedis(parseLongPositive(bloggerCountRaw), -1);
		redis.opsForHash().put("ugc_follower_count", String.valueOf(bloggerUserId), String.valueOf(nextBlogger));

		String idolCountRaw = (String) redis.opsForHash().get("ugc_idol_count", String.valueOf(followerUserId));
		long nextIdol = checkAndCountfromRedis(parseLongPositive(idolCountRaw), -1);
		redis.opsForHash().put("ugc_idol_count", String.valueOf(followerUserId), String.valueOf(nextIdol));
	}

	private static String followerField(long userId, long followerUserId) {
		return userId + "::" + followerUserId;
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
