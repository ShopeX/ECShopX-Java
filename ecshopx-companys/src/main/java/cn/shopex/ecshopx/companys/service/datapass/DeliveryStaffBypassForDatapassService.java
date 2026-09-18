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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeliveryStaffBypassForDatapassService {

	private final OperatorsMapper operatorsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public DeliveryStaffBypassForDatapassService(
			OperatorsMapper operatorsMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public boolean existsSelfDeliveryStaffForMobile(long companyId, String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return false;
		}
		String enc = sensitiveFieldEncryptor.encrypt(mobile.trim());
		Long cnt = operatorsMapper.selectCount(new LambdaQueryWrapper<Operators>()
				.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getMobile, enc)
				.eq(Operators::getOperatorType, "self_delivery_staff"));
		return cnt != null && cnt > 0;
	}
}
