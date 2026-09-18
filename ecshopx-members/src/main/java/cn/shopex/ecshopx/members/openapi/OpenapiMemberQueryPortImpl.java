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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberQueryPort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v1.OpenapiThirdApiV1MemberMemberInfoService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberQueryPortImpl implements OpenapiMemberQueryPort {

	private final OpenapiThirdApiV1MemberMemberInfoService memberInfoService;

	public OpenapiMemberQueryPortImpl(OpenapiThirdApiV1MemberMemberInfoService memberInfoService) {
		this.memberInfoService = memberInfoService;
	}

	@Override
	public Map<String, Object> memberInfo(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean unionidPresent,
			String unionidRaw,
			boolean unionidTruthy) {
		return memberInfoService.executeOpenapiMemberInfo(
				companyId,
				mobileQueryParam,
				unionidQueryParam,
				body,
				mobilePresent,
				mobileRaw,
				mobileTruthy,
				unionidPresent,
				unionidRaw,
				unionidTruthy);
	}
}
