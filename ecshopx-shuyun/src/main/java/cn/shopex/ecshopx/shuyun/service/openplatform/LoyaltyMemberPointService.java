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

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformPointPort;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D9：开放平台积分写/读 changelog。 */
@Service
@Primary
public class LoyaltyMemberPointService implements ShuyunOpenPlatformPointPort {

	private static final Logger log = LoggerFactory.getLogger(LoyaltyMemberPointService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public LoyaltyMemberPointService(
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
	public boolean isOpenPlatformPointEnabled(long companyId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		return InboundSignedCallbackPreparer.isEligible(cfg);
	}

	@Override
	public boolean changePoint(
			long companyId,
			long userId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			Map<String, Object> otherParams) {
		if (!isOpenPlatformPointEnabled(companyId) || userId < 1 || point == 0) {
			return false;
		}
		long reg = resolveRegDistributor(companyId, userId);
		if (reg < 1) {
			log.info(
					"Shuyun point.change skipped: missing reg_distributor companyId={} userId={}",
					companyId,
					userId);
			return false;
		}
		Map<String, Object> body =
				PointChangePayloadBuilder.build(
						userId,
						companyId,
						point,
						plus,
						journalType,
						record,
						orderId == null ? "" : orderId,
						resolveShopId(reg),
						otherParams);
		try {
			gatewayClient.postJson(
					companyId, ShuyunOpenPlatformGatewayActions.MEMBER_POINT_CHANGE, body, "offline");
			log.info(
					"Shuyun point.change ok companyId={} userId={} changePoint={}",
					companyId,
					userId,
					body.get("changePoint"));
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun point.change failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return false;
		}
	}

	@Override
	public Map<String, Object> searchChangelog(
			long companyId, long userId, long regDistributorId, int pageNo, int pageSize) {
		if (!isOpenPlatformPointEnabled(companyId) || userId < 1 || regDistributorId < 1) {
			return null;
		}
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("id", String.valueOf(userId));
		query.put("platCode", "OFFLINE");
		query.put("shopId", resolveShopId(regDistributorId));
		query.put("pageNum", Math.max(1, pageNo));
		query.put("pageSize", Math.max(1, pageSize));
		try {
			JsonNode node =
					gatewayClient.getQuery(
							companyId,
							ShuyunOpenPlatformGatewayActions.MEMBER_POINT_CHANGELOG_SEARCH,
							query,
							"offline");
			Map<String, Object> out = new LinkedHashMap<>();
			if (node != null && node.isObject()) {
				out.put("totals", node.path("totals").asInt(0));
				out.put("pageNum", node.path("pageNum").asInt(pageNo));
				out.put("pageSize", node.path("pageSize").asInt(pageSize));
				out.put("list", node.path("list"));
			} else {
				out.put("totals", 0);
				out.put("pageNum", pageNo);
				out.put("pageSize", pageSize);
				out.put("list", List.of());
			}
			return out;
		} catch (Exception e) {
			log.error(
					"Shuyun point.changelog.search failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return null;
		}
	}

	@Override
	public Long queryValidPoint(long companyId, long userId) {
		if (!isOpenPlatformPointEnabled(companyId) || userId < 1) {
			return null;
		}
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (cfg == null || !StringUtils.hasText(cfg.getAuthValue())) {
			return null;
		}
		long reg = resolveRegDistributor(companyId, userId);
		if (reg < 1) {
			return null;
		}
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("id", String.valueOf(userId));
		query.put("platCode", "OFFLINE");
		query.put("shopId", resolveShopId(reg));
		query.put("tenant", cfg.getAuthValue().trim());
		try {
			JsonNode node =
					gatewayClient.getQuery(
							companyId,
							ShuyunOpenPlatformGatewayActions.MEMBER_ENHANCE_QUERY_DETAIL,
							query,
							"offline");
			JsonNode data = node;
			if (node != null && node.has("data") && node.get("data").isObject()) {
				data = node.get("data");
			}
			if (data == null || !data.isObject()) {
				return null;
			}
			for (String key : List.of("validPoint", "pointAsserts")) {
				JsonNode v = data.get(key);
				if (v != null && !v.isNull() && !(v.isTextual() && v.asText().isBlank())) {
					return v.asLong();
				}
			}
			return null;
		} catch (Exception e) {
			log.warn(
					"Shuyun enhance.member.query.detail point query skipped companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return null;
		}
	}

	private long resolveRegDistributor(long companyId, long userId) {
		Long reg =
				jdbcTemplate.query(
						"""
						SELECT COALESCE(NULLIF(reg_distributor,0), NULLIF(offline_reg_distributor,0), 0)
						FROM members WHERE company_id=? AND user_id=? LIMIT 1
						""",
						rs -> rs.next() ? rs.getLong(1) : 0L,
						companyId,
						userId);
		return reg == null ? 0L : reg;
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
