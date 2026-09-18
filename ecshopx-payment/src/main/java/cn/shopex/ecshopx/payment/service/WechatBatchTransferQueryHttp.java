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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.port.weixin.WechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import cn.shopex.ecshopx.payment.v3.WechatPayV3AuthHeader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信 v3「查询批次单」GET，对应 PHP
 * <code>v3/transfer/batches/batch-id/{batchId}?need_query_detail=false</code>。
 */
@Service
public class WechatBatchTransferQueryHttp implements WechatBatchTransferQueryPort {

	private static final String HOST = "https://api.mch.weixin.qq.com";

	private final ObjectMapper objectMapper;

	public WechatBatchTransferQueryHttp(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	@SuppressWarnings("unused")
	public JsonNode queryBalanceOrder(long companyId, WechatMerchantV3ApiMaterial material, String outBatchId) {
		if (!StringUtils.hasText(outBatchId)) {
			return objectMapper.createObjectNode();
		}
		String path =
				"/v3/transfer/batches/batch-id/" + outBatchId.trim() + "?need_query_detail=false";
		String auth = WechatPayV3AuthHeader.buildAuthorization(material, "GET", path, "");
		HttpRequest req =
				HttpRequest.newBuilder()
						.uri(URI.create(HOST + path))
						.header("Authorization", auth)
						.header("Accept", "application/json")
						.GET()
						.build();
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> resp =
					client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			String body = resp.body() == null ? "" : resp.body();
			if (!StringUtils.hasText(body)) {
				return objectMapper.createObjectNode();
			}
			return objectMapper.readTree(body);
		} catch (Exception e) {
			return objectMapper.createObjectNode();
		}
	}
}
