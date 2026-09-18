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
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 按配置选择 stub / kaquan issuer。对齐 PHP
 * {@code shuyun_open_platform.offline_benefit_issuer}。
 */
@Service
@Primary
public class OfflineBenefitItemIssuerRouter implements OfflineBenefitItemIssuer {

	private final ShuyunOpenPlatformProperties properties;
	private final OfflineBenefitKaquanIssuer kaquanIssuer;
	private final OfflineBenefitStubIssuer stubIssuer;

	public OfflineBenefitItemIssuerRouter(
			ShuyunOpenPlatformProperties properties,
			OfflineBenefitKaquanIssuer kaquanIssuer,
			OfflineBenefitStubIssuer stubIssuer) {
		this.properties = properties;
		this.kaquanIssuer = kaquanIssuer;
		this.stubIssuer = stubIssuer;
	}

	@Override
	public OfflineBenefitIssueResult issue(
			ShuyunOfflineBenefitSendBatch batch, ShuyunOfflineBenefitSendItem item) {
		String mode = properties.getOfflineBenefitIssuer();
		if (mode != null && "stub".equalsIgnoreCase(mode.trim())) {
			return stubIssuer.issue(batch, item);
		}
		return kaquanIssuer.issue(batch, item);
	}
}
