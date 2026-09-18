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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import cn.shopex.ecshopx.common.port.weixin.WechatPayMerchantContextPort;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatPayMerchantContextService implements WechatPayMerchantContextPort {

	private static final String MSG = "请检查微信支付相关配置是否完成";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WechatPayMerchantContextService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public WechatMerchantV3ApiMaterial requireForV3Api(long companyId) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L));
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		List<String> fields = List.of("merchant_id", "cert", "cert_key");
		for (String field : fields) {
			if (PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get(field))) {
				throw new ResourceException(MSG);
			}
		}
		String mchId = str(cfg.get("merchant_id"));
		String certPath = str(cfg.get("cert"));
		String keyPath = str(cfg.get("cert_key"));
		if (!StringUtils.hasText(mchId) || !StringUtils.hasText(certPath) || !StringUtils.hasText(keyPath)) {
			throw new ResourceException(MSG);
		}
		try {
			String serialHex = readSerialHexFromCertPem(certPath);
			PrivateKey pk = readPkcs8PrivateKey(Path.of(keyPath));
			return new WechatMerchantV3ApiMaterial(mchId, serialHex, pk);
		} catch (Exception e) {
			throw new ResourceException(MSG);
		}
	}

	private static String readSerialHexFromCertPem(String certPath) throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (InputStream in = Files.newInputStream(Path.of(certPath))) {
			X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
			String s = cert.getSerialNumber().toString(16).toUpperCase(Locale.ROOT);
			if ((s.length() & 1) == 1) {
				return "0" + s;
			}
			return s;
		}
	}

	private static PrivateKey readPkcs8PrivateKey(Path keyPath) throws Exception {
		String pem = Files.readString(keyPath, StandardCharsets.UTF_8);
		if (pem.contains("BEGIN RSA PRIVATE KEY")) {
			throw new IllegalArgumentException("pkcs1");
		}
		String pk =
				pem.replace("-----BEGIN PRIVATE KEY-----", "")
						.replace("-----END PRIVATE KEY-----", "")
						.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(pk);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		return KeyFactory.getInstance("RSA").generatePrivate(spec);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
