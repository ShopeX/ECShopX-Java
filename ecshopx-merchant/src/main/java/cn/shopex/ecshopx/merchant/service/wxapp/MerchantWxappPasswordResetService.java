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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorPasswordResetPort;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorPasswordResetResult;
import cn.shopex.ecshopx.merchant.port.MerchantWxappMainOperatorIdLookupPort;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantWxappPasswordResetService {

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantRepository merchantRepository;
	private final MerchantWxappMainOperatorIdLookupPort mainOperatorIdLookupPort;
	private final MerchantOperatorPasswordResetPort merchantOperatorPasswordResetPort;

	public MerchantWxappPasswordResetService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantRepository merchantRepository,
			MerchantWxappMainOperatorIdLookupPort mainOperatorIdLookupPort,
			MerchantOperatorPasswordResetPort merchantOperatorPasswordResetPort) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantRepository = merchantRepository;
		this.mainOperatorIdLookupPort = mainOperatorIdLookupPort;
		this.merchantOperatorPasswordResetPort = merchantOperatorPasswordResetPort;
	}

	/** @return {@link Collections#emptyList()} → JSON {@code []}, or a map with {@code mobile}/{@code password}. */
	public Object reset(long companyId, long accountId) {
		MerchantSettlementApply apply = merchantSettlementApplyMapper.selectById(accountId);
		if (apply == null) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (apply.getCompanyId() == null || apply.getCompanyId() != companyId) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (!"2".equals(apply.getAuditStatus()) || apply.isDisabled()) {
			return Collections.emptyList();
		}
		Merchant merchant = merchantRepository.findByCompanyIdAndSettlementApplyId(companyId, accountId);
		if (merchant == null || merchant.getId() == null) {
			throw new ResourceException("商户数据不存在");
		}
		long operatorId = mainOperatorIdLookupPort
				.findMainMerchantOperatorId(companyId, merchant.getId())
				.orElseThrow(() -> new ResourceException("操作员信息不完整"));
		MerchantOperatorPasswordResetResult result =
				merchantOperatorPasswordResetPort.resetForMerchantConsole(companyId, operatorId);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("mobile", result.mobile());
		out.put("password", result.plainPassword());
		return out;
	}
}
