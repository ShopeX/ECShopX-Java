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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeBatchSavePort;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** B3 / D10：等级 query → 本地 batchSave。 */
@Service
public class LoyaltyGradeSyncService {

	private static final Logger log = LoggerFactory.getLogger(LoyaltyGradeSyncService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final OpenapiMemberCardGradeBatchSavePort gradeBatchSavePort;
	private final JdbcTemplate jdbcTemplate;

	public LoyaltyGradeSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			OpenapiMemberCardGradeBatchSavePort gradeBatchSavePort,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.gradeBatchSavePort = gradeBatchSavePort;
		this.jdbcTemplate = jdbcTemplate;
	}

	public Map<String, Object> syncByCompanyIdWithReport(long companyId) {
		try {
			int synced = syncByCompanyId(companyId);
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("ok", true);
			ok.put("synced_count", synced);
			return ok;
		} catch (GradeSyncValidationException e) {
			Map<String, Object> fail = new LinkedHashMap<>();
			fail.put("ok", false);
			fail.put("error_code", "LOYALTY_GRADE_SYNC_VALIDATION_FAILED");
			fail.put("message", e.getMessage());
			fail.put("failures", e.getFailures());
			return fail;
		}
	}

	public int syncByCompanyId(long companyId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			throw new ResourceException("数云开放网关未就绪或未完成授权，暂无法同步等级");
		}
		Long virtualDistributorId =
				jdbcTemplate.query(
						"""
						SELECT distributor_id FROM distribution_distributor
						WHERE company_id=? AND distributor_self=1 LIMIT 1
						""",
						rs -> rs.next() ? rs.getLong(1) : null,
						companyId);
		if (virtualDistributorId == null || virtualDistributorId < 1) {
			throw new ResourceException("Virtual distributor not found for loyalty grade sync.");
		}
		String shopId = resolveShopId(virtualDistributorId);
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("shopId", shopId);
		query.put("platCode", "OFFLINE");
		JsonNode node;
		try {
			node =
					gatewayClient.getQuery(
							companyId,
							ShuyunOpenPlatformGatewayActions.LOYALTY_CARD_GRADE_QUERY,
							query,
							"offline");
		} catch (Exception e) {
			throw new ResourceException(
					StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Loyalty grade query failed");
		}
		JsonNode data = node;
		if (node != null && node.has("data")) {
			data = node.get("data");
		}
		if (data == null || !data.isObject()) {
			throw new ResourceException("Loyalty grade query returned empty result.");
		}
		JsonNode gradesNode = data.get("grades");
		if (gradesNode == null || !gradesNode.isArray()) {
			throw new ResourceException("Loyalty grade query missing grades array.");
		}
		List<Map<String, Object>> mapped = new ArrayList<>();
		List<Map<String, Object>> failures = new ArrayList<>();
		int idx = 0;
		for (JsonNode row : gradesNode) {
			if (row == null || !row.isObject()) {
				failures.add(Map.of("index", idx, "reason", "invalid_grade_row", "message", "grade row is not object"));
				idx++;
				continue;
			}
			long gradeId = row.path("gradeId").asLong(0);
			String gradeName = row.path("name").asText("").trim();
			long gradeLevel = row.path("id").asLong(0);
			if (gradeId <= 0 || !StringUtils.hasText(gradeName) || gradeLevel <= 0) {
				Map<String, Object> f = new LinkedHashMap<>();
				f.put("index", idx);
				f.put("reason", "invalid_grade_row");
				f.put("message", "gradeId/name/id invalid");
				f.put("gradeId", row.path("gradeId").asText(null));
				f.put("name", row.path("name").asText(null));
				f.put("id", row.path("id").asText(null));
				failures.add(f);
				idx++;
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("grade_id", String.valueOf(gradeId));
			m.put("grade_name", gradeName);
			m.put("grade_level", gradeLevel);
			mapped.add(m);
			idx++;
		}
		if (!failures.isEmpty()) {
			throw new GradeSyncValidationException(failures, "Loyalty grade sync failed with row-level validation errors.");
		}
		gradeBatchSavePort.batchSave(companyId, mapped);
		log.info("Shuyun loyalty grade sync ok companyId={} synced={}", companyId, mapped.size());
		return mapped.size();
	}

	private String resolveShopId(long distributorId) {
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String id = String.valueOf(distributorId);
		if (!StringUtils.hasText(suffix) || id.endsWith(suffix)) {
			return id;
		}
		return id + suffix;
	}

	public static final class GradeSyncValidationException extends RuntimeException {
		private final List<Map<String, Object>> failures;

		public GradeSyncValidationException(List<Map<String, Object>> failures, String message) {
			super(message);
			this.failures = failures;
		}

		public List<Map<String, Object>> getFailures() {
			return failures;
		}
	}
}
