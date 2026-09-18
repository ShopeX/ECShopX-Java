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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappTradeRatePraiseService {

	private final TradeRateMapper tradeRateMapper;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService;
	private final WxappTradeRatePraiseNumService wxappTradeRatePraiseNumService;

	public WxappTradeRatePraiseService(
			TradeRateMapper tradeRateMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService,
			WxappTradeRatePraiseNumService wxappTradeRatePraiseNumService) {
		this.tradeRateMapper = tradeRateMapper;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.wxappTradeRatePraiseCheckService = wxappTradeRatePraiseCheckService;
		this.wxappTradeRatePraiseNumService = wxappTradeRatePraiseNumService;
	}

	public Map<String, Integer> ratePraise(long companyId, long userId, String trimmed) {
		String t = trimmed == null ? "" : trimmed.trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException("参数错误");
		}
		if ("0".equals(t)) {
			throw new ResourceException("参数错误");
		}
		long rateId;
		try {
			rateId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("评价不存在");
		}
		if (rateId < 0L) {
			throw new ResourceException("评价不存在");
		}
		if (rateId == 0L) {
			throw new ResourceException("参数错误");
		}

		LambdaQueryWrapper<TradeRate> w = new LambdaQueryWrapper<>();
		w.eq(TradeRate::getCompanyId, companyId).eq(TradeRate::getRateId, rateId);
		TradeRate row = tradeRateMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("评价不存在");
		}

		String redisRateKey = String.valueOf(row.getRateId());
		boolean praised = wxappTradeRatePraiseCheckService.ratePraiseCheck(companyId, userId, redisRateKey);
		String userKey = "ratePraiseUser:" + companyId + ":" + redisRateKey;

		if (praised) {
			sharedStringRedisTemplate.opsForHash().delete(userKey, String.valueOf(userId));
			sharedStringRedisTemplate.opsForHash().increment("ratePraise", redisRateKey, -1L);
			return Map.of("count", wxappTradeRatePraiseNumService.ratePraiseNum(redisRateKey));
		}

		Long incrResult = sharedStringRedisTemplate.opsForHash().increment("ratePraise", redisRateKey, 1L);
		long newCount = incrResult;
		if (newCount > Integer.MAX_VALUE) {
			throw new ResourceException("参数错误");
		}
		int count = (int) newCount;
		if (count > 0) {
			sharedStringRedisTemplate
					.opsForHash()
					.put(userKey, String.valueOf(userId), String.valueOf(Instant.now().getEpochSecond()));
		}
		return Map.of("count", count);
	}
}
