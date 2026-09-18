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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D6：会员 OFFLINE {@code shuyun.loyalty.member.register}。 */
@Service
public class MemberRegisterService {

	private static final Logger log = LoggerFactory.getLogger(MemberRegisterService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final MemberEnhanceCardSyncService enhanceCardSyncService;
	private final JdbcTemplate jdbcTemplate;

	public MemberRegisterService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			MemberEnhanceCardSyncService enhanceCardSyncService,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.enhanceCardSyncService = enhanceCardSyncService;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 店务建档 OFFLINE register；成功后写 {@code offline_reg_distributor}。
	 *
	 * @return true 当网关调用成功并落库扩展列
	 */
	public boolean registerOfflineAfterCreate(long companyId, long userId, long distributorId, String mobile) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		if (userId < 1 || distributorId < 1 || !StringUtils.hasText(mobile)) {
			log.info(
					"Shuyun member.register skipped: invalid args companyId={} userId={} distributorId={}",
					companyId,
					userId,
					distributorId);
			return false;
		}
		String shopId = resolveShopId(distributorId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", String.valueOf(userId));
		payload.put("platCode", "OFFLINE");
		payload.put("shopId", shopId);
		payload.put("mobile", mobile.trim());
		try {
			gatewayClient.postJson(companyId, ShuyunOpenPlatformGatewayActions.MEMBER_REGISTER, payload, "offline");
			jdbcTemplate.update(
					"""
					UPDATE members SET offline_reg_distributor=?
					WHERE company_id=? AND user_id=?
					""",
					(int) distributorId,
					companyId,
					userId);
			enhanceCardSyncService.syncUserCardCodeAfterRegister(companyId, userId, distributorId);
			log.info(
					"Shuyun member.register offline ok companyId={} userId={} distributorId={}",
					companyId,
					userId,
					distributorId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun member.register failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return false;
		}
	}

	/**
	 * wxapp 线上 register 骨架：成功写 {@code shuyun_open_online_wxapp_sync_at}。
	 * bind.push / enhance 卡号后续再补。
	 */
	public boolean registerWxappOnline(
			long companyId, long userId, long distributorId, String mobile, String unionId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		Integer syncedAt =
				jdbcTemplate.query(
						"""
						SELECT shuyun_open_online_wxapp_sync_at FROM members
						WHERE company_id=? AND user_id=? LIMIT 1
						""",
						rs -> rs.next() ? (Integer) rs.getObject(1) : null,
						companyId,
						userId);
		if (syncedAt != null && syncedAt > 0) {
			return true;
		}
		if (userId < 1 || distributorId < 1 || !StringUtils.hasText(mobile)) {
			return false;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", String.valueOf(userId));
		payload.put("platCode", "OFFLINE");
		payload.put("shopId", resolveShopId(distributorId));
		payload.put("mobile", mobile.trim());
		if (StringUtils.hasText(unionId)) {
			payload.put("omid", unionId.trim());
		}
		try {
			gatewayClient.postJson(companyId, ShuyunOpenPlatformGatewayActions.MEMBER_REGISTER, payload, "offline");
			int now = (int) (System.currentTimeMillis() / 1000L);
			jdbcTemplate.update(
					"""
					UPDATE members SET shuyun_open_online_wxapp_sync_at=?
					WHERE company_id=? AND user_id=?
					""",
					now,
					companyId,
					userId);
			log.info("Shuyun member.register wxapp ok companyId={} userId={}", companyId, userId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun member.register wxapp failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return false;
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
