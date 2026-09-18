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

package cn.shopex.ecshopx.point.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointLogV2ListPort;
import cn.shopex.ecshopx.point.openapi.thirdapi.v2.OpenapiThirdApiV2MemberPointLogListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberPointLogV2ListPortImpl implements OpenapiMemberPointLogV2ListPort {

	private final OpenapiThirdApiV2MemberPointLogListService listService;

	public OpenapiMemberPointLogV2ListPortImpl(
			OpenapiThirdApiV2MemberPointLogListService listService) {
		this.listService = listService;
	}

	@Override
	public Map<String, Object> listMemberPointLogs(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean startDatePresent,
			String startDateRaw,
			boolean endDatePresent,
			String endDateRaw,
			int page,
			int pageSize) {
		return listService.executeOpenapiList(
				companyId,
				mobilePresent,
				mobileRaw,
				startDatePresent,
				startDateRaw,
				endDatePresent,
				endDateRaw,
				page,
				pageSize);
	}
}
