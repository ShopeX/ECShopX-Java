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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.deposit.domain.UserDeposit;
import cn.shopex.ecshopx.deposit.mapper.UserDepositMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserDepositBalanceReadService {

	private final UserDepositMapper userDepositMapper;
	private final StringRedisTemplate redis;

	public UserDepositBalanceReadService(
			UserDepositMapper userDepositMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.userDepositMapper = userDepositMapper;
		this.redis = redis;
	}

	public long getUserDepositTotal(long companyId, long userId) {
		String redisKey = "userDepositTotal_" + companyId;
		String field = String.valueOf(userId);
		Object raw = redis.opsForHash().get(redisKey, field);
		if (raw == null) {
			UserDeposit row =
					userDepositMapper.selectOne(
							new LambdaQueryWrapper<UserDeposit>()
									.eq(UserDeposit::getCompanyId, companyId)
									.eq(UserDeposit::getUserId, userId)
									.last("LIMIT 1"));
			if (row == null || row.getMoney() == null) {
				return 0L;
			}
			return row.getMoney();
		}
		try {
			long deposit = new BigDecimal(raw.toString().trim()).longValue();
			return deposit > 0 ? deposit : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
