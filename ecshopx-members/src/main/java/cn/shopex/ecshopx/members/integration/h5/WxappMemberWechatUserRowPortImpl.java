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

package cn.shopex.ecshopx.members.integration.h5;

import cn.shopex.ecshopx.common.members.port.WxappMemberWechatUserRowPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberWechatUserRowPortImpl")
public class WxappMemberWechatUserRowPortImpl implements WxappMemberWechatUserRowPort {

	private final MemberAccountService memberAccountService;

	public WxappMemberWechatUserRowPortImpl(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	@Override
	public Map<String, Object> loadByUserId(long companyId, long userId) {
		Map<String, Object> assoc = memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
		if (assoc == null || assoc.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashMap<String, Object> wxFilter = new LinkedHashMap<>();
		wxFilter.put("company_id", companyId);
		Object u = assoc.get("unionid");
		if (u != null) {
			wxFilter.put("unionid", String.valueOf(u));
		}
		Map<String, Object> wxMap = memberAccountService.getWechatSimpleUser(wxFilter);
		if (wxMap == null || wxMap.isEmpty()) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(wxMap);
	}
}
