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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterFrontGetPromoterQrcodeService {

	private static final Logger log = LoggerFactory.getLogger(PromoterFrontGetPromoterQrcodeService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public PromoterFrontGetPromoterQrcodeService(
			WechatAuthQueryService wechatAuthQueryService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, String> getPromoterQrcode(long companyId, Map<String, Object> claims, String pathRaw) {
		Object v = claims != null ? claims.get("wxapp_appid") : null;
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return Map.of("qrcode", "");
		}

		String wxappAppid = String.valueOf(claims.get("wxapp_appid")).trim();

		if (!wechatAuthQueryService.isAuthorizerAppidBoundToCompany(companyId, wxappAppid)) {
			throw new BadRequestException("小程序未绑定，请重新绑定", 400001);
		}

		String scene = "uid=" + String.valueOf(claims.get("user_id"));

		String page =
				StringUtils.hasText(pathRaw != null ? pathRaw.trim() : "")
						? pathRaw.trim()
						: "pages/index";
		if (page.startsWith("/")) {
			page = page.substring(1);
		}

		log.debug("推荐关系跟踪 scene：{}", scene);

		byte[] raw = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappAppid, scene, page);
		String b64 = Base64.getEncoder().encodeToString(raw);
		String dataUrl = "data:image/jpg;base64," + b64;
		return Map.of("qrcode", dataUrl);
	}
}
