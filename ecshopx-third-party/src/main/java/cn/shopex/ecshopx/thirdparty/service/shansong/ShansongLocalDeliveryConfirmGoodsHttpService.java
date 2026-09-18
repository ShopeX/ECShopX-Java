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

package cn.shopex.ecshopx.thirdparty.service.shansong;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongCompanyCredentialsPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongLocalDeliveryConfirmGoodsPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongOpenCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service("shansongLocalDeliveryConfirmGoodsHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.shansong", name = "http-enabled", havingValue = "true")
public class ShansongLocalDeliveryConfirmGoodsHttpService implements ShansongLocalDeliveryConfirmGoodsPort {

	private static final int HTTP_OK = 200;

	private final ShansongCompanyCredentialsPort shansongCompanyCredentialsPort;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public ShansongLocalDeliveryConfirmGoodsHttpService(
			ShansongCompanyCredentialsPort shansongCompanyCredentialsPort, ObjectMapper objectMapper) {
		this.shansongCompanyCredentialsPort = shansongCompanyCredentialsPort;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public void confirmGoodsReturn(long companyId, String issOrderNo) {
		if (!StringUtils.hasText(issOrderNo)) {
			throw new ResourceException("闪送确认退回缺少运单号");
		}
		ShansongOpenCredentials cred = shansongCompanyCredentialsPort.loadForCompany(companyId);
		String host = cred.online() ? "https://open.ishansong.com" : "http://open.s.bingex.com";
		String url = host + "/openapi/merchants/v5/confirmGoodsReturn";
		try {
			String dataJson = objectMapper.writeValueAsString(Map.of("issOrderNo", issOrderNo.trim()));
			long ts = System.currentTimeMillis() / 1000L;
			String sign = signShansong(cred.appSecret(), cred.clientId(), cred.shopId(), dataJson, ts);
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("clientId", cred.clientId());
			form.add("shopId", cred.shopId());
			form.add("timestamp", Long.toString(ts));
			form.add("data", dataJson);
			form.add("sign", sign);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful()) {
				throw new ResourceException("闪送确认退回请求失败");
			}
			String raw = resp.getBody();
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("闪送接口无响应");
			}
			JsonNode root = objectMapper.readTree(raw);
			int status = root.path("status").asInt(-1);
			if (status != HTTP_OK) {
				String msg = root.path("msg").asText("闪送确认退回失败");
				throw new ResourceException(msg);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("闪送确认退回调用异常: " + e.getMessage());
		}
	}

	private static String signShansong(String appSecret, String clientId, String shopId, String dataJson, long timestamp) {
		StringBuilder sb = new StringBuilder();
		sb.append(appSecret == null ? "" : appSecret);
		sb.append("clientId").append(clientId == null ? "" : clientId);
		if (dataJson != null && !dataJson.isEmpty()) {
			sb.append("data").append(dataJson);
		}
		sb.append("shopId").append(shopId == null ? "" : shopId);
		sb.append("timestamp").append(timestamp);
		return md5HexUpper(sb.toString());
	}

	private static String md5HexUpper(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString().toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
