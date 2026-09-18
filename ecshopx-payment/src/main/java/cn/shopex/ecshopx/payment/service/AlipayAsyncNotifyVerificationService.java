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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.service.dto.AlipayNotifySigningMaterial;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayAsyncNotifyVerificationService {

	public void verifySignedNotify(Map<String, String> params, AlipayNotifySigningMaterial material) {
		if (params == null || params.isEmpty()) {
			throw new BadRequestException("支付宝通知参数无效");
		}
		if (!StringUtils.hasText(params.get("sign"))) {
			throw new BadRequestException("支付宝通知缺少签名字段");
		}
		String publicKey = material.getAlipayPublicKey();
		if (!StringUtils.hasText(publicKey)) {
			throw new BadRequestException("支付宝公钥未配置");
		}
		String signType = params.get("sign_type");
		if (!StringUtils.hasText(signType)) {
			signType = "RSA2";
		}
		try {
			Map<String, String> forVerify = new LinkedHashMap<>(params);
			boolean ok = AlipaySignature.rsaCheckV1(
					forVerify, publicKey, StandardCharsets.UTF_8.name(), signType);
			if (!ok) {
				throw new BadRequestException("支付宝异步通知验签失败");
			}
		} catch (AlipayApiException e) {
			throw new BadRequestException("支付宝异步通知验签失败");
		}
	}
}
