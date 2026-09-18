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

import cn.shopex.ecshopx.common.dispatch.RechargeSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdapayDepositTradeRechargeCallbackService {

	private final DepositTradeMapper depositTradeMapper;
	private final UserDepositBalanceMutationService userDepositBalanceMutationService;
	private final StringRedisTemplate redis;
	private final RechargeSendSmsNoticeJobDispatchPublisher rechargeSendSmsNoticeJobDispatchPublisher;

	public AdapayDepositTradeRechargeCallbackService(
			DepositTradeMapper depositTradeMapper,
			UserDepositBalanceMutationService userDepositBalanceMutationService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			RechargeSendSmsNoticeJobDispatchPublisher rechargeSendSmsNoticeJobDispatchPublisher) {
		this.depositTradeMapper = depositTradeMapper;
		this.userDepositBalanceMutationService = userDepositBalanceMutationService;
		this.redis = redis;
		this.rechargeSendSmsNoticeJobDispatchPublisher = rechargeSendSmsNoticeJobDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void rechargeCallback(String depositTradeId, String status, Map<String, Object> options) {
		if (!StringUtils.hasText(depositTradeId)) {
			throw new ResourceException("储值单不存在或主键无效，无法完成回调");
		}
		DepositTrade row = depositTradeMapper.selectById(depositTradeId);
		if (row == null) {
			throw new ResourceException("储值单不存在或主键无效，无法完成回调");
		}
		if (!"SUCCESS".equals(status)) {
			return;
		}
		if ("SUCCESS".equals(row.getTradeStatus())) {
			throw new ResourceException("更新已处理，不需要更新");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<DepositTrade> uw = new LambdaUpdateWrapper<>();
		uw.eq(DepositTrade::getDepositTradeId, depositTradeId)
				.set(DepositTrade::getTradeStatus, status)
				.set(DepositTrade::getBankType, str(options.get("bank_type")))
				.set(DepositTrade::getTransactionId, str(options.get("transaction_id")))
				.set(DepositTrade::getPayType, str(options.get("pay_type")))
				.set(DepositTrade::getTimeExpire, String.valueOf(now));
		int n = depositTradeMapper.update(null, uw);
		if (n <= 0) {
			throw new ResourceException("储值单状态更新失败");
		}
		row = depositTradeMapper.selectById(depositTradeId);
		long totalFeeFen = parseMoneyFen(row.getCurPayFee());
		long rechargeMoneyFen = parseMoneyFen(row.getCurPayFee());
		long companyId = parseLong(row.getCompanyId());
		long userId = parseLong(row.getUserId());
		addDepositToRedis(companyId, userId, totalFeeFen, rechargeMoneyFen);
		long smsTotalFeeFen = resolveRechargeNoticeTotalFeeFen(row);
		if (StringUtils.hasText(row.getMobile())) {
			scheduleRechargeSendSmsNoticeAfterCommit(companyId, userId, row.getMobile(), smsTotalFeeFen);
		}
	}

	private void scheduleRechargeSendSmsNoticeAfterCommit(
			long companyId, long userId, String mobile, long totalFeeFen) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							rechargeSendSmsNoticeJobDispatchPublisher.publishRechargeSendSmsNotice(
									companyId, userId, mobile, totalFeeFen);
						}
					});
		} else {
			rechargeSendSmsNoticeJobDispatchPublisher.publishRechargeSendSmsNotice(
					companyId, userId, mobile, totalFeeFen);
		}
	}

	private long resolveRechargeNoticeTotalFeeFen(DepositTrade row) {
		if (StringUtils.hasText(row.getMoney())) {
			return parseMoneyFen(row.getMoney());
		}
		return parseMoneyFen(row.getCurPayFee());
	}

	private void addDepositToRedis(long companyId, long userId, long rechargeTotalFeeFen, long rechargeMoneyFen) {
		redis.opsForHash().increment("shopDepositTotal", String.valueOf(companyId), rechargeTotalFeeFen);
		userDepositBalanceMutationService.addUserDepositTotal(companyId, userId, rechargeTotalFeeFen);
		String dayKey = "dayRechargeTotal" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		redis.opsForHash().increment(dayKey, String.valueOf(companyId), rechargeMoneyFen);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLong(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseMoneyFen(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
