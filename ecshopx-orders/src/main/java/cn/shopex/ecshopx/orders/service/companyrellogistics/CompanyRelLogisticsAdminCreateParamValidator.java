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

package cn.shopex.ecshopx.orders.service.companyrellogistics;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.support.ScalarEmptyCompat;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CompanyRelLogisticsAdminCreateParamValidator {

	public Optional<Integer> tryParseCorpIdForDeletePath(String rawPathCorpId) {
		if (isRequiredViolation(rawPathCorpId)) {
			throw new BadRequestException("物流公司id必填");
		}
		String t = String.valueOf(rawPathCorpId).trim();
		try {
			return Optional.of(Integer.parseInt(t));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public NormalizedCompanyRelLogisticsCreateParams validateAndNormalize(Map<String, Object> merged) {
		if (isRequiredViolation(merged.get("corp_id"))) {
			throw new BadRequestException("物流公司id必填");
		}
		if (isRequiredViolation(merged.get("corp_name"))) {
			throw new BadRequestException("物流公司简称必填");
		}
		if (isRequiredViolation(merged.get("corp_code"))) {
			throw new BadRequestException("快递鸟代码必填");
		}
		if (isRequiredViolation(merged.get("kuaidi_code"))) {
			throw new BadRequestException("快递100代码必填");
		}

		String corpIdStr = String.valueOf(merged.get("corp_id")).trim();
		try {
			Integer.parseInt(corpIdStr);
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException("物流公司id必填");
		}

		Integer corpId = ScalarEmptyCompat.isEmpty(merged.get("corp_id"))
				? null
				: Integer.valueOf(Integer.parseInt(String.valueOf(merged.get("corp_id")).trim()));
		String corpName = String.valueOf(merged.get("corp_name")).trim();
		String corpCode = String.valueOf(merged.get("corp_code")).trim();
		String kuaidiCode = String.valueOf(merged.get("kuaidi_code")).trim();

		long distributorId;
		if (!merged.containsKey("distributor_id") || ScalarEmptyCompat.isEmpty(merged.get("distributor_id"))) {
			distributorId = 0L;
		} else {
			distributorId = EspierAdminJwtControllerSupport.parseDistributorId(
					String.valueOf(merged.get("distributor_id")).trim());
		}

		return new NormalizedCompanyRelLogisticsCreateParams(corpId, corpName, corpCode, kuaidiCode, distributorId);
	}

	private static boolean isRequiredViolation(Object raw) {
		if (raw == null) {
			return true;
		}
		return String.valueOf(raw).trim().isEmpty();
	}
}
