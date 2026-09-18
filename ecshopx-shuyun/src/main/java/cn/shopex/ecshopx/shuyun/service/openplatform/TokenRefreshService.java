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

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.CompanyShuyunOpenPlatformConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * O1：GET open-client 触发刷新；不写库。新 token 靠 C1。
 * 对齐 PHP {@code ShuyunOpenPlatformTokenRefreshService} /
 * {@code ShuyunOpenPlatformScheduledTokenRefreshRunner}。
 */
@Service
public class TokenRefreshService {

	private static final Logger log = LoggerFactory.getLogger(TokenRefreshService.class);

	private final CompanyShuyunOpenPlatformConfigMapper configMapper;
	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final RestTemplate restTemplate;

	public TokenRefreshService(
			CompanyShuyunOpenPlatformConfigMapper configMapper,
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties) {
		this.configMapper = configMapper;
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		int timeoutMs = (int) (properties.getTimeoutSeconds() * 1000);
		factory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 10_000)));
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));
		this.restTemplate = new RestTemplate(factory);
	}

	/**
	 * @param companyId null=扫 eligible；非空则指定公司（跳过 is_enabled）
	 */
	public Map<String, Integer> run(Long companyId) {
		List<CompanyShuyunOpenPlatformConfig> rows;
		boolean ignoreEnabled = companyId != null;
		if (companyId != null) {
			CompanyShuyunOpenPlatformConfig row = openPlatformConfigService.findByCompanyId(companyId);
			rows = row != null ? List.of(row) : List.of();
		} else {
			rows = findEligibleForScheduledRefresh();
		}
		int attempted = rows.size();
		int ok = 0;
		for (CompanyShuyunOpenPlatformConfig row : rows) {
			if (triggerRefresh(row, ignoreEnabled)) {
				ok++;
			}
		}
		Map<String, Integer> stats = new LinkedHashMap<>();
		stats.put("attempted", attempted);
		stats.put("ok", ok);
		stats.put("failed", attempted - ok);
		return stats;
	}

	List<CompanyShuyunOpenPlatformConfig> findEligibleForScheduledRefresh() {
		return configMapper.selectList(
				new LambdaQueryWrapper<CompanyShuyunOpenPlatformConfig>()
						.eq(CompanyShuyunOpenPlatformConfig::getIsEnabled, 1)
						.isNotNull(CompanyShuyunOpenPlatformConfig::getAppId)
						.ne(CompanyShuyunOpenPlatformConfig::getAppId, "")
						.isNotNull(CompanyShuyunOpenPlatformConfig::getAccessToken)
						.ne(CompanyShuyunOpenPlatformConfig::getAccessToken, "")
						.and(w -> w.isNull(CompanyShuyunOpenPlatformConfig::getIsOverDue)
								.or()
								.ne(CompanyShuyunOpenPlatformConfig::getIsOverDue, "1")));
	}

	boolean triggerRefresh(CompanyShuyunOpenPlatformConfig config, boolean ignoreEnabledCheck) {
		if (!ignoreEnabledCheck && (config.getIsEnabled() == null || config.getIsEnabled() != 1)) {
			log.debug(
					"数云 open-client Token 刷新跳过：is_enabled!=1 companyId={}",
					config.getCompanyId());
			return false;
		}
		String appId = config.getAppId();
		if (!StringUtils.hasText(appId)) {
			log.debug("数云 open-client Token 刷新跳过：无 app_id companyId={}", config.getCompanyId());
			return false;
		}
		if ("1".equals(config.getIsOverDue())) {
			log.debug(
					"数云 open-client Token 刷新跳过：is_over_due=1 companyId={} appId={}",
					config.getCompanyId(),
					appId);
			return false;
		}
		String requestUrl = buildRefreshUri(appId);
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(URI.create(requestUrl), String.class);
			int status = response.getStatusCode().value();
			boolean ok = status >= 200 && status < 300;
			if (ok) {
				log.info(
						"数云 open-client Token 刷新 GET 成功 requestUrl={} companyId={} appId={} httpStatus={}",
						requestUrl,
						config.getCompanyId(),
						appId,
						status);
			} else {
				log.warn(
						"数云 open-client Token 刷新 GET 非成功状态 requestUrl={} companyId={} appId={} httpStatus={}",
						requestUrl,
						config.getCompanyId(),
						appId,
						status);
			}
			return ok;
		} catch (RestClientException e) {
			log.error(
					"数云 open-client Token 刷新 GET 异常 requestUrl={} companyId={} appId={} error={}",
					requestUrl,
					config.getCompanyId(),
					appId,
					e.getMessage());
			return false;
		}
	}

	String buildRefreshUri(String appId) {
		String base = properties.getTokenRefreshBaseUri();
		if (base == null) {
			base = "";
		}
		base = base.replaceAll("/+$", "");
		return base + "/client/callback/token/" + appId + "/v2";
	}
}
