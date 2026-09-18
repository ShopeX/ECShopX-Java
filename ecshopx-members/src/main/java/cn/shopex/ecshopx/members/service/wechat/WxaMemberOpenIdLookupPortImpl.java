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

package cn.shopex.ecshopx.members.service.wechat;

import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaMemberOpenIdLookupPortImpl implements WxaMemberOpenIdLookupPort {

	private final MemberAccountService memberAccountService;

	public WxaMemberOpenIdLookupPortImpl(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	@Override
	public String resolveOpenId(long userId, String authorizerAppid) {
		if (userId <= 0L || !StringUtils.hasText(authorizerAppid)) {
			return "";
		}
		Map<String, Object> wxUser = memberAccountService.getWechatUserInfo(Map.of(
				"user_id", userId,
				"authorizer_appid", authorizerAppid.trim()));
		Object o = wxUser.get("open_id");
		return o == null ? "" : o.toString().trim();
	}
}
