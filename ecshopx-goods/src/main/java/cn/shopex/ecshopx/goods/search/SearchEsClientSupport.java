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

package cn.shopex.ecshopx.goods.search;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClientBuilder;

/** Search ES RestClient 认证与 TLS 配置（校验 + 应用到 builder）。 */
public final class SearchEsClientSupport {

	public enum AuthMode {
		NONE,
		BASIC,
		API_KEY
	}

	private SearchEsClientSupport() {
	}

	public static AuthMode resolveAuthMode(SearchEsProperties properties) {
		boolean basic = hasText(properties.getUsername());
		boolean apiKey = hasText(properties.getApiKey());
		if (basic && apiKey) {
			throw new IllegalStateException(
					"ecshopx.search.es: Basic Auth and API Key Auth are mutually exclusive");
		}
		if (basic) {
			if (!hasText(properties.getPassword())) {
				throw new IllegalStateException(
						"ecshopx.search.es.password is required when username is set");
			}
			return AuthMode.BASIC;
		}
		if (apiKey) {
			return AuthMode.API_KEY;
		}
		return AuthMode.NONE;
	}

	public static void validateSsl(SearchEsProperties properties) {
		SearchEsProperties.Ssl ssl = properties.getSsl();
		if (ssl == null) {
			return;
		}
		boolean hasTruststore = hasText(ssl.getTruststore());
		boolean hasKeystore = hasText(ssl.getKeystore());
		if (!hasTruststore && !hasKeystore) {
			return;
		}
		List<String> hosts = properties.getHosts();
		if (hosts != null) {
			for (String raw : hosts) {
				if (raw != null && raw.trim().regionMatches(true, 0, "http:", 0, 5)) {
					throw new IllegalStateException(
							"ecshopx.search.es.ssl materials require https hosts; found http URL: " + raw.trim());
				}
			}
		}
		if (hasKeystore && !hasText(ssl.getKeystorePassword())) {
			throw new IllegalStateException(
					"ecshopx.search.es.ssl.keystore-password is required when keystore is set");
		}
		if (hasTruststore) {
			requireReadableFile("truststore", ssl.getTruststore());
		}
		if (hasKeystore) {
			requireReadableFile("keystore", ssl.getKeystore());
		}
	}

	public static void configure(RestClientBuilder builder, SearchEsProperties properties) {
		AuthMode mode = resolveAuthMode(properties);
		validateSsl(properties);
		boolean needHttpCallback = mode == AuthMode.BASIC || hasSslMaterials(properties.getSsl());
		if (needHttpCallback) {
			builder.setHttpClientConfigCallback(httpClientBuilder -> {
				if (mode == AuthMode.BASIC) {
					CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
					credentialsProvider.setCredentials(
							AuthScope.ANY,
							new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
					httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
				}
				if (hasSslMaterials(properties.getSsl())) {
					httpClientBuilder.setSSLContext(buildSslContext(properties.getSsl()));
				}
				return httpClientBuilder;
			});
		}
		if (mode == AuthMode.API_KEY) {
			Header[] headers = new Header[] {
					new BasicHeader("Authorization", "ApiKey " + properties.getApiKey().trim())
			};
			builder.setDefaultHeaders(headers);
		}
	}

	static SSLContext buildSslContext(SearchEsProperties.Ssl ssl) {
		try {
			SSLContextBuilder custom = SSLContexts.custom();
			if (hasText(ssl.getTruststore())) {
				KeyStore trustStore = loadKeyStore(
						ssl.getTruststore(),
						ssl.getTruststoreType(),
						passwordChars(ssl.getTruststorePassword()));
				custom.loadTrustMaterial(trustStore, null);
			}
			if (hasText(ssl.getKeystore())) {
				char[] keystorePassword = passwordChars(ssl.getKeystorePassword());
				char[] keyPassword = hasText(ssl.getKeyPassword())
						? ssl.getKeyPassword().toCharArray()
						: keystorePassword;
				KeyStore keyStore = loadKeyStore(ssl.getKeystore(), ssl.getKeystoreType(), keystorePassword);
				custom.loadKeyMaterial(keyStore, keyPassword);
			}
			return custom.build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to build Search ES SSL context: " + ex.getMessage(), ex);
		}
	}

	private static KeyStore loadKeyStore(String path, String type, char[] password) throws Exception {
		String storeType = hasText(type) ? type : "PKCS12";
		KeyStore keyStore = KeyStore.getInstance(storeType);
		try (InputStream in = Files.newInputStream(Path.of(path))) {
			keyStore.load(in, password);
		}
		return keyStore;
	}

	private static void requireReadableFile(String label, String path) {
		Path file = Path.of(path);
		if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
			throw new IllegalStateException(
					"ecshopx.search.es.ssl." + label + " is missing or unreadable: " + path);
		}
	}

	private static boolean hasSslMaterials(SearchEsProperties.Ssl ssl) {
		return ssl != null && (hasText(ssl.getTruststore()) || hasText(ssl.getKeystore()));
	}

	private static char[] passwordChars(String password) {
		return password == null ? new char[0] : password.toCharArray();
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
