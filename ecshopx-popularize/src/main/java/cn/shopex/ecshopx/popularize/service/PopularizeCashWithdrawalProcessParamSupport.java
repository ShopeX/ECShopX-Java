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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.util.StringUtils;

/** Resolves cash-withdrawal process params from query and/or JSON body (admin PUT). */
public final class PopularizeCashWithdrawalProcessParamSupport {

	private PopularizeCashWithdrawalProcessParamSupport() {}

	public static String resolveProcessType(String processTypeQuery, Map<String, Object> body) {
		String resolved = null;
		if (StringUtils.hasText(processTypeQuery)) {
			resolved = processTypeQuery.trim();
		} else if (body != null && body.get("process_type") != null) {
			resolved = body.get("process_type").toString().trim();
		}
		if (!StringUtils.hasText(resolved)) {
			throw new ResourceException("参数错误");
		}
		return resolved;
	}

	public static String resolveRemarks(String remarksQuery, Map<String, Object> body) {
		if (StringUtils.hasText(remarksQuery)) {
			return remarksQuery.trim();
		}
		if (body != null && body.containsKey("remarks")) {
			Object r = body.get("remarks");
			return r == null ? null : r.toString();
		}
		return remarksQuery;
	}
}
