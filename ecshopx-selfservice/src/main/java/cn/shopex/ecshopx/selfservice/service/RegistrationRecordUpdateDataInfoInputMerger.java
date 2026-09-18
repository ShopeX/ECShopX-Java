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
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordUpdateDataInfoCommand;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationRecordUpdateDataInfoInputMerger {

	private final MessageSource messageSource;

	public RegistrationRecordUpdateDataInfoInputMerger(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public RegistrationRecordUpdateDataInfoCommand merge(HttpServletRequest request, JsonNode bodyRoot, Locale locale) {
		long recordId;
		if (bodyRoot != null && bodyRoot.has("record_id")) {
			JsonNode n = bodyRoot.get("record_id");
			if (isRequiredEmptyJsonRecordId(n)) {
				throw new BadRequestException(
						messageSource.getMessage("selfservice.registration_record.record_id_required", null, locale));
			}
			recordId = intvalJsonNode(n);
		} else {
			if (!request.getParameterMap().containsKey("record_id")) {
				throw new BadRequestException(
						messageSource.getMessage("selfservice.registration_record.record_id_required", null, locale));
			}
			String p = request.getParameter("record_id");
			if (p == null || p.isEmpty()) {
				throw new BadRequestException(
						messageSource.getMessage("selfservice.registration_record.record_id_required", null, locale));
			}
			recordId = LeadingNumberParser.parseAsLong(p);
		}

		boolean writeRemark;
		String remarkValue = "";
		String contentType = request.getContentType();
		boolean jsonLike =
				contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json");
		if (jsonLike && bodyRoot != null) {
			writeRemark = bodyRoot.has("remark") && !bodyRoot.get("remark").isNull();
			if (writeRemark) {
				JsonNode remarkNode = bodyRoot.get("remark");
				if (remarkNode.isObject() || remarkNode.isArray()) {
					remarkValue = remarkNode.toString();
				} else {
					remarkValue = remarkNode.asText();
				}
			}
		} else {
			writeRemark = request.getParameterMap().containsKey("remark");
			if (writeRemark) {
				remarkValue = Objects.requireNonNullElse(request.getParameter("remark"), "");
			}
		}

		return new RegistrationRecordUpdateDataInfoCommand(recordId, writeRemark, remarkValue);
	}

	private static boolean isRequiredEmptyJsonRecordId(JsonNode n) {
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
