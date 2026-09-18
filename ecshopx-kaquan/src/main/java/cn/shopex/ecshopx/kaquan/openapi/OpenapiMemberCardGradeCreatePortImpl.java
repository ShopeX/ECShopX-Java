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

package cn.shopex.ecshopx.kaquan.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeCreatePort;
import cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2.OpenapiThirdApiV2MemberCardGradeCreateService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberCardGradeCreatePortImpl implements OpenapiMemberCardGradeCreatePort {

	private final OpenapiThirdApiV2MemberCardGradeCreateService createService;

	public OpenapiMemberCardGradeCreatePortImpl(
			OpenapiThirdApiV2MemberCardGradeCreateService createService) {
		this.createService = createService;
	}

	@Override
	public Map<String, Object> create(
			long companyId,
			String gradeNameRaw,
			String discountRaw,
			String totalConsumptionRaw,
			String backgroundPicUrlRaw,
			String externalIdRaw) {
		return createService.executeOpenapiCreate(
				companyId,
				gradeNameRaw,
				discountRaw,
				totalConsumptionRaw,
				backgroundPicUrlRaw,
				externalIdRaw);
	}
}
