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

package cn.shopex.ecshopx.companys.service.operator.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fallback when no primary {@link CompanySmsSendPort} is present (e.g. tests without the Aliyun SMS module).
 * Full applications use the Aliyun-backed adapter from {@code ecshopx-aliyunsms}.
 */
@Service
public class DefaultCompanySmsSendAdapter implements CompanySmsSendPort {

	private static final Logger log = LoggerFactory.getLogger(DefaultCompanySmsSendAdapter.class);

	@Override
	public boolean sendVerificationCode(long companyId, String mobile, String verificationCode) {
		log.debug(
				"Company SMS skipped: fallback adapter (no primary channel) companyId={}",
				companyId);
		return false;
	}
}
