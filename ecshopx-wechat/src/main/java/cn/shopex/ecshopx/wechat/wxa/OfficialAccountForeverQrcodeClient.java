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

package cn.shopex.ecshopx.wechat.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class OfficialAccountForeverQrcodeClient {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final RestTemplate restTemplate = new RestTemplate();

	public OfficialAccountForeverQrcodeClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public String createForeverShowQrcodeUrl(String authorizerAppid, String scenePayload) {
		String appid = authorizerAppid == null ? "" : authorizerAppid.trim();
		if (appid.isEmpty()) {
			throw new ResourceException("创建永久二维码失败：未绑定公众号");
		}
		WxMpService mp = wxJavaMpRuntime.mp(appid);
		try {
			String ticket;
			Long sceneId = parseLimitSceneIdIfApplicable(scenePayload);
			if (sceneId != null) {
				ticket = mp.getQrcodeService().qrCodeCreateLastTicket(sceneId.intValue()).getTicket();
			} else {
				String payload = scenePayload == null ? "" : scenePayload;
				ticket = mp.getQrcodeService().qrCodeCreateLastTicket(payload).getTicket();
			}
			return mp.getQrcodeService().qrCodePictureUrl(ticket);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
	}

	public byte[] downloadImageBytes(String imageUrl) {
		try {
			ResponseEntity<byte[]> resp = restTemplate.getForEntity(imageUrl, byte[].class);
			if (!resp.getStatusCode().is2xxSuccessful()) {
				throw new ResourceException("获取二维码图片失败");
			}
			byte[] raw = resp.getBody();
			if (raw == null || raw.length == 0) {
				throw new ResourceException("获取二维码图片失败：响应为空");
			}
			return raw;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("获取二维码图片失败：" + e.getMessage());
		}
	}

	/**
	 * 场景值为纯数字且满足 {@code 0 < n < 100000} 时使用 {@code QR_LIMIT_SCENE}，否则使用字符串场景。
	 */
	private static Long parseLimitSceneIdIfApplicable(String scenePayload) {
		if (scenePayload == null || scenePayload.isEmpty()) {
			return null;
		}
		for (int i = 0; i < scenePayload.length(); i++) {
			if (!Character.isDigit(scenePayload.charAt(i))) {
				return null;
			}
		}
		try {
			long n = Long.parseLong(scenePayload);
			if (n > 0 && n < 100_000L) {
				return n;
			}
			return null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
