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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappCanReservationRightsParamValidator {

	private static final String ERR_MSG = "获取权益列表出错.";

	public record ValidatedPageParams(int page, int pageSize) {}

	/**
	 * 校验 query 原始字符串；失败抛出 {@link ResourceException}。
	 */
	public ValidatedPageParams validate(String pageRaw, String pageSizeRaw) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		validateOneField(pageRaw, "page", 1, null, errors);
		validateOneField(pageSizeRaw, "pageSize", 1, 100, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(ERR_MSG);
		}
		int page = parseIntUnchecked(pageRaw);
		int pageSize = parseIntUnchecked(pageSizeRaw);
		return new ValidatedPageParams(page, pageSize);
	}

	private static void validateOneField(
			String raw, String field, int minInclusive, Integer maxInclusive, Map<String, List<String>> errors) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			errors.put(field, List.of(field + " 字段是必须的。"));
			return;
		}
		String t = raw.trim();
		long v;
		try {
			v = Long.parseLong(t);
		} catch (NumberFormatException e) {
			errors.put(field, List.of(field + " 必须是一个整数。"));
			return;
		}
		if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
			errors.put(field, List.of(field + " 必须是一个整数。"));
			return;
		}
		int iv = (int) v;
		if (iv < minInclusive) {
			errors.put(field, List.of(field + " 必须至少为 " + minInclusive + "。"));
			return;
		}
		if (maxInclusive != null && iv > maxInclusive) {
			errors.put(field, List.of(field + " 不能大于 " + maxInclusive + "。"));
		}
	}

	private static int parseIntUnchecked(String raw) {
		return (int) Long.parseLong(raw.trim());
	}
}
