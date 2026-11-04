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

package cn.shopex.ecshopx.youshu.integration;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF guard for Tencent Youshu backend API base URIs.
 *
 * <p>Official hosts (账号说明): prod {@code zhls.qq.com}, sandbox {@code test.zhls.qq.com}; https only.
 */
public final class YoushuBaseUriGuard {

	private static final Set<String> ALLOWED_HOSTS = Set.of("zhls.qq.com", "test.zhls.qq.com");

	private static final String REJECT_MSG = "有数 API 地址仅允许 https://zhls.qq.com 或 https://test.zhls.qq.com";

	private YoushuBaseUriGuard() {}

	/** Blank/null is allowed for optional setting fields. */
	public static void requireOptional(String baseUri) {
		if (baseUri == null || baseUri.isBlank()) {
			return;
		}
		normalizeAndRequire(baseUri);
	}

	/** Normalize trailing slashes and require an allowlisted https host. */
	public static String normalizeAndRequire(String baseUri) {
		if (baseUri == null || baseUri.isBlank()) {
			throw new BadRequestException(REJECT_MSG);
		}
		String trimmed = baseUri.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		URI uri;
		try {
			uri = URI.create(trimmed);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException(REJECT_MSG);
		}
		String scheme = uri.getScheme();
		if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
			throw new BadRequestException(REJECT_MSG);
		}
		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			throw new BadRequestException(REJECT_MSG);
		}
		String normalizedHost = host.toLowerCase(Locale.ROOT);
		if (!ALLOWED_HOSTS.contains(normalizedHost)) {
			throw new BadRequestException(REJECT_MSG);
		}
		if (uri.getUserInfo() != null) {
			throw new BadRequestException(REJECT_MSG);
		}
		String path = uri.getRawPath();
		if (path != null && !path.isEmpty() && !"/".equals(path)) {
			throw new BadRequestException(REJECT_MSG);
		}
		if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
			throw new BadRequestException(REJECT_MSG);
		}
		return "https://" + normalizedHost;
	}
}
