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

package cn.shopex.ecshopx.members.service.h5.bind;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import cn.shopex.ecshopx.members.service.h5.H5JwtIssuer;
import cn.shopex.ecshopx.members.service.h5.MembersProtocolLogOnLoginService;
import com.nimbusds.jose.JOSEException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappMemberBindFacade {

	private final WxappMemberBindTxService wxappMemberBindTxService;

	private final MemberAccountService memberAccountService;

	private final MembersProtocolLogOnLoginService membersProtocolLogOnLoginService;

	private final H5JwtIssuer h5JwtIssuer;

	public WxappMemberBindFacade(
			WxappMemberBindTxService wxappMemberBindTxService,
			MemberAccountService memberAccountService,
			MembersProtocolLogOnLoginService membersProtocolLogOnLoginService,
			H5JwtIssuer h5JwtIssuer) {
		this.wxappMemberBindTxService = wxappMemberBindTxService;
		this.memberAccountService = memberAccountService;
		this.membersProtocolLogOnLoginService = membersProtocolLogOnLoginService;
		this.h5JwtIssuer = h5JwtIssuer;
	}

	public String bindMember(Map<String, Object> mergedRequest) {
		Map<String, Object> userInfoForToken = wxappMemberBindTxService.bindInTransaction(mergedRequest);
		Map<String, Object> tokenData = memberAccountService.getTokenData(userInfoForToken);
		H5GenericUser u = new H5GenericUser(tokenData);
		membersProtocolLogOnLoginService.appendAcceptedProtocolsIfNeeded(u, mergedRequest);
		try {
			return h5JwtIssuer.issue(u);
		} catch (JOSEException e) {
			throw new ResourceException("未知错误！");
		}
	}
}
