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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class WxpayAsyncNotifyVerificationService {

	public void verifySignedNotify(Map<String, String> notifyParams, String apiKey) {
		if (notifyParams == null || notifyParams.isEmpty()) {
			throw new BadRequestException("无效的微信支付通知");
		}
		if (!StringUtils.hasText(apiKey)) {
			throw new BadRequestException("微信支付信息未配置，请联系商家");
		}
		String provided = notifyParams.get("sign");
		if (!StringUtils.hasText(provided)) {
			throw new BadRequestException("无效的微信支付通知");
		}
		String computed = signParams(new TreeMap<>(notifyParams), apiKey);
		if (!computed.equalsIgnoreCase(provided.trim())) {
			throw new BadRequestException("微信支付验签失败");
		}
	}

	static String signParams(TreeMap<String, String> sorted, String apiKey) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			if (e.getValue() == null || e.getValue().isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		sb.append("&key=").append(apiKey);
		return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
	}
}
