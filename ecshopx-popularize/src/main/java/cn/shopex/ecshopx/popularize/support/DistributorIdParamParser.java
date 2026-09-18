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

package cn.shopex.ecshopx.popularize.support;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Normalizes distributor id list parameters for brokerage queries.
 */
public final class DistributorIdParamParser {

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private DistributorIdParamParser() {
	}

	/**
	 * @param raw      raw value for {@code distributor_id} or {@code dIds}
	 * @param paramName {@code "distributor_id"} or {@code "dIds"} for error messages
	 * @return non-null list (possibly empty) when the key was present and should be evaluated; {@code null} when the
	 *         caller should treat the key as absent / ignorable (null or blank scalar)
	 */
	public static List<Long> parseDistributorIdListParam(Object raw, String paramName) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException(paramName + " 格式错误");
			}
			return List.of(v);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			if (t.contains(",")) {
				return parseCommaSeparatedIds(t, paramName);
			}
			if (!DIGITS_ONLY.matcher(t).matches()) {
				throw new BadRequestException(paramName + " 格式错误");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				return List.of(v);
			} catch (NumberFormatException e) {
				throw new BadRequestException(paramName + " 格式错误");
			}
		}
		if (raw instanceof Iterable<?> it) {
			return parseIterableElements(it, paramName);
		}
		if (raw instanceof Object[] arr) {
			return parseIterableElements(java.util.Arrays.asList(arr), paramName);
		}
		if (raw instanceof long[] arr) {
			return parseLongPrimitiveArray(arr, paramName);
		}
		if (raw instanceof int[] arr) {
			List<Long> out = new ArrayList<>();
			for (int v : arr) {
				if (v <= 0) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof Integer[] arr) {
			return parseIterableElements(java.util.Arrays.asList(arr), paramName);
		}
		if (raw instanceof Long[] arr) {
			return parseIterableElements(java.util.Arrays.asList(arr), paramName);
		}
		throw new BadRequestException(paramName + " 格式错误");
	}

	private static List<Long> parseLongPrimitiveArray(long[] arr, String paramName) {
		List<Long> out = new ArrayList<>();
		for (long v : arr) {
			if (v <= 0L) {
				throw new BadRequestException(paramName + " 格式错误");
			}
			out.add(v);
		}
		return out;
	}

	private static List<Long> parseCommaSeparatedIds(String t, String paramName) {
		String[] parts = t.split(",");
		LinkedHashSet<Long> ordered = new LinkedHashSet<>();
		for (String part : parts) {
			String seg = part.trim();
			if (!StringUtils.hasText(seg)) {
				continue;
			}
			if (!DIGITS_ONLY.matcher(seg).matches()) {
				throw new BadRequestException(paramName + " 格式错误");
			}
			try {
				long v = Long.parseLong(seg);
				if (v <= 0L) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				ordered.add(v);
			} catch (NumberFormatException e) {
				throw new BadRequestException(paramName + " 格式错误");
			}
		}
		if (ordered.isEmpty()) {
			throw new BadRequestException(paramName + " 格式错误");
		}
		return new ArrayList<>(ordered);
	}

	private static List<Long> parseIterableElements(Iterable<?> it, String paramName) {
		List<Long> out = new ArrayList<>();
		for (Object el : it) {
			if (el == null) {
				continue;
			}
			if (el instanceof Number n) {
				long v = n.longValue();
				if (v <= 0L) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				out.add(v);
				continue;
			}
			if (el instanceof String s) {
				String t = s.trim();
				if (!StringUtils.hasText(t)) {
					continue;
				}
				if (t.contains(",")) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				if (!DIGITS_ONLY.matcher(t).matches()) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				try {
					long v = Long.parseLong(t);
					if (v <= 0L) {
						throw new BadRequestException(paramName + " 格式错误");
					}
					out.add(v);
				} catch (NumberFormatException e) {
					throw new BadRequestException(paramName + " 格式错误");
				}
				continue;
			}
			throw new BadRequestException(paramName + " 格式错误");
		}
		return out;
	}

	/**
	 * Resolves {@code oo.distributor_id IN (...)} list: {@code dIds} overrides {@code distributor_id} when
	 * {@code dIds} key is present, value non-null, and parsed list is non-empty.
	 */
	public static List<Long> resolveDistributorIdsForOrderInFilter(java.util.Map<String, Object> params) {
		List<Long> dIdsList = null;
		if (params.containsKey("dIds") && params.get("dIds") != null) {
			dIdsList = parseDistributorIdListParam(params.get("dIds"), "dIds");
			if (dIdsList != null && !dIdsList.isEmpty()) {
				return dIdsList;
			}
		}
		if (params.containsKey("distributor_id") && params.get("distributor_id") != null) {
			List<Long> distList = parseDistributorIdListParam(params.get("distributor_id"), "distributor_id");
			if (distList != null && !distList.isEmpty()) {
				return distList;
			}
		}
		return null;
	}
}
