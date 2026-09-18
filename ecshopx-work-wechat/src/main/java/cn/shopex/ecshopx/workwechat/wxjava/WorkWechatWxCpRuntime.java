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

package cn.shopex.ecshopx.workwechat.wxjava;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.stereotype.Component;

@Component
public class WorkWechatWxCpRuntime {

	private static final int TOKEN_EXPIRES_SECONDS = 7200;

	public WxCpService cpBearer(String accessToken) {
		String tok = accessToken == null ? "" : accessToken.trim();
		WxCpDefaultConfigImpl cfg = new WxCpDefaultConfigImpl();
		cfg.setCorpId("bearer");
		cfg.setCorpSecret("");
		cfg.updateAccessToken(tok, TOKEN_EXPIRES_SECONDS);
		WxCpServiceImpl impl = new WxCpServiceImpl();
		impl.setWxCpConfigStorage(cfg);
		return impl;
	}

	public WxCpService cpDirect(String corpId, String corpSecret) {
		WxCpDefaultConfigImpl cfg = new WxCpDefaultConfigImpl();
		cfg.setCorpId(corpId == null ? "" : corpId.trim());
		cfg.setCorpSecret(corpSecret == null ? "" : corpSecret.trim());
		WxCpServiceImpl impl = new WxCpServiceImpl();
		impl.setWxCpConfigStorage(cfg);
		return impl;
	}
}
