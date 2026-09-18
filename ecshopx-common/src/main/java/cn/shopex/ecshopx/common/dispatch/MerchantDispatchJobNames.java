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

package cn.shopex.ecshopx.common.dispatch;

public final class MerchantDispatchJobNames {

	public static final String MERCHANT_AUDIT_SUCCESS_NOTICE =
			"job:1:MerchantBundle\\Jobs\\MerchantAuditSuccessNotice";

	public static final String MERCHANT_AUDIT_FAIL_NOTICE =
			"job:2:MerchantBundle\\Jobs\\MerchantAuditFailNotice";

	public static final String MERCHANT_ENTER_SUCCESS_NOTICE =
			"job:3:MerchantBundle\\Jobs\\MerchantEnterSuccessNotice";

	public static final String MERCHANT_RESET_PASSWORD_NOTICE =
			"job:4:MerchantBundle\\Jobs\\MerchantResetPasswordNotice";

	private MerchantDispatchJobNames() {
	}
}
