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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2ListPort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberListService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberV2ListPortImpl implements OpenapiMemberV2ListPort {

	private final OpenapiThirdApiV2MemberListService listService;

	public OpenapiMemberV2ListPortImpl(OpenapiThirdApiV2MemberListService listService) {
		this.listService = listService;
	}

	@Override
	public Map<String, Object> listMembers(long companyId, int page, int pageSize, Map<String, Object> rawParams) {
		return listService.executeOpenapiList(companyId, page, pageSize, rawParams);
	}
}
