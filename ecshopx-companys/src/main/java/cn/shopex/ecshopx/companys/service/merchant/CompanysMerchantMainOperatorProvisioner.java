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
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.merchant.port.MerchantMainOperatorProvisioner;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysMerchantMainOperatorProvisioner implements MerchantMainOperatorProvisioner {

	private static final String OPERATOR_TYPE_MERCHANT = "merchant";

	private final OperatorsMapper operatorsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
	private final SecureRandom secureRandom = new SecureRandom();

	public CompanysMerchantMainOperatorProvisioner(
			OperatorsMapper operatorsMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public String createMainMerchantOperator(long companyId, String mobilePlain, String loginNamePlain, long merchantId) {
		if (!StringUtils.hasText(mobilePlain)) {
			throw new ResourceException("请填写手机号");
		}
		if (!StringUtils.hasText(loginNamePlain)) {
			throw new ResourceException("请填写账号名");
		}
		String encMobile = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		Map<String, Object> byMobile = operatorsMapper.getOperatorByMobile(encMobile, OPERATOR_TYPE_MERCHANT);
		if (byMobile != null && !byMobile.isEmpty()) {
			throw new ResourceException("该手机号已被使用");
		}
		Map<String, Object> byLogin = operatorsMapper.selectInfo(
				loginNamePlain.trim(), null, OPERATOR_TYPE_MERCHANT, null, null, null);
		if (byLogin != null && !byLogin.isEmpty()) {
			throw new ResourceException("该账号名已被使用");
		}
		String plainPassword = String.valueOf(100000 + secureRandom.nextInt(900000));
		int now = (int) (System.currentTimeMillis() / 1000L);
		Operators op = new Operators();
		op.setCompanyId(companyId);
		op.setMobile(encMobile);
		op.setLoginName(loginNamePlain.trim());
		op.setOperatorType(OPERATOR_TYPE_MERCHANT);
		op.setPassword(bcrypt.encode(plainPassword));
		op.setEid(null);
		op.setPassportUid(null);
		op.setCreated(now);
		op.setUpdated(now);
		op.setIsDisable(false);
		op.setIsDealerMain(false);
		op.setIsMerchantMain(true);
		op.setMerchantId(merchantId);
		op.setIsDistributorMain(false);
		operatorsMapper.insert(op);
		return plainPassword;
	}

	@Override
	public void createMainMerchantOperatorWithoutPassword(
			long companyId, String mobilePlain, String loginNamePlain, long merchantId) {
		if (!StringUtils.hasText(mobilePlain)) {
			throw new ResourceException("请填写手机号");
		}
		if (!StringUtils.hasText(loginNamePlain)) {
			throw new ResourceException("请填写账号名");
		}
		String encMobile = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		Map<String, Object> byMobile = operatorsMapper.getOperatorByMobile(encMobile, OPERATOR_TYPE_MERCHANT);
		if (byMobile != null && !byMobile.isEmpty()) {
			throw new ResourceException("该手机号已被使用");
		}
		Map<String, Object> byLogin = operatorsMapper.selectInfo(
				loginNamePlain.trim(), null, OPERATOR_TYPE_MERCHANT, null, null, null);
		if (byLogin != null && !byLogin.isEmpty()) {
			throw new ResourceException("该账号名已被使用");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Operators op = new Operators();
		op.setCompanyId(companyId);
		op.setMobile(encMobile);
		op.setLoginName(loginNamePlain.trim());
		op.setOperatorType(OPERATOR_TYPE_MERCHANT);
		op.setPassword(null);
		op.setEid(null);
		op.setPassportUid(null);
		op.setCreated(now);
		op.setUpdated(now);
		op.setIsDisable(false);
		op.setIsDealerMain(false);
		op.setIsMerchantMain(true);
		op.setMerchantId(merchantId);
		op.setIsDistributorMain(false);
		operatorsMapper.insert(op);
	}
}
