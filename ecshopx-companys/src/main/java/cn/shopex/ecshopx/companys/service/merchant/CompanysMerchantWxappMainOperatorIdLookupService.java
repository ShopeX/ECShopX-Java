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

package cn.shopex.ecshopx.companys.service.merchant;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.merchant.port.MerchantWxappMainOperatorIdLookupPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CompanysMerchantWxappMainOperatorIdLookupService implements MerchantWxappMainOperatorIdLookupPort {

	private final OperatorsMapper operatorsMapper;

	public CompanysMerchantWxappMainOperatorIdLookupService(OperatorsMapper operatorsMapper) {
		this.operatorsMapper = operatorsMapper;
	}

	@Override
	public Optional<Long> findMainMerchantOperatorId(long companyId, long merchantId) {
		LambdaQueryWrapper<Operators> q = new LambdaQueryWrapper<>();
		q.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getOperatorType, "merchant")
				.eq(Operators::getMerchantId, merchantId);
		Operators row = operatorsMapper.selectOne(q);
		if (row == null || row.getOperatorId() == null || row.getOperatorId() <= 0) {
			return Optional.empty();
		}
		return Optional.of(row.getOperatorId());
	}
}
