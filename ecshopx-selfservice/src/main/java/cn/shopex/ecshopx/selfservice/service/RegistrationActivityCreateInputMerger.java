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
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationActivityCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationActivityCreateInputMerger {

	private static final String[] JSON_QUERY_KEYS = {
		"activity_id",
		"temp_id",
		"activity_name",
		"start_time",
		"end_time",
		"join_limit",
		"is_sms_notice",
		"is_wxapp_notice",
		"area",
		"place",
		"address",
		"intro",
		"show_fields",
		"pics",
		"gift_points",
		"is_allow_duplicate",
		"is_allow_cancel",
		"is_offline_verify",
		"is_need_check",
		"is_white_list",
		"enterprise_ids",
		"group_no",
		"member_level",
		"distributor_ids",
		"join_tips",
		"submit_form_tips",
		"content",
		"distributor_id"
	};

	private final ObjectMapper objectMapper;

	public RegistrationActivityCreateInputMerger(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void mergeQueryIntoBody(HttpServletRequest request, RegistrationActivityCreateRequest body) {
		String ct = request.getContentType();
		boolean isJson = ct != null && ct.toLowerCase().contains("application/json");
		if (isJson) {
			mergeJsonQuery(request, body);
		} else {
			mergeFormStyleQuery(request, body);
		}
	}

	private void mergeJsonQuery(HttpServletRequest request, RegistrationActivityCreateRequest body) {
		for (String key : JSON_QUERY_KEYS) {
			String q = request.getParameter(key);
			if (!StringUtils.hasText(q)) {
				continue;
			}
			switch (key) {
				case "activity_id" -> {
					if (body.getActivityId() == null) {
						body.setActivityId(parseLongParam(q, key));
					}
				}
				case "temp_id" -> {
					if (body.getTempId() == null) {
						body.setTempId(parseLongParam(q, key));
					}
				}
				case "activity_name" -> {
					if (!StringUtils.hasText(body.getActivityName())) {
						body.setActivityName(q);
					}
				}
				case "start_time" -> {
					if (body.getStartTimeRaw() == null) {
						body.setStartTimeRaw(q);
					}
				}
				case "end_time" -> {
					if (body.getEndTimeRaw() == null) {
						body.setEndTimeRaw(q);
					}
				}
				case "join_limit" -> {
					if (body.getJoinLimit() == null) {
						body.setJoinLimit(parseIntParam(q, key));
					}
				}
				case "is_sms_notice" -> {
					if (body.getIsSmsNotice() == null) {
						body.setIsSmsNotice(q);
					}
				}
				case "is_wxapp_notice" -> {
					if (body.getIsWxappNotice() == null) {
						body.setIsWxappNotice(q);
					}
				}
				case "area" -> {
					if (body.getArea() == null) {
						body.setArea(readJsonTreeOrThrow(q));
					}
				}
				case "place" -> {
					if (!StringUtils.hasText(body.getPlace())) {
						body.setPlace(q);
					}
				}
				case "address" -> {
					if (!StringUtils.hasText(body.getAddress())) {
						body.setAddress(q);
					}
				}
				case "intro" -> {
					if (!StringUtils.hasText(body.getIntro())) {
						body.setIntro(q);
					}
				}
				case "show_fields" -> {
					if (body.getShowFields() == null) {
						body.setShowFields(readJsonTreeOrThrow(q));
					}
				}
				case "pics" -> {
					if (body.getPics() == null) {
						body.setPics(readJsonTreeOrThrow(q));
					}
				}
				case "gift_points" -> {
					if (body.getGiftPoints() == null) {
						body.setGiftPoints(parseIntParam(q, key));
					}
				}
				case "is_allow_duplicate" -> {
					if (body.getIsAllowDuplicate() == null) {
						body.setIsAllowDuplicate(q);
					}
				}
				case "is_allow_cancel" -> {
					if (body.getIsAllowCancel() == null) {
						body.setIsAllowCancel(q);
					}
				}
				case "is_offline_verify" -> {
					if (body.getIsOfflineVerify() == null) {
						body.setIsOfflineVerify(q);
					}
				}
				case "is_need_check" -> {
					if (body.getIsNeedCheck() == null) {
						body.setIsNeedCheck(q);
					}
				}
				case "is_white_list" -> {
					if (body.getIsWhiteList() == null) {
						body.setIsWhiteList(q);
					}
				}
				case "enterprise_ids" -> {
					if (body.getEnterpriseIds() == null) {
						body.setEnterpriseIds(readJsonTreeOrThrow(q));
					}
				}
				case "group_no" -> {
					if (!StringUtils.hasText(body.getGroupNo())) {
						body.setGroupNo(q);
					}
				}
				case "member_level" -> {
					if (body.getMemberLevel() == null) {
						body.setMemberLevel(readJsonTreeOrThrow(q));
					}
				}
				case "distributor_ids" -> {
					if (body.getDistributorIds() == null) {
						body.setDistributorIds(readJsonTreeOrThrow(q));
					}
				}
				case "join_tips" -> {
					if (!StringUtils.hasText(body.getJoinTips())) {
						body.setJoinTips(q);
					}
				}
				case "submit_form_tips" -> {
					if (!StringUtils.hasText(body.getSubmitFormTips())) {
						body.setSubmitFormTips(q);
					}
				}
				case "content" -> {
					if (!StringUtils.hasText(body.getContent())) {
						body.setContent(q);
					}
				}
				case "distributor_id" -> {
					if (body.getDistributorId() == null) {
						body.setDistributorId(parseLongParam(q, key));
					}
				}
				default -> {
					// no-op
				}
			}
		}
	}

	private JsonNode readJsonTreeOrThrow(String q) {
		try {
			return objectMapper.readTree(q);
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("参数 JSON 无效");
		}
	}

	private int parseIntParam(String q, String key) {
		try {
			return Integer.parseInt(q.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(key + " 无效");
		}
	}

	private long parseLongParam(String q, String key) {
		try {
			return Long.parseLong(q.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(key + " 无效");
		}
	}

	private void mergeFormStyleQuery(HttpServletRequest request, RegistrationActivityCreateRequest body) {
		Map<String, Object> flat = FlexibleHttpServletParameterMap.toObjectMap(request);
		if (body.getActivityId() == null) {
			Object raw = flat.get("activity_id");
			if (raw instanceof Number n) {
				body.setActivityId(n.longValue());
			} else if (raw instanceof String s && StringUtils.hasText(s)) {
				body.setActivityId(parseLongParam(s, "activity_id"));
			}
		}
		fillObjectIfNull(body::getArea, body::setArea, flat, "area");
		fillObjectIfNull(body::getShowFields, body::setShowFields, flat, "show_fields");
		fillObjectIfNull(body::getPics, body::setPics, flat, "pics");
		fillObjectIfNull(body::getMemberLevel, body::setMemberLevel, flat, "member_level");
		fillObjectIfNull(body::getEnterpriseIds, body::setEnterpriseIds, flat, "enterprise_ids");
		fillObjectIfNull(body::getDistributorIds, body::setDistributorIds, flat, "distributor_ids");
	}

	private void fillObjectIfNull(
			java.util.function.Supplier<Object> getter,
			java.util.function.Consumer<Object> setter,
			Map<String, Object> flat,
			String key) {
		if (getter.get() != null) {
			return;
		}
		Object raw = flat.get(key);
		if (raw == null) {
			return;
		}
		try {
			setter.accept(objectMapper.convertValue(raw, Object.class));
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("参数 JSON 无效");
		}
	}
}
