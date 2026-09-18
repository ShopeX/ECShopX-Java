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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.util.Map;

public final class OpenapiThirdApiV2AftersalesListParams {

	private OpenapiThirdApiV2AftersalesListParams() {}

	public record PageSpec(int page, int pageSize) {}

	public static PageSpec resolve(String pageParam, String pageSizeParam, Map<String, Object> body) {
		String pageRaw = OpenapiRequestParams.originalString(pageParam, body, "page");
		String pageSizeRaw = OpenapiRequestParams.originalString(pageSizeParam, body, "page_size");

		int rawPageSizeParsed = parsePhpInt(pageSizeRaw, 20);
		if (rawPageSizeParsed >= 500) {
			throw new ResourceException("每页的大小最大为500！");
		}

		int page = Math.max(parsePhpInt(pageRaw, 1), 1);
		int pageSize = Math.max(parsePhpInt(pageSizeRaw, 20), 20);

		if (page < 1) {
			throw new ResourceException("当前页面最小值为1");
		}
		if (pageSize < 1 || pageSize > 500) {
			throw new ResourceException("每页显示数量1-500");
		}

		return new PageSpec(page, pageSize);
	}

	private static int parsePhpInt(String raw, int defaultValue) {
		if (raw == null || raw.isEmpty()) {
			return defaultValue;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			if (t.contains(".")) {
				return (int) Double.parseDouble(t);
			}
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
