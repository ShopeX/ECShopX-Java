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

package cn.shopex.ecshopx.common.cron.youshu;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下对齐 PHP 对不可解析 API 主机的失败路径：打点与 Noop 相同，但对 {@code
 * credentials.baseUri()} 的 host 做 {@link InetAddress#getByName} 校验，不可解析时抛
 * {@link IllegalStateException}（与对端 cURL/日志语义对拍），可解析时仍返回 {@code
 * test-ds-1}。
 */
@Slf4j
public class DnsAwareStubYoushuDataSourceApiPort implements YoushuDataSourceApiPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public String getOrCreateDataSourceId(
			String merchantId, int dataSourceType, YoushuOpenApiCredentials credentials) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][youshu-datasource-http] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {merchantId, dataSourceType, credentials}));
		String baseUri = credentials == null ? null : credentials.baseUri();
		if (baseUri == null || baseUri.isBlank()) {
			throw new IllegalStateException("Could not resolve host: <empty>");
		}
		URI uri;
		try {
			uri = URI.create(baseUri.trim());
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Could not resolve host: <invalid-uri>", e);
		}
		String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			throw new IllegalStateException("Could not resolve host: <no-host>");
		}
		try {
			InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Could not resolve host: " + host, e);
		}
		return "test-ds-1";
	}
}
