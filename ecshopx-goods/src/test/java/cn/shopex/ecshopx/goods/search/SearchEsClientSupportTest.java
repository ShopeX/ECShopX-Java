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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.goods.search.SearchEsClientSupport.AuthMode;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchEsClientSupportTest {

	@Test
	void noCredentialsResolvesToNone() {
		SearchEsProperties props = baseProps();
		assertThat(SearchEsClientSupport.resolveAuthMode(props)).isEqualTo(AuthMode.NONE);
	}

	@Test
	void blankApiKeyIsTreatedAsDisabled() {
		SearchEsProperties props = baseProps();
		props.setApiKey("   ");
		assertThat(SearchEsClientSupport.resolveAuthMode(props)).isEqualTo(AuthMode.NONE);
	}

	@Test
	void basicAuthRequiresPassword() {
		SearchEsProperties props = baseProps();
		props.setUsername("elastic");
		props.setPassword(" ");
		assertThatThrownBy(() -> SearchEsClientSupport.resolveAuthMode(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("password");
	}

	@Test
	void basicAuthResolvesWhenUsernameAndPasswordSet() {
		SearchEsProperties props = baseProps();
		props.setUsername("elastic");
		props.setPassword("secret");
		assertThat(SearchEsClientSupport.resolveAuthMode(props)).isEqualTo(AuthMode.BASIC);
	}

	@Test
	void apiKeyAuthResolvesWhenApiKeySet() {
		SearchEsProperties props = baseProps();
		props.setApiKey("dGVzdDpwYXNz");
		assertThat(SearchEsClientSupport.resolveAuthMode(props)).isEqualTo(AuthMode.API_KEY);
	}

	@Test
	void basicAndApiKeyTogetherFail() {
		SearchEsProperties props = baseProps();
		props.setUsername("elastic");
		props.setPassword("secret");
		props.setApiKey("dGVzdDpwYXNz");
		assertThatThrownBy(() -> SearchEsClientSupport.resolveAuthMode(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("mutually exclusive");
	}

	@Test
	void httpHostWithTruststoreFails() throws Exception {
		Path truststore = createPkcs12(Files.createTempFile("es-trust", ".p12"), "changeit");
		SearchEsProperties props = baseProps();
		props.setHosts(List.of("http://127.0.0.1:9200"));
		props.getSsl().setTruststore(truststore.toString());
		props.getSsl().setTruststorePassword("changeit");
		assertThatThrownBy(() -> SearchEsClientSupport.validateSsl(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("http");
	}

	@Test
	void keystoreRequiresPassword() throws Exception {
		Path keystore = createPkcs12(Files.createTempFile("es-key", ".p12"), "changeit");
		SearchEsProperties props = httpsProps();
		props.getSsl().setKeystore(keystore.toString());
		props.getSsl().setKeystorePassword(" ");
		assertThatThrownBy(() -> SearchEsClientSupport.validateSsl(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("keystore-password");
	}

	@Test
	void missingTruststoreFileFails(@TempDir Path dir) {
		SearchEsProperties props = httpsProps();
		props.getSsl().setTruststore(dir.resolve("missing.p12").toString());
		assertThatThrownBy(() -> SearchEsClientSupport.validateSsl(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("truststore");
	}

	@Test
	void httpsWithReadableTruststorePassesValidation(@TempDir Path dir) throws Exception {
		Path truststore = createPkcs12(dir.resolve("trust.p12"), "changeit");
		SearchEsProperties props = httpsProps();
		props.getSsl().setTruststore(truststore.toString());
		props.getSsl().setTruststorePassword("changeit");
		SearchEsClientSupport.validateSsl(props);
	}

	private static SearchEsProperties baseProps() {
		SearchEsProperties props = new SearchEsProperties();
		props.setHosts(List.of("http://127.0.0.1:9200"));
		return props;
	}

	private static SearchEsProperties httpsProps() {
		SearchEsProperties props = new SearchEsProperties();
		props.setHosts(List.of("https://es.example.com:9200"));
		return props;
	}

	private static Path createPkcs12(Path path, String password) throws Exception {
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, null);
		try (OutputStream out = Files.newOutputStream(path)) {
			ks.store(out, password.toCharArray());
		}
		return path;
	}
}
