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
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantSettlementApplyProgressService {

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantSettlementApplyAgreeAgreementService agreeAgreementService;

	public MerchantSettlementApplyProgressService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantSettlementApplyAgreeAgreementService agreeAgreementService) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.agreeAgreementService = agreeAgreementService;
	}

	public int resolveProgressStepWithSideEffects(long companyId, long accountId) {
		MerchantSettlementApply row = merchantSettlementApplyMapper.selectById(accountId);
		if (row == null || row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ResourceException("获取账号信息失败");
		}
		int resultStep;
		if (StringUtils.hasText(row.getLicenseUrl())) {
			resultStep = 4;
			if (!row.isAgreeAgreement()) {
				agreeAgreementService.agreeAgreementOrSwallow(row, companyId);
			}
		} else if (StringUtils.hasText(row.getMerchantName())) {
			resultStep = 3;
		} else if (StringUtils.hasText(row.getSettledType())) {
			resultStep = 2;
		} else {
			resultStep = 1;
		}
		return resultStep;
	}
}
