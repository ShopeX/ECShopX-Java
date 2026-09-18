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

package cn.shopex.ecshopx.members.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberNotifyBecomeFriendPort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v1.OpenapiThirdApiV1MemberNotifyBecomeFriendService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberNotifyBecomeFriendPortImpl implements OpenapiMemberNotifyBecomeFriendPort {

	private final OpenapiThirdApiV1MemberNotifyBecomeFriendService notifyBecomeFriendService;

	public OpenapiMemberNotifyBecomeFriendPortImpl(
			OpenapiThirdApiV1MemberNotifyBecomeFriendService notifyBecomeFriendService) {
		this.notifyBecomeFriendService = notifyBecomeFriendService;
	}

	@Override
	public void notifyBecomeFriend(
			long companyId, String unionid, String salespersonCode, String isBecomeFriendRaw) {
		notifyBecomeFriendService.executeNotifyBecomeFriend(
				companyId, unionid, salespersonCode, isBecomeFriendRaw);
	}
}
