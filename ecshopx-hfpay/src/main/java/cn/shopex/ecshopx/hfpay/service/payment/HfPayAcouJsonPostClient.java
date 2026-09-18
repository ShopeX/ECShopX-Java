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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class HfPayAcouJsonPostClient {

	private final HfPayCfcaKernelService cfcaKernelService;
	private final RestClient hfpayAcouRestClient;
	private final ObjectMapper objectMapper;
	private final ObjectWriter signPayloadWriter;

	public HfPayAcouJsonPostClient(
			HfPayCfcaKernelService cfcaKernelService,
			ObjectMapper objectMapper,
			@Qualifier("hfpayAcouRestClient") RestClient hfpayAcouRestClient) {
		this.cfcaKernelService = cfcaKernelService;
		this.objectMapper = objectMapper;
		this.hfpayAcouRestClient = hfpayAcouRestClient;
		ObjectMapper copy = objectMapper.copy();
		copy.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
		this.signPayloadWriter = copy.writer();
	}

	public Map<String, Object> corp01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/corp01", payload, setting);
	}

	public Map<String, Object> solo01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/solo01", payload, setting);
	}

	public Map<String, Object> bind01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/bind01", payload, setting);
	}

	public Map<String, Object> user01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/user01", payload, setting);
	}

	public Map<String, Object> unbd01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/unbd01", payload, setting);
	}

	public Map<String, Object> qry001(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/alse/qry001", payload, setting);
	}

	/** 交易状态查询（取现对账用），与汇付 /api/alse/qry008 一致。 */
	public Map<String, Object> qry008(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/alse/qry008", payload, setting);
	}

	public Map<String, Object> cash01(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/acou/cash01", payload, setting);
	}

	public Map<String, Object> pay026(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/hfpay/pay026", payload, setting);
	}

	public Map<String, Object> pay012(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/hfpay/pay012", payload, setting);
	}

	public Map<String, Object> pay006(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/hfpay/pay006", payload, setting);
	}

	public Map<String, Object> reb001(Map<String, Object> setting, Map<String, Object> payload) {
		return postSigned("/api/hfpay/reb001", payload, setting);
	}

	private Map<String, Object> postSigned(String path, Map<String, Object> payload, Map<String, Object> setting) {
		String json;
		try {
			json = signPayloadWriter.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("签名错误");
		}
		String pfxPath = String.valueOf(setting.get("pfx_file_url"));
		String pfxPassword = String.valueOf(setting.get("pfx_password"));
		String checkValue = cfcaKernelService.signAttachedPkcs7Base64(json, pfxPath, pfxPassword);

		String merCustId = String.valueOf(payload.get("mer_cust_id")).trim();
		String versionField = String.valueOf(payload.get("version"));

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("mer_cust_id", merCustId);
		form.add("version", versionField);
		form.add("check_value", checkValue);

		String responseBody;
		try {
			ResponseEntity<String> entity = hfpayAcouRestClient.post()
					.uri(path)
					.contentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8"))
					.body(form)
					.retrieve()
					.onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), (req, res) -> {
						throw new ResourceException("解密错误");
					})
					.toEntity(String.class);
			responseBody = entity.getBody();
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("解密错误");
		}

		if (!StringUtils.hasText(responseBody)) {
			throw new ResourceException("解密错误");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (Exception e) {
			throw new ResourceException("解密错误");
		}
		if (root == null || !root.has("check_value") || root.get("check_value").isNull()) {
			throw new ResourceException("解密错误");
		}
		String respCheck = root.get("check_value").asText();
		if (!StringUtils.hasText(respCheck)) {
			throw new ResourceException("解密错误");
		}
		String caPath = String.valueOf(setting.get("ca_pfx_file_url"));
		String ocaPath = String.valueOf(setting.get("oca31_pfx_file_url"));
		Map<String, Object> decrypted = cfcaKernelService.decryptResponseCheckValue(respCheck, caPath, ocaPath);
		if (decrypted == null) {
			decrypted = new LinkedHashMap<>();
		}
		return decrypted;
	}
}
