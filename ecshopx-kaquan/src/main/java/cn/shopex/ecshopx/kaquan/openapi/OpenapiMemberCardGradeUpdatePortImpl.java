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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeUpdatePort;
import cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2.OpenapiThirdApiV2MemberCardGradeUpdateService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberCardGradeUpdatePortImpl implements OpenapiMemberCardGradeUpdatePort {

	private final OpenapiThirdApiV2MemberCardGradeUpdateService updateService;

	public OpenapiMemberCardGradeUpdatePortImpl(
			OpenapiThirdApiV2MemberCardGradeUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public Map<String, Object> update(
			long companyId,
			String gradeIdRaw,
			Optional<String> gradeNamePresent,
			Optional<String> discountPresent,
			Optional<String> totalConsumptionPresent,
			Optional<String> backgroundPicUrlPresent,
			Optional<String> externalIdPresent) {
		return updateService.executeOpenapiUpdate(
				companyId,
				gradeIdRaw,
				gradeNamePresent,
				discountPresent,
				totalConsumptionPresent,
				backgroundPicUrlPresent,
				externalIdPresent);
	}
}
