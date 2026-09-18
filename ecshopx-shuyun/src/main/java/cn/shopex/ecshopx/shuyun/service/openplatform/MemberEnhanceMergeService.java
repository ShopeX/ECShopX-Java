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

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberEnhanceMergePort;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * D8：Front 读会员时合并 enhance.member.post 资料字段。
 * 对齐 PHP {@code MemberService::mergeShuyunOpenPlatformEnhanceIntoMemberInfo}。
 */
@Service
@Primary
public class MemberEnhanceMergeService implements ShuyunOpenPlatformMemberEnhanceMergePort {

	private static final Logger log = LoggerFactory.getLogger(MemberEnhanceMergeService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public MemberEnhanceMergeService(
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
	public void mergeEnhanceIntoMemberInfo(long companyId, long userId, Map<String, Object> memberInfo) {
		if (memberInfo == null || memberInfo.isEmpty() || !isOpenPlatformMemberEnabled(companyId) || userId < 1) {
			return;
		}
		long regDistributorId = toLong(memberInfo.get("reg_distributor"));
		if (regDistributorId < 1) {
			return;
		}
		List<Map<String, Object>> distRows =
				jdbcTemplate.queryForList(
						"""
						SELECT distributor_id FROM distribution_distributor
						WHERE company_id=? AND distributor_id=? LIMIT 1
						""",
						companyId,
						regDistributorId);
		if (distRows.isEmpty()) {
			return;
		}
		Map<String, Object> snapshot = queryEnhanceSnapshot(companyId, userId, regDistributorId);
		if (snapshot == null || snapshot.isEmpty()) {
			return;
		}
		String name = stringVal(snapshot.get("name"));
		if (StringUtils.hasText(name)) {
			memberInfo.put("username", name);
			putRequestField(memberInfo, "username", name);
		}
		String birthday = stringVal(snapshot.get("birthday"));
		if (StringUtils.hasText(birthday)) {
			memberInfo.put("birthday", birthday);
			putRequestField(memberInfo, "birthday", birthday);
		}
		String sex = mapGenderToLocalSex(stringVal(snapshot.get("gender")));
		if (sex != null) {
			memberInfo.put("sex", sex);
			putRequestField(memberInfo, "sex", sex);
		}
	}

	private Map<String, Object> queryEnhanceSnapshot(long companyId, long userId, long distributorId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", String.valueOf(userId));
		body.put("platCode", "OFFLINE");
		body.put("shopId", resolveShopId(distributorId));
		try {
			JsonNode raw =
					gatewayClient.postJson(
							companyId, ShuyunOpenPlatformGatewayActions.MEMBER_ENHANCE_POST, body, "offline");
			return normalizePayload(raw);
		} catch (Exception e) {
			log.warn(
					"Shuyun OPEN enhance.member merge skipped. companyId={} userId={} err={}",
					companyId,
					userId,
					e.getMessage());
			return null;
		}
	}

	static Map<String, Object> normalizePayload(JsonNode raw) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (raw == null || raw.isNull()) {
			return out;
		}
		JsonNode node = raw;
		if (raw.has("data") && !raw.get("data").isNull()) {
			node = raw.get("data");
		}
		if (node.has("member") && node.get("member").isObject()) {
			node = node.get("member");
		}
		for (String k : new String[] {"name", "realName", "nick", "nickName", "memberName"}) {
			JsonNode v = node.get(k);
			if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
				out.put("name", v.asText().trim());
				break;
			}
		}
		for (String k : new String[] {"birthday", "birth"}) {
			JsonNode v = node.get(k);
			if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
				out.put("birthday", v.asText().trim());
				break;
			}
		}
		for (String k : new String[] {"gender", "sex"}) {
			JsonNode v = node.get(k);
			if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
				out.put("gender", v.asText().trim());
				break;
			}
		}
		return out;
	}

	static String mapGenderToLocalSex(String gender) {
		if (!StringUtils.hasText(gender)) {
			return null;
		}
		String g = gender.trim().toUpperCase(Locale.ROOT);
		if ("1".equals(g) || "M".equals(g) || "MALE".equals(g) || "男".equals(gender.trim())) {
			return "1";
		}
		if ("2".equals(g) || "F".equals(g) || "FEMALE".equals(g) || "女".equals(gender.trim())) {
			return "2";
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void putRequestField(Map<String, Object> memberInfo, String key, Object value) {
		Object rf = memberInfo.get("requestFields");
		if (!(rf instanceof Map<?, ?>)) {
			return;
		}
		((Map<String, Object>) rf).put(key, value);
	}

	private String resolveShopId(long distributorId) {
		String suffix =
				properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(stringVal(v));
		} catch (Exception e) {
			return 0L;
		}
	}
}
