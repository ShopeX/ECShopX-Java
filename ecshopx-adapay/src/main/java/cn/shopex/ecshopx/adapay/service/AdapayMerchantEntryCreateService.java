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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMerchantEntryCreateService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^.-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^.-]+)*@"
					+ "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	public AdapayMerchantEntryCreateService(AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader) {
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
	}

	public void createOrThrowDisabled(long companyId, long operatorId, Map<String, String> params) {
		Map<String, String> p = new LinkedHashMap<>(params);

		requirePhone11(p, "usr_phone", "注册手机号格式不正确");
		requireText(p, "cont_name", "联系人姓名必填");
		requirePhone11(p, "cont_phone", "联系人手机号码格式不正确");
		requireEmail(p, "customer_email", "电子邮箱格式不正确");
		requireText(p, "mer_name", "商户名必填");
		requireText(p, "mer_short_name", "商户名简称必填");
		requireText(p, "reg_addr", "注册地址必填");
		requireText(p, "cust_addr", "经营地址必填");
		requireText(p, "cust_tel", "商户电话必填");
		requireText(p, "legal_name", "法人/负责人 姓名 必填");
		requireLegalIdno(p);
		requirePhone11(p, "legal_mp", "法人/负责人手机号格式不正确");
		requireText(p, "legal_start_cert_id_expires", "法人/负责人身份证有效期（始）必填");
		requireText(p, "legal_id_expires", "法人/负责人身份证有效期（至）必填");
		requireText(p, "card_id_mask", "结算银行卡号必填");
		requireText(p, "bank_code", "结算银行卡所属银行code必填");
		requireText(p, "card_name", "结算银行卡开户姓名必填");
		requireText(p, "bank_acct_type", "结算银行账户类型必填");
		requireText(p, "prov_code", "结算银行卡省份编码必填");
		requireText(p, "area_code", "结算银行卡地区编码必填");
		requireText(p, "is_sms", "是否短信提醒必传");

		p.put("entry_mer_type", "1");

		if (!StringUtils.hasText(p.get("license_code"))) {
			throw new BadRequestException("营业执照编码 企业时必填");
		}
		if (!StringUtils.hasText(p.get("mer_start_valid_date")) || !StringUtils.hasText(p.get("mer_valid_date"))) {
			throw new BadRequestException("商户有效日期 企业时必填");
		}

		if ("2".equals(p.get("entry_mer_type")) && "1".equals(p.get("bank_acct_type"))) {
			throw new ResourceException("结算银行账户类型 小微只能是对私");
		}

		long epochSeconds = Instant.now().getEpochSecond();
		String requestId = "merchant_entry_" + companyId + "_" + epochSeconds;
		Map<String, String> assembly = new LinkedHashMap<>();
		assembly.put("company_id", String.valueOf(companyId));
		assembly.put("legal_type", "0");
		assembly.put("request_id", requestId);
		assembly.put("operator_id", String.valueOf(operatorId));

		adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		throw new ResourceException("暂不支持开户流程");
	}

	private static void requireText(Map<String, String> p, String key, String message) {
		if (!StringUtils.hasText(p.get(key))) {
			throw new BadRequestException(message);
		}
	}

	private static void requirePhone11(Map<String, String> p, String key, String message) {
		String v = p.get(key);
		if (!StringUtils.hasText(v) || v.length() != 11 || !allDigits(v)) {
			throw new BadRequestException(message);
		}
	}

	private static boolean allDigits(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static void requireEmail(Map<String, String> p, String key, String message) {
		String v = p.get(key);
		if (!StringUtils.hasText(v) || !EMAIL_PATTERN.matcher(v.trim()).matches()) {
			throw new BadRequestException(message);
		}
	}

	private static void requireLegalIdno(Map<String, String> p) {
		String v = p.get("legal_idno");
		if (!StringUtils.hasText(v) || !LEGAL_CERT_PATTERN.matcher(v.trim()).matches()) {
			throw new BadRequestException("法人/负责人证件号码格式不正确");
		}
	}
}
