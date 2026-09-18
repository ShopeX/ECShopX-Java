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

package cn.shopex.ecshopx.common.util;

import org.apache.commons.codec.digest.DigestUtils;

/** 加价购购物车 Redis 键：与列表读取、写入使用同一 SHA1 输入串。 */
public final class PlusBuyCartRedisKeys {

	private PlusBuyCartRedisKeys() {}

	public static String sha1InputPayload(long companyId, long userId, Long marketing_id) {
		String marketingIdRaw = marketing_id == null ? "" : String.valueOf(marketing_id);
		return companyId + "userId" + userId + "marketingId" + marketingIdRaw;
	}

	public static String redisKey(long companyId, long userId, Long marketing_id) {
		return "plusbuy:" + DigestUtils.sha1Hex(sha1InputPayload(companyId, userId, marketing_id));
	}
}
