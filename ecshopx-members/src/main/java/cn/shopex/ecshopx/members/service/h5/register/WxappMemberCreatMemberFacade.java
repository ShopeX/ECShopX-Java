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

package cn.shopex.ecshopx.members.service.h5.register;

import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.members.service.h5.H5LoginOrchestrator;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginAttemptResult;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginResponseData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WxappMemberCreatMemberFacade {

	private final WxappMemberCreatMemberService wxappMemberCreatMemberService;

	private final H5LoginOrchestrator h5LoginOrchestrator;

	public WxappMemberCreatMemberFacade(
			WxappMemberCreatMemberService wxappMemberCreatMemberService,
			H5LoginOrchestrator h5LoginOrchestrator) {
		this.wxappMemberCreatMemberService = wxappMemberCreatMemberService;
		this.h5LoginOrchestrator = h5LoginOrchestrator;
	}

	public ApiResult<Object> creatMember(Map<String, Object> mergedPostData) {
		String apiFrom = Objects.toString(mergedPostData.get("api_from"), "").trim();
		String authType = Objects.toString(mergedPostData.get("auth_type"), "").trim();
		boolean branchA = "h5app".equals(apiFrom) && !"wxapp".equals(authType);

		Map<String, Object> memberResultMap = wxappMemberCreatMemberService.creatMember(mergedPostData);

		if (!branchA) {
			return ApiResult.ok(memberResultMap);
		}

		Map<String, Object> credentials = new LinkedHashMap<>();
		credentials.put("username", mergedPostData.get("mobile"));
		credentials.put("password", mergedPostData.get("password"));
		credentials.put("company_id", mergedPostData.get("company_id"));

		H5LoginAttemptResult r = h5LoginOrchestrator.attempt(credentials);
		if (r.success()) {
			return ApiResult.ok(new H5LoginResponseData(r.jwtToken()));
		}
		return ApiResult.ok(new H5LoginResponseData(Boolean.FALSE));
	}
}
