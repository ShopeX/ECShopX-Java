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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorLoginMobileOccupancyPort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysMerchantOperatorLoginMobileOccupancyPort implements MerchantOperatorLoginMobileOccupancyPort {

	private static final String OPERATOR_TYPE_MERCHANT = "merchant";

	private final OperatorsMapper operatorsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public CompanysMerchantOperatorLoginMobileOccupancyPort(
			OperatorsMapper operatorsMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public void assertNotOccupiedByOtherMerchant(String mobilePlain, long excludeMerchantId) {
		if (!StringUtils.hasText(mobilePlain)) {
			return;
		}
		String encMobile = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		Map<String, Object> byMobile = operatorsMapper.getOperatorByMobile(encMobile, OPERATOR_TYPE_MERCHANT);
		if (byMobile == null || byMobile.isEmpty()) {
			return;
		}
		Object mid = byMobile.get("merchant_id");
		long merchantId = mid instanceof Number n ? n.longValue() : 0L;
		if (excludeMerchantId > 0 && merchantId == excludeMerchantId) {
			return;
		}
		throw new ResourceException("该手机号已被使用");
	}
}
