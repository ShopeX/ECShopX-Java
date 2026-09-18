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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRemindService {

	private static final Logger log = LoggerFactory.getLogger(AftersalesRemindService.class);

	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>", Pattern.DOTALL);

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AftersalesRemindService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> setRemind(LinkedHashMap<String, Object> merged, HttpServletRequest request) {
		mergeJwtCompanyId(request, merged, true);

		Object introObj = merged.get("intro");
		String introRaw = introObj == null ? "" : (introObj instanceof String s ? s : String.valueOf(introObj));
		String introTxt = stripHtmlTagsLoose(introRaw);

		if (introTxt.codePointCount(0, introTxt.length()) > 200) {
			throw new BadRequestException("售后内容最大长度不超过200个汉字");
		}

		Optional<String> isOpenOpt = resolveIsOpenWhenPresent(merged);

		if (isOpenOpt.isPresent() && "true".equals(isOpenOpt.get()) && !StringUtils.hasText(introTxt)) {
			throw new BadRequestException("售后内容必填");
		}

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(merged);
		if (!merged.containsKey("intro")) {
			payload.put("intro", introObj == null ? "" : introObj);
		}
		if (merged.containsKey("is_open")) {
			payload.put("is_open", isOpenOpt.orElseThrow());
		}

		String key = buildAftersalesRemindRedisKey(merged.get("company_id"));
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数序列化失败");
		}
		companysRedisTemplate.opsForValue().set(key, json);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	public Map<String, Object> getRemind(HttpServletRequest request, boolean allowOperatorFallback) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		mergeJwtCompanyId(request, merged, allowOperatorFallback);
		String key = buildAftersalesRemindRedisKey(merged.get("company_id"));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isEmpty()) {
			return defaultRemindStructure();
		}
		Map<String, Object> parsed;
		try {
			parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			log.warn("aftersales remind redis json parse failed", e);
			return defaultRemindStructure();
		}
		if (parsed == null || parsed.isEmpty()) {
			return defaultRemindStructure();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("intro", "");
		out.put("is_open", Boolean.FALSE);
		out.putAll(parsed);
		boolean isOpen = out.get("is_open") instanceof String s && "true".equals(s);
		out.put("is_open", isOpen);
		Object introObj = out.get("intro");
		String introStr = introObj == null ? "" : (introObj instanceof String s ? s : String.valueOf(introObj));
		out.put("intro", introStr);
		return out;
	}

	private static LinkedHashMap<String, Object> defaultRemindStructure() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("intro", "");
		out.put("is_open", Boolean.FALSE);
		return out;
	}

	private void mergeJwtCompanyId(
			HttpServletRequest request, LinkedHashMap<String, Object> merged, boolean allowOperatorFallback) {
		if (!allowOperatorFallback) {
			Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
			if (rawAuth == null) {
				rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
			}
			if (!(rawAuth instanceof Map<?, ?> frontMap)) {
				throw new UnauthorizedException("未登录");
			}
			merged.put("company_id", frontMap.get("company_id"));
			return;
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		merged.put("company_id", companyIdObj);
	}

	private static String buildAftersalesRemindRedisKey(Object companyIdRaw) {
		return "aftersalesRemind:" + (companyIdRaw == null ? "" : String.valueOf(companyIdRaw));
	}

	private static String stripHtmlTagsLoose(String raw) {
		if (raw == null) {
			return "";
		}
		return HTML_TAG_PATTERN.matcher(raw).replaceAll("");
	}

	private static Optional<String> resolveIsOpenWhenPresent(LinkedHashMap<String, Object> merged) {
		if (!merged.containsKey("is_open")) {
			return Optional.empty();
		}
		Object raw = merged.get("is_open");
		if (raw == null) {
			throw new BadRequestException("是否开启参数不正确");
		}
		if (raw instanceof Boolean b) {
			return Optional.of(b ? "true" : "false");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if ("true".equalsIgnoreCase(t)) {
				return Optional.of("true");
			}
			if ("false".equalsIgnoreCase(t)) {
				return Optional.of("false");
			}
			throw new BadRequestException("是否开启参数不正确");
		}
		throw new BadRequestException("是否开启参数不正确");
	}
}
