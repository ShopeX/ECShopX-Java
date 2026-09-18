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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.common.util.PlusBuyCartRedisKeys;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappH5CartPlusBuyResetService {

	private static final Logger log = LoggerFactory.getLogger(WxappH5CartPlusBuyResetService.class);

	private final StringRedisTemplate stringRedisTemplate;

	public WxappH5CartPlusBuyResetService(@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void resetPlusBuyCart(long companyId, long userId, Map<String, Object> cartListResult) {
		if (companyId <= 0L || userId <= 0L || cartListResult == null || cartListResult.isEmpty()) {
			return;
		}
		Set<Long> marketingIds = new LinkedHashSet<>();
		collectMarketingIds(cartListResult, marketingIds);
		for (Long marketingId : marketingIds) {
			if (marketingId == null || marketingId <= 0L) {
				continue;
			}
			String key = PlusBuyCartRedisKeys.redisKey(companyId, userId, Long.valueOf(marketingId));
			Boolean deleted = stringRedisTemplate.delete(key);
			log.info("resetPlusBuyCart companyId={} userId={} marketingId={} deleted={}", companyId, userId, marketingId, deleted);
		}
	}

	private static void collectMarketingIds(Object node, Set<Long> out) {
		if (node == null) {
			return;
		}
		if (node instanceof Map<?, ?> m) {
			putIfPositiveLong(m.get("marketing_id"), out);
			Object promo = m.get("promotion_activity");
			if (promo instanceof Map<?, ?> pm) {
				putIfPositiveLong(pm.get("marketing_id"), out);
			}
			for (Object v : m.values()) {
				collectMarketingIds(v, out);
			}
		} else if (node instanceof List<?> list) {
			for (Object v : list) {
				collectMarketingIds(v, out);
			}
		}
	}

	private static void putIfPositiveLong(Object raw, Set<Long> out) {
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v > 0L) {
				out.add(v);
			}
		} else if (raw != null) {
			try {
				long v = Long.parseLong(raw.toString().trim());
				if (v > 0L) {
					out.add(v);
				}
			} catch (NumberFormatException ignored) {
			}
		}
	}
}
