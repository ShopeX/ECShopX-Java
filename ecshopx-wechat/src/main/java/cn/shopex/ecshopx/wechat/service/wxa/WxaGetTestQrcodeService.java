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
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaTrialQrcodeClient;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaGetTestQrcodeService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaTrialQrcodeClient wxaTrialQrcodeClient;

	public WxaGetTestQrcodeService(
			WechatAuthQueryService wechatAuthQueryService, WxaTrialQrcodeClient wxaTrialQrcodeClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaTrialQrcodeClient = wxaTrialQrcodeClient;
	}

	public Map<String, Object> getTestQrcode(long companyId, String wxaAppId, String isDirectRaw) {
		String app = wxaAppId == null ? "" : wxaAppId.trim();
		if (!WxaUploadWxaService.wxaAppIdHasText(app)) {
			throw new BadRequestException("wxaAppId 不能为空");
		}
		WechatAuth auth =
				wechatAuthQueryService
						.findBoundAuthByCompanyAndAuthorizerAppid(companyId, app)
						.orElseThrow(() -> new ResourceException("小程序未绑定，请重新绑定", 400, 400001));
		boolean withPagePath = !isDirectQueryEffectivelyEmpty(isDirectRaw);
		byte[] image = wxaTrialQrcodeClient.getTrialQrcodeBytes(auth, withPagePath);
		String b64 = Base64.getEncoder().encodeToString(image);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("base64Image", "data:image/jpg;base64," + b64);
		return data;
	}

	/**
	 * Query {@code is_direct} treated as empty when absent, blank, or {@code "0"} after trim
	 * (non-empty non-zero strings request the page path branch).
	 */
	private static boolean isDirectQueryEffectivelyEmpty(String raw) {
		if (raw == null) {
			return true;
		}
		String s = raw.trim();
		if (!StringUtils.hasText(s)) {
			return true;
		}
		return "0".equals(s);
	}
}
