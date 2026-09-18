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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefApplyWxaCodeService {

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public CommunityChiefApplyWxaCodeService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> buildChiefApplyWxaCode(
			long companyId, String operatorType, String distributorIdQueryRaw, String path) {
		String page;
		if (path == null || !StringUtils.hasText(path.trim())) {
			page = "pages/index";
		} else {
			page = path.trim();
		}
		if (page.startsWith("/")) {
			page = page.substring(1);
		}

		String suffix;
		if ("distributor".equals(operatorType)) {
			suffix = distributorIdQueryRaw == null ? "" : distributorIdQueryRaw;
		} else {
			suffix = "0";
		}
		String scene = "did=" + suffix;

		String wxaAppId =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, TEMPLATE_NAME_YYKWEISHOP)
						.orElse(null);
		if (wxaAppId == null) {
			throw new ResourceException("没有绑定小程序");
		}

		byte[] jpeg;
		try {
			jpeg = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppId, scene, page);
		} catch (Exception e) {
			throw new ResourceException("小程序还从未通过审核，无法生成小程序码，请查看体验二维码");
		}

		String base64Image = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(jpeg);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("base64Image", base64Image);
		return out;
	}
}
