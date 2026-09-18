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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagUpdatePort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagUpdateService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberTagUpdatePortImpl implements OpenapiMemberTagUpdatePort {

	private final OpenapiThirdApiV2MemberTagUpdateService updateService;

	public OpenapiMemberTagUpdatePortImpl(OpenapiThirdApiV2MemberTagUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public Map<String, Object> updateTag(
			long companyId,
			String tagIdRaw,
			String tagNameRaw,
			String descriptionRaw,
			boolean descriptionParamPresent,
			String tagColorRaw,
			String fontColorRaw,
			String categoryIdRaw,
			boolean categoryIdParamPresent) {
		return updateService.executeOpenapiUpdateTag(
				companyId, tagIdRaw, tagNameRaw, descriptionRaw, descriptionParamPresent,
				tagColorRaw, fontColorRaw, categoryIdRaw, categoryIdParamPresent);
	}
}
