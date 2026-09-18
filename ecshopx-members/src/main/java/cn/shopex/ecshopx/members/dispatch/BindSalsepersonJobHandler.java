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
import cn.shopex.ecshopx.members.service.h5.bind.MemberSalespersonBindMarketingCoordinator;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BindSalsepersonJobHandler implements DispatchHandler {

	private final MemberSalespersonBindMarketingCoordinator memberSalespersonBindMarketingCoordinator;

	public BindSalsepersonJobHandler(MemberSalespersonBindMarketingCoordinator memberSalespersonBindMarketingCoordinator) {
		this.memberSalespersonBindMarketingCoordinator = memberSalespersonBindMarketingCoordinator;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		String unionid = trimToEmpty(payload.get("unionid"));
		String workUserid = trimToEmpty(payload.get("work_userid"));
		int customerType = readInt(payload.get("customer_type"), 0);
		String mobile = trimToEmpty(payload.get("mobile"));
		long userId = readLong(payload.get("user_id"), 0L);
		if (companyId <= 0L
				|| !StringUtils.hasText(unionid)
				|| !StringUtils.hasText(workUserid)
				|| customerType <= 0) {
			return;
		}
		memberSalespersonBindMarketingCoordinator.executeSalespersonBindForMember(
				companyId, unionid, workUserid, customerType, mobile, userId);
	}

	private static String trimToEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
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

	private static int readInt(Object raw, int defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
