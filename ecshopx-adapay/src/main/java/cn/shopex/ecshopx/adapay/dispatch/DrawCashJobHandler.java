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

package cn.shopex.ecshopx.adapay.dispatch;

import cn.shopex.ecshopx.adapay.service.AdapayDrawCashWithdrawService;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DrawCashJobHandler implements DispatchHandler {

	private final AdapayDrawCashWithdrawService adapayDrawCashWithdrawService;

	public DrawCashJobHandler(AdapayDrawCashWithdrawService adapayDrawCashWithdrawService) {
		this.adapayDrawCashWithdrawService = adapayDrawCashWithdrawService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = toLong(payload == null ? null : payload.get("company_id"));
		long memberId = toLong(payload == null ? null : payload.get("member_id"));
		String settleAccountId = settleId(payload == null ? null : payload.get("settle_account_id"));
		adapayDrawCashWithdrawService.runScheduledAutoDrawCash(companyId, memberId, settleAccountId);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String settleId(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o);
	}
}
