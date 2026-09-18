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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.client.wx.WxOpenPlatformClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MemberNoAuthDecryptPhoneService {

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String, Object> getNoAuthDecryptPhoneNumber(
			String appid, String code, String encryptedData, String iv) {
		if (!StringUtils.hasText(appid)) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		if (!StringUtils.hasText(encryptedData) || !StringUtils.hasText(iv)) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}

		Map<String, String> session =
				wxOpenPlatformClient.miniProgramCode2SessionForDecryptPhone(appid, code);

		String phoneJson =
				wxOpenPlatformClient.decryptWxMiniProgramPhoneNumberForBadRequest(
						session.get("session_key"), iv, encryptedData);

		Map<String, Object> data;
		try {
			data = objectMapper.readValue(phoneJson, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("授权手机号失败");
		}
		if (data == null) {
			throw new BadRequestException("授权手机号失败");
		}
		return data;
	}
}
