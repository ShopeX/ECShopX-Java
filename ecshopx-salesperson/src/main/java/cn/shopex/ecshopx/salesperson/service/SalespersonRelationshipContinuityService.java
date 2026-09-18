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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.dispatch.SalespersonRelationshipContinuityJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalespersonRelationshipContinuityService {

	private final SalespersonRelationshipContinuityJobDispatchPublisher
			salespersonRelationshipContinuityJobDispatchPublisher;

	public Map<String, Object> relationshipContinuity(long companyId, Map<String, Object> input) {
		String eventType = rawString(input.get("event_type"));
		String eventId = rawString(input.get("event_id"));
		String userType = rawString(input.get("user_type"));
		String userId = rawString(input.get("user_id"));
		if (isMissingContinuityParam(eventType)
				|| isMissingContinuityParam(eventId)
				|| isMissingContinuityParam(userType)
				|| isMissingContinuityParam(userId)) {
			throw new BadRequestException("参数不正确");
		}
		LinkedHashMap<String, Object> jobPayload = new LinkedHashMap<>(input);
		jobPayload.put("company_id", companyId);
		salespersonRelationshipContinuityJobDispatchPublisher.publish(companyId, jobPayload);
		return Map.of("status", Boolean.TRUE);
	}

	private static String rawString(Object value) {
		return value == null ? "" : value.toString();
	}

	/**
	 * Empty string and the literal {@code "0"} count as absent; whitespace-only values do not.
	 */
	private static boolean isMissingContinuityParam(String value) {
		return value.isEmpty() || "0".equals(value);
	}
}
