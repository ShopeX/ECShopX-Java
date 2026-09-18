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

package cn.shopex.ecshopx.orders.service.companyrellogistics;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.support.ScalarEmptyCompat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyRelLogisticsAdminCreateService {

	private final CompanyRelLogisticsMapper mapper;
	private final CompanyRelLogisticsAdminCreateParamValidator validator;

	public CompanyRelLogisticsAdminCreateService(
			CompanyRelLogisticsMapper mapper, CompanyRelLogisticsAdminCreateParamValidator validator) {
		this.mapper = mapper;
		this.validator = validator;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createCompanyLogistics(
			long companyId, String operatorType, Long operatorIdOrNull, Map<String, Object> merged) {
		if (companyId > Integer.MAX_VALUE || companyId < Integer.MIN_VALUE) {
			throw new BadRequestException("company_id 无效");
		}
		int companyIdInt = (int) companyId;

		NormalizedCompanyRelLogisticsCreateParams p = validator.validateAndNormalize(merged);
		long supplierId =
				Objects.equals(operatorType, "supplier") && operatorIdOrNull != null ? operatorIdOrNull.longValue() : 0L;

		CompanyRelLogistics e = new CompanyRelLogistics();
		if (p.corpId() != null) {
			e.setCorpId(p.corpId());
		}
		if (merged.containsKey("corp_name") && ScalarEmptyCompat.isNotEmpty(merged.get("corp_name"))) {
			e.setCorpName(p.corpName());
		}
		if (merged.containsKey("corp_code") && ScalarEmptyCompat.isNotEmpty(merged.get("corp_code"))) {
			e.setCorpCode(p.corpCode());
		}
		if (merged.containsKey("kuaidi_code") && ScalarEmptyCompat.isNotEmpty(merged.get("kuaidi_code"))) {
			e.setKuaidiCode(p.kuaidiCode());
		}
		e.setCompanyId(companyIdInt);
		e.setDistributorId(p.distributorId());
		e.setSupplierId(supplierId);

		mapper.insert(e);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("id", e.getId());
		data.put("corp_id", p.corpId() != null ? p.corpId() : e.getCorpId());
		data.put("company_id", companyIdInt);
		data.put("corp_code", e.getCorpCode());
		data.put("kuaidi_code", e.getKuaidiCode());
		data.put("corp_name", e.getCorpName());
		data.put("distributor_id", e.getDistributorId());
		data.put("supplier_id", e.getSupplierId());
		return data;
	}
}
