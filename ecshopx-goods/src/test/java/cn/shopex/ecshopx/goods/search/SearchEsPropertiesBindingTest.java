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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class SearchEsPropertiesBindingTest {

	@Test
	void bindsAuthAndSslProperties() {
		Map<String, String> map = new HashMap<>();
		map.put("ecshopx.search.es.username", "elastic");
		map.put("ecshopx.search.es.password", "secret");
		map.put("ecshopx.search.es.api-key", "dGVzdA==");
		map.put("ecshopx.search.es.ssl.truststore", "/tmp/trust.p12");
		map.put("ecshopx.search.es.ssl.truststore-password", "changeit");
		map.put("ecshopx.search.es.ssl.truststore-type", "PKCS12");
		map.put("ecshopx.search.es.ssl.keystore", "/tmp/key.p12");
		map.put("ecshopx.search.es.ssl.keystore-password", "changeit");
		map.put("ecshopx.search.es.ssl.keystore-type", "PKCS12");
		map.put("ecshopx.search.es.ssl.key-password", "keypass");

		SearchEsProperties props = new Binder(new MapConfigurationPropertySource(map))
				.bind("ecshopx.search.es", SearchEsProperties.class)
				.get();

		assertThat(props.getUsername()).isEqualTo("elastic");
		assertThat(props.getPassword()).isEqualTo("secret");
		assertThat(props.getApiKey()).isEqualTo("dGVzdA==");
		assertThat(props.getSsl().getTruststore()).isEqualTo("/tmp/trust.p12");
		assertThat(props.getSsl().getTruststorePassword()).isEqualTo("changeit");
		assertThat(props.getSsl().getTruststoreType()).isEqualTo("PKCS12");
		assertThat(props.getSsl().getKeystore()).isEqualTo("/tmp/key.p12");
		assertThat(props.getSsl().getKeystorePassword()).isEqualTo("changeit");
		assertThat(props.getSsl().getKeystoreType()).isEqualTo("PKCS12");
		assertThat(props.getSsl().getKeyPassword()).isEqualTo("keypass");
	}
}
