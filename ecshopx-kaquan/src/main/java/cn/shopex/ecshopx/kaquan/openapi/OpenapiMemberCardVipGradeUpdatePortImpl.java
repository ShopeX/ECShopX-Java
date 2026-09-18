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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeUpdatePort;
import cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2.OpenapiThirdApiV2MemberCardVipGradeUpdateService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberCardVipGradeUpdatePortImpl implements OpenapiMemberCardVipGradeUpdatePort {

	private final OpenapiThirdApiV2MemberCardVipGradeUpdateService updateService;

	public OpenapiMemberCardVipGradeUpdatePortImpl(
			OpenapiThirdApiV2MemberCardVipGradeUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public Map<String, Object> update(
			long companyId,
			String vipGradeIdRaw,
			Optional<String> gradeNamePresent,
			Optional<String> monthlyFeePresent,
			Optional<String> quarterFeePresent,
			Optional<String> yearFeePresent,
			Optional<String> discountPresent,
			Optional<String> guideTitlePresent,
			Optional<String> descriptionPresent,
			Optional<String> isDefaultPresent,
			Optional<String> isDisabledPresent,
			Optional<String> externalIdPresent) {
		return updateService.executeOpenapiUpdate(
				companyId,
				vipGradeIdRaw,
				gradeNamePresent,
				monthlyFeePresent,
				quarterFeePresent,
				yearFeePresent,
				discountPresent,
				guideTitlePresent,
				descriptionPresent,
				isDefaultPresent,
				isDisabledPresent,
				externalIdPresent);
	}
}
