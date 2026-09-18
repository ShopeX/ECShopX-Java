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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.OfficialAccountForeverQrcodeClient;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OffiaccountForeverQrcodeService {

	private static final Logger log = LoggerFactory.getLogger(OffiaccountForeverQrcodeService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final OfficialAccountForeverQrcodeClient officialAccountForeverQrcodeClient;

	public OffiaccountForeverQrcodeService(
			WechatAuthQueryService wechatAuthQueryService,
			OfficialAccountForeverQrcodeClient officialAccountForeverQrcodeClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.officialAccountForeverQrcodeClient = officialAccountForeverQrcodeClient;
	}

	public Map<String, Object> getOffiaccountCodeForever(long companyId, String authorizerAppid, boolean includeBase64) {
		String trimmed = authorizerAppid == null ? "" : authorizerAppid.trim();
		if (!StringUtils.hasText(trimmed)) {
			return emptyUrlAndImagePayload();
		}
		if (!wechatAuthQueryService.isAuthorizerAppidBoundToCompany(companyId, trimmed)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		try {
			String showUrl = officialAccountForeverQrcodeClient.createForeverShowQrcodeUrl(trimmed, trimmed);
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("url", showUrl);
			if (includeBase64) {
				byte[] raw = officialAccountForeverQrcodeClient.downloadImageBytes(showUrl);
				if (raw != null && raw.length > 0) {
					data.put("base64Image", "data:image/jpg;base64," + Base64.getEncoder().encodeToString(raw));
				} else {
					data.put("base64Image", "");
				}
			}
			return data;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("getOffiaccountCodeForever failed, companyId={}, authorizerAppid={}", companyId, authorizerAppid, e);
			return emptyUrlAndImagePayload();
		}
	}

	private static LinkedHashMap<String, Object> emptyUrlAndImagePayload() {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("url", "");
		data.put("base64Image", "");
		return data;
	}
}
