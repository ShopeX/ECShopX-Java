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
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterFrontNewQrcodePngService {

	private static final Logger log = LoggerFactory.getLogger(PromoterFrontNewQrcodePngService.class);

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public PromoterFrontNewQrcodePngService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WechatAuthQueryService wechatAuthQueryService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public byte[] getPromoterNewQrcodePng(long companyId, String appidQuery, String puidRaw, String pathRaw) {
		boolean needsResolveDefaultAppid =
				(appidQuery == null || appidQuery.isEmpty() || "0".equals(appidQuery));
		String wxappAppid;
		if (needsResolveDefaultAppid) {
			Optional<String> resolved = weappAuthorizerAppidRepository.findAuthorizerAppid(companyId, "yykweishop");
			wxappAppid = resolved.orElse(null);
		} else {
			wxappAppid = appidQuery;
		}

		if (companyId > 0L
				&& wxappAppid != null
				&& !wxappAppid.isEmpty()
				&& !"0".equals(wxappAppid)) {
			if (!wechatAuthQueryService.isAuthorizerAppidBoundToCompany(companyId, wxappAppid)) {
				throw new BadRequestException("小程序未绑定，请重新绑定", 400001);
			}
		}

		if (wxappAppid == null || wxappAppid.isEmpty() || "0".equals(wxappAppid)) {
			throw new BadRequestException("缺少小程序 appid");
		}

		String scene = "puid=" + (puidRaw == null ? "" : String.valueOf(puidRaw));
		String page = StringUtils.hasText(pathRaw) ? pathRaw.trim() : "pages/index";
		if (page.startsWith("/")) {
			page = page.substring(1);
		}

		log.debug("推广邀请码，推荐关系跟踪 scene：{}", scene);

		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappAppid, scene, page);
	}
}
