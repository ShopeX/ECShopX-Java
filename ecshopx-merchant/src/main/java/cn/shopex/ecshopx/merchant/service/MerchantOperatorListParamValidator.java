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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantOperatorListParamValidator {

	public void validateFromMap(Map<String, Object> params) {
		int page = requirePositiveInt(
				MerchantListParamValidator.scalarFrom(params.get("page")),
				"当前页数为大于0的整数",
				"当前页数为大于0的整数");
		Object pageSizeRaw = MerchantListParamValidator.scalarFrom(params.get("pageSize"));
		if (pageSizeRaw == null) {
			pageSizeRaw = MerchantListParamValidator.scalarFrom(params.get("page_size"));
		}
		int pageSize = requirePositiveInt(
				pageSizeRaw, "每页数量为1-50的整数", "每页数量为1-50的整数");
		if (pageSize < 1 || pageSize > 50) {
			throw new ResourceException("每页数量为1-50的整数");
		}
		params.put("page", page);
		params.put("pageSize", pageSize);
	}

	private static int requirePositiveInt(Object scalar, String missingMsg, String nonPositiveMsg) {
		if (scalar == null) {
			throw new ResourceException(missingMsg);
		}
		String t = String.valueOf(scalar).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(missingMsg);
		}
		try {
			if (scalar instanceof Number n) {
				int v = n.intValue();
				if (v < 1) {
					throw new ResourceException(nonPositiveMsg);
				}
				return v;
			}
			int v = Integer.parseInt(t);
			if (v < 1) {
				throw new ResourceException(nonPositiveMsg);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(missingMsg);
		}
	}
}
