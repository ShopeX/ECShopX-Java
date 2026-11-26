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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 热词跳转配置（对齐会员中心 / 会员任务 {@code jump_page}）。
 *
 * <p>落库为 JSON 字符串，例如：
 * {@code {"linkPage":"link","id":"registActivity","title":"报名活动"}}。
 * 接口入参可接受同结构对象或 JSON 字符串；出参返回落库字符串（与会员任务一致）。
 */
public final class SearchHotwordJumpPage {

	private static final ObjectMapper JSON = new ObjectMapper();

	public static final int MAX_LENGTH = 512;

	private SearchHotwordJumpPage() {}

	/**
	 * 归一化并校验；空值返回 {@code null}。非法结构写入 {@code errors} 的 {@code jump_page} 键。
	 */
	public static String normalize(Object raw, Map<String, List<String>> errors) {
		if (raw == null) {
			return null;
		}
		Map<String, Object> data;
		if (raw instanceof Map<?, ?> map) {
			data = copyStringKeys(map);
		} else {
			String text = raw.toString().trim();
			if (text.isEmpty()) {
				return null;
			}
			try {
				Object parsed = JSON.readValue(text, Object.class);
				if (!(parsed instanceof Map<?, ?> map)) {
					errors.put("jump_page", List.of("validation.json"));
					return null;
				}
				data = copyStringKeys(map);
			} catch (JsonProcessingException e) {
				errors.put("jump_page", List.of("validation.json"));
				return null;
			}
		}
		if (data.isEmpty()) {
			return null;
		}

		Object linkPageRaw = data.get("linkPage");
		if (linkPageRaw == null) {
			linkPageRaw = data.get("link_page");
		}
		String linkPage = linkPageRaw == null ? "" : linkPageRaw.toString().trim();
		if (linkPage.isEmpty()) {
			errors.put("jump_page", List.of("validation.required"));
			return null;
		}

		Object idRaw = data.get("id");
		String id = idRaw == null ? "" : idRaw.toString().trim();
		Object webIdRaw = data.get("webId");
		if (webIdRaw == null) {
			webIdRaw = data.get("web_id");
		}
		String webId = webIdRaw == null ? "" : webIdRaw.toString().trim();

		if ("custom_page".equals(linkPage)) {
			if (id.isEmpty() || webId.isEmpty()) {
				errors.put("jump_page", List.of("validation.required"));
				return null;
			}
		} else if (id.isEmpty()) {
			errors.put("jump_page", List.of("validation.required"));
			return null;
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("linkPage", linkPage);
		payload.put("id", id);
		if ("custom_page".equals(linkPage)) {
			payload.put("webId", webId);
		}
		Object titleRaw = data.get("title");
		if (titleRaw != null) {
			String title = titleRaw.toString().trim();
			if (!title.isEmpty()) {
				payload.put("title", title);
			}
		}
		Object webTitleRaw = data.get("web_title");
		if (webTitleRaw == null) {
			webTitleRaw = data.get("webTitle");
		}
		if (webTitleRaw != null) {
			String webTitle = webTitleRaw.toString().trim();
			if (!webTitle.isEmpty()) {
				payload.put("web_title", webTitle);
			}
		}

		try {
			String serialized = JSON.writeValueAsString(payload);
			if (serialized.length() > MAX_LENGTH) {
				errors.put("jump_page", List.of("validation.max.string"));
				return null;
			}
			return serialized;
		} catch (JsonProcessingException e) {
			errors.put("jump_page", List.of("validation.json"));
			return null;
		}
	}

	/**
	 * 出参：与会员中心一致，返回落库的 JSON <strong>字符串</strong>
	 *（如 {@code {"linkPage":"goods","id":"1","title":"某商品"}}）；空返回 {@code null}。
	 */
	public static String toResponseValue(String stored) {
		if (stored == null || stored.isBlank()) {
			return null;
		}
		return stored.trim();
	}

	private static Map<String, Object> copyStringKeys(Map<?, ?> map) {
		Map<String, Object> out = new LinkedHashMap<>();
		map.forEach((k, v) -> {
			if (k != null) {
				out.put(k.toString(), v);
			}
		});
		return out;
	}

	public static BadRequestException invalidAlone() {
		return new BadRequestException(
				"jump_page 格式无效", Map.of("jump_page", List.of("validation.json")), 422);
	}
}
