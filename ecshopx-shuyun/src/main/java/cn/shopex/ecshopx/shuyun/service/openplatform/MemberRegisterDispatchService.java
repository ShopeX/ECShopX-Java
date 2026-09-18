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

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D6：建会员成功事件 → 店务 OFFLINE register（异步骨架；失败仅日志，不做 PHP 式补偿删会员）。 */
@Service
public class MemberRegisterDispatchService {

	private static final Logger log = LoggerFactory.getLogger(MemberRegisterDispatchService.class);

	private final MemberRegisterService memberRegisterService;
	private final JdbcTemplate jdbcTemplate;

	public MemberRegisterDispatchService(MemberRegisterService memberRegisterService, JdbcTemplate jdbcTemplate) {
		this.memberRegisterService = memberRegisterService;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void dispatchCreateMemberSuccess(long companyId, long userId, long distributorId, String mobileHint) {
		if (companyId < 1 || userId < 1) {
			return;
		}
		long distId = distributorId;
		String mobile = mobileHint == null ? "" : mobileHint.trim();
		if (distId < 1 || !StringUtils.hasText(mobile)) {
			List<Map<String, Object>> rows =
					jdbcTemplate.queryForList(
							"""
							SELECT mobile, reg_distributor, offline_reg_distributor
							FROM members WHERE company_id=? AND user_id=? LIMIT 1
							""",
							companyId,
							userId);
			if (rows.isEmpty()) {
				log.info("Shuyun member.register dispatch skipped: member not found companyId={} userId={}", companyId, userId);
				return;
			}
			Map<String, Object> m = rows.get(0);
			if (!StringUtils.hasText(mobile)) {
				mobile = m.get("mobile") == null ? "" : String.valueOf(m.get("mobile")).trim();
			}
			if (distId < 1) {
				distId = toLong(m.get("reg_distributor"));
			}
			if (distId < 1) {
				distId = toLong(m.get("offline_reg_distributor"));
			}
		}
		if (distId < 1 || !StringUtils.hasText(mobile)) {
			log.info(
					"Shuyun member.register dispatch skipped: no distributor/mobile companyId={} userId={}",
					companyId,
					userId);
			return;
		}
		memberRegisterService.registerOfflineAfterCreate(companyId, userId, distId, mobile);
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
}
