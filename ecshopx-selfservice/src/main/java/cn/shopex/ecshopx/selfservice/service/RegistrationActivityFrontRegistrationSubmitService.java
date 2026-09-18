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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontSubmitMessageKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontRegistrationSubmitService {

	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789]\\d{9}$");

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final ObjectMapper contentUnicodeJsonMapper;
	private final RegistrationActivityFrontValidityService registrationActivityFrontValidityService;
	private final RegistrationRecordFrontSaveRecordService registrationRecordFrontSaveRecordService;
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final MessageSource messageSource;

	public RegistrationActivityFrontRegistrationSubmitService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			RegistrationActivityFrontValidityService registrationActivityFrontValidityService,
			RegistrationRecordFrontSaveRecordService registrationRecordFrontSaveRecordService,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			MessageSource messageSource) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.contentUnicodeJsonMapper = objectMapper.copy();
		this.contentUnicodeJsonMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
		this.registrationActivityFrontValidityService = registrationActivityFrontValidityService;
		this.registrationRecordFrontSaveRecordService = registrationRecordFrontSaveRecordService;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.messageSource = messageSource;
	}

	public Map<String, Object> joinActivity(
			Locale locale,
			long userId,
			long companyId,
			String authMobilePlain,
			String wxappAppid,
			String openId,
			long activityId,
			long distributorId) {
		String redisKey = "joinActivity:" + userId;
		Boolean first = companysRedisTemplate.opsForValue().setIfAbsent(redisKey, "1");
		if (Boolean.TRUE.equals(first)) {
			companysRedisTemplate.expire(redisKey, Duration.ofSeconds(3));
		} else {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.JOIN_ACTIVITY_TOO_POPULAR_SHORT, null, locale));
		}

		RegistrationActivity activity =
				registrationActivityFrontValidityService.checkActivityValid(
						userId, companyId, activityId, 0L, distributorId, locale);

		if (activity.getTempId() != null && activity.getTempId() != 0L) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.JOIN_ACTIVITY_PLEASE_FILL_FORM, null, locale));
		}

		RegistrationRecord record =
				registrationRecordFrontSaveRecordService.saveRecord(
						userId,
						companyId,
						wxappAppid,
						openId,
						authMobilePlain,
						authMobilePlain,
						"",
						distributorId,
						0L,
						"",
						activity,
						locale);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("record_data", registrationRecordRowMapAssembler.toApiRow(record));
		out.put("activity_info", registrationActivityFrontValidityService.toActivitySubmitInfoMap(activity));
		return out;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> registrationSubmit(
			HttpServletRequest request,
			Locale locale,
			long userId,
			long companyId,
			String authMobilePlain,
			String wxappAppid,
			String openId,
			long activityId,
			long recordId,
			long distributorId,
			String trueName,
			Object formdataContentRaw) {
		String redisKey = "registrationSubmit:" + userId;
		Boolean rateOk = companysRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(3));
		if (!Boolean.TRUE.equals(rateOk)) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_TOO_POPULAR, null, locale));
		}
		return registrationSubmitCore(
				locale,
				userId,
				companyId,
				authMobilePlain,
				wxappAppid,
				openId,
				activityId,
				recordId,
				distributorId,
				trueName,
				formdataContentRaw);
	}

	private Map<String, Object> registrationSubmitCore(
			Locale locale,
			long userId,
			long companyId,
			String authMobilePlain,
			String wxappAppid,
			String openId,
			long activityId,
			long recordId,
			long distributorId,
			String trueName,
			Object formdataContentRaw) {
		RegistrationActivity activity =
				registrationActivityFrontValidityService.checkActivityValid(
						userId, companyId, activityId, recordId, distributorId, locale);

		List<Map<String, Object>> contentList = parseTopLevelContent(formdataContentRaw, locale);

		String workingTrueName = trueName == null ? "" : trueName;
		String formMobilePlain = authMobilePlain == null ? "" : authMobilePlain.trim();

		for (int key = 0; key < contentList.size(); key++) {
			Map<String, Object> card = contentList.get(key);
			Object formdataObj = card.get("formdata");
			if (formdataObj == null || !(formdataObj instanceof List<?> rawFormdata)) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> formdataList = (List<Map<String, Object>>) (List<?>) rawFormdata;
			for (int k = 0; k < formdataList.size(); k++) {
				Object rowObj = formdataList.get(k);
				if (!(rowObj instanceof Map<?, ?> rowMap)) {
					throw new ResourceException(
							messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> value = (Map<String, Object>) rowMap;
				Object answer = value.get("answer");
				if ("true".equals(String.valueOf(value.get("is_required")).trim()) && isAnswerEmpty(answer)) {
					String fieldTitle = Objects.toString(value.get("field_title"), "");
					Object cardTitleObj = card.get("title");
					boolean hasCardTitle = !isAnswerEmpty(cardTitleObj);
					if (hasCardTitle) {
						String cardTitle = Objects.toString(cardTitleObj, "").trim();
						throw new ResourceException(
								messageSource.getMessage(
										RegistrationActivityFrontSubmitMessageKeys.FIELD_REQUIRED_WITH_CARD,
										new Object[] {cardTitle, fieldTitle},
										locale));
					}
					throw new ResourceException(
							messageSource.getMessage(
									RegistrationActivityFrontSubmitMessageKeys.FIELD_REQUIRED,
									new Object[] {fieldTitle},
									locale));
				}
				if (!isAnswerEmpty(answer) && answer instanceof Collection<?> ac) {
					String joined = ac.stream().map(Objects::toString).collect(Collectors.joining(","));
					value.put("answer", joined);
				}
				if (!StringUtils.hasText(workingTrueName) && "username".equals(value.get("field_name"))) {
					workingTrueName = Objects.toString(value.get("answer"), "");
				}
				Object answerForScalar = value.get("answer");
				if ("mobile".equals(value.get("field_name")) && !isAnswerEmpty(answerForScalar)) {
					String mobileStr = Objects.toString(value.get("answer"), "");
					if (!MOBILE_CN.matcher(mobileStr).matches()) {
						throw new ResourceException(
								messageSource.getMessage(
										RegistrationActivityFrontSubmitMessageKeys.PLEASE_ENTER_CORRECT_MOBILE, null, locale));
					}
					formMobilePlain = mobileStr;
				}
				formdataList.set(k, value);
			}
			card.put("formdata", formdataList);
			contentList.set(key, card);
		}

		String contentJson;
		try {
			contentJson = contentUnicodeJsonMapper.writeValueAsString(contentList);
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
		}

		RegistrationRecord record =
				registrationRecordFrontSaveRecordService.saveRecord(
						userId,
						companyId,
						wxappAppid,
						openId,
						authMobilePlain,
						formMobilePlain,
						workingTrueName,
						distributorId,
						recordId,
						contentJson,
						activity,
						locale);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("record_data", registrationRecordRowMapAssembler.toApiRow(record));
		out.put("activity_info", registrationActivityFrontValidityService.toActivitySubmitInfoMap(activity));
		return out;
	}

	private List<Map<String, Object>> parseTopLevelContent(Object formdataContentRaw, Locale locale) {
		if (formdataContentRaw == null) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_EMPTY, null, locale));
		}
		if (formdataContentRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_EMPTY, null, locale));
			}
			try {
				JsonNode root = objectMapper.readTree(t);
				if (!root.isArray()) {
					throw new ResourceException(
							messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
				}
				return jsonNodeArrayToContentList(root, locale);
			} catch (JsonProcessingException e) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
			}
		}
		if (formdataContentRaw instanceof Collection<?> col) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : col) {
				if (!(o instanceof Map<?, ?>)) {
					throw new ResourceException(
							messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
				}
				Map<String, Object> card =
						objectMapper.convertValue(o, new TypeReference<LinkedHashMap<String, Object>>() {});
				out.add(card);
			}
			validateContentListShape(out, locale);
			return out;
		}
		throw new ResourceException(
				messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
	}

	private List<Map<String, Object>> jsonNodeArrayToContentList(JsonNode root, Locale locale) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode el : root) {
			if (!el.isObject()) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
			}
			Map<String, Object> card =
					objectMapper.convertValue(el, new TypeReference<LinkedHashMap<String, Object>>() {});
			out.add(card);
		}
		validateContentListShape(out, locale);
		return out;
	}

	private void validateContentListShape(List<Map<String, Object>> contentList, Locale locale) {
		for (Map<String, Object> card : contentList) {
			Object formdataObj = card.get("formdata");
			if (formdataObj == null || !(formdataObj instanceof List<?>)) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.REGISTRATION_DATA_INVALID, null, locale));
			}
		}
	}

	/**
	 * 判定表单答案是否视为「空」：null、false、空串、单字符 {@code '0'}、数值 0、空集合/空 Map/长度为 0 的数组视为空；
	 * 字符串与其它 CharSequence 不做 trim；不递归检查集合或 Map 内元素。
	 */
	private static boolean isAnswerEmpty(Object answer) {
		if (answer == null) {
			return true;
		}
		if (answer instanceof Boolean b) {
			return !b;
		}
		if (answer instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (answer instanceof CharSequence cs) {
			int len = cs.length();
			return len == 0 || (len == 1 && cs.charAt(0) == '0');
		}
		if (answer instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (answer instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (answer instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (answer instanceof Object[] arr) {
			return arr.length == 0;
		}
		return false;
	}
}
