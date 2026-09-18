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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WxaAuthorizerAppIdByTemplateService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityWxaCodeService {

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";
	private static final String WXA_PAGE = "pages/index";
	/** {@link WxaAuthorizerAppIdByTemplateService#requireAuthorizerAppid} when no weapp row / empty appid */
	private static final String WX_UNBOUND_RESOURCE_MESSAGE = "未绑定小程序,稍后配置";
	/** Same compact error envelope as other wxa code failures (message + status_code). */
	private static final String WX_UNBOUND_CLIENT_MESSAGE = "当前账号未绑定公众号或小程序，请先授权绑定";

	private final WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public SeckillActivityWxaCodeService(
			WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.wxaAuthorizerAppIdByTemplateService = wxaAuthorizerAppIdByTemplateService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> getSeckillWxaCode(
			long companyId, String seckillIdRaw, String seckillTypeRaw, String distributorIdRaw) {
		String stRaw = seckillTypeRaw == null ? "" : seckillTypeRaw.trim();
		String stype = "normal".equals(stRaw) ? "1" : "2";

		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		String sid = seckillIdRaw == null ? "" : seckillIdRaw.trim();
		m.put("sid", sid);
		m.put("stype", stype);
		if (shouldIncludeDistributorIdInScene(distributorIdRaw)) {
			m.put("dtid", distributorIdRaw.trim());
		}

		String scene = buildSceneQueryString(m);
		if (scene.length() > 32) {
			throw new BadRequestException("参数非法");
		}

		String appid;
		try {
			appid =
					wxaAuthorizerAppIdByTemplateService.requireAuthorizerAppid(
							companyId, TEMPLATE_NAME_YYKWEISHOP);
		} catch (ResourceException e) {
			if (WX_UNBOUND_RESOURCE_MESSAGE.equals(e.getMessage())) {
				throw new BadRequestException(WX_UNBOUND_CLIENT_MESSAGE, 400);
			}
			throw e;
		}
		byte[] jpeg = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(appid, scene, WXA_PAGE);

		String dataUri = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(jpeg);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("code", dataUri);
		return out;
	}

	private static boolean shouldIncludeDistributorIdInScene(String distributorIdRaw) {
		if (distributorIdRaw == null) {
			return false;
		}
		String t = distributorIdRaw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return true;
	}

	private static String buildSceneQueryString(LinkedHashMap<String, String> ordered) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> e : ordered.entrySet()) {
			if (!first) {
				sb.append('&');
			}
			first = false;
			String key = e.getKey();
			String value = e.getValue() == null ? "" : e.getValue();
			sb.append(key)
					.append('=')
					.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		}
		return sb.toString();
	}
}
