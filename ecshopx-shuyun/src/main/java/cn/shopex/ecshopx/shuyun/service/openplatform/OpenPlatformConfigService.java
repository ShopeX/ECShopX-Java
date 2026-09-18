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
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.CompanyShuyunOpenPlatformConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * B1/B2：管理端读写配置。对齐 PHP {@code ShuyunOpenPlatformManageConfigService}。
 */
@Service
public class OpenPlatformConfigService {

	public static final String OFFLINE_PLAT_CODE = "OFFLINE";

	private static final Set<String> ACCESS_TOKEN_PATCH_KEYS = Set.of("access_token", "enabled", "is_enabled");
	private static final Set<String> CREDENTIAL_BOOTSTRAP_KEYS = Set.of("app_id", "app_secret", "enabled", "is_enabled");
	private static final Set<String> SYNC_ENABLE_ONLY_KEYS = Set.of("enabled", "is_enabled");

	private final CompanyShuyunOpenPlatformConfigMapper configMapper;
	private final ShuyunOpenPlatformProperties properties;

	public OpenPlatformConfigService(
			CompanyShuyunOpenPlatformConfigMapper configMapper, ShuyunOpenPlatformProperties properties) {
		this.configMapper = configMapper;
		this.properties = properties;
	}

	public CompanyShuyunOpenPlatformConfig findByCompanyId(long companyId) {
		return configMapper.selectOne(
				new LambdaQueryWrapper<CompanyShuyunOpenPlatformConfig>()
						.eq(CompanyShuyunOpenPlatformConfig::getCompanyId, companyId)
						.last("LIMIT 1"));
	}

	public CompanyShuyunOpenPlatformConfig findByAppId(String appId) {
		if (!StringUtils.hasText(appId)) {
			return null;
		}
		return configMapper.selectOne(
				new LambdaQueryWrapper<CompanyShuyunOpenPlatformConfig>()
						.eq(CompanyShuyunOpenPlatformConfig::getAppId, appId)
						.last("LIMIT 1"));
	}

	public CompanyShuyunOpenPlatformConfig findByAuthValue(String authValue) {
		if (!StringUtils.hasText(authValue)) {
			return null;
		}
		return configMapper.selectOne(
				new LambdaQueryWrapper<CompanyShuyunOpenPlatformConfig>()
						.eq(CompanyShuyunOpenPlatformConfig::getAuthValue, authValue)
						.last("LIMIT 1"));
	}

	/** plat_code 归一化（trim+lower）匹配启用行。 */
	public List<CompanyShuyunOpenPlatformConfig> findEnabledByNormalizedPlatCode(String normalizedPlatCode) {
		if (!StringUtils.hasText(normalizedPlatCode)) {
			return List.of();
		}
		List<CompanyShuyunOpenPlatformConfig> enabled =
				configMapper.selectList(
						new LambdaQueryWrapper<CompanyShuyunOpenPlatformConfig>()
								.eq(CompanyShuyunOpenPlatformConfig::getIsEnabled, 1)
								.isNotNull(CompanyShuyunOpenPlatformConfig::getPlatCode));
		return enabled.stream()
				.filter(r -> r.getPlatCode() != null
						&& normalizedPlatCode.equals(r.getPlatCode().trim().toLowerCase()))
				.toList();
	}

	/** B1：无库记录时返回空壳默认结构，便于管理端表单渲染。 */
	public Map<String, Object> getAdminView(long companyId) {
		CompanyShuyunOpenPlatformConfig row = findByCompanyId(companyId);
		Map<String, Object> view = new LinkedHashMap<>();
		if (row == null) {
			view.put("company_id", companyId);
			view.put("plat_code", OFFLINE_PLAT_CODE);
			view.put("app_id", "");
			view.put("app_secret_masked", "");
			view.put("access_token", "");
			view.put("is_enabled", false);
			view.put("is_over_due", "");
			return view;
		}
		view.put("company_id", row.getCompanyId());
		view.put("plat_code", row.getPlatCode());
		view.put("app_id", row.getAppId());
		view.put("app_secret_masked", maskSecret(row.getAppSecret()));
		view.put("access_token", row.getAccessToken());
		view.put("is_enabled", row.getIsEnabled() != null && row.getIsEnabled() == 1);
		view.put("is_over_due", row.getIsOverDue());
		return view;
	}

	/** B2 */
	public void saveFromAdmin(long companyId, Map<String, Object> input) {
		if (input == null) {
			input = Map.of();
		}
		CompanyShuyunOpenPlatformConfig row = findByCompanyId(companyId);
		if (isAccessTokenOnlyPatch(input)) {
			assertManualAccessTokenGate(row);
		} else if (isSyncEnableOnlyPatch(input)) {
			if (row == null) {
				throw new ResourceException("无配置记录，无法变更启用状态；请先保存应用凭据。");
			}
		} else if (isCredentialBootstrapPatch(input)) {
			if (row == null) {
				row = newRowForCompany(companyId);
			}
		} else {
			assertManageGate(row);
		}
		if (row == null) {
			throw new ResourceException("无配置记录，请先保存应用凭据。");
		}

		if (input.containsKey("app_id")) {
			Object v = input.get("app_id");
			if (v != null && StringUtils.hasText(String.valueOf(v))) {
				row.setAppId(String.valueOf(v).trim());
			}
		}
		if (input.containsKey("app_secret")) {
			Object v = input.get("app_secret");
			if (v != null && StringUtils.hasText(String.valueOf(v))) {
				row.setAppSecret(String.valueOf(v));
			}
		}
		applyAuthValueFromConfigIfEmpty(row);
		if (input.containsKey("access_token")) {
			Object v = input.get("access_token");
			if (v != null && StringUtils.hasText(String.valueOf(v))) {
				row.setAccessToken(String.valueOf(v));
				row.setIsOverDue("0");
			} else {
				row.setAccessToken(null);
			}
		}
		if (input.containsKey("enabled")) {
			if (toBool(input.get("enabled"))) {
				assertEnableGate(row);
				row.setIsEnabled(1);
			} else {
				row.setIsEnabled(0);
			}
		} else if (input.containsKey("is_enabled")) {
			if (toBool(input.get("is_enabled"))) {
				assertEnableGate(row);
				row.setIsEnabled(1);
			} else {
				row.setIsEnabled(0);
			}
		}

		row.setPlatCode(OFFLINE_PLAT_CODE);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getId() == null) {
			row.setCreated(now);
			row.setUpdated(now);
			configMapper.insert(row);
		} else {
			row.setUpdated(now);
			configMapper.updateById(row);
		}
	}

	private CompanyShuyunOpenPlatformConfig newRowForCompany(long companyId) {
		CompanyShuyunOpenPlatformConfig row = new CompanyShuyunOpenPlatformConfig();
		row.setCompanyId(companyId);
		row.setIsEnabled(0);
		return row;
	}

	private void applyAuthValueFromConfigIfEmpty(CompanyShuyunOpenPlatformConfig row) {
		if (StringUtils.hasText(row.getAuthValue())) {
			return;
		}
		String authValue = properties.getAuthValue() == null ? "" : properties.getAuthValue().trim();
		if (StringUtils.hasText(authValue)) {
			row.setAuthValue(authValue);
		}
	}

	private void assertManageGate(CompanyShuyunOpenPlatformConfig row) {
		if (row == null) {
			throw new ResourceException("无配置记录，请先保存应用凭据。");
		}
	}

	private void assertEnableGate(CompanyShuyunOpenPlatformConfig row) {
		if (!StringUtils.hasText(row.getAccessToken())) {
			throw new ResourceException("须先获取有效 access_token 后再开启数云同步。");
		}
		if ("1".equals(row.getIsOverDue())) {
			throw new ResourceException("access_token 已过期，请先刷新 token 后再开启数云同步。");
		}
	}

	private void assertManualAccessTokenGate(CompanyShuyunOpenPlatformConfig row) {
		if (row == null) {
			throw new ResourceException("无配置记录，无法写入 access_token；请先保存应用凭据或在库中建立 company 配置行。");
		}
		if (!StringUtils.hasText(row.getAppId()) || !StringUtils.hasText(row.getAppSecret())) {
			throw new ResourceException("库中缺少 app_id 或 app_secret，无法写入 access_token；请先保存应用凭据。");
		}
	}

	private boolean isSyncEnableOnlyPatch(Map<String, Object> input) {
		if (input.isEmpty()) {
			return false;
		}
		if (!input.containsKey("enabled") && !input.containsKey("is_enabled")) {
			return false;
		}
		for (String key : input.keySet()) {
			if (!SYNC_ENABLE_ONLY_KEYS.contains(key)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAccessTokenOnlyPatch(Map<String, Object> input) {
		if (input.isEmpty() || !input.containsKey("access_token")) {
			return false;
		}
		for (String key : input.keySet()) {
			if (!ACCESS_TOKEN_PATCH_KEYS.contains(key)) {
				return false;
			}
		}
		return true;
	}

	private boolean isCredentialBootstrapPatch(Map<String, Object> input) {
		if (input.isEmpty()) {
			return false;
		}
		if (!input.containsKey("app_id") && !input.containsKey("app_secret")) {
			return false;
		}
		for (String key : input.keySet()) {
			if (!CREDENTIAL_BOOTSTRAP_KEYS.contains(key)) {
				return false;
			}
		}
		return true;
	}

	static String maskSecret(String secret) {
		if (!StringUtils.hasText(secret)) {
			return "";
		}
		if (secret.length() <= 4) {
			return "****";
		}
		return "****" + secret.substring(secret.length() - 4);
	}

	private static boolean toBool(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v == null) {
			return false;
		}
		String s = String.valueOf(v).trim().toLowerCase();
		return "1".equals(s) || "true".equals(s) || "yes".equals(s) || "on".equals(s);
	}
}
