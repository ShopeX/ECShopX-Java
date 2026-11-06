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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantUpdateMerchantParamValidator {

	public void validateAndNormalizeForUpdate(Map<String, Object> params) {
		if (params.containsKey("contact_mobile")) {
			String cm = String.valueOf(params.get("contact_mobile")).trim();
			if (!MerchantCreateParamValidator.isCnMobile(cm)) {
				throw new ResourceException("手机号格式不正确，请确认后再重试");
			}
		}
		if (params.containsKey("legal_cert_id")) {
			requireSizeString(params.get("legal_cert_id"), 18, "身份证号码必须是18位");
		}
		if (params.containsKey("audit_goods")) {
			String agNorm = MerchantCreateParamNormalizer.normalizeScalarToString(params.get("audit_goods"));
			params.put("audit_goods", !"false".equals(agNorm));
		}
		if (params.containsKey("bank_acct_type") && params.containsKey("bank_mobile")) {
			String bankAcct = String.valueOf(params.get("bank_acct_type")).trim();
			if ("2".equals(bankAcct)) {
				String bm = String.valueOf(params.get("bank_mobile")).trim();
				if (StringUtils.hasText(bm) && !MerchantCreateParamValidator.isCnMobile(bm)) {
					throw new ResourceException("银行预留手机号格式不正确，请确认后再重试");
				}
			}
		}
	}

	private static void requireSizeString(Object v, int size, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.length() != size) {
			throw new ResourceException(message);
		}
	}
}
