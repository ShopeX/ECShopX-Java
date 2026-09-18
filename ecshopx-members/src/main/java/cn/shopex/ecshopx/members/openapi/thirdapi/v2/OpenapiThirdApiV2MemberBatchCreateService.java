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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.dispatch.OpenapiCreateMemberJobDispatchPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberBatchCreateService {

	private final OpenapiThirdApiV2MemberCreateService createService;
	private final OpenapiCreateMemberJobDispatchPublisher createMemberJobDispatchPublisher;

	public OpenapiThirdApiV2MemberBatchCreateService(
			OpenapiThirdApiV2MemberCreateService createService,
			OpenapiCreateMemberJobDispatchPublisher createMemberJobDispatchPublisher) {
		this.createService = createService;
		this.createMemberJobDispatchPublisher = createMemberJobDispatchPublisher;
	}

	public Map<String, Object> batchCreate(
			long companyId, boolean hasDataKey, List<Map<String, Object>> items) {
		if (!hasDataKey) {
			return Map.of("status", 0);
		}

		List<Map<String, Object>> dataItems = items == null ? List.of() : items;
		if (dataItems.size() > 50) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_ERROR, "批量创建最多支持50个！");
		}

		List<Map<String, Object>> jobForms = new ArrayList<>();
		for (Map<String, Object> datum : dataItems) {
			jobForms.add(createService.validateDatumAndToPhpFormData(companyId, datum));
		}

		for (Map<String, Object> form : jobForms) {
			createMemberJobDispatchPublisher.enqueueCreateMember(companyId, form);
		}

		return Map.of("status", 1);
	}
}
