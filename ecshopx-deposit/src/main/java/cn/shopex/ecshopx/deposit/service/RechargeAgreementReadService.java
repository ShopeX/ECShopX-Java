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

import cn.shopex.ecshopx.deposit.domain.RechargeAgreement;
import cn.shopex.ecshopx.deposit.mapper.RechargeAgreementMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class RechargeAgreementReadService {

	private final RechargeAgreementMapper rechargeAgreementMapper;

	public RechargeAgreementReadService(RechargeAgreementMapper rechargeAgreementMapper) {
		this.rechargeAgreementMapper = rechargeAgreementMapper;
	}

	public Object getAgreementPayload(long companyId) {
		String id = String.valueOf(companyId);
		RechargeAgreement row = rechargeAgreementMapper.selectById(id);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", row.getCompanyId());
		m.put("content", row.getContent());
		return m;
	}
}
