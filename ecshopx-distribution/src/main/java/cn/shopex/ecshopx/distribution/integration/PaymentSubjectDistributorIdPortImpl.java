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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class PaymentSubjectDistributorIdPortImpl implements PaymentSubjectDistributorIdPort {

	private final DistributorMapper distributorMapper;

	public PaymentSubjectDistributorIdPortImpl(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	@Override
	public long resolveActualDistributorId(long companyId, long distributorId) {
		if (distributorId == 0L) {
			return 0L;
		}
		Distributor distributor = distributorMapper.selectOne(
				new LambdaQueryWrapper<Distributor>()
						.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.last("LIMIT 1"));
		if (distributor == null) {
			return 0L;
		}
		Integer paymentSubject = distributor.getPaymentSubject();
		if (paymentSubject != null && paymentSubject == 1) {
			return distributorId;
		}
		return 0L;
	}
}
