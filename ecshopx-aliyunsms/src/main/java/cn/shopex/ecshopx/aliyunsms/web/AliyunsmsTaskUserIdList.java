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

package cn.shopex.ecshopx.aliyunsms.web;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Parses recipient member IDs from merged admin task parameters: reads {@code user_id} first, otherwise
 * {@code user_id[]}. A {@code null} result means the task applies to all company members (no explicit ID
 * list). A non-{@code null} result is a non-empty list of member user IDs to target.
 */
public final class AliyunsmsTaskUserIdList {

	private AliyunsmsTaskUserIdList() {}

	/**
	 * @return {@code null} when the request selects all company members; otherwise a non-empty list of
	 *     member user IDs.
	 */
	public static List<Long> parseForTaskAdd(Map<String, Object> merged) {
		final Object raw;
		if (merged.containsKey("user_id")) {
			raw = merged.get("user_id");
		} else if (merged.containsKey("user_id[]")) {
			raw = merged.get("user_id[]");
		} else {
			return null;
		}

		if (raw == null) {
			return null;
		}
		if (raw instanceof String s && s.trim().isEmpty()) {
			return null;
		}
		if (raw instanceof Collection<?> c && c.isEmpty()) {
			return null;
		}
		if (raw instanceof Object[] arr && arr.length == 0) {
			return null;
		}
		if (raw instanceof Map<?, ?>) {
			throw new BadRequestException("参数格式错误");
		}

		if (raw instanceof Boolean b && !b) {
			return null;
		}
		if (raw instanceof Number n && n.doubleValue() == 0.0) {
			return null;
		}
		if (raw instanceof String s && "0".equals(s.trim())) {
			return null;
		}

		final List<Object> elements;
		if (raw instanceof Collection<?> col) {
			elements = new ArrayList<>(col);
		} else if (raw instanceof Object[] array) {
			elements = new ArrayList<>(Arrays.asList(array));
		} else if (isScalarRoot(raw)) {
			elements = List.of(raw);
		} else {
			throw new BadRequestException("参数格式错误");
		}

		List<Object> flat = new ArrayList<>();
		for (Object item : elements) {
			if (item instanceof Collection<?> ic) {
				for (Object x : ic) {
					flat.add(x);
				}
			} else if (item instanceof Object[] ia) {
				for (Object x : ia) {
					flat.add(x);
				}
			} else {
				flat.add(item);
			}
		}

		for (Object x : flat) {
			if (x instanceof Collection<?> || x instanceof Object[]) {
				throw new BadRequestException("参数格式错误");
			}
		}

		List<Long> out = new ArrayList<>();
		for (Object elem : flat) {
			if (elem == null) {
				continue;
			}
			if (elem instanceof Map<?, ?>) {
				throw new BadRequestException("参数格式错误");
			}
			if (elem instanceof Collection<?> || elem instanceof Object[]) {
				throw new BadRequestException("参数格式错误");
			}
			long v = Long.parseLong(String.valueOf(elem).trim());
			out.add(v);
		}

		if (out.isEmpty()) {
			return null;
		}
		return out;
	}

	private static boolean isScalarRoot(Object raw) {
		return raw instanceof Number || raw instanceof String || raw instanceof Boolean;
	}
}
