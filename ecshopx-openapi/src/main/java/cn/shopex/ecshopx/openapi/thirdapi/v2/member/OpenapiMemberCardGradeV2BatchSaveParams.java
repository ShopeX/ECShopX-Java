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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiMemberCardGradeV2BatchSaveParams {

	private static final ObjectMapper JSON = new ObjectMapper();

	private OpenapiMemberCardGradeV2BatchSaveParams() {}

	/**
	 * 解析 grade_info JSON 数组。任一失败条件 → E5001「请求参数错误」。
	 */
	public static List<Map<String, Object>> parseGradeInfoArray(String gradeInfoRaw) {
		if (!StringUtils.hasText(gradeInfoRaw)) {
			throw paramError();
		}
		try {
			List<?> parsed = JSON.readValue(gradeInfoRaw, List.class);
			if (parsed == null || parsed.isEmpty()) {
				throw paramError();
			}
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object item : parsed) {
				if (item instanceof Map<?, ?> map) {
					Map<String, Object> copy = new LinkedHashMap<>();
					for (Map.Entry<?, ?> entry : map.entrySet()) {
						if (entry.getKey() instanceof String key) {
							copy.put(key, entry.getValue());
						}
					}
					result.add(copy);
				}
			}
			if (result.isEmpty()) {
				throw paramError();
			}
			return result;
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw paramError();
		}
	}

	private static OpenapiMemberV2FailException paramError() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误");
	}
}
