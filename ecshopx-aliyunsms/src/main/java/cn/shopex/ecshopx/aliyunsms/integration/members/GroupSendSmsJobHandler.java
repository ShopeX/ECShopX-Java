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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.members.admin.MemberBatchFanOutSmsOutboundPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GroupSendSmsJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(GroupSendSmsJobHandler.class);

	private final MemberBatchFanOutSmsOutboundPort memberBatchFanOutSmsOutboundPort;

	public GroupSendSmsJobHandler(
			@Qualifier("memberBatchFanOutSmsOutboundPortImpl")
					MemberBatchFanOutSmsOutboundPort memberBatchFanOutSmsOutboundPort) {
		this.memberBatchFanOutSmsOutboundPort = memberBatchFanOutSmsOutboundPort;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		String body = payload.get("sms_content") == null ? "" : String.valueOf(payload.get("sms_content"));
		List<String> mobiles = extractPhoneList(payload.get("send_to_phones"));
		int withMobile = 0;
		for (String m : mobiles) {
			if (!StringUtils.hasText(m)) {
				continue;
			}
			String trimmed = m.trim();
			withMobile++;
			try {
				memberBatchFanOutSmsOutboundPort.sendOne(companyId, trimmed, body);
			} catch (RuntimeException ex) {
				log.warn(
						"[group-sms] sendOne failed companyId={} mobileLen={} contentLen={}: {}",
						companyId,
						trimmed.length(),
						body.length(),
						ex.getMessage());
			}
		}
		log.info(
				"[group-sms] job handled companyId={} withMobile={} contentLen={}",
				companyId,
				withMobile,
				body.length());
	}

	private static long readLong(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static List<String> extractPhoneList(Object raw) {
		List<String> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof List<?> list) {
			for (Object el : list) {
				if (el != null) {
					out.add(String.valueOf(el));
				}
			}
			return out;
		}
		if (raw instanceof String[] arr) {
			for (String s : arr) {
				if (s != null) {
					out.add(s);
				}
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			for (Object el : arr) {
				if (el != null) {
					out.add(String.valueOf(el));
				}
			}
			return out;
		}
		return out;
	}
}
