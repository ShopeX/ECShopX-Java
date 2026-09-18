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

package cn.shopex.ecshopx.aliyunsms.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberMassSmsFanOutDispatchPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("adminMemberMassSmsFanOutDispatchPortImpl")
public class AdminMemberMassSmsFanOutDispatchPortImpl implements AdminMemberMassSmsFanOutDispatchPort {

	private final GroupSendSmsJobDispatchPublisher groupSendSmsJobDispatchPublisher;

	public AdminMemberMassSmsFanOutDispatchPortImpl(GroupSendSmsJobDispatchPublisher groupSendSmsJobDispatchPublisher) {
		this.groupSendSmsJobDispatchPublisher = groupSendSmsJobDispatchPublisher;
	}

	@Override
	public void dispatchFanOutAfterPersist(long companyId, List<String> mobilesOrdered, String smsContentPlainOrNull) {
		if (mobilesOrdered == null || mobilesOrdered.isEmpty()) {
			return;
		}
		List<String> phones = new ArrayList<>();
		for (String raw : mobilesOrdered) {
			if (raw == null) {
				continue;
			}
			String t = raw.trim();
			if (!t.isEmpty()) {
				phones.add(t);
			}
		}
		if (phones.isEmpty()) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("send_to_phones", phones);
		payload.put("sms_content", smsContentPlainOrNull == null ? "" : smsContentPlainOrNull);
		payload.put("operator", "管理员");
		payload.put("sender", "");
		payload.put("distributor_id", 0L);
		groupSendSmsJobDispatchPublisher.enqueueGroupSendSms(payload);
	}
}
