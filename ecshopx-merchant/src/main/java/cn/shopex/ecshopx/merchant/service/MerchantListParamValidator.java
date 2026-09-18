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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantListParamValidator {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

	public void validateFromMap(Map<String, Object> params) {
		requirePositiveInt(scalarFrom(params.get("page")), "page 不能为空", "page 必须大于等于 1");
		int pageSize = requirePositiveInt(
				scalarFrom(params.get("pageSize")), "pageSize 不能为空", "pageSize 必须大于等于 1");
		if (pageSize > 1000) {
			throw new ResourceException("pageSize 必须在 1～1000 之间");
		}
		validateTimeStartIfPresent(params.get("time_start"));
	}

	private void validateTimeStartIfPresent(Object raw) {
		if (raw == null) {
			return;
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				return;
			}
			if (c.size() != 2) {
				throw new ResourceException("time_start 须包含开始与结束两个时间");
			}
			Object[] arr = c.toArray();
			requireParsedEpoch(arr[0], "time_start 开始时间无效");
			requireParsedEpoch(arr[1], "time_start 结束时间无效");
			return;
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return;
			}
			throw new ResourceException("time_start 须包含开始与结束两个时间");
		}
		throw new ResourceException("time_start 须包含开始与结束两个时间");
	}

	private static void requireParsedEpoch(Object o, String message) {
		String t = o == null ? "" : String.valueOf(o).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(message);
		}
		Integer sec = parseToEpochSecond(t);
		if (sec == null) {
			throw new ResourceException(message);
		}
	}

	static Integer parseToEpochSecond(String s) {
		try {
			long v = Long.parseLong(s.trim());
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		} catch (NumberFormatException ignored) {
			// fall through
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return (int) ldt.atZone(CN).toEpochSecond();
		} catch (DateTimeParseException e1) {
			try {
				LocalDate ld = LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
				return (int) ld.atStartOfDay(CN).toEpochSecond();
			} catch (DateTimeParseException e2) {
				return null;
			}
		}
	}

	private static int requirePositiveInt(Object scalar, String missingMsg, String nonPositiveMsg) {
		if (scalar == null) {
			throw new ResourceException(missingMsg);
		}
		String t = String.valueOf(scalar).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(missingMsg);
		}
		try {
			if (scalar instanceof Number n) {
				int v = n.intValue();
				if (v < 1) {
					throw new ResourceException(nonPositiveMsg);
				}
				return v;
			}
			int v = Integer.parseInt(t);
			if (v < 1) {
				throw new ResourceException(nonPositiveMsg);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(missingMsg);
		}
	}

	public static Object scalarFrom(Object v) {
		if (v instanceof List<?> list && !list.isEmpty()) {
			return list.get(0);
		}
		return v;
	}

	static int pageFromValidatedMap(Map<String, Object> params) {
		return requirePositiveIntLoose(scalarFrom(params.get("page")));
	}

	static int pageSizeFromValidatedMap(Map<String, Object> params) {
		return requirePositiveIntLoose(scalarFrom(params.get("pageSize")));
	}

	private static int requirePositiveIntLoose(Object scalar) {
		if (scalar instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(scalar).trim());
	}

	static Integer createdGteFromMap(Map<String, Object> params) {
		return rangeStartFromTimeStart(params.get("time_start"));
	}

	static Integer createdLteFromMap(Map<String, Object> params) {
		return rangeEndFromTimeStart(params.get("time_start"));
	}

	private static Integer rangeStartFromTimeStart(Object raw) {
		List<String> pair = extractTimePair(raw);
		if (pair == null) {
			return null;
		}
		return parseToEpochSecond(pair.get(0));
	}

	private static Integer rangeEndFromTimeStart(Object raw) {
		List<String> pair = extractTimePair(raw);
		if (pair == null) {
			return null;
		}
		return parseToEpochSecond(pair.get(1));
	}

	private static List<String> extractTimePair(Object raw) {
		if (!(raw instanceof Collection<?> c) || c.size() != 2) {
			return null;
		}
		Object[] arr = c.toArray();
		String a = arr[0] == null ? "" : String.valueOf(arr[0]).trim();
		String b = arr[1] == null ? "" : String.valueOf(arr[1]).trim();
		if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
			return null;
		}
		return List.of(a, b);
	}

	static String stringFilterFromMap(Map<String, Object> params, String key) {
		Object v = params.get(key);
		Object s = scalarFrom(v);
		if (s == null) {
			return null;
		}
		String t = String.valueOf(s).trim();
		return StringUtils.hasText(t) ? t : null;
	}
}
