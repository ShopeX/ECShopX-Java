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

package cn.shopex.ecshopx.payment.service.settings;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingBooleanParsing;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WxpayPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WxpayPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();
		String redisKey = PaymentSettingRedisKeys.wxpayRedisKey(cmd.companyId(), cmd.distributorId());

		if (PaymentSettingBooleanParsing.truthyForPaymentTypeOpenFlag(scalar.get("is_open"))) {
			companysRedisTemplate
					.opsForValue()
					.set(PaymentSettingRedisKeys.paymentTypeOpenConfigKey(cmd.companyId()), "wxpay");
		}

		String rawExisting = companysRedisTemplate.opsForValue().get(redisKey);
		Map<String, Object> redisData =
				rawExisting != null
						? PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawExisting)
						: Map.of();

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("app_id", scalar.get("app_id"));
		data.put("merchant_id", scalar.get("merchant_id"));
		data.put("key", scalar.get("key"));
		data.put("is_servicer", scalar.get("is_servicer"));
		data.put("servicer_merchant_id", scalar.get("servicer_merchant_id"));
		data.put("servicer_app_id", scalar.get("servicer_app_id"));
		data.put("is_open", scalar.get("is_open"));
		data.put("app_app_id", scalar.get("app_app_id"));

		if (cmd.files().containsKey("cert")) {
			MultipartFile cert = cmd.files().get("cert");
			if (cert == null || !StringUtils.hasText(cert.getOriginalFilename())) {
				throw new BadRequestException("请上传apiclient_cert.pem");
			}
			if (!"apiclient_cert.pem".equals(cert.getOriginalFilename())) {
				throw new BadRequestException("请上传apiclient_cert.pem");
			}
			try {
				data.put("cert", new String(cert.getBytes(), StandardCharsets.ISO_8859_1));
			} catch (Exception e) {
				throw new BadRequestException("请上传apiclient_cert.pem");
			}
		} else {
			data.put("cert", redisData.get("cert"));
		}

		if (cmd.files().containsKey("cert_key")) {
			MultipartFile certKey = cmd.files().get("cert_key");
			if (certKey == null || !StringUtils.hasText(certKey.getOriginalFilename())) {
				throw new BadRequestException("请上传apiclient_key.pem");
			}
			if (!"apiclient_key.pem".equals(certKey.getOriginalFilename())) {
				throw new BadRequestException("请上传apiclient_key.pem");
			}
			try {
				data.put("cert_key", new String(certKey.getBytes(), StandardCharsets.ISO_8859_1));
			} catch (Exception e) {
				throw new BadRequestException("请上传apiclient_key.pem");
			}
		} else {
			data.put("cert_key", redisData.get("cert_key"));
		}

		if (PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("app_id"))) {
			throw new BadRequestException("公众账号ID不能为空");
		}
		if (PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("merchant_id"))) {
			throw new BadRequestException("商户号不能为空");
		}
		if (PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("key"))) {
			throw new BadRequestException("API密钥不能为空");
		}
		if (PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("cert"))) {
			throw new BadRequestException("商户证书不能为空");
		}
		if (PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("cert_key"))) {
			throw new BadRequestException("商户证书秘钥不能为空");
		}

		Object appId = data.get("app_id");
		if (appId != null && StringUtils.hasText(String.valueOf(appId))) {
			companysRedisTemplate
					.opsForValue()
					.set(
							PaymentSettingRedisKeys.wechatPaymentCompanyByAppIdKey(String.valueOf(appId).trim()),
							String.valueOf(cmd.companyId()));
		}
		Object appAppId = data.get("app_app_id");
		if (appAppId != null && StringUtils.hasText(String.valueOf(appAppId))) {
			companysRedisTemplate
					.opsForValue()
					.set(
							PaymentSettingRedisKeys.wechatAppPaymentCompanyByAppIdKey(
									String.valueOf(appAppId).trim()),
							String.valueOf(cmd.companyId()));
		}

		if ("true".equals(String.valueOf(data.get("is_servicer")).trim())) {
			Object smid = data.get("servicer_merchant_id");
			Object said = data.get("servicer_app_id");
			if (!StringUtils.hasText(smid == null ? "" : String.valueOf(smid).trim())
					|| !StringUtils.hasText(said == null ? "" : String.valueOf(said).trim())) {
				throw new BadRequestException("开启特约商户服务商APPID和服务商商户号必填！");
			}
			companysRedisTemplate
					.opsForValue()
					.set(
							PaymentSettingRedisKeys.wechatServicerPaymentCompanyByAppIdKey(
									String.valueOf(said).trim()),
							String.valueOf(cmd.companyId()));
		}

		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}
}
