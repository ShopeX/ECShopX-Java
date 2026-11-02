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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 对齐 PHP {@code AftersalesService::sendBackConfirm} / {@code confirmReceipt} 后 DigiOS 入库调整触发点。
 */
@Service
public class AftersalesMerchantSendbackConfirmService {

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundAsyncPort aftersalesRefundAsyncPort;
	private final TransactionTemplate transactionTemplate;

	public AftersalesMerchantSendbackConfirmService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundAsyncPort aftersalesRefundAsyncPort,
			PlatformTransactionManager platformTransactionManager) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundAsyncPort = aftersalesRefundAsyncPort;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void sendBackConfirm(long companyId, long aftersalesBn) {
		Aftersales aftersales = aftersalesMapper.selectOne(
				new LambdaQueryWrapper<Aftersales>()
						.eq(Aftersales::getCompanyId, companyId)
						.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (aftersales == null) {
			throw new ResourceException("售后单不存在");
		}
		String aftersalesType = aftersales.getAftersalesType();
		int now = (int) (System.currentTimeMillis() / 1000L);
		transactionTemplate.executeWithoutResult(status -> {
			LambdaUpdateWrapper<Aftersales> uw = new LambdaUpdateWrapper<>();
			uw.eq(Aftersales::getCompanyId, companyId)
					.eq(Aftersales::getAftersalesBn, aftersalesBn)
					.set(Aftersales::getProgress, 8)
					.set(Aftersales::getUpdateTime, now);
			if (aftersalesMapper.update(null, uw) == 0) {
				throw new ResourceException("售后单不存在");
			}
			LambdaUpdateWrapper<AftersalesDetail> duw = new LambdaUpdateWrapper<>();
			duw.eq(AftersalesDetail::getCompanyId, companyId)
					.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
					.set(AftersalesDetail::getProgress, 8)
					.set(AftersalesDetail::getUpdateTime, now);
			aftersalesDetailMapper.update(null, duw);
			if ("REFUND_GOODS".equalsIgnoreCase(aftersalesType)) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						aftersalesRefundAsyncPort.scheduleDigiOsStoreAdjustmentOnAftersalesReturn(
								companyId, aftersalesBn);
					}
				});
			}
		});
	}
}
