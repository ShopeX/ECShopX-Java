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

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D7 子集：{@code shuyun.private.bind.push}。 */
@Service
public class MemberBindPushService {

	private static final Logger log = LoggerFactory.getLogger(MemberBindPushService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;

	public MemberBindPushService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
	}

	public void pushSingle(long companyId, long distributorId, String platAccount, String unionId, String weixinOpenId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			throw new IllegalStateException("Shuyun open platform unavailable for bind.push");
		}
		if (!StringUtils.hasText(platAccount)
				|| !StringUtils.hasText(unionId)
				|| !StringUtils.hasText(weixinOpenId)) {
			throw new IllegalArgumentException("platAccount/unionId/weixinOpenId required for bind.push");
		}
		String partner =
				StringUtils.hasText(properties.getGatewayPartner()) ? properties.getGatewayPartner().trim() : "nnormal";
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("platCode", "OFFLINE");
		row.put("platAccount", platAccount.trim());
		row.put("shopId", shopId(distributorId));
		row.put("unionId", unionId.trim());
		row.put("weixinOpenId", weixinOpenId.trim());
		row.put("partner", partner);
		gatewayClient.postJson(
				companyId, ShuyunOpenPlatformGatewayActions.MEMBER_BIND_PUSH, List.of(row), "offline");
		log.info("Shuyun bind.push ok companyId={} platAccount={}", companyId, platAccount);
	}

	private String shopId(long distributorId) {
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}
}
