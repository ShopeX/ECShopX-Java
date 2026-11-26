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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.UserDeposit;
import cn.shopex.ecshopx.deposit.mapper.UserDepositMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UserDepositBalanceMutationService {

	private static final String BALANCE_SHORT = "余额不足，支付失败";

	private final UserDepositMapper userDepositMapper;
	private final StringRedisTemplate redis;

	public UserDepositBalanceMutationService(
			UserDepositMapper userDepositMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.userDepositMapper = userDepositMapper;
		this.redis = redis;
	}

	public void addUserDepositTotal(long companyId, long userId, long moneyFen) {
		applyUserDepositTotalDelta(companyId, userId, moneyFen);
	}

	public void applyUserDepositTotalDelta(long companyId, long userId, long signedDeltaFen) {
		if (signedDeltaFen == 0L) {
			return;
		}
		int now = (int) Instant.now().getEpochSecond();
		long minBalance = signedDeltaFen < 0L ? -signedDeltaFen : 0L;
		int affected = userDepositMapper.addMoneyDelta(companyId, userId, signedDeltaFen, minBalance, now);
		long newMoney;
		if (affected > 0) {
			newMoney = currentMoney(companyId, userId);
		} else if (signedDeltaFen < 0L) {
			throw new ResourceException(BALANCE_SHORT);
		} else {
			UserDeposit row = new UserDeposit();
			row.setCompanyId(companyId);
			row.setUserId(userId);
			row.setMoney(signedDeltaFen);
			row.setCreated(now);
			row.setUpdated(now);
			try {
				userDepositMapper.insert(row);
				newMoney = signedDeltaFen;
			} catch (RuntimeException insertEx) {
				int retried = userDepositMapper.addMoneyDelta(companyId, userId, signedDeltaFen, 0L, now);
				if (retried == 0) {
					throw insertEx;
				}
				newMoney = currentMoney(companyId, userId);
			}
		}
		scheduleRedisRefresh(companyId, userId, newMoney);
	}

	private long currentMoney(long companyId, long userId) {
		UserDeposit existing =
				userDepositMapper.selectOne(
						new LambdaQueryWrapper<UserDeposit>()
								.eq(UserDeposit::getCompanyId, companyId)
								.eq(UserDeposit::getUserId, userId)
								.last("LIMIT 1"));
		if (existing == null || existing.getMoney() == null) {
			return 0L;
		}
		return existing.getMoney();
	}

	private void scheduleRedisRefresh(long companyId, long userId, long newMoney) {
		String redisKey = "userDepositTotal_" + companyId;
		String field = String.valueOf(userId);
		String value = String.valueOf(newMoney);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							redis.opsForHash().put(redisKey, field, value);
						}
					});
		} else {
			redis.opsForHash().put(redisKey, field, value);
		}
	}
}
