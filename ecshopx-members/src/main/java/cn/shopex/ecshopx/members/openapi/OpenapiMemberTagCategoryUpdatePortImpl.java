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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryUpdatePort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagCategoryUpdateService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberTagCategoryUpdatePortImpl implements OpenapiMemberTagCategoryUpdatePort {

	private final OpenapiThirdApiV2MemberTagCategoryUpdateService updateService;

	public OpenapiMemberTagCategoryUpdatePortImpl(
			OpenapiThirdApiV2MemberTagCategoryUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public Map<String, Object> updateTagCategory(
			long companyId,
			String categoryIdRaw,
			String categoryNameRaw,
			String sortRaw,
			boolean sortParamPresent) {
		return updateService.executeOpenapiUpdateTagCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, sortParamPresent);
	}
}
