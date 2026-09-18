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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagDeletePort;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagDeleteService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberTagDeletePortImpl implements OpenapiMemberTagDeletePort {

	private final OpenapiThirdApiV2MemberTagDeleteService deleteService;

	public OpenapiMemberTagDeletePortImpl(
			OpenapiThirdApiV2MemberTagDeleteService deleteService) {
		this.deleteService = deleteService;
	}

	@Override
	public Map<String, Object> deleteTag(long companyId, String tagIdRaw) {
		return deleteService.executeOpenapiDeleteTag(companyId, tagIdRaw);
	}
}
