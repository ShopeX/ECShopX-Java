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

import cn.shopex.ecshopx.common.members.port.WxappMemberEmployeePurchaseFlagsPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberInviteTicketPort;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberEmployeePurchaseFlagsPortImpl")
public class WxappMemberEmployeePurchaseFlagsPortImpl implements WxappMemberEmployeePurchaseFlagsPort {

	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final WxappMemberInviteTicketPort wxappMemberInviteTicketPort;

	public WxappMemberEmployeePurchaseFlagsPortImpl(
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			WxappMemberInviteTicketPort wxappMemberInviteTicketPort) {
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.wxappMemberInviteTicketPort = wxappMemberInviteTicketPort;
	}

	@Override
	public void applyEmployeeFlag(long companyId, long userId, Map<String, Object> memberInfo) {
		long cnt =
				employeesMapper.selectCount(
						Wrappers.<Employees>lambdaQuery()
								.eq(Employees::getCompanyId, companyId)
								.eq(Employees::getUserId, userId)
								.eq(Employees::getDisabled, false));
		memberInfo.put("is_employee", cnt > 0L);
		memberInfo.put("is_relative", Boolean.FALSE);
	}

	@Override
	public void applyActivityRelativeFlags(
			long companyId, long activityId, long userId, Map<String, Object> memberInfo) {
		if (activityId <= 0L) {
			return;
		}
		Relatives relative =
				relativesMapper.selectOne(
						Wrappers.<Relatives>lambdaQuery()
								.eq(Relatives::getCompanyId, companyId)
								.eq(Relatives::getActivityId, activityId)
								.eq(Relatives::getUserId, userId)
								.eq(Relatives::getDisabled, false)
								.last("LIMIT 1"));
		if (relative != null) {
			memberInfo.put("is_relative", Boolean.TRUE);
		}
	}

	@Override
	public void applyInviteRelativeFlags(
			long companyId, long userId, Map<String, Object> memberInfo, String inviteCode) {
		long[] triple = wxappMemberInviteTicketPort.decodeInviteTripleOrThrow(companyId, inviteCode);
		applyActivityRelativeFlags(companyId, triple[1], userId, memberInfo);
	}
}
