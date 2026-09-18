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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaUploadWxaCodeUnlimitService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public WxaUploadWxaCodeUnlimitService(
			WechatAuthQueryService wechatAuthQueryService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> uploadWxaCodeUnlimit(
			long companyId, String jwtAuthorizerAppid, HttpServletRequest request) {
		String q = request.getParameter("wxaAppId");
		String jwt = jwtAuthorizerAppid == null ? "" : jwtAuthorizerAppid.trim();
		String effectiveAppId;
		if (WxaUploadWxaService.wxaAppIdHasText(q == null ? "" : q.trim())) {
			effectiveAppId = q.trim();
		} else if (WxaUploadWxaService.wxaAppIdHasText(jwt)) {
			effectiveAppId = jwt;
		} else {
			throw new BadRequestException("需显式 wxaAppId 或 JWT authorizer_appid");
		}
		if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, effectiveAppId)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		String pageRaw = request.getParameter("page");
		String page;
		if (!StringUtils.hasText(pageRaw == null ? "" : pageRaw.trim())) {
			page = "pages/index";
		} else {
			page = pageRaw.trim();
		}
		LinkedHashMap<String, String> params = new LinkedHashMap<>();
		String rawQs = request.getQueryString();
		if (rawQs != null && !rawQs.isEmpty()) {
			Set<String> seenNames = new HashSet<>();
			for (String seg : rawQs.split("&", -1)) {
				String name;
				String val;
				int idx = seg.indexOf('=');
				try {
					if (idx < 0) {
						name = URLDecoder.decode(seg, StandardCharsets.UTF_8);
						val = "";
					} else {
						name = URLDecoder.decode(seg.substring(0, idx), StandardCharsets.UTF_8);
						val = URLDecoder.decode(seg.substring(idx + 1), StandardCharsets.UTF_8);
					}
				} catch (IllegalArgumentException e) {
					throw new BadRequestException("查询参数编码无效");
				}
				String trimmedKey = name == null ? "" : name.trim();
				if (seenNames.contains(trimmedKey)) {
					continue;
				}
				seenNames.add(trimmedKey);
				if (trimmedKey.equals("regionauth_id")) {
					continue;
				}
				if (trimmedKey.equals("wxaAppId") || trimmedKey.equals("page")) {
					continue;
				}
				if (trimmedKey.equals("collection_id")) {
					continue;
				}
				if (val == null) {
					continue;
				}
				if (!StringUtils.hasText(val.trim())) {
					continue;
				}
				if ("undefined".equals(val.trim())) {
					continue;
				}
				if ("0".equals(val.trim())) {
					continue;
				}
				if (trimmedKey.equals("distributor_id")) {
					params.put("did", val.trim());
				} else {
					params.put(trimmedKey, val.trim());
				}
			}
		}
		String scene;
		if (params.isEmpty()) {
			scene = "1";
		} else {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Map.Entry<String, String> e : params.entrySet()) {
				if (!first) {
					sb.append('&');
				}
				first = false;
				sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
				sb.append('=');
				sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
			}
			scene = sb.toString();
		}
		byte[] image = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(effectiveAppId, scene, page);
		String b64 = Base64.getEncoder().encodeToString(image);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("base64Image", "data:image/jpg;base64," + b64);
		return data;
	}
}
