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

package cn.shopex.ecshopx.salesperson.service.signin;

/**
 * 大屏签到 wxcode 业务状态，与 SignService 常量一致。
 */
public final class SalespersonSigninWxcodeStatus {

	public static final int STATUS_WXCODE_WRIT = 0;
	public static final int STATUS_WXCODE_SWEEP = 1;
	public static final int STATUS_WXCODE_SIGNIN = 2;
	public static final int STATUS_WXCODE_ERROR = 3;
	public static final int STATUS_WXCODE_EXPIRED = 4;
	public static final int STATUS_WXCODE_NOTHING = 5;
	public static final int STATUS_WXCODE_SIGNOUT = 6;
	public static final int STATUS_WXCODE_AUTHFAIL = 7;

	private SalespersonSigninWxcodeStatus() {
	}
}
