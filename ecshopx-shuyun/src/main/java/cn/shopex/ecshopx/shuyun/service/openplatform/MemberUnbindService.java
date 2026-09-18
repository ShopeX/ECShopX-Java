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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberUnbindPort;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D7：member.unbind。 */
@Service
@Primary
public class MemberUnbindService implements ShuyunOpenPlatformMemberUnbindPort {

	private static final Logger log = LoggerFactory.getLogger(MemberUnbindService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public MemberUnbindService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean isOpenPlatformMemberEnabled(long companyId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		return InboundSignedCallbackPreparer.isEligible(cfg);
	}

	@Override
	public boolean unbindIfNeeded(long companyId, long userId, boolean skip) {
		if (skip || !isOpenPlatformMemberEnabled(companyId) || userId < 1) {
			return false;
		}
		Long distributorId =
				jdbcTemplate.query(
						"""
						SELECT COALESCE(NULLIF(offline_reg_distributor,0), NULLIF(reg_distributor,0), 0)
						FROM members WHERE company_id=? AND user_id=? LIMIT 1
						""",
						rs -> rs.next() ? rs.getLong(1) : 0L,
						companyId,
						userId);
		if (distributorId == null || distributorId < 1) {
			distributorId =
					jdbcTemplate.query(
							"""
							SELECT distributor_id FROM distribution_distributor
							WHERE company_id=? AND distributor_self=1 LIMIT 1
							""",
							rs -> rs.next() ? rs.getLong(1) : null,
							companyId);
		}
		if (distributorId == null || distributorId < 1) {
			throw new ResourceException("开放网关会员解绑失败：未找到门店");
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", String.valueOf(userId));
		body.put("platCode", "OFFLINE");
		body.put("shopId", resolveShopId(distributorId));
		try {
			gatewayClient.postJson(companyId, ShuyunOpenPlatformGatewayActions.MEMBER_UNBIND, body, "offline");
			log.info("Shuyun member.unbind ok companyId={} userId={}", companyId, userId);
			return true;
		} catch (Exception e) {
			String msg = e.getMessage() == null ? "" : e.getMessage().trim();
			throw new ResourceException(msg.isEmpty() ? "开放网关会员解绑失败，请稍后重试" : msg);
		}
	}

	private String resolveShopId(long distributorId) {
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}
}
