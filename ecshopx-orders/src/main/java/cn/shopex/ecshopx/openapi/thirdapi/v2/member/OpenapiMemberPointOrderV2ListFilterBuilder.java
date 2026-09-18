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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberPointOrderV2ListFilterBuilder {

	private final MemberAccountService memberAccountService;

	public OpenapiMemberPointOrderV2ListFilterBuilder(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	public record FilterSpec(
			long companyId,
			Long userId,
			int createTimeGte,
			Long orderId) {}

	public FilterSpec build(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean orderIdPresent,
			String orderIdRaw) {
		Long userId = null;
		if (mobilePresent) {
			Members member = null;
			if (mobileRaw != null && !mobileRaw.isBlank()) {
				member =
						memberAccountService.findMemberByCompanyAndMobile(
								companyId, mobileRaw.trim());
			}
			userId = member != null && member.getUserId() != null ? member.getUserId() : 0L;
		}

		int createTimeGte =
				(int) LocalDate.now()
						.withDayOfYear(1)
						.atStartOfDay(ZoneId.systemDefault())
						.toEpochSecond();

		Long orderId = null;
		if (orderIdPresent && !phpEmpty(orderIdRaw)) {
			try {
				orderId = Long.parseLong(orderIdRaw.trim());
			} catch (NumberFormatException e) {
				orderId = -1L;
			}
		}

		return new FilterSpec(companyId, userId, createTimeGte, orderId);
	}

	private static boolean phpEmpty(String raw) {
		return raw == null || raw.isEmpty();
	}
}
