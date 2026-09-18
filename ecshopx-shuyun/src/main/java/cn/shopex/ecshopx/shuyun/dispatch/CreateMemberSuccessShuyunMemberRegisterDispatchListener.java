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

package cn.shopex.ecshopx.shuyun.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.shuyun.service.openplatform.MemberRegisterDispatchService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CreateMemberSuccessShuyunMemberRegisterDispatchListener implements DispatchListener {

	private final MemberRegisterDispatchService memberRegisterDispatchService;

	public CreateMemberSuccessShuyunMemberRegisterDispatchListener(
			MemberRegisterDispatchService memberRegisterDispatchService) {
		this.memberRegisterDispatchService = memberRegisterDispatchService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		long companyId = toLong(first(payload, "company_id", "companyId"));
		long userId = toLong(first(payload, "user_id", "userId"));
		long distributorId = toLong(first(payload, "distributor_id", "distributorId", "reg_distributor"));
		String mobile = first(payload, "mobile") == null ? "" : String.valueOf(first(payload, "mobile")).trim();
		memberRegisterDispatchService.dispatchCreateMemberSuccess(companyId, userId, distributorId, mobile);
	}

	private static Object first(Map<String, Object> payload, String... keys) {
		for (String k : keys) {
			if (payload != null && payload.containsKey(k) && payload.get(k) != null) {
				return payload.get(k);
			}
		}
		return null;
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0L;
		}
	}
}
