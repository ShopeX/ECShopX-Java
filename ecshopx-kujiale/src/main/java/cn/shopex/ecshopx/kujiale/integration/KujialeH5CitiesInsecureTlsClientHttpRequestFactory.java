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

package cn.shopex.ecshopx.kujiale.integration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Optional {@link SimpleClientHttpRequestFactory} that disables TLS certificate and hostname verification
 * for HTTPS outbound calls. Enable only via {@code ecshopx.kujiale.cities-json.insecure-tls=true}; not for production.
 */
public class KujialeH5CitiesInsecureTlsClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	private static final TrustManager[] TRUST_ALL = new TrustManager[] {
		new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}
	};

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		super.prepareConnection(connection, httpMethod);
		if (connection instanceof HttpsURLConnection https) {
			try {
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, TRUST_ALL, new SecureRandom());
				https.setSSLSocketFactory(sslContext.getSocketFactory());
				https.setHostnameVerifier((hostname, session) -> true);
			} catch (GeneralSecurityException e) {
				throw new IOException("TLS setup failed", e);
			}
		}
	}
}
