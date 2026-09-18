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
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationActivityDeleteService {

	private final RegistrationActivityMapper registrationActivityMapper;

	public RegistrationActivityDeleteService(RegistrationActivityMapper registrationActivityMapper) {
		this.registrationActivityMapper = registrationActivityMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Object deleteData(HttpServletRequest httpRequest, Map<String, Object> body) {
		Object raw = resolveActivityIdRaw(httpRequest, body);
		if (isLooseFalsyActivityIdRaw(raw)) {
			return Collections.emptyList();
		}
		long activityId = normalizeToActivityId(raw);
		try {
			registrationActivityMapper.deleteById(activityId);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("无法删除该活动");
		}
		Map<String, Object> out = new LinkedHashMap<>(1);
		out.put("status", Boolean.TRUE);
		return out;
	}

	private static Object resolveActivityIdRaw(HttpServletRequest httpRequest, Map<String, Object> body) {
		String ct = httpRequest.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase(Locale.ROOT).contains("application/json");
		if (jsonLike && body.containsKey("activity_id") && body.get("activity_id") != null) {
			return body.get("activity_id");
		}
		Object fromFlat = FlexibleHttpServletParameterMap.toObjectMap(httpRequest).get("activity_id");
		if (fromFlat != null) {
			return fromFlat;
		}
		return httpRequest.getParameter("activity_id");
	}

	private static boolean isLooseFalsyActivityIdRaw(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (raw instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return true;
			}
			return isLooseFalsyActivityIdRaw(list.get(0));
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	private static long normalizeToActivityId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			return parseActivityIdIntvalStyle(s);
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return 0L;
			}
			return normalizeToActivityId(list.get(0));
		}
		if (raw instanceof Boolean b) {
			return b ? 1L : 0L;
		}
		return parseActivityIdIntvalStyle(String.valueOf(raw));
	}

	private static long parseActivityIdIntvalStyle(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw;
		int len = s.length();
		int i = 0;
		while (i < len && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		boolean negative = false;
		char c = s.charAt(i);
		if (c == '+') {
			i++;
		} else if (c == '-') {
			negative = true;
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		int startDigits = i;
		while (i < len && Character.isDigit(s.charAt(i))) {
			i++;
		}
		if (i == startDigits) {
			return 0L;
		}
		String digitStr = s.substring(startDigits, i);
		try {
			BigInteger bi = new BigInteger(negative ? "-" + digitStr : digitStr);
			if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
				return Long.MAX_VALUE;
			}
			if (bi.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
				return Long.MIN_VALUE;
			}
			return bi.longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
