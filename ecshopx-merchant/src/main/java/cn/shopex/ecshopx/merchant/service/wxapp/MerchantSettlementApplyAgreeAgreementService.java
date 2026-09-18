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

package cn.shopex.ecshopx.merchant.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantMainOperatorProvisioner;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class MerchantSettlementApplyAgreeAgreementService {

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantRepository merchantRepository;
	private final MerchantMainOperatorProvisioner merchantMainOperatorProvisioner;

	public MerchantSettlementApplyAgreeAgreementService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantRepository merchantRepository,
			MerchantMainOperatorProvisioner merchantMainOperatorProvisioner) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantRepository = merchantRepository;
		this.merchantMainOperatorProvisioner = merchantMainOperatorProvisioner;
	}

	@Transactional(rollbackFor = Exception.class)
	public void agreeAgreementOrSwallow(MerchantSettlementApply row, long companyId) {
		try {
			int now = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<MerchantSettlementApply> uw = new LambdaUpdateWrapper<>();
			uw.eq(MerchantSettlementApply::getId, row.getId())
					.set(MerchantSettlementApply::isAgreeAgreement, true)
					.set(MerchantSettlementApply::getUpdated, now);
			int rows = merchantSettlementApplyMapper.update(null, uw);
			if (rows == 0) {
				return;
			}
			Merchant merchant = merchantRepository.findByCompanyIdAndSettlementApplyId(companyId, row.getId());
			if (merchant == null || merchant.getId() == null) {
				throw new ResourceException("商户信息获取失败");
			}
			String mobilePlain = row.getMobile();
			merchantMainOperatorProvisioner.createMainMerchantOperatorWithoutPassword(
					companyId, mobilePlain, mobilePlain, merchant.getId());
		} catch (Exception ignored) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		}
	}
}
