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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionGoodsWxaCodeStreamService {

	private static final String TEMPLATE_YYKWEISHOP = "yykweishop";

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public DistributionGoodsWxaCodeStreamService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public byte[] buildJpegBytes(long companyId, String itemId, String distributorId) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		if (itemId == null || !StringUtils.hasText(itemId.trim())) {
			fieldErrors.put("item_id", List.of("validation.required"));
		}
		if (distributorId == null || !StringUtils.hasText(distributorId.trim())) {
			fieldErrors.put("distributor_id", List.of("validation.required"));
		}
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("获取小程序码参数出错，请检查.", fieldErrors);
		}
		String itemTrim = itemId.trim();
		String distTrim = distributorId.trim();

		String wxaAppid = weappAuthorizerAppidRepository
				.findAuthorizerAppid(companyId, TEMPLATE_YYKWEISHOP)
				.orElse(null);
		if (wxaAppid == null) {
			throw new ResourceException("没有开通此小程序，不能下载.");
		}

		String scene = "id=" + itemTrim + "&dtid=" + distTrim;
		try {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppid, scene, "pages/item/espier-detail");
		} catch (Exception e) {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppid, scene, "pages/goodsdetail");
		}
	}
}
