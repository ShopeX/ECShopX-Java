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
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Resolves settlement apply by mall member {@code user_id} (ECX-10617).
 * Creates a draft apply on first entry when platform settlement is open.
 */
@Service
public class MerchantSettlementApplyMemberResolveService {

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;

	public MerchantSettlementApplyMemberResolveService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
	}

	public MerchantWxappAuthAttributes resolveAuth(long companyId, long userId) {
		MerchantSettlementApply existing = merchantSettlementApplyMapper.selectOne(
				new LambdaQueryWrapper<MerchantSettlementApply>()
						.eq(MerchantSettlementApply::getCompanyId, companyId)
						.eq(MerchantSettlementApply::getUserId, userId)
						.last("LIMIT 1"));
		if (existing != null && existing.getId() != null) {
			return new MerchantWxappAuthAttributes(companyId, existing.getId());
		}

		Map<String, Object> base = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
		if (Boolean.FALSE.equals(base.get("status"))) {
			throw new ResourceException("平台不支持商户入驻");
		}

		MerchantSettlementApply row = new MerchantSettlementApply();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setMobile("");
		row.setAgreeAgreement(true);
		row.setAuditStatus("1");
		row.setSource("h5");
		row.setDisabled(true);
		row.setCreated((int) (System.currentTimeMillis() / 1000L));
		row.setUpdated(null);
		int inserted = merchantSettlementApplyMapper.insert(row);
		if (inserted < 1 || row.getId() == null) {
			throw new ResourceException("提交失败，请重试");
		}
		return new MerchantWxappAuthAttributes(companyId, row.getId());
	}
}
