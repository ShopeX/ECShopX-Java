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

package cn.shopex.ecshopx.employeepurchase.integration.members;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.WxappMemberInviteTicketPort;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseInviteHashidsSupport;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseInviteRedisService;
import org.springframework.stereotype.Service;

@Service("wxappMemberInviteTicketPortImpl")
public class WxappMemberInviteTicketPortImpl implements WxappMemberInviteTicketPort {

	private final EmployeePurchaseInviteRedisService employeePurchaseInviteRedisService;
	private final EmployeePurchaseInviteHashidsSupport employeePurchaseInviteHashidsSupport;

	public WxappMemberInviteTicketPortImpl(
			EmployeePurchaseInviteRedisService employeePurchaseInviteRedisService,
			EmployeePurchaseInviteHashidsSupport employeePurchaseInviteHashidsSupport) {
		this.employeePurchaseInviteRedisService = employeePurchaseInviteRedisService;
		this.employeePurchaseInviteHashidsSupport = employeePurchaseInviteHashidsSupport;
	}

	@Override
	public long[] decodeInviteTripleOrThrow(long companyId, String inviteCode) {
		String ticket = employeePurchaseInviteRedisService.getActiveInviteTicketOrThrow(companyId, inviteCode);
		long[] decoded = employeePurchaseInviteHashidsSupport.decodeTicketOrEmpty(ticket);
		if (decoded.length < 3 || decoded[0] <= 0L || decoded[1] <= 0L || decoded[2] <= 0L) {
			throw new ResourceException("分享链接已失效");
		}
		return decoded;
	}
}
