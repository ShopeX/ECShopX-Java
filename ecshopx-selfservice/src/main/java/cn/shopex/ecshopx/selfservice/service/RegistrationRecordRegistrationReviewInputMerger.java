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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordRegistrationReviewCommand;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationRecordRegistrationReviewInputMerger {

	private final MessageSource messageSource;

	public RegistrationRecordRegistrationReviewInputMerger(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public RegistrationRecordRegistrationReviewCommand merge(HttpServletRequest request, JsonNode bodyRoot, Locale locale) {
		long recordId =
				mergeRequiredLongField(request, bodyRoot, "record_id", "selfservice.registration_record.record_id_required", locale);
		boolean approvalPassed = mergeRequiredApprovalPassed(request, bodyRoot, locale);
		String reason = mergeReason(request, bodyRoot);
		return new RegistrationRecordRegistrationReviewCommand(recordId, approvalPassed, reason);
	}

	private String mergeReason(HttpServletRequest request, JsonNode bodyRoot) {
		if (bodyRoot != null && bodyRoot.has("reason")) {
			JsonNode n = bodyRoot.get("reason");
			if (n == null || n.isNull()) {
				return "";
			}
			if (n.isTextual()) {
				return StringUtils.trimWhitespace(n.asText());
			}
			return StringUtils.trimWhitespace(n.asText(""));
		}
		String p = request.getParameter("reason");
		if (p == null) {
			return "";
		}
		return StringUtils.trimWhitespace(p);
	}

	private boolean mergeRequiredApprovalPassed(HttpServletRequest request, JsonNode bodyRoot, Locale locale) {
		JsonNode fromBody = bodyRoot != null && bodyRoot.has("status") ? bodyRoot.get("status") : null;
		if (fromBody != null) {
			if (isRequiredEmptyJsonValue(fromBody)) {
				throw new BadRequestException(
						messageSource.getMessage("selfservice.registration_record.approval_result_required", null, locale));
			}
			return parseApprovalPassedJson(fromBody);
		}
		if (!request.getParameterMap().containsKey("status")) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.registration_record.approval_result_required", null, locale));
		}
		String p = request.getParameter("status");
		if (p == null || !StringUtils.hasText(p.trim())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.registration_record.approval_result_required", null, locale));
		}
		return parseApprovalPassedString(p.trim());
	}

	private static boolean parseApprovalPassedJson(JsonNode n) {
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isTextual()) {
			return parseApprovalPassedString(n.asText().trim());
		}
		if (n.isNumber()) {
			return "true".equalsIgnoreCase(n.asText().trim());
		}
		return false;
	}

	private static boolean parseApprovalPassedString(String trimmed) {
		return "true".equalsIgnoreCase(trimmed);
	}

	private long mergeRequiredLongField(
			HttpServletRequest request, JsonNode bodyRoot, String field, String messageCode, Locale locale) {
		if (bodyRoot != null && bodyRoot.has(field)) {
			JsonNode n = bodyRoot.get(field);
			if (isRequiredEmptyJsonValue(n)) {
				throw new BadRequestException(messageSource.getMessage(messageCode, null, locale));
			}
			return intvalJsonNode(n);
		}
		if (!request.getParameterMap().containsKey(field)) {
			throw new BadRequestException(messageSource.getMessage(messageCode, null, locale));
		}
		String p = request.getParameter(field);
		if (p == null || p.isEmpty()) {
			throw new BadRequestException(messageSource.getMessage(messageCode, null, locale));
		}
		return LeadingNumberParser.parseAsLong(p);
	}

	private static boolean isRequiredEmptyJsonValue(JsonNode n) {
		if (n == null || n.isNull()) {
			return true;
		}
		if (n.isArray() && n.size() == 0) {
			return true;
		}
		if (n.isObject() && n.size() == 0) {
			return true;
		}
		if (n.isTextual()) {
			return !StringUtils.hasText(n.asText().trim());
		}
		return false;
	}

	private static long intvalJsonNode(JsonNode n) {
		if (n.isBoolean()) {
			return n.booleanValue() ? 1L : 0L;
		}
		if (n.isNumber()) {
			return (long) n.asDouble();
		}
		if (n.isTextual()) {
			return LeadingNumberParser.parseAsLong(n.asText());
		}
		if (n.isArray() || n.isObject()) {
			return 0L;
		}
		if (n.isBinary()) {
			return 0L;
		}
		return 0L;
	}
}
