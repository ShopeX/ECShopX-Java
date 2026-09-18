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

import cn.shopex.ecshopx.common.members.h5.H5WxappRegisterEmployeeAuthenticator;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeAuthenticationService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class H5WxappRegisterEmployeeAuthenticatorBridge implements H5WxappRegisterEmployeeAuthenticator {

	private final EmployeeAuthenticationService employeeAuthenticationService;

	public H5WxappRegisterEmployeeAuthenticatorBridge(EmployeeAuthenticationService employeeAuthenticationService) {
		this.employeeAuthenticationService = employeeAuthenticationService;
	}

	@Override
	public void authenticate(Map<String, Object> mergedParams) {
		employeeAuthenticationService.authenticate(mergedParams);
	}
}
