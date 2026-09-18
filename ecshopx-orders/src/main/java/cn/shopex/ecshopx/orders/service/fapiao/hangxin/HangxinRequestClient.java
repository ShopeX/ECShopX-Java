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

package cn.shopex.ecshopx.orders.service.fapiao.hangxin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HangxinRequestClient {

	private final RestClient restClient;

	public HangxinRequestClient(
			@Value("${ecshopx.fapiao.hangxin.connect-timeout-ms:10000}") int connectTimeoutMs,
			@Value("${ecshopx.fapiao.hangxin.read-timeout-ms:60000}") int readTimeoutMs) {
		HttpClient httpClient =
				HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	public String postXml(String url, String xmlBody) {
		if (!StringUtils.hasText(url)) {
			throw new ResourceException("发票接口地址未配置");
		}
		try {
			return restClient
					.post()
					.uri(url)
					.contentType(MediaType.APPLICATION_XML)
					.acceptCharset(StandardCharsets.UTF_8)
					.body(xmlBody)
					.retrieve()
					.body(String.class);
		} catch (RestClientException e) {
			throw new ResourceException("航信接口调用失败: " + e.getMessage());
		}
	}
}
