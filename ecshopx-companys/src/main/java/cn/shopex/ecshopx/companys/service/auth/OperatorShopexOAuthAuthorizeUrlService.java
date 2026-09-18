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

package cn.shopex.ecshopx.companys.service.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OperatorShopexOAuthAuthorizeUrlService {

	private static final String DEFAULT_OPENAPI_SHOPEX_URL = "https://openapi.shopex.cn";

	private final String shopAdminUrl;
	private final String prismCoreAppKey;
	private final String openapiShopexUrl;

	public OperatorShopexOAuthAuthorizeUrlService(
			@Value("${common.shop-admin-url:}") String shopAdminUrl,
			@Value("${ecshopx.thirdparty.prism-core.app-key:}") String prismCoreAppKey,
			@Value("${common.openapi-shopex-url:https://openapi.shopex.cn}") String openapiShopexUrl) {
		this.shopAdminUrl = shopAdminUrl != null ? shopAdminUrl : "";
		this.prismCoreAppKey = prismCoreAppKey != null ? prismCoreAppKey : "";
		this.openapiShopexUrl = openapiShopexUrl != null ? openapiShopexUrl : "";
	}

	public String buildAuthorizeUrl() {
		String callback = shopAdminUrl + "iframeLogin";
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("response_type", "code");
		if (!prismCoreAppKey.isBlank()) {
			queryParams.put("client_id", prismCoreAppKey);
		}
		queryParams.put("redirect_uri", callback);
		queryParams.put("view", "ydsaas_iframe_login");
		queryParams.put("reg", "ydsaas_login");
		queryParams.put("direct_reg_uri", shopAdminUrl);

		StringBuilder query = new StringBuilder();
		for (Map.Entry<String, String> e : queryParams.entrySet()) {
			if (query.length() > 0) {
				query.append('&');
			}
			String v = e.getValue() != null ? e.getValue() : "";
			query.append(e.getKey())
					.append('=')
					.append(URLEncoder.encode(v, StandardCharsets.UTF_8));
		}

		String openapiBase =
				openapiShopexUrl.isBlank() ? DEFAULT_OPENAPI_SHOPEX_URL : openapiShopexUrl;
		String base = openapiBase.replaceAll("/+$", "");
		return base + "/oauth/authorize?" + query;
	}

	public String buildOauthLogoutUrl() {
		String callback = shopAdminUrl + "login";
		String encodedCallback = URLEncoder.encode(callback, StandardCharsets.UTF_8);
		String openapiBase =
				openapiShopexUrl.isBlank() ? DEFAULT_OPENAPI_SHOPEX_URL : openapiShopexUrl;
		String base = openapiBase.replaceAll("/+$", "");
		return base + "/oauth/logout?redirect_uri=" + encodedCallback;
	}
}
