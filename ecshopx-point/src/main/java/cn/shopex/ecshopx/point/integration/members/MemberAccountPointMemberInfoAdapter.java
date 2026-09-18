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

package cn.shopex.ecshopx.point.integration.members;

import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 生产环境将 {@link PointMemberDmPointMemberInfoReadPort} 桥接到
 * {@link MemberAccountService#getMemberInfo}。
 */
@Service
@Profile("!test-cron")
public class MemberAccountPointMemberInfoAdapter implements PointMemberDmPointMemberInfoReadPort {

	private final MemberAccountService memberAccountService;

	public MemberAccountPointMemberInfoAdapter(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	@Override
	public Map<String, Object> getMemberInfo(long userId, long companyId) {
		return memberAccountService.getMemberInfo(userId, companyId);
	}
}
