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

package cn.shopex.ecshopx.adapay.service.callback;

import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class AdapaySubMerchantSettleAccountGateway {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final AdapayCallbackProperties properties;

	public AdapaySubMerchantSettleAccountGateway(
			@Qualifier("adapayCallbackRestTemplate") RestTemplate adapayCallbackRestTemplate,
			ObjectMapper objectMapper,
			AdapayCallbackProperties properties) {
		this.restTemplate = adapayCallbackRestTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public Map<String, Object> deleteSettleAccount(
			long companyId, String appId, String settleId, long memberTablePk) {
		if (!StringUtils.hasText(properties.getSettleOutboundBaseUrl())) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "succeeded");
			return Map.of("data", data);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("app_id", appId);
		body.put("member_id", memberTablePk);
		body.put("settle_account_id", settleId);
		body.put("api_method", "SettleAccount.delete");
		return postJson(body);
	}

	public Map<String, Object> createSettleAccount(
			long companyId, String appId, long memberTablePk, Map<String, Object> accountInfo) {
		if (!StringUtils.hasText(properties.getSettleOutboundBaseUrl())) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "succeeded");
			data.put("id", "local_settle_account");
			return Map.of("data", data);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("app_id", appId);
		body.put("member_id", memberTablePk);
		body.put("api_method", "SettleAccount.create");
		body.put("channel", "bank_account");
		body.put("account_info", accountInfo == null ? Map.of() : accountInfo);
		return postJson(body);
	}

	/**
	 * 将本地结算账户行映射为 AdaPay SettleAccount.create 所需的 {@code account_info} 子对象。
	 */
	public static Map<String, Object> toAccountInfoMap(AdapaySettleAccount row) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (row == null) {
			m.put("card_id", "");
			m.put("card_name", "");
			m.put("cert_id", "");
			m.put("cert_type", "00");
			m.put("tel_no", "");
			m.put("bank_code", "");
			m.put("bank_name", "");
			m.put("bank_acct_type", "");
			m.put("prov_code", "");
			m.put("area_code", "");
			return m;
		}
		m.put("card_id", row.getCardId() == null ? "" : row.getCardId());
		m.put("card_name", row.getCardName() == null ? "" : row.getCardName());
		m.put("cert_id", row.getCertId() == null ? "" : row.getCertId());
		m.put(
				"cert_type",
				StringUtils.hasText(row.getCertType()) ? row.getCertType() : "00");
		m.put("tel_no", row.getTelNo() == null ? "" : row.getTelNo());
		m.put("bank_code", row.getBankCode() == null ? "" : row.getBankCode());
		m.put("bank_name", row.getBankName() == null ? "" : row.getBankName());
		m.put("bank_acct_type", row.getBankAcctType() == null ? "" : row.getBankAcctType());
		m.put("prov_code", row.getProvCode() == null ? "" : row.getProvCode());
		m.put("area_code", row.getAreaCode() == null ? "" : row.getAreaCode());
		return m;
	}

	private Map<String, Object> postJson(Map<String, Object> body) {
		String url = properties.getSettleOutboundBaseUrl();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String json;
		try {
			json = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("status", "failed");
			err.put("error_msg", "请求序列化失败");
			return Map.of("data", err);
		}
		ResponseEntity<String> resp =
				restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
		try {
			return objectMapper.readValue(resp.getBody(), new TypeReference<>() {});
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("status", "failed");
			err.put("error_msg", "响应解析失败");
			return Map.of("data", err);
		}
	}
}
