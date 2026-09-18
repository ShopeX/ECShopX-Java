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

package cn.shopex.ecshopx.promotions.service.bargain;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserBargainFriendWxaCodeService {

	private static final String WXA_PAGE = "boost/pages/flop/index";

	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public UserBargainFriendWxaCodeService(WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> getBargainFriendWxaCode(String wxappAppid, long userId, String bargainId) {
		if (wxappAppid == null || wxappAppid.isEmpty() || userId <= 0L
				|| bargainId == null || bargainId.length() < 1) {
			throw new BadRequestException("参数非法");
		}
		String scene = "uid=" + Long.toString(userId) + "&bid=" + bargainId;
		if (scene.length() > 32) {
			throw new BadRequestException("参数非法");
		}
		byte[] jpeg = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappAppid, scene, WXA_PAGE);
		String dataUri = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(jpeg);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("base64Image", dataUri);
		return out;
	}
}
