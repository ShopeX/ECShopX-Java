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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class HfPayAcouFile01Client {

	private static final Logger log = LoggerFactory.getLogger(HfPayAcouFile01Client.class);

	private final RestClient hfpayFile01RestClient;
	private final HfPayFile01SignJsonSerializer signJsonSerializer;
	private final HfPayCfcaKernelService cfcaKernelService;
	private final ObjectMapper objectMapper;

	public HfPayAcouFile01Client(
			@Qualifier("hfpayFile01RestClient") RestClient hfpayFile01RestClient,
			HfPayFile01SignJsonSerializer signJsonSerializer,
			HfPayCfcaKernelService cfcaKernelService,
			ObjectMapper objectMapper) {
		this.hfpayFile01RestClient = hfpayFile01RestClient;
		this.signJsonSerializer = signJsonSerializer;
		this.cfcaKernelService = cfcaKernelService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> file01(
			Map<String, Object> setting,
			String attachNo,
			String transType,
			String attachType,
			MultipartFile file) {
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
		String signJson = signJsonSerializer.toSignJsonString(merCustId, attachNo, transType, attachType);
		String pfxPath = String.valueOf(setting.get("pfx_file_url"));
		String pfxPassword = String.valueOf(setting.get("pfx_password"));
		String checkValue = cfcaKernelService.signAttachedPkcs7Base64(signJson, pfxPath, pfxPassword);

		String ext = "";
		String orig = file.getOriginalFilename();
		if (StringUtils.hasText(orig) && orig.contains(".")) {
			ext = orig.substring(orig.lastIndexOf('.') + 1);
		}
		String uploadFilename = Instant.now().getEpochSecond() + "." + ext;

		byte[] fileBytes;
		try {
			fileBytes = file.getBytes();
		} catch (IOException e) {
			throw new ResourceException("解密错误");
		}

		MultipartBodyBuilder mp = new MultipartBodyBuilder();
		mp.part("mer_cust_id", merCustId);
		mp.part("version", "10");
		mp.part("check_value", checkValue);
		mp.part("attach_file", new ByteArrayResource(fileBytes) {
			@Override
			public String getFilename() {
				return uploadFilename;
			}
		});

		String url = "https://hfpay.cloudpnr.com/api/alseFile/file01";
		if (log.isDebugEnabled()) {
			log.debug(
					"hfpay file01 POST url={} mer_cust_id={} version=10 check_value_len={} attach_file_name={} attach_file_bytes={}",
					url,
					merCustId,
					checkValue != null ? checkValue.length() : 0,
					orig,
					fileBytes.length);
		}

		String responseBody;
		try {
			ResponseEntity<String> entity = hfpayFile01RestClient.post()
					.uri("/api/alseFile/file01")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(mp.build())
					.retrieve()
					.onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), (req, res) -> {
						throw new ResourceException("解密错误");
					})
					.toEntity(String.class);
			responseBody = entity.getBody();
			if (log.isDebugEnabled()) {
				log.debug(
						"hfpay file01 response status={} body_len={}",
						entity.getStatusCode(),
						responseBody != null ? responseBody.length() : 0);
			}
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
		return cfcaKernelService.decryptResponseCheckValue(respCheck, caPath, ocaPath);
	}
}
