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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Sends company verification SMS through the PHP-aligned channel router.
 */
@Service
@Primary
public class AliyunCompanyVerificationSmsSendAdapter implements CompanySmsSendPort {

	private static final String SCENE_TITLE_VERIFICATION = "verification_code";

	private final CompanySmsChannelRouter companySmsChannelRouter;

	public AliyunCompanyVerificationSmsSendAdapter(CompanySmsChannelRouter companySmsChannelRouter) {
		this.companySmsChannelRouter = companySmsChannelRouter;
	}

	@Override
	public boolean sendVerificationCode(long companyId, String mobile, String verificationCode) {
		return sendVerificationCodeForScene(companyId, mobile, verificationCode, SCENE_TITLE_VERIFICATION);
	}

	@Override
	public boolean sendVerificationCodeForScene(
			long companyId, String mobile, String verificationCode, String sceneTitle) {
		String resolvedSceneTitle =
				sceneTitle == null || sceneTitle.isBlank() ? SCENE_TITLE_VERIFICATION : sceneTitle.trim();
		return companySmsChannelRouter.send(
				companyId,
				mobile,
				resolvedSceneTitle,
				Map.of("code", verificationCode));
	}

	@Override
	public boolean sendVerificationCode(
			long companyId, String mobile, String verificationCode, String forgetSmsExactBody) {
		return companySmsChannelRouter.send(
				companyId,
				mobile,
				SCENE_TITLE_VERIFICATION,
				Map.of("code", verificationCode),
				forgetSmsExactBody);
	}
}
