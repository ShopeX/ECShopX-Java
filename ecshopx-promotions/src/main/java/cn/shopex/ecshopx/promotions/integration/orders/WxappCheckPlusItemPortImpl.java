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

package cn.shopex.ecshopx.promotions.integration.orders;

import cn.shopex.ecshopx.common.util.PlusBuyCartRedisKeys;
import cn.shopex.ecshopx.orders.port.WxappCheckPlusItemPort;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappCheckPlusItemPortImpl implements WxappCheckPlusItemPort {

	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public WxappCheckPlusItemPortImpl(
			MarketingGiftItemsMapper marketingGiftItemsMapper, StringRedisTemplate stringRedisTemplate) {
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public void checkPlusItem(long companyId, long userId, Map<String, Object> mergedInput) {
		long itemId = parseItemId(mergedInput.get("item_id"));
		long itemIdToStore = itemId;

		if (itemId > 0L) {
			if (marketingIdRawAbsent(mergedInput)) {
				LambdaQueryWrapper<MarketingGiftItems> w = baseGiftWrapper(companyId, itemId);
				w.isNull(MarketingGiftItems::getMarketingId).last("LIMIT 1");
				MarketingGiftItems row = marketingGiftItemsMapper.selectOne(w);
				if (row == null) {
					itemIdToStore = 0L;
				}
			} else {
				Long mid = parseMarketingIdToLong(mergedInput.get("marketing_id"));
				if (mid == null) {
					itemIdToStore = 0L;
				} else {
					LambdaQueryWrapper<MarketingGiftItems> w = baseGiftWrapper(companyId, itemId);
					w.eq(MarketingGiftItems::getMarketingId, mid).last("LIMIT 1");
					MarketingGiftItems row = marketingGiftItemsMapper.selectOne(w);
					if (row == null) {
						itemIdToStore = 0L;
					}
				}
			}
		}

		Long marketingIdForRedis = resolveMarketingIdForRedis(mergedInput);
		String key = PlusBuyCartRedisKeys.redisKey(companyId, userId, marketingIdForRedis);
		stringRedisTemplate.opsForValue().set(key, String.valueOf(itemIdToStore));
	}

	private static LambdaQueryWrapper<MarketingGiftItems> baseGiftWrapper(long companyId, long itemId) {
		LambdaQueryWrapper<MarketingGiftItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingGiftItems::getCompanyId, companyId).eq(MarketingGiftItems::getItemId, itemId);
		return w;
	}

	private static boolean marketingIdRawAbsent(Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("marketing_id")) {
			return true;
		}
		Object raw = mergedInput.get("marketing_id");
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	private static Long resolveMarketingIdForRedis(Map<String, Object> mergedInput) {
		if (marketingIdRawAbsent(mergedInput)) {
			return null;
		}
		return parseMarketingIdToLong(mergedInput.get("marketing_id"));
	}

	/** 解析失败返回 null（调用方已保证非 absent 时用于 DB；Redis 侧失败则视为 null）。 */
	private static Long parseMarketingIdToLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseItemId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
