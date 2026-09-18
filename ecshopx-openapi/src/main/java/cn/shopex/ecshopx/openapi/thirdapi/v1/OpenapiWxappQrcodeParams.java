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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class OpenapiWxappQrcodeParams {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final Set<String> ALLOWED_PATH_TYPES =
			Set.of(
					"index",
					"goods_list",
					"goods_detail",
					"recommend_list",
					"recommend_detail",
					"share_land");

	private OpenapiWxappQrcodeParams() {}

	public sealed interface ParseResult permits Ok, Fail {}

	public record Ok(String pathType, Map<String, Object> scene, String widthRaw) implements ParseResult {}

	public record Fail(String message) implements ParseResult {}

	public static ParseResult parse(
			String pathTypeParam,
			String sceneParam,
			String widthParam,
			Map<String, Object> body) {
		String pathTypeRaw = OpenapiRequestParams.originalString(pathTypeParam, body, "path_type");
		String sceneRaw = OpenapiRequestParams.originalString(sceneParam, body, "scene");
		String widthRaw = OpenapiRequestParams.originalString(widthParam, body, "width");

		Map<String, Object> sceneParsed = null;
		if (StringUtils.hasText(sceneRaw) && !"0".equals(sceneRaw)) {
			try {
				sceneParsed =
						OBJECT_MAPPER.readValue(sceneRaw, new TypeReference<Map<String, Object>>() {});
			} catch (Exception e) {
				return new Fail("参数错误");
			}
		}

		if (!StringUtils.hasText(pathTypeRaw)) {
			return new Fail("参数错误");
		}
		if (!ALLOWED_PATH_TYPES.contains(pathTypeRaw)) {
			return new Fail("参数错误");
		}

		if (sceneParsed == null) {
			return new Fail("参数错误");
		}

		if ("goods_detail".equals(pathTypeRaw) || "recommend_detail".equals(pathTypeRaw)) {
			if (!sceneParsed.containsKey("id") || !isValidSceneId(sceneParsed.get("id"))) {
				return new Fail("参数错误");
			}
		}

		return new Ok(pathTypeRaw, sceneParsed, widthRaw);
	}

	private static boolean isValidSceneId(Object id) {
		if (id == null) {
			return false;
		}
		if (id instanceof Float || id instanceof Double) {
			return false;
		}
		if (id instanceof Integer i) {
			return i >= 1;
		}
		if (id instanceof Long l) {
			return l >= 1L;
		}
		if (id instanceof Number n) {
			long v = n.longValue();
			return v >= 1L && n.doubleValue() == v;
		}
		if (id instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				return false;
			}
			try {
				long v = Long.parseLong(s);
				return v >= 1L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}
}
