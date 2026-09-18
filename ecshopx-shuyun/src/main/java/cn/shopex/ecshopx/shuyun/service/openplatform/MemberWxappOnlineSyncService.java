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
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformWxappMemberSyncPort;
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
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * wxapp 登录后 OPEN 同步：register → enhance 卡号 → bind.push → 写 {@code shuyun_open_online_wxapp_sync_at}。
 * 店务已 offline_reg 时仅 bind.push。
 */
@Service
@Primary
public class MemberWxappOnlineSyncService implements ShuyunOpenPlatformWxappMemberSyncPort {

	private static final Logger log = LoggerFactory.getLogger(MemberWxappOnlineSyncService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final MemberBindPushService bindPushService;
	private final MemberEnhanceCardSyncService enhanceCardSyncService;
	private final JdbcTemplate jdbcTemplate;

	public MemberWxappOnlineSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			MemberBindPushService bindPushService,
			MemberEnhanceCardSyncService enhanceCardSyncService,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.bindPushService = bindPushService;
		this.enhanceCardSyncService = enhanceCardSyncService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean syncWxappOnlineIfEnabled(
			long companyId,
			long userId,
			String mobile,
			String unionId,
			String openId,
			long distributorIdHint,
			boolean failHard) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		if (userId < 1 || !StringUtils.hasText(mobile) || !StringUtils.hasText(unionId) || !StringUtils.hasText(openId)) {
			return false;
		}
		Map<String, Object> member = loadMember(companyId, userId);
		if (member == null) {
			return false;
		}
		int syncedAt = toInt(member.get("shuyun_open_online_wxapp_sync_at"));
		if (syncedAt > 0) {
			return true;
		}
		long distributorId = distributorIdHint > 0 ? distributorIdHint : resolveDistributorId(member);
		if (distributorId < 1) {
			log.info(
					"Shuyun wxapp sync skipped: no distributor companyId={} userId={}",
					companyId,
					userId);
			return false;
		}
		try {
			int offlineReg = toInt(member.get("offline_reg_distributor"));
			if (offlineReg > 0) {
				bindPushService.pushSingle(
						companyId, offlineReg, String.valueOf(userId), unionId.trim(), openId.trim());
				markWxappSynced(companyId, userId);
				return true;
			}
			registerThenEnhanceBind(companyId, userId, distributorId, mobile.trim(), unionId.trim(), openId.trim());
			return true;
		} catch (Exception e) {
			log.warn(
					"Shuyun wxapp online sync failed companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			if (failHard) {
				throw new ResourceException("会员绑定失败，请稍后重试");
			}
			return false;
		}
	}

	private void registerThenEnhanceBind(
			long companyId, long userId, long distributorId, String mobile, String unionId, String openId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", String.valueOf(userId));
		payload.put("platCode", "OFFLINE");
		payload.put("shopId", shopId(distributorId));
		payload.put("mobile", mobile);
		if (StringUtils.hasText(unionId)) {
			payload.put("omid", unionId);
		}
		gatewayClient.postJson(companyId, ShuyunOpenPlatformGatewayActions.MEMBER_REGISTER, payload, "offline");
		enhanceCardSyncService.syncUserCardCodeAfterRegister(companyId, userId, distributorId);
		bindPushService.pushSingle(companyId, distributorId, String.valueOf(userId), unionId, openId);
		markWxappSynced(companyId, userId);
		log.info("Shuyun wxapp online sync ok companyId={} userId={}", companyId, userId);
	}

	private void markWxappSynced(long companyId, long userId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		jdbcTemplate.update(
				"""
				UPDATE members SET shuyun_open_online_wxapp_sync_at=?
				WHERE company_id=? AND user_id=?
				""",
				now,
				companyId,
				userId);
	}

	private Map<String, Object> loadMember(long companyId, long userId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT user_id, mobile, reg_distributor, offline_reg_distributor,
						       shuyun_open_online_wxapp_sync_at
						FROM members WHERE company_id=? AND user_id=? LIMIT 1
						""",
						companyId,
						userId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private static long resolveDistributorId(Map<String, Object> member) {
		long reg = toLong(member.get("reg_distributor"));
		if (reg > 0) {
			return reg;
		}
		return toLong(member.get("offline_reg_distributor"));
	}

	private String shopId(long distributorId) {
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0;
		}
	}
}
