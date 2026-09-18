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

import cn.shopex.ecshopx.common.cron.merchant.CronDistributionCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class MerchantPaymentCronDistributionWithdrawalStatusService
		implements CronDistributionCashWithdrawalStatusWritePort {

	private final CashWithdrawalMapper cashWithdrawalMapper;

	public MerchantPaymentCronDistributionWithdrawalStatusService(CashWithdrawalMapper cashWithdrawalMapper) {
		this.cashWithdrawalMapper = cashWithdrawalMapper;
	}

	@Override
	public void updateStatusById(long id, String status) {
		LambdaUpdateWrapper<CashWithdrawal> uw = new LambdaUpdateWrapper<>();
		uw.eq(CashWithdrawal::getId, id);
		uw.set(CashWithdrawal::getStatus, status);
		cashWithdrawalMapper.update(null, uw);
	}
}
