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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PcQrcodeService {

	private final WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public PcQrcodeService(
			WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService,
			WechatAuthQueryService wechatAuthQueryService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.wxaAuthorizerAppIdByTemplateService = wxaAuthorizerAppIdByTemplateService;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public LinkedHashMap<String, Object> getPcQrcode(
			long companyId, String idRaw, String uidRaw, String dtidRaw, String pagesRaw) {
		String authorizerAppid = wxaAuthorizerAppIdByTemplateService.requireAuthorizerAppidForPcLogin(companyId);
		boolean idTruthy = hasPresentInputScalar(idRaw);
		if (!wechatAuthQueryService.isAuthorizerAppidBoundToCompany(companyId, authorizerAppid)) {
			throw new BadRequestException("小程序未绑定，请重新绑定", 400001);
		}
		String page = resolveMiniProgramPageForPcQrcode(idTruthy, pagesRaw);
		String scene = buildSceneForPcQrcode(companyId, idRaw, uidRaw, dtidRaw);
		byte[] raw = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(authorizerAppid, scene, page);
		String base64 = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(raw);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("base64Image", base64);
		return data;
	}

	private static boolean hasPresentInputScalar(String raw) {
		if (raw == null || raw.equals("") || raw.equals("0")) {
			return false;
		}
		return true;
	}

	private static boolean isAbsentPagesInput(String pagesRaw) {
		if (pagesRaw == null || pagesRaw.equals("") || pagesRaw.equals("0")) {
			return true;
		}
		return false;
	}

	private static String resolveMiniProgramPageForPcQrcode(boolean idPresent, String pagesRaw) {
		if (idPresent && isAbsentPagesInput(pagesRaw)) {
			return "pages/item/espier-detail";
		}
		if (pagesRaw != null) {
			return pagesRaw;
		}
		return "pages/index";
	}

	private static String buildSceneForPcQrcode(long companyId, String idRaw, String uidRaw, String dtidRaw) {
		LinkedHashMap<String, String> optional = new LinkedHashMap<>();
		if (hasPresentInputScalar(idRaw)) {
			optional.put("id", idRaw.trim());
		}
		if (hasPresentInputScalar(uidRaw)) {
			optional.put("uid", uidRaw.trim());
		}
		if (hasPresentInputScalar(dtidRaw)) {
			optional.put("dtid", dtidRaw.trim());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("cid=").append(URLEncoder.encode(String.valueOf(companyId), StandardCharsets.UTF_8));
		for (Map.Entry<String, String> e : optional.entrySet()) {
			sb.append("&")
					.append(e.getKey())
					.append("=")
					.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}
}
