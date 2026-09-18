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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class OpenapiImageDeleteParams {

	private OpenapiImageDeleteParams() {}

	public sealed interface ParseResult permits Ok, TooMany {}

	public record Ok(List<String> imageIds) implements ParseResult {}

	public record TooMany() implements ParseResult {}

	public static ParseResult parse(String imageIdParam, Map<String, Object> body) {
		String raw = OpenapiRequestParams.originalString(imageIdParam, body, "image_id");
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			throw new ResourceException("请选择要删除的图片");
		}
		String[] parts = raw.split(",", -1);
		if (parts.length > 100) {
			return new TooMany();
		}
		return new Ok(Arrays.asList(parts));
	}
}
