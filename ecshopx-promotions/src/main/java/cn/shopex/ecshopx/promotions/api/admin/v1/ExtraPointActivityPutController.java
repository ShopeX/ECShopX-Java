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

package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.ExtraPointActivityCreateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1ExtraPointActivityPut")
@RequestMapping("/api/v1/promotions/extrapoint")
public class ExtraPointActivityPutController {

	private final ExtraPointActivityCreateService extraPointActivityCreateService;
	private final LangueProperties langueProperties;

	public ExtraPointActivityPutController(
			ExtraPointActivityCreateService extraPointActivityCreateService,
			LangueProperties langueProperties) {
		this.extraPointActivityCreateService = extraPointActivityCreateService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.extrapoints.update")
	@PutMapping(name = "修改额外积分活动", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivity(
			@FlexibleBody Map<String, Object> body, HttpServletRequest request) {
		Map<String, Object> merged = ExtraPointActivityAdminSupport.mergeInput(request, body);
		Map<String, Object> jwt = ExtraPointActivityAdminSupport.readOperatorJwtMap(request);
		long companyId = ExtraPointActivityAdminSupport.readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = extraPointActivityCreateService.createActivity(merged, langTag, false, true);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.extrapoints.invalid")
	@PutMapping(
			value = "/invalid",
			name = "额外积分失效",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateStatusInvalid(
			@FlexibleBody(required = false) Map<String, Object> body, HttpServletRequest request) {
		if (body == null) {
			body = Map.of();
		}
		Map<String, Object> merged = ExtraPointActivityAdminSupport.mergeInput(request, body);
		long companyId =
				ExtraPointActivityAdminSupport.readCompanyIdFromOperatorJwtMap(
						ExtraPointActivityAdminSupport.readOperatorJwtMap(request));
		Map<String, Object> row = extraPointActivityCreateService.updateStatusInvalid(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(row));
	}
}
