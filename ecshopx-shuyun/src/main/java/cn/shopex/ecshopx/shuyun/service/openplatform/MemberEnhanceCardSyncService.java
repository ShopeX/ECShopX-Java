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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** enhance.member.post → 回写 {@code members.user_card_code}。 */
@Service
public class MemberEnhanceCardSyncService {

	private static final Logger log = LoggerFactory.getLogger(MemberEnhanceCardSyncService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public MemberEnhanceCardSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void syncUserCardCodeAfterRegister(long companyId, long userId, long distributorId) {
		if (companyId < 1 || userId < 1 || distributorId < 1) {
			return;
		}
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", String.valueOf(userId));
		body.put("platCode", "OFFLINE");
		body.put("shopId", shopId(distributorId));
		try {
			JsonNode data =
					gatewayClient.postJson(
							companyId, ShuyunOpenPlatformGatewayActions.MEMBER_ENHANCE_POST, body, "offline");
			String memberId = extractMemberId(data);
			if (!StringUtils.hasText(memberId)) {
				return;
			}
			jdbcTemplate.update(
					"""
					UPDATE members SET user_card_code=?
					WHERE company_id=? AND user_id=?
					""",
					memberId,
					companyId,
					userId);
			log.info(
					"Shuyun enhance card sync ok companyId={} userId={} memberId={}",
					companyId,
					userId,
					memberId);
		} catch (Exception e) {
			log.warn(
					"Shuyun enhance.member card sync failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
		}
	}

	static String extractMemberId(JsonNode data) {
		if (data == null || data.isNull()) {
			return "";
		}
		JsonNode node = data;
		if (data.has("data") && !data.get("data").isNull()) {
			node = data.get("data");
		}
		for (String key : new String[] {"memberId", "member_id", "id"}) {
			JsonNode v = node.get(key);
			if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
				return v.asText().trim();
			}
		}
		return "";
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
