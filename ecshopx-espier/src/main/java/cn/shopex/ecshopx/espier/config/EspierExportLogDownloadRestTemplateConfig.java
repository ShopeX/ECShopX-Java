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

package cn.shopex.ecshopx.espier.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EspierExportLogDownloadRestTemplateConfig {

	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 120_000;

	private static final SSLContext TRUST_ALL_TLS;

	static {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(
					null,
					new TrustManager[] {
						new X509TrustManager() {
							@Override
							public void checkClientTrusted(X509Certificate[] chain, String authType) {}

							@Override
							public void checkServerTrusted(X509Certificate[] chain, String authType) {}

							@Override
							public X509Certificate[] getAcceptedIssuers() {
								return new X509Certificate[0];
							}
						}
					},
					new SecureRandom());
			TRUST_ALL_TLS = ctx;
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	@Bean("espierExportLogDownloadRestTemplate")
	public RestTemplate espierExportLogDownloadRestTemplate() {
		InsecureHttpsClientHttpRequestFactory factory = new InsecureHttpsClientHttpRequestFactory();
		factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
		factory.setReadTimeout(READ_TIMEOUT_MS);
		return new RestTemplate(factory);
	}

	private static final class InsecureHttpsClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

		@Override
		protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
			super.prepareConnection(connection, httpMethod);
			if (connection instanceof HttpsURLConnection https) {
				https.setSSLSocketFactory(TRUST_ALL_TLS.getSocketFactory());
				https.setHostnameVerifier((hostname, session) -> true);
			}
		}
	}
}
