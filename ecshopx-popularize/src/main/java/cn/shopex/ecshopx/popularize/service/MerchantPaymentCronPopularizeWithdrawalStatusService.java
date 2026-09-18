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

import cn.shopex.ecshopx.common.cron.merchant.CronPopularizeCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.popularize.domain.PromoterCashWithdrawal;
import cn.shopex.ecshopx.popularize.mapper.PromoterCashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class MerchantPaymentCronPopularizeWithdrawalStatusService
		implements CronPopularizeCashWithdrawalStatusWritePort {

	private final PromoterCashWithdrawalMapper promoterCashWithdrawalMapper;

	public MerchantPaymentCronPopularizeWithdrawalStatusService(
			PromoterCashWithdrawalMapper promoterCashWithdrawalMapper) {
		this.promoterCashWithdrawalMapper = promoterCashWithdrawalMapper;
	}

	@Override
	public void updateStatusById(long id, String status) {
		LambdaUpdateWrapper<PromoterCashWithdrawal> uw = new LambdaUpdateWrapper<>();
		uw.eq(PromoterCashWithdrawal::getId, id);
		uw.set(PromoterCashWithdrawal::getStatus, status);
		promoterCashWithdrawalMapper.update(null, uw);
	}
}
