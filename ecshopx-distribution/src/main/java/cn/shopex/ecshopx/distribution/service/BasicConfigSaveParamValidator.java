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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class BasicConfigSaveParamValidator {

	public static final String RETURN_NAME_REQUIRED = "请填写退换货收货人";

	public static final String RETURN_ADDRESS_REQUIRED = "请填写退换货地址";

	public static final String RETURN_PHONE_REQUIRED = "请填写退换货联系方式";

	private BasicConfigSaveParamValidator() {}

	public static void validateRequiredReturns(Map<String, Object> merged) {
		if (!StringUtils.hasText(stringParam(merged, "return_name"))) {
			throw new ResourceException(RETURN_NAME_REQUIRED);
		}
		if (!StringUtils.hasText(stringParam(merged, "return_address"))) {
			throw new ResourceException(RETURN_ADDRESS_REQUIRED);
		}
		if (!StringUtils.hasText(stringParam(merged, "return_phone"))) {
			throw new ResourceException(RETURN_PHONE_REQUIRED);
		}
	}

	private static String stringParam(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		return v == null ? null : v.toString();
	}
}
