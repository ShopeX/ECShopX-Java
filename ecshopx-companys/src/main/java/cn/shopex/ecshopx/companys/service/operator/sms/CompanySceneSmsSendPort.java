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

import java.util.Map;

/**
 * Sends template SMS for configured merchant scenes (non-verification flows).
 */
public interface CompanySceneSmsSendPort {

	/**
	 * Sends the dealer account password reset SMS for scene {@code dealer_account_reset_pwd}.
	 *
	 * @param dealerDisplayName value for template variable {@code dealer}
	 * @param plainPassword value for template variable {@code password}
	 */
	void sendDealerAccountResetPwd(
			long companyId, String mobile, String dealerDisplayName, String plainPassword);

	/**
	 * Sends SMS for a scene identified by {@code sceneTitle} (e.g. {@code registration_success_notice}), using
	 * variable keys aligned with the scene template configuration.
	 */
	void sendSceneTemplatedSms(long companyId, String mobilePlain, String sceneTitle, Map<String, String> variables);
}
