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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.SendMemberEmailActivationJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SendMemberEmailActivationJobDispatchPublisherImpl
		implements SendMemberEmailActivationJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public SendMemberEmailActivationJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(long companyId, String email, String clientIp, String deviceId, String activationBaseUrl) {
		Map<String, Object> payload = new LinkedHashMap<>(5);
		payload.put("company_id", companyId);
		payload.put("email", email);
		payload.put("client_ip", clientIp == null ? "" : clientIp);
		payload.put("device_id", deviceId == null ? "" : deviceId);
		payload.put("activation_base_url", activationBaseUrl == null ? "" : activationBaseUrl);
		dispatchFacade.dispatchJob(
				MembersBundleDispatchJobNames.SEND_MEMBER_EMAIL_ACTIVATION_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));
	}
}
