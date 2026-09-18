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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantSettlementApplyDuplicateCheckService {

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;

	public MerchantSettlementApplyDuplicateCheckService(MerchantSettlementApplyMapper merchantSettlementApplyMapper) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
	}

	public void assertMobileNotUsedInApply(String mobilePlain) {
		if (!StringUtils.hasText(mobilePlain)) {
			return;
		}
		Long cnt = merchantSettlementApplyMapper.selectCount(new LambdaQueryWrapper<MerchantSettlementApply>()
				.eq(MerchantSettlementApply::getMobile, mobilePlain.trim()));
		if (cnt != null && cnt > 0) {
			throw new ResourceException("账号的手机号已经存在，请确认后再重试");
		}
	}

	public void assertSocialCreditNotDuplicateForWxappSave(long companyId, long excludeApplyId, String socialCreditPlain) {
		if (!StringUtils.hasText(socialCreditPlain)) {
			return;
		}
		Long cnt = merchantSettlementApplyMapper.selectCount(new LambdaQueryWrapper<MerchantSettlementApply>()
				.eq(MerchantSettlementApply::getCompanyId, companyId)
				.eq(MerchantSettlementApply::getSocialCreditCodeId, socialCreditPlain.trim())
				.ne(MerchantSettlementApply::getId, excludeApplyId)
				.ne(MerchantSettlementApply::getAuditStatus, "3"));
		if (cnt != null && cnt > 0) {
			throw new ResourceException("统一社会信用代码已存在，请检查后再重新提交");
		}
	}
}
