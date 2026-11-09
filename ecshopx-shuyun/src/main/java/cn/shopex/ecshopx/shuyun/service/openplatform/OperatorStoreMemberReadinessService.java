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
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 移动收银：在拉取购物车前，将会员登记到当前门店对应的开放平台线下侧（与店务建档后同步语义一致）。对齐 PHP
 * {@code OperatorStoreMemberReadinessService}。
 */
@Service
public class OperatorStoreMemberReadinessService {

	private static final Logger log = LoggerFactory.getLogger(OperatorStoreMemberReadinessService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final MemberRegisterService memberRegisterService;
	private final JdbcTemplate jdbcTemplate;

	public OperatorStoreMemberReadinessService(
			OpenPlatformConfigService openPlatformConfigService,
			MemberRegisterService memberRegisterService,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.memberRegisterService = memberRegisterService;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @return map with {@code synced} and {@code skipped}
	 */
	public Map<String, Object> ensureOfflineMemberAtStore(long companyId, long distributorId, long userId) {
		if (userId <= 0) {
			throw new ResourceException("请指定会员");
		}
		if (distributorId <= 0) {
			throw new ResourceException("请选择门店");
		}

		CompanyShuyunOpenPlatformConfig config = openPlatformConfigService.findByCompanyId(companyId);
		if (config == null || config.getIsEnabled() == null || config.getIsEnabled() != 1) {
			return skipped();
		}
		if (!InboundSignedCallbackPreparer.isEligible(config)) {
			return skipped();
		}

		Map<String, Object> memberRow = loadMember(companyId, userId);
		if (memberRow == null || !memberRow.containsKey("user_id") || !memberRow.containsKey("company_id")) {
			throw new ResourceException("未找到该会员");
		}
		long memberCompanyId = toLong(memberRow.get("company_id"));
		if (memberCompanyId != companyId) {
			throw new ResourceException("未找到该会员");
		}

		if (MemberOpenPlatformSyncState.isRegisteredWithOpenPlatform(memberRow)) {
			return skipped();
		}

		String mobile = memberRow.get("mobile") == null ? "" : String.valueOf(memberRow.get("mobile")).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("会员手机号缺失，无法完成门店登记");
		}

		boolean registerSucceeded = false;
		try {
			registerSucceeded =
					memberRegisterService.registerOfflineAfterCreate(
							companyId, userId, distributorId, mobile);
			if (!registerSucceeded) {
				throw new ResourceException("门店会员登记失败：数云 member.register 未成功");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn(
					"Operator store member readiness: open platform member.register failed. companyId={} userId={}"
							+ " distributorId={} err={}",
					companyId,
					userId,
					distributorId,
					e.getMessage());
			throw new ResourceException("门店会员登记失败：" + e.getMessage());
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("synced", true);
		out.put("skipped", false);
		return out;
	}

	private static Map<String, Object> skipped() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("synced", false);
		out.put("skipped", true);
		return out;
	}

	private Map<String, Object> loadMember(long companyId, long userId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT user_id, company_id, mobile, offline_reg_distributor,
						       shuyun_open_online_wxapp_sync_at
						FROM members WHERE company_id=? AND user_id=? LIMIT 1
						""",
						companyId,
						userId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private static long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
