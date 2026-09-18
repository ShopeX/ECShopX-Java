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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryDeletePort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagCategoryDeleteService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberTagCategoryDeletePortImpl implements OpenapiMemberTagCategoryDeletePort {

	private final OpenapiThirdApiV2MemberTagCategoryDeleteService deleteService;

	public OpenapiMemberTagCategoryDeletePortImpl(
			OpenapiThirdApiV2MemberTagCategoryDeleteService deleteService) {
		this.deleteService = deleteService;
	}

	@Override
	public Map<String, Object> deleteTagCategory(long companyId, String categoryIdRaw) {
		return deleteService.executeOpenapiDeleteTagCategory(companyId, categoryIdRaw);
	}
}
