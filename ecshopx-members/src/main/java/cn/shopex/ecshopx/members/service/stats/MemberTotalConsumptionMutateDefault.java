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

package cn.shopex.ecshopx.members.service.stats;

import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 生产环境会员累计消费 Redis 写入；键算法与 {@link MemberTotalConsumptionReadService} 一致。
 */
@Service
public class MemberTotalConsumptionMutateDefault implements MemberTotalConsumptionMutatePort {

	private final StringRedisTemplate redis;
	private final MemberTotalConsumptionReadService readService;

	public MemberTotalConsumptionMutateDefault(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			MemberTotalConsumptionReadService readService) {
		this.redis = redis;
		this.readService = readService;
	}

	@Override
	public void addFenToTotalOrSetZero(long userId, BigDecimal deltaFen) {
		if (userId <= 0L) {
			return;
		}
		BigDecimal d = deltaFen == null ? BigDecimal.ZERO : deltaFen;
		BigDecimal before = readService.getTotalConsumption(userId);
		if (before.add(d).compareTo(BigDecimal.ZERO) > 0) {
			long inc = d.setScale(0, RoundingMode.UNNECESSARY).longValue();
			redis.opsForValue().increment(redisKey(userId), inc);
		} else {
			redis.opsForValue().set(redisKey(userId), "0");
		}
	}

	private String redisKey(long userId) {
		return "totalConsumption:" + sha1HexLowerUtf8(String.valueOf(userId));
	}

	private static String sha1HexLowerUtf8(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
