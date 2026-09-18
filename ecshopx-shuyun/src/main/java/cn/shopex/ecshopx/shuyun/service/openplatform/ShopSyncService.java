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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** D1：店铺同步 shop.batch.register。 */
@Service
public class ShopSyncService {

	private static final Logger log = LoggerFactory.getLogger(ShopSyncService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public ShopSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean syncShop(long companyId, long distributorId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (cfg == null || !org.springframework.util.StringUtils.hasText(cfg.getAuthValue())) {
			log.info(
					"Shuyun shop sync skipped: empty auth_value companyId={} distributorId={}",
					companyId,
					distributorId);
			return false;
		}
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			log.info(
					"Shuyun shop sync skipped: not eligible companyId={} distributorId={}",
					companyId,
					distributorId);
			return false;
		}
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT distributor_id, name, address, mobile, is_valid, province, city, area
						FROM distribution_distributor
						WHERE company_id=? AND distributor_id=?
						LIMIT 1
						""",
						companyId,
						distributorId);
		if (rows.isEmpty()) {
			log.warn("Shuyun shop sync: distributor not found companyId={} distributorId={}", companyId, distributorId);
			return false;
		}
		Map<String, Object> d = rows.get(0);
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String shopId = distributorId + suffix;
		Map<String, Object> shop = new LinkedHashMap<>();
		shop.put("shop_id", shopId);
		shop.put("shop_name", String.valueOf(d.getOrDefault("name", "")));
		shop.put("plat_code", "OFFLINE");
		shop.put("status", "1");
		shop.put("address", composeAddress(d));
		shop.put("mobile", String.valueOf(d.getOrDefault("mobile", "")));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("tenant_name", cfg.getAppId());
		body.put("app_id", cfg.getAppId());
		body.put("shops", List.of(shop));

		try {
			gatewayClient.postJson(
					companyId, ShuyunOpenPlatformGatewayActions.SHOP_BATCH_REGISTER, body, "offline");
			log.info("Shuyun shop_sync ok companyId={} distributorId={}", companyId, distributorId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun shop sync failed companyId={} distributorId={} err={}",
					companyId,
					distributorId,
					e.getMessage());
			return false;
		}
	}

	private static String composeAddress(Map<String, Object> d) {
		return String.valueOf(d.getOrDefault("province", ""))
				+ String.valueOf(d.getOrDefault("city", ""))
				+ String.valueOf(d.getOrDefault("area", ""))
				+ String.valueOf(d.getOrDefault("address", ""));
	}
}
