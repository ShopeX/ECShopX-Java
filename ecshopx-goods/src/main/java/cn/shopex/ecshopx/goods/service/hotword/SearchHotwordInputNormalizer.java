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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 热词保存入参归一化与校验（ECX-11390 F-002）。
 *
 * <p>接受 {@code application/json}、{@code multipart/form-data}、{@code application/x-www-form-urlencoded}，
 * 键名兼容 snake_case 与 camelCase；一次收集全部字段错误后统一抛 422，便于 admin 表单逐字段提示。
 */
public final class SearchHotwordInputNormalizer {

	private static final Pattern COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

	private static final int MAX_CONTENT_LENGTH = 100;

	private static final int MAX_URL_LENGTH = 512;

	private static final int MAX_SORT = 999_999;

	private SearchHotwordInputNormalizer() {}

	/** 归一化后的保存入参。 */
	public record Normalized(
			String content,
			String type,
			String textColor,
			String backgroundColor,
			String iconUrl,
			String imageUrl,
			Integer sort,
			Integer startTime,
			Integer endTime,
			String channel,
			String jumpPage) {}

	/**
	 * 合并表单参数与请求体：表单字段（含 {@code xxx[]} 数组写法）作为基底，请求体同名键覆盖。
	 */
	public static Map<String, Object> mergeRequestParams(Map<String, String[]> params, Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (params != null) {
			params.forEach((key, values) -> {
				if (key == null || values == null || values.length == 0) {
					return;
				}
				String name = key.endsWith("[]") ? key.substring(0, key.length() - 2) : key;
				if (values.length == 1 && !key.endsWith("[]")) {
					merged.put(name, values[0]);
					return;
				}
				List<String> list = new ArrayList<>();
				for (String value : values) {
					list.add(value);
				}
				merged.put(name, list);
			});
		}
		if (body != null) {
			merged.putAll(body);
		}
		return merged;
	}

	/** 编辑主键：缺省返回 {@code null}（新增）。 */
	public static Long optionalId(Map<String, Object> input) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		Long id = null;
		Object raw = pick(input, "id");
		if (raw != null) {
			String text = raw.toString().trim();
			if (!text.isEmpty()) {
				try {
					long parsed = Long.parseLong(text);
					if (parsed <= 0L) {
						errors.put("id", List.of("validation.min.numeric"));
					} else {
						id = parsed;
					}
				} catch (NumberFormatException e) {
					errors.put("id", List.of("validation.integer"));
				}
			}
		}
		throwIfAny(errors);
		return id;
	}

	/** 归一化并校验保存入参。 */
	public static Normalized normalize(Map<String, Object> input) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		Map<String, Object> source = input == null ? Map.of() : input;

		String content = trimToNull(pick(source, "content"));
		if (content == null) {
			errors.put("content", List.of("validation.required"));
		} else if (content.length() > MAX_CONTENT_LENGTH) {
			errors.put("content", List.of("validation.max.string"));
			content = content.substring(0, MAX_CONTENT_LENGTH);
		}

		String type = trimToNull(pick(source, "type"));
		if (type == null) {
			type = SearchHotword.TYPE_TEXT;
		} else {
			type = type.toLowerCase(Locale.ROOT);
			if (!SearchHotword.TYPE_TEXT.equals(type) && !SearchHotword.TYPE_IMAGE.equals(type)) {
				errors.put("type", List.of("validation.in"));
			}
		}

		boolean textType = !SearchHotword.TYPE_IMAGE.equals(type);

		// 形态切换不清理样式字段：{@code type=image} 时保留已配置的颜色/图标，避免来回切换丢配置
		String textColor = trimToNull(pick(source, "text_color"));
		if (textColor == null) {
			if (textType) {
				textColor = "#000000";
			}
		} else if (!COLOR.matcher(textColor).matches()) {
			errors.put("text_color", List.of("validation.regex"));
		}

		String backgroundColor = trimToNull(pick(source, "background_color"));
		if (backgroundColor == null) {
			if (textType) {
				backgroundColor = "#FFFFFF";
			}
		} else if (!COLOR.matcher(backgroundColor).matches()) {
			errors.put("background_color", List.of("validation.regex"));
		}

		String iconUrl = validateUrl(trimToNull(pick(source, "icon_url")), "icon_url", errors);

		String imageUrl = trimToNull(pick(source, "image_url"));
		if (textType) {
			imageUrl = null;
		} else if (imageUrl == null) {
			errors.put("image_url", List.of("validation.required"));
		} else {
			imageUrl = validateUrl(imageUrl, "image_url", errors);
		}

		Integer sort = parseSort(pick(source, "sort"), errors);

		Integer startTime = SearchHotwordTime.parse(pick(source, "start_time"), "start_time");
		if (startTime == null) {
			errors.put("start_time", List.of("validation.required"));
		}
		Integer endTime = SearchHotwordTime.parse(pick(source, "end_time"), "end_time");
		if (endTime == null) {
			errors.put("end_time", List.of("validation.required"));
		}
		if (startTime != null && endTime != null && endTime.longValue() < startTime.longValue()) {
			errors.put("end_time", List.of("validation.after_or_equal"));
		}

		String channel = null;
		Object channelRaw = pick(source, "channel");
		if (channelRaw == null) {
			errors.put("channel", List.of("validation.required"));
		} else {
			try {
				channel = SearchHotwordChannels.normalizeSaveChannel(channelRaw);
			} catch (BadRequestException e) {
				errors.put("channel", List.of("validation.in"));
			}
		}

		// 跳转对齐会员中心 jump_page：{ linkPage, id, title? } 对象或 JSON 字符串
		Object jumpRaw = pick(source, "jump_page");
		if (jumpRaw == null) {
			// 兼容误传：若仍带旧字段则拒绝，引导改用 jump_page
			if (pick(source, "link_page") != null || pick(source, "link_params") != null) {
				errors.put("jump_page", List.of("validation.required"));
			}
		}
		String jumpPage = SearchHotwordJumpPage.normalize(jumpRaw, errors);

		throwIfAny(errors);
		return new Normalized(
				content,
				type,
				textColor,
				backgroundColor,
				iconUrl,
				imageUrl,
				sort,
				startTime,
				endTime,
				channel,
				jumpPage);
	}

	private static Integer parseSort(Object raw, Map<String, List<String>> errors) {
		if (raw == null || raw.toString().isBlank()) {
			return Integer.valueOf(0);
		}
		Integer parsed = null;
		if (raw instanceof Number number) {
			long value = number.longValue();
			parsed = Math.abs(value) > MAX_SORT ? null : Integer.valueOf((int) value);
		} else {
			try {
				long value = Long.parseLong(raw.toString().trim());
				parsed = Math.abs(value) > MAX_SORT ? null : Integer.valueOf((int) value);
			} catch (NumberFormatException ignored) {
				parsed = null;
			}
		}
		if (parsed == null) {
			errors.put("sort", List.of("validation.integer"));
		}
		return parsed;
	}

	private static String validateUrl(String value, String field, Map<String, List<String>> errors) {
		if (value == null) {
			return null;
		}
		if (value.length() > MAX_URL_LENGTH) {
			errors.put(field, List.of("validation.max.string"));
			return null;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		boolean ok = lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/");
		if (!ok) {
			errors.put(field, List.of("validation.url"));
			return null;
		}
		return value;
	}

	private static Object pick(Map<String, Object> source, String key) {
		Object value = source.get(key);
		if (value != null) {
			return value;
		}
		String camel = camelCase(key);
		return source.get(camel);
	}

	private static String camelCase(String snake) {
		StringBuilder sb = new StringBuilder();
		boolean upper = false;
		for (char c : snake.toCharArray()) {
			if (c == '_') {
				upper = true;
				continue;
			}
			sb.append(upper ? Character.toUpperCase(c) : c);
			upper = false;
		}
		return sb.toString();
	}

	private static String trimToNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.toString().trim();
		return text.isEmpty() ? null : text;
	}

	private static void throwIfAny(Map<String, List<String>> errors) {
		if (errors.isEmpty()) {
			return;
		}
		throw new BadRequestException("参数校验失败", errors, 422);
	}
}
