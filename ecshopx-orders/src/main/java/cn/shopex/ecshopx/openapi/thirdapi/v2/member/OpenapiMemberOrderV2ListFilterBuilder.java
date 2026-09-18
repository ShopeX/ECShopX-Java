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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberOrderV2ListFilterBuilder {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final MemberAccountService memberAccountService;

	public OpenapiMemberOrderV2ListFilterBuilder(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	public record FilterSpec(
			long companyId,
			Long userId,
			int createTimeGte,
			Integer createTimeLte) {}

	public FilterSpec build(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean startDatePresent,
			String startDateRaw,
			boolean endDatePresent,
			String endDateRaw) {
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

		int defaultStart =
				(int) LocalDate.now()
						.withDayOfYear(1)
						.atStartOfDay(ZoneId.systemDefault())
						.toEpochSecond();

		int createTimeGte;
		if (startDatePresent && !phpEmpty(startDateRaw)) {
			createTimeGte = parseDateTimeToEpoch(startDateRaw);
		} else {
			createTimeGte = defaultStart;
		}

		Integer createTimeLte = null;
		if (endDatePresent && !phpEmpty(endDateRaw)) {
			createTimeLte = parseDateTimeToEpoch(endDateRaw);
		}

		return new FilterSpec(companyId, userId, createTimeGte, createTimeLte);
	}

	private static int parseDateTimeToEpoch(String raw) {
		String t = raw.trim();
		if (t.matches("\\d+")) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR, "时间格式有误");
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(t, DATETIME_FMT);
			return (int) ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException e) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR, "时间格式有误");
		}
	}

	private static boolean phpEmpty(String raw) {
		return raw == null || raw.isEmpty() || "0".equals(raw);
	}
}
