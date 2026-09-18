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

package cn.shopex.ecshopx.employeepurchase.h5;

import cn.shopex.ecshopx.common.members.h5.H5WxappRegisterInviteBinder;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeRelativeBindService;
import org.springframework.stereotype.Component;

@Component
public class H5WxappRegisterInviteBinderBridge implements H5WxappRegisterInviteBinder {

	private final EmployeeRelativeBindService employeeRelativeBindService;

	public H5WxappRegisterInviteBinderBridge(EmployeeRelativeBindService employeeRelativeBindService) {
		this.employeeRelativeBindService = employeeRelativeBindService;
	}

	@Override
	public void lockInviteCode(long companyId, String inviteCode) {
		employeeRelativeBindService.lockInviteCodeOnly(companyId, inviteCode);
	}

	@Override
	public void bindRelativeAndConsumeInvite(long companyId, long userId, String memberMobile, String inviteCode) {
		employeeRelativeBindService.bindRelativeAndConsumeInvite(companyId, userId, memberMobile, inviteCode);
	}

	@Override
	public void unlockInviteCode(long companyId, String inviteCode) {
		employeeRelativeBindService.unlockInviteCodeOnly(companyId, inviteCode);
	}
}
