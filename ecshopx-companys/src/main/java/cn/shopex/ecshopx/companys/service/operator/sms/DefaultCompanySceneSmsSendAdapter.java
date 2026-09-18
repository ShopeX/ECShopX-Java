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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Fallback when no primary {@link CompanySceneSmsSendPort} is present. Full applications use the Aliyun-backed
 * adapter from {@code ecshopx-aliyunsms}.
 */
@Service
public class DefaultCompanySceneSmsSendAdapter implements CompanySceneSmsSendPort {

	@Override
	public void sendDealerAccountResetPwd(
			long companyId, String mobile, String dealerDisplayName, String plainPassword) {
		throw new ResourceException("短信通道未配置或不可用");
	}

	@Override
	public void sendSceneTemplatedSms(long companyId, String mobilePlain, String sceneTitle, Map<String, String> variables) {
		// no-op: full stack uses Aliyun-backed adapter when present
	}
}
