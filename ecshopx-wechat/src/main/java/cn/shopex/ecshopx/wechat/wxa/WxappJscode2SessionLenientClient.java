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

import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import java.util.LinkedHashMap;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappJscode2SessionLenientClient {

	public static final class Jscode2SessionLenientResult {

		private final boolean wechatBusinessError;

		private final LinkedHashMap<String, String> openidUnionid;

		private Jscode2SessionLenientResult(boolean wechatBusinessError, LinkedHashMap<String, String> openidUnionid) {
			this.wechatBusinessError = wechatBusinessError;
			this.openidUnionid = openidUnionid;
		}

		public static Jscode2SessionLenientResult wechatBusinessError() {
			return new Jscode2SessionLenientResult(true, emptyOpenidUnionid());
		}

		public static Jscode2SessionLenientResult success(LinkedHashMap<String, String> openidUnionid) {
			return new Jscode2SessionLenientResult(false, openidUnionid);
		}

		public boolean isWechatBusinessError() {
			return wechatBusinessError;
		}

		public LinkedHashMap<String, String> getOpenidUnionid() {
			return openidUnionid;
		}
	}

	private final WxJavaMaRuntime wxJavaMaRuntime;

	public WxappJscode2SessionLenientClient(WxJavaMaRuntime wxJavaMaRuntime) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public Jscode2SessionLenientResult jscode2sessionLenient(String appid, String secret, String jsCode) {
		LinkedHashMap<String, String> empty = emptyOpenidUnionid();
		if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret) || !StringUtils.hasText(jsCode)) {
			return Jscode2SessionLenientResult.success(empty);
		}
		try {
			WxMaJscode2SessionResult session =
					wxJavaMaRuntime.maDirect(appid.trim(), secret.trim()).jsCode2SessionInfo(jsCode.trim());
			LinkedHashMap<String, String> out = new LinkedHashMap<>();
			out.put("openid", nz(session.getOpenid()));
			out.put("unionid", nz(session.getUnionid()));
			return Jscode2SessionLenientResult.success(out);
		} catch (WxErrorException e) {
			return Jscode2SessionLenientResult.wechatBusinessError();
		} catch (Exception e) {
			return Jscode2SessionLenientResult.success(empty);
		}
	}

	private static String nz(String s) {
		return s != null ? s : "";
	}

	private static LinkedHashMap<String, String> emptyOpenidUnionid() {
		LinkedHashMap<String, String> empty = new LinkedHashMap<>();
		empty.put("openid", "");
		empty.put("unionid", "");
		return empty;
	}
}
