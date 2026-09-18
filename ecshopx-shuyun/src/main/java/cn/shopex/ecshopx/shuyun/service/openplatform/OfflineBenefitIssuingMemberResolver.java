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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * customerId → 本地 userId。对齐 PHP {@code ShuyunOfflineBenefitIssuingMemberResolver}。
 */
@Component
public class OfflineBenefitIssuingMemberResolver {

	private final ShuyunOpenPlatformProperties properties;

	public OfflineBenefitIssuingMemberResolver(ShuyunOpenPlatformProperties properties) {
		this.properties = properties;
	}

	public Long resolveLocalUserId(long companyId, String shuyunCustomerId) {
		String id = shuyunCustomerId == null ? "" : shuyunCustomerId.trim();
		if (!StringUtils.hasText(id)) {
			return null;
		}
		String mode = properties.getOfflineBenefitMemberResolveMode();
		if (mode == null || mode.isBlank() || "numeric_user_id".equalsIgnoreCase(mode.trim())) {
			if (id.matches("\\d{1,20}")) {
				return Long.parseLong(id);
			}
			return null;
		}
		return null;
	}
}
