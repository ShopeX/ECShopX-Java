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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.net.URI;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SearchEsProperties.class)
public class SearchEsClientConfig {

	@Bean(destroyMethod = "close")
	public RestClient searchEsRestClient(SearchEsProperties properties) {
		List<String> hosts = properties.getHosts();
		if (hosts == null || hosts.isEmpty()) {
			throw new IllegalStateException("ecshopx.search.es.hosts must not be empty");
		}
		HttpHost[] httpHosts = hosts.stream().map(SearchEsClientConfig::toHttpHost).toArray(HttpHost[]::new);
		RestClientBuilder builder = RestClient.builder(httpHosts);
		builder.setRequestConfigCallback(cb -> cb
				.setConnectTimeout(properties.getConnectTimeoutMs())
				.setSocketTimeout(properties.getSocketTimeoutMs()));
		return builder.build();
	}

	@Bean(destroyMethod = "close")
	public ElasticsearchTransport searchEsTransport(RestClient searchEsRestClient) {
		return new RestClientTransport(searchEsRestClient, new JacksonJsonpMapper());
	}

	@Bean
	public ElasticsearchClient searchElasticsearchClient(ElasticsearchTransport searchEsTransport) {
		return new ElasticsearchClient(searchEsTransport);
	}

	private static HttpHost toHttpHost(String raw) {
		URI uri = URI.create(raw.trim());
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost();
		if (host == null) {
			throw new IllegalArgumentException("Invalid Elasticsearch host URL: " + raw);
		}
		int port = uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
		return new HttpHost(host, port, scheme);
	}
}
