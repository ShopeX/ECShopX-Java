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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOAuthGetOpenIdService {

	private static final String OPEN_PLATFORM_WOA_INVALID_MSG = "公众号信息有误！";

	private final OpenPlatformWoaFacade openPlatformWoaFacade;
	private final OfficialAccountOAuthFacade officialAccountOAuthFacade;

	public WxappOAuthGetOpenIdService(
			OpenPlatformWoaFacade openPlatformWoaFacade,
			OfficialAccountOAuthFacade officialAccountOAuthFacade) {
		this.openPlatformWoaFacade = openPlatformWoaFacade;
		this.officialAccountOAuthFacade = officialAccountOAuthFacade;
	}

	public String getOpenId(long companyId, String code) {
		if (code == null || code.isEmpty() || "0".equals(code)) {
			throw new ResourceException("缺少参数");
		}
		Map<String, Object> woaApp;
		try {
			woaApp = openPlatformWoaFacade.getWoaApp(companyId, "weixin", "touch");
		} catch (ResourceException e) {
			if (OPEN_PLATFORM_WOA_INVALID_MSG.equals(e.getMessage())) {
				throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定");
			}
			throw e;
		}
		return officialAccountOAuthFacade.getOpenIdByOAuthCode(woaApp, code);
	}
}
