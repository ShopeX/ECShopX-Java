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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysActivateInfoService {

	private final ShopMenuService shopMenuService;
	private final JdbcTemplate jdbcTemplate;
	private final StringRedisTemplate stringRedisTemplate;
	private final LicenseActivationProperties licenseActivationProperties;

	@Value("${common.h5-base-url:}")
	private String h5BaseUrlRaw;

	@Value("${APP_URL:}")
	private String appUrlRaw;

	@Value("${DISK_DRIVER:}")
	private String diskDriverRaw;

	@Value("${info.app.version:}")
	private String infoAppVersionRaw;

	public CompanysActivateInfoService(
			ShopMenuService shopMenuService,
			JdbcTemplate jdbcTemplate,
			StringRedisTemplate stringRedisTemplate,
			LicenseActivationProperties licenseActivationProperties) {
		this.shopMenuService = shopMenuService;
		this.jdbcTemplate = jdbcTemplate;
		this.stringRedisTemplate = stringRedisTemplate;
		this.licenseActivationProperties = licenseActivationProperties;
	}

	public Map<String, Object> getActivateInfo(long companyId, HttpServletRequest request) {
		String productModel = shopMenuService.resolveProductModelKeyForCompany(Long.valueOf(companyId));
		long expiredAtSec =
				LocalDate.of(2037, 1, 1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("company_id", Long.valueOf(companyId));
		result.put("product_model", productModel);
		result.put("valid", "true");
		result.put("is_valid", Boolean.TRUE);
		result.put("expired_at", Long.valueOf(expiredAtSec));
		result.put("desc", "ecshopX开源商业版本");
		result.put("resouce_id", Integer.valueOf(0));
		result.put("source", "opensource");

		String h5Base = h5BaseUrlRaw == null ? "" : h5BaseUrlRaw.trim();
		String strippedBase = trimLeadingTrailingSlashes(h5Base);
		result.put("h5_url", strippedBase + "?company_id=" + companyId);

		long nowSec = System.currentTimeMillis() / 1000L;
		boolean dueReminder = (expiredAtSec - nowSec) < (30L * 24L * 3600L);
		result.put("due_reminder", Boolean.valueOf(dueReminder));

		result.put("product_model", productModel);
		result.put("version", resolveDisplayVersion());

		String jv = System.getProperty("java.version");
		result.put("java_runtime_version", jv != null ? jv : "");
		String osName = System.getProperty("os.name");
		result.put("os", osName != null ? osName : "");

		String webServer = "";
		if (request != null) {
			webServer = Optional.ofNullable(request.getServletContext())
					.map(ServletContext::getServerInfo)
					.orElse("");
			if (webServer == null) {
				webServer = "";
			}
		}
		result.put("web_server", webServer);

		result.put("db_version", queryDbVersion());

		String bootVer = SpringBootVersion.getVersion();
		result.put("lumen_version", bootVer != null ? bootVer : "");

		result.put("app_url", blankToEmpty(appUrlRaw));
		result.put("disk_driver", blankToEmpty(diskDriverRaw));
		result.put("redis_version", readRedisServerVersionSafe());

		Map<String, Object> license = new LinkedHashMap<>();
		license.put("show_expier_tip", Integer.valueOf(0));
		result.put("license", license);

		return result;
	}

	private String resolveDisplayVersion() {
		String fromLicense = licenseActivationProperties.getVersion();
		if (fromLicense != null && StringUtils.hasText(fromLicense.trim())) {
			return fromLicense.trim();
		}
		if (infoAppVersionRaw != null && StringUtils.hasText(infoAppVersionRaw.trim())) {
			return infoAppVersionRaw.trim();
		}
		return "-";
	}

	private static String blankToEmpty(String raw) {
		if (raw == null) {
			return "";
		}
		String t = raw.trim();
		return t.isEmpty() ? "" : t;
	}

	private static String trimLeadingTrailingSlashes(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int start = 0;
		int end = s.length();
		while (start < end && s.charAt(start) == '/') {
			start++;
		}
		while (end > start && s.charAt(end - 1) == '/') {
			end--;
		}
		return s.substring(start, end);
	}

	private String queryDbVersion() {
		return jdbcTemplate.query("SELECT VERSION()", rs -> {
			if (!rs.next()) {
				return "";
			}
			String v = rs.getString(1);
			return v != null ? v : "";
		});
	}

	private String readRedisServerVersionSafe() {
		try {
			String v = stringRedisTemplate.execute((RedisCallback<String>) connection -> {
				Properties p = connection.serverCommands().info("server");
				if (p == null) {
					return "";
				}
				String rv = p.getProperty("redis_version");
				return rv != null ? rv.trim() : "";
			});
			return v != null ? v : "";
		} catch (RuntimeException e) {
			return "";
		}
	}
}
