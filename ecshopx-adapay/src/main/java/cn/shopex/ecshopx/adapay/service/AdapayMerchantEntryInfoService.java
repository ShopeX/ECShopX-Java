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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdapayMerchantEntryInfoService {

	private final AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdapayMerchantEntryInfoService(
			AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.adapayMerchantEntryInfoLoader = adapayMerchantEntryInfoLoader;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Object merchantEntryInfo(long companyId) {
		Optional<AdapayMerchantEntryInfoContext> ctxOpt = adapayMerchantEntryInfoLoader.load(companyId);
		if (ctxOpt.isEmpty()) {
			return Collections.emptyList();
		}

		AdapayMerchantEntryInfoContext ctx = ctxOpt.get();
		AdapayMerchantEntry e = ctx.entry();
		String bankName = ctx.bankName();
		String provName = ctx.provName();
		String areaName = ctx.areaName();

		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("request_id", e.getRequestId());
		row.put("usr_phone", sensitiveFieldEncryptor.decrypt(e.getUsrPhone()));
		row.put("cont_name", sensitiveFieldEncryptor.decrypt(e.getContName()));
		row.put("cont_phone", sensitiveFieldEncryptor.decrypt(e.getContPhone()));
		row.put("customer_email", e.getCustomerEmail());
		row.put("mer_name", e.getMerName());
		row.put("mer_short_name", e.getMerShortName());
		row.put("license_code", e.getLicenseCode());
		row.put("reg_addr", e.getRegAddr());
		row.put("cust_addr", e.getCustAddr());
		row.put("cust_tel", sensitiveFieldEncryptor.decrypt(e.getCustTel()));
		row.put("mer_start_valid_date", e.getMerStartValidDate());
		row.put("mer_valid_date", e.getMerValidDate());
		row.put("legal_name", sensitiveFieldEncryptor.decrypt(e.getLegalName()));
		row.put("legal_type", e.getLegalType());
		row.put("legal_idno", sensitiveFieldEncryptor.decrypt(e.getLegalIdno()));
		row.put("legal_mp", sensitiveFieldEncryptor.decrypt(e.getLegalMp()));
		row.put("legal_start_cert_id_expires", e.getLegalStartCertIdExpires());
		row.put("legal_id_expires", e.getLegalIdExpires());
		row.put("card_id_mask", sensitiveFieldEncryptor.decrypt(e.getCardIdMask()));
		row.put("bank_code", e.getBankCode());
		row.put("card_name", sensitiveFieldEncryptor.decrypt(e.getCardName()));
		row.put("bank_acct_type", e.getBankAcctType());
		row.put("prov_code", e.getProvCode());
		row.put("area_code", e.getAreaCode());
		row.put("rsa_public_key", e.getRsaPublicKey());
		row.put("entry_mer_type", e.getEntryMerType());
		row.put("test_api_key", e.getTestApiKey());
		row.put("live_api_key", e.getLiveApiKey());
		row.put("login_pwd", e.getLoginPwd());
		row.put("app_id_list", e.getAppIdList());
		row.put("sign_view_url", e.getSignViewUrl());
		row.put("is_sms", e.getIsSms());
		row.put("status", e.getStatus());
		row.put("error_msg", e.getErrorMsg());
		row.put("create_time", e.getCreateTime());
		row.put("update_time", e.getUpdateTime());
		row.put("bank_name", bankName);
		row.put("prov_name", provName);
		row.put("area_name", areaName);
		return row;
	}
}
