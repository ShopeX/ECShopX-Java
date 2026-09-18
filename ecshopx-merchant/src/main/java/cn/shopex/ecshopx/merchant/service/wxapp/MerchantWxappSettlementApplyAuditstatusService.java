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
import cn.shopex.ecshopx.merchant.port.MerchantWxappSettlementApplyLoginInfoPort;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyDetailQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantWxappSettlementApplyAuditstatusService {

	private final MerchantSettlementApplyDetailQueryService detailQueryService;
	private final MerchantRepository merchantRepository;
	private final MerchantWxappSettlementApplyLoginInfoPort loginInfoPort;

	public MerchantWxappSettlementApplyAuditstatusService(
			MerchantSettlementApplyDetailQueryService detailQueryService,
			MerchantRepository merchantRepository,
			MerchantWxappSettlementApplyLoginInfoPort loginInfoPort) {
		this.detailQueryService = detailQueryService;
		this.merchantRepository = merchantRepository;
		this.loginInfoPort = loginInfoPort;
	}

	public Map<String, Object> build(long companyId, long accountId, String acceptLanguageHeader) {
		Map<String, Object> detail = detailQueryService.getDetail(accountId, acceptLanguageHeader);
		LinkedHashMap<String, Object> base = new LinkedHashMap<>();
		Object auditRaw = detail.get("audit_status");
		String auditStatusStr =
				auditRaw instanceof String s ? s : (auditRaw == null ? "" : String.valueOf(auditRaw));
		base.put("audit_status", auditStatusStr);
		Object memo = detail.get("audit_memo");
		base.put("audit_memo", memo == null ? "" : String.valueOf(memo));

		Map<String, Object> loginExtras;
		if (!"2".equals(auditStatusStr)) {
			loginExtras = Map.of();
		} else {
			Merchant merchant = merchantRepository.findByCompanyIdAndSettlementApplyId(companyId, accountId);
			if (merchant == null) {
				throw new ResourceException("商户数据不存在");
			}
			loginExtras = loginInfoPort.buildMobileAndMaybePassword(companyId, merchant.getId());
		}
		base.putAll(loginExtras);
		return base;
	}
}
