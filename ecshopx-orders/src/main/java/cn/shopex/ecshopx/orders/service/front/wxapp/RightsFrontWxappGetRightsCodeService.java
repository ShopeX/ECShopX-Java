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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RightsFrontWxappGetRightsCodeService {

	private final StringRedisTemplate wechatStringRedisTemplate;

	private final MemberBarcodeGenerateService memberBarcodeGenerateService;

	public RightsFrontWxappGetRightsCodeService(
			@Qualifier("wechatStringRedisTemplate") StringRedisTemplate wechatStringRedisTemplate,
			MemberBarcodeGenerateService memberBarcodeGenerateService) {
		this.wechatStringRedisTemplate = wechatStringRedisTemplate;
		this.memberBarcodeGenerateService = memberBarcodeGenerateService;
	}

	public Map<String, Object> getRightsCode(String rightsId) {
		StringBuilder sb = new StringBuilder(16);
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = 0; i < 16; i++) {
			sb.append(r.nextInt(10));
		}
		String code = sb.toString();

		wechatStringRedisTemplate.opsForValue().set(
				"timescardcode:" + code,
				rightsId == null ? "" : rightsId,
				Duration.ofSeconds(60));

		LinkedHashMap<String, String> urls =
				memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(code);

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("barcode_url", urls.get("barcode_url"));
		body.put("qrcode_url", urls.get("qrcode_url"));
		body.put("code", code);
		body.put("_ignore_data", Boolean.TRUE);
		return body;
	}
}
