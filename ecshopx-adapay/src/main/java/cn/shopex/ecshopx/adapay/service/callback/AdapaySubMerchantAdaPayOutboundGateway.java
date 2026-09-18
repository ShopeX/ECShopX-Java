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
import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class AdapaySubMerchantAdaPayOutboundGateway {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final AdapayCallbackProperties properties;
	private final FileStorageService fileStorageService;
	private final String adapayNotifyUrl;

	public AdapaySubMerchantAdaPayOutboundGateway(
			@Qualifier("adapayCallbackRestTemplate") RestTemplate adapayCallbackRestTemplate,
			ObjectMapper objectMapper,
			AdapayCallbackProperties properties,
			FileStorageService fileStorageService,
			@Value("${adapay.notify-url:}") String adapayNotifyUrl) {
		this.restTemplate = adapayCallbackRestTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.fileStorageService = fileStorageService;
		this.adapayNotifyUrl = adapayNotifyUrl == null ? "" : adapayNotifyUrl;
	}

	public Map<String, Object> savePersonMember(
			long companyId, String appId, AdapayMember member, boolean isUpdate) {
		if (!StringUtils.hasText(properties.getSettleOutboundBaseUrl())) {
			return Map.of("data", Map.of("status", "succeeded"));
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("app_id", appId);
		body.put("adapay_func_code", "members.realname");
		body.put("member_id", member.getId());
		body.put("tel_no", member.getTelNo() == null ? "" : member.getTelNo());
		body.put("user_name", member.getUserName() == null ? "" : member.getUserName());
		body.put("cert_type", "00");
		body.put("cert_id", member.getCertId() == null ? "" : member.getCertId());
		body.put("api_method", isUpdate ? "Member.update" : "Member.create");
		return postAdaPayRequest(body);
	}

	public Map<String, Object> saveCorpMember(
			long companyId,
			String appId,
			Map<String, Object> mergedMemberAndCorp,
			AdapayCorpMember corpRow,
			boolean isUpdate,
			boolean autoCreateSettle) {
		if (!StringUtils.hasText(properties.getSettleOutboundBaseUrl())) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "succeeded");
			return Map.of("data", data);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("app_id", appId);
		body.put("member_id", str(mergedMemberAndCorp.get("member_id")));
		String orderNo =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.withZone(ZoneId.systemDefault())
								.format(Instant.now())
						+ ThreadLocalRandom.current().nextInt(1000, 10000)
						+ str(mergedMemberAndCorp.get("member_id"));
		body.put("order_no", orderNo);
		body.put("name", str(mergedMemberAndCorp.get("name")));
		body.put("prov_code", str(mergedMemberAndCorp.get("prov_code")));
		body.put("area_code", str(mergedMemberAndCorp.get("area_code")));
		body.put("social_credit_code", str(mergedMemberAndCorp.get("social_credit_code")));
		body.put("social_credit_code_expires", str(mergedMemberAndCorp.get("social_credit_code_expires")));
		body.put("business_scope", str(mergedMemberAndCorp.get("business_scope")));
		body.put("legal_person", str(mergedMemberAndCorp.get("legal_person")));
		body.put("legal_cert_id", str(mergedMemberAndCorp.get("legal_cert_id")));
		body.put("legal_cert_id_expires", str(mergedMemberAndCorp.get("legal_cert_id_expires")));
		body.put("legal_mp", str(mergedMemberAndCorp.get("legal_mp")));
		body.put("address", str(mergedMemberAndCorp.get("address")));
		body.put("zip_code", str(mergedMemberAndCorp.get("zip_code")));
		body.put("telphone", str(mergedMemberAndCorp.get("telphone")));
		body.put("email", str(mergedMemberAndCorp.get("email")));
		body.put("notify_url", adapayNotifyUrl);
		if (!isUpdate && autoCreateSettle) {
			body.put("bank_code", str(mergedMemberAndCorp.get("bank_code")));
			body.put("bank_acct_type", str(mergedMemberAndCorp.get("bank_acct_type")));
			body.put("card_no", str(mergedMemberAndCorp.get("card_no")));
			body.put("card_name", str(mergedMemberAndCorp.get("card_name")));
		}
		byte[] zip = loadCorpAttachZipOrNull(corpRow);
		if (zip != null) {
			body.put("attach_file", "");
			body.put("file_content", Base64.getEncoder().encodeToString(zip));
		}
		body.put("api_method", isUpdate ? "CorpMember.update" : "CorpMember.create");
		return postAdaPayRequest(body);
	}

	public Map<String, Object> postAdaPayRequest(Map<String, Object> body) {
		return postJson(body);
	}

	private byte[] loadCorpAttachZipOrNull(AdapayCorpMember corp) {
		if (corp == null || !StringUtils.hasText(corp.getAttachFile())) {
			return null;
		}
		try {
			byte[] b = fileStorageService.get("file", corp.getAttachFile().trim());
			if (b == null || b.length == 0) {
				throw new ResourceException("企业附件读取失败");
			}
			return b;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("企业附件读取失败");
		}
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

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
