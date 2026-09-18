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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.service.upload.EspierImageUploadClient;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorWxaCodeOrchestrator {

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final EspierImageUploadClient espierImageUploadClient;

	public DistributorWxaCodeOrchestrator(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			EspierImageUploadClient espierImageUploadClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.espierImageUploadClient = espierImageUploadClient;
	}

	public Map<String, Object> buildWxaCodePayload(long companyId, long distributorId, String codeType, String templateName) {
		String nameForDb = (templateName != null && !templateName.isBlank()) ? templateName.trim() : "";
		// 处理顺序：template_name → getWxappidByTemplateName → codetype / getWxaDistributorCodeStream
		String wxaAppId = weappAuthorizerAppidRepository
				.findAuthorizerAppid(companyId, nameForDb)
				.orElseThrow(() -> new ResourceException("没有开通此小程序，不能下载"));
		String page;
		String scene;
		switch (codeType) {
			case "index" -> {
				page = "pages/index";
				scene = "dtid=" + distributorId;
			}
			case "store" -> {
				page = "subpages/store/index";
				scene = "dtid=" + distributorId;
			}
			case "scancode" -> {
				page = "pages/qrcode-buy";
				scene = "dtid=" + distributorId + "&qrcode=true";
			}
			default -> throw new ResourceException("codetype 无效");
		}
		byte[] first = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppId, scene, page);
		String base64Image = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(first);
		byte[] second = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppId, scene, page);
		String url = "";
		try {
			url = espierImageUploadClient.uploadDecodedImage(companyId, second, "image/jpeg");
		} catch (Exception ignored) {
			url = "";
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("base64Image", base64Image);
		result.put("tempname", nameForDb);
		result.put("url", url == null ? "" : url);
		return result;
	}
}
