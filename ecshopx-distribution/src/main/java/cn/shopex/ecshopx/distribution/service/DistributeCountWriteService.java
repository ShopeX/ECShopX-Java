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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributeCountWriteService {

	private final StringRedisTemplate companysRedisTemplate;

	public DistributeCountWriteService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void applyCashWithdrawal(long companyId, String distributorId, int money) {
		long m = money;
		String cashMetric = DistributeCountReadService.METRIC_CASH_WITHDRAWAL_REBATE;
		String freezeMetric = DistributeCountReadService.METRIC_FREEZE_CASH_WITHDRAWAL_REBATE;
		long promoterAfterCash = incrementPromoterField(distributorId, cashMetric, -m);
		long companyAfterCash = incrementCompanyField(companyId, cashMetric, -m);
		if (promoterAfterCash < 0L || companyAfterCash < 0L) {
			incrementPromoterField(distributorId, cashMetric, m);
			incrementCompanyField(companyId, cashMetric, m);
			throw new ResourceException("申请提现金额额度超出限制");
		}
		incrementPromoterField(distributorId, freezeMetric, m);
		incrementCompanyField(companyId, freezeMetric, m);
	}

	public void agreeCashWithdrawal(long companyId, String distributorId, int money) {
		long delta = -money;
		incrPromoterAndCompany(
				companyId, distributorId, DistributeCountReadService.METRIC_FREEZE_CASH_WITHDRAWAL_REBATE, delta);
	}

	public void rejectCashWithdrawal(long companyId, String distributorId, int money) {
		incrPromoterAndCompany(
				companyId, distributorId, DistributeCountReadService.METRIC_FREEZE_CASH_WITHDRAWAL_REBATE, -money);
		incrPromoterAndCompany(
				companyId, distributorId, DistributeCountReadService.METRIC_CASH_WITHDRAWAL_REBATE, money);
	}

	private void incrPromoterAndCompany(long companyId, String distributorId, String metric, long increment) {
		incrementPromoterField(distributorId, metric, increment);
		incrementCompanyField(companyId, metric, increment);
	}

	private long incrementPromoterField(String distributorId, String metric, long delta) {
		long distId = Long.parseLong(distributorId.trim());
		long promoterShard = Math.floorDiv(distId, 20L);
		String promoterKey = "promoterPopularizeCount:" + promoterShard;
		String promoterField = metric + "-" + distId;
		Long v = companysRedisTemplate.opsForHash().increment(promoterKey, promoterField, delta);
		return Objects.requireNonNullElse(v, 0L);
	}

	private long incrementCompanyField(long companyId, String metric, long delta) {
		long companyShard = Math.floorDiv(companyId, 20L);
		String companyKey = "companyPopularizeCount:" + companyShard;
		String companyField = metric + "-" + companyId;
		Long v = companysRedisTemplate.opsForHash().increment(companyKey, companyField, delta);
		return Objects.requireNonNullElse(v, 0L);
	}
}
