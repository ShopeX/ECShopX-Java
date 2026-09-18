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

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaTrialQrcodeClient {

	private static final int DEFAULT_QRCODE_WIDTH = 430;

	private static final String CREATE_WXA_QRCODE_PATH = "/pages/index";

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WxaTrialQrcodeClient(WxJavaMaRuntime wxJavaMaRuntime) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public byte[] getTrialQrcodeBytes(WechatAuth auth, boolean withPagePath) {
		if (auth == null || !StringUtils.hasText(auth.getAuthorizerAppid())) {
			throw new ResourceException("小程序 AppId 无效");
		}
		String appid = auth.getAuthorizerAppid().trim();
		WxMaService svc = resolveMaService(auth, appid);
		byte[] raw;
		try {
			if (withPagePath) {
				raw = svc.getQrcodeService().createQrcodeBytes(CREATE_WXA_QRCODE_PATH, DEFAULT_QRCODE_WIDTH);
			} else {
				raw = svc.getCodeService().getQrCode(null);
			}
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
		if (raw == null || raw.length == 0) {
			throw new ResourceException("获取体验版二维码失败：响应为空");
		}
		if (looksLikeJsonError(raw)) {
			JsonNode root;
			try {
				root = objectMapper.readTree(raw);
			} catch (Exception e) {
				throw new ResourceException("获取体验版二维码失败");
			}
			String msg = root.path("errmsg").asText("获取体验版二维码失败");
			throw new ResourceException(msg);
		}
		return raw;
	}

	private WxMaService resolveMaService(WechatAuth auth, String appid) {
		boolean direct = auth.getIsDirect() != null && auth.getIsDirect() == 1;
		if (!direct) {
			return wxJavaMaRuntime.ma(appid);
		}
		String secret = auth.getAuthorizerAppsecret();
		if (!StringUtils.hasText(secret)) {
			throw new ResourceException("当前公众号或小程序未配置secret", 400, 400001);
		}
		return wxJavaMaRuntime.maDirect(appid, secret.trim());
	}

	private static boolean looksLikeJsonError(byte[] raw) {
		for (int i = 0; i < raw.length; i++) {
			byte b = raw[i];
			if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
				continue;
			}
			return b == '{';
		}
		return false;
	}
}
