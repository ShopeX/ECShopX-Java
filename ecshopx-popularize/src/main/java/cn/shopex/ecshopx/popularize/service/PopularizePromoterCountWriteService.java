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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.cron.PopularizeBrokerageSettleRedisPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PopularizePromoterCountWriteService {

	private static final String METRIC_CASH_WITHDRAWAL_REBATE = "cashWithdrawalRebate";

	private static final String METRIC_FREEZE_CASH_WITHDRAWAL_REBATE = "freezeCashWithdrawalRebate";

	private static final String METRIC_PAYED_REBATE = "payedRebate";

	private final PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;
	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final PopularizeBrokerageSettleRedisPort popularizeBrokerageSettleRedisPort;

	public PopularizePromoterCountWriteService(
			PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService,
			PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			PopularizeBrokerageSettleRedisPort popularizeBrokerageSettleRedisPort) {
		this.popularizeBrokerageStatisticsIncrementService = popularizeBrokerageStatisticsIncrementService;
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.popularizeBrokerageSettleRedisPort = popularizeBrokerageSettleRedisPort;
	}

	/**
	 * 与 PHP addSettleRebate 一致：先 Redis 两指标增量，再写 popularize_brokerage_statistics（顺序不得调换）。
	 */
	public void addSettleRebateForSchedule(long companyId, long userId, long rebateMinor) {
		popularizeBrokerageSettleRedisPort.applySettleRebateHashIncr(companyId, userId, rebateMinor);
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of("cash_withdrawal_rebate", rebateMinor, "no_close_rebate", -rebateMinor), userId, companyId);
	}

	public void applyCashWithdrawal(long companyId, long userId, int moneyInt) {
		long m = moneyInt;
		incrPromoterAndCompany(companyId, userId, METRIC_CASH_WITHDRAWAL_REBATE, -m);
		PromoterBrokerageStatistics row =
				promoterBrokerageStatisticsMapper.selectOne(
						new LambdaQueryWrapper<PromoterBrokerageStatistics>()
								.eq(PromoterBrokerageStatistics::getUserId, userId)
								.eq(PromoterBrokerageStatistics::getCompanyId, companyId)
								.last("LIMIT 1"));
		long rebate =
				row == null || row.getCashWithdrawalRebate() == null ? 0L : row.getCashWithdrawalRebate();
		if (rebate == 0L) {
			throw new ResourceException("申请提现金额额度超出限制");
		}
		if (rebate - m < 0) {
			incrPromoterAndCompany(companyId, userId, METRIC_CASH_WITHDRAWAL_REBATE, m);
			throw new ResourceException("申请提现金额额度超出限制");
		}
		incrPromoterAndCompany(companyId, userId, METRIC_FREEZE_CASH_WITHDRAWAL_REBATE, m);
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of("cash_withdrawal_rebate", -m, "freeze_cash_withdrawal_rebate", m), userId, companyId);
	}

	public void agreeCashWithdrawal(long companyId, long userId, int moneyInt) {
		long rebate =
				popularizeBrokerageStatisticsIncrementService.loadCashWithdrawalRebate(userId, companyId);
		if (rebate < 0L) {
			throw new ResourceException("申请提现金额额度超出限制");
		}
		long m = moneyInt;
		incrPromoterAndCompany(companyId, userId, METRIC_FREEZE_CASH_WITHDRAWAL_REBATE, -m);
		incrPromoterAndCompany(companyId, userId, METRIC_PAYED_REBATE, m);
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of("freeze_cash_withdrawal_rebate", -m, "payed_rebate", m), userId, companyId);
	}

	public void rejectCashWithdrawal(long companyId, long userId, int moneyInt) {
		long m = moneyInt;
		incrPromoterAndCompany(companyId, userId, METRIC_FREEZE_CASH_WITHDRAWAL_REBATE, -m);
		incrPromoterAndCompany(companyId, userId, METRIC_CASH_WITHDRAWAL_REBATE, m);
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of("freeze_cash_withdrawal_rebate", -m, "cash_withdrawal_rebate", m), userId, companyId);
	}

	private void incrPromoterAndCompany(long companyId, long userId, String metric, long increment) {
		incrementPromoterField(userId, metric, increment);
		incrementCompanyField(companyId, metric, increment);
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
