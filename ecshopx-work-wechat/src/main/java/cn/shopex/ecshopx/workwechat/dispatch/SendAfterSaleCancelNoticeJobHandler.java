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

package cn.shopex.ecshopx.workwechat.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SendAfterSaleCancelNoticeJobHandler implements DispatchHandler {

	private final SendAfterSaleCancelNoticeWorker worker;

	public SendAfterSaleCancelNoticeJobHandler(SendAfterSaleCancelNoticeWorker worker) {
		this.worker = worker;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		long aftersalesBn = extractLong(payload.get("aftersales_bn"));
		boolean ok = worker.execute(companyId, aftersalesBn);
		if (!ok) {
			log.debug("aftersale-cancel notice skipped companyId={} aftersalesBn={}", companyId, aftersalesBn);
		}
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
