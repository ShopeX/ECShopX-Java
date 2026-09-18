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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.members.service.email.MemberEmailActivationService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 邮箱注册成功后异步发送激活链接邮件 job 的处理者；payload 见
 * {@code SendMemberEmailActivationJobDispatchPublisherImpl}。
 */
@Component
public class SendMemberEmailActivationJobHandler implements DispatchHandler {

	private final MemberEmailActivationService memberEmailActivationService;

	public SendMemberEmailActivationJobHandler(MemberEmailActivationService memberEmailActivationService) {
		this.memberEmailActivationService = memberEmailActivationService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = longOf(payload.get("company_id"));
		String email = asString(payload.get("email"));
		String clientIp = asString(payload.get("client_ip"));
		String deviceId = asString(payload.get("device_id"));
		String base = asString(payload.get("activation_base_url"));
		memberEmailActivationService.sendActivationLinkEmail(companyId, email, clientIp, deviceId, base);
	}

	private static long longOf(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
