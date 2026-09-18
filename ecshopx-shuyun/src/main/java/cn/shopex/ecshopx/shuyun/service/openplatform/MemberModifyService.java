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
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberModifyPort;
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

/** D7：member.modify。 */
@Service
@Primary
public class MemberModifyService implements ShuyunOpenPlatformMemberModifyPort {

	private static final Logger log = LoggerFactory.getLogger(MemberModifyService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public MemberModifyService(
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
	public boolean modifyIfNeeded(long companyId, long userId, Map<String, Object> memberFields) {
		if (!isOpenPlatformMemberEnabled(companyId) || userId < 1) {
			return false;
		}
		Map<String, Object> changes = new LinkedHashMap<>();
		if (memberFields != null) {
			if (memberFields.containsKey("username")) {
				changes.put("name", String.valueOf(memberFields.get("username")));
			}
			if (memberFields.containsKey("birthday")) {
				Object b = memberFields.get("birthday");
				if (b != null && StringUtils.hasText(String.valueOf(b).trim())) {
					changes.put("birthday", String.valueOf(b).trim());
				}
			}
			if (memberFields.containsKey("sex")) {
				String sex = String.valueOf(memberFields.get("sex")).trim();
				if ("1".equals(sex) || "2".equals(sex)) {
					changes.put("gender", "1".equals(sex) ? "M" : "F");
				}
			}
		}
		if (changes.isEmpty()) {
			return false;
		}
		Long virtualId =
				jdbcTemplate.query(
						"""
						SELECT distributor_id FROM distribution_distributor
						WHERE company_id=? AND distributor_self=1 LIMIT 1
						""",
						rs -> rs.next() ? rs.getLong(1) : null,
						companyId);
		if (virtualId == null || virtualId < 1) {
			throw new ResourceException("开放网关会员资料同步失败：未找到虚拟店");
		}
		Map<String, Object> body = new LinkedHashMap<>(changes);
		body.put("id", String.valueOf(userId));
		body.put("platCode", "OFFLINE");
		body.put("shopId", resolveShopId(virtualId));
		try {
			gatewayClient.putJson(companyId, ShuyunOpenPlatformGatewayActions.MEMBER_MODIFY, body, "offline");
			log.info("Shuyun member.modify ok companyId={} userId={}", companyId, userId);
			return true;
		} catch (Exception e) {
			String msg = e.getMessage() == null ? "" : e.getMessage().trim();
			throw new ResourceException(msg.isEmpty() ? "开放网关会员资料同步失败，请稍后重试" : msg);
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
