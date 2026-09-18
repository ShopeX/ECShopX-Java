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

package cn.shopex.ecshopx.popularize.port;

import cn.shopex.ecshopx.common.cron.PopularizeBrokerageSettleRedisPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PopularizeBrokerageSettleRedisPortImpl implements PopularizeBrokerageSettleRedisPort {

	private static final String METRIC_CASH_WITHDRAWAL_REBATE = "cashWithdrawalRebate";

	private static final String METRIC_NO_CLOSE_REBATE = "noCloseRebate";

	private final StringRedisTemplate companysRedisTemplate;

	public PopularizeBrokerageSettleRedisPortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@Override
	public void applySettleRebateHashIncr(long companyId, long userId, long rebateMinor) {
		incrementPromoterField(userId, METRIC_CASH_WITHDRAWAL_REBATE, rebateMinor);
		incrementPromoterField(userId, METRIC_NO_CLOSE_REBATE, -rebateMinor);
		incrementCompanyField(companyId, METRIC_CASH_WITHDRAWAL_REBATE, rebateMinor);
		incrementCompanyField(companyId, METRIC_NO_CLOSE_REBATE, -rebateMinor);
	}

	private void incrementPromoterField(long userId, String metric, long delta) {
		long promoterShard = Math.floorDiv(userId, 20L);
		String promoterKey = "promoterPopularizeCount:" + promoterShard;
		String promoterField = metric + "-" + userId;
		companysRedisTemplate.opsForHash().increment(promoterKey, promoterField, delta);
	}

	private void incrementCompanyField(long companyId, String metric, long delta) {
		long companyShard = Math.floorDiv(companyId, 20L);
		String companyKey = "companyPopularizeCount:" + companyShard;
		String companyField = metric + "-" + companyId;
		companysRedisTemplate.opsForHash().increment(companyKey, companyField, delta);
	}
}
