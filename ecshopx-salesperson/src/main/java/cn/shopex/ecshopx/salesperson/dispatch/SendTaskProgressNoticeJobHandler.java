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

package cn.shopex.ecshopx.salesperson.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskWorkWechatNoticeService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SendTaskProgressNoticeJobHandler implements DispatchHandler {

	private final SalespersonTaskWorkWechatNoticeService salespersonTaskWorkWechatNoticeService;

	public SendTaskProgressNoticeJobHandler(
			SalespersonTaskWorkWechatNoticeService salespersonTaskWorkWechatNoticeService) {
		this.salespersonTaskWorkWechatNoticeService = salespersonTaskWorkWechatNoticeService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload, "company_id");
		long taskId = extractLong(payload, "task_id");
		long salespersonId = extractLong(payload, "salesperson_id");
		if (companyId <= 0L || taskId <= 0L || salespersonId <= 0L) {
			return;
		}
		String username = extractOptionalString(payload, "username");
		salespersonTaskWorkWechatNoticeService.sendTaskProgressNotice(companyId, taskId, salespersonId, username);
	}

	private static long extractLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String extractOptionalString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw);
		return s.isEmpty() ? null : s;
	}
}
