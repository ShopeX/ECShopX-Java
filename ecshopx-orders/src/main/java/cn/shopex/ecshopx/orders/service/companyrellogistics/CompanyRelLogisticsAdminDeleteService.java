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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CompanyRelLogisticsAdminDeleteService {

	private final CompanyRelLogisticsMapper mapper;
	private final CompanyRelLogisticsAdminCreateParamValidator validator;

	public CompanyRelLogisticsAdminDeleteService(
			CompanyRelLogisticsMapper mapper, CompanyRelLogisticsAdminCreateParamValidator validator) {
		this.mapper = mapper;
		this.validator = validator;
	}

	public void deleteCompanyLogistics(
			long companyId,
			String operatorType,
			Long operatorIdOrNull,
			String pathCorpIdRaw,
			int distributorId) {
		if (companyId > Integer.MAX_VALUE || companyId < Integer.MIN_VALUE) {
			throw new BadRequestException("company_id 无效");
		}
		Optional<Integer> corpOpt = validator.tryParseCorpIdForDeletePath(pathCorpIdRaw);
		if (corpOpt.isEmpty()) {
			return;
		}
		int corpId = corpOpt.get().intValue();
		long supplierId =
				Objects.equals(operatorType, "supplier") && operatorIdOrNull != null ? operatorIdOrNull.longValue() : 0L;
		int companyIdInt = (int) companyId;

		LambdaQueryWrapper<CompanyRelLogistics> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CompanyRelLogistics::getCorpId, corpId)
				.eq(CompanyRelLogistics::getCompanyId, companyIdInt)
				.eq(CompanyRelLogistics::getDistributorId, (long) distributorId)
				.eq(CompanyRelLogistics::getSupplierId, supplierId);
		mapper.delete(wrapper);
	}
}
