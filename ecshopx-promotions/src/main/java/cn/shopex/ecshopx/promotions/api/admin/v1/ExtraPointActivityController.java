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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1ExtraPointActivity")
@RequestMapping("/api/v1/promotions/extrapoint")
public class ExtraPointActivityController {

	private final ExtraPointActivityCreateService extraPointActivityCreateService;
	private final LangueProperties langueProperties;

	public ExtraPointActivityController(
			ExtraPointActivityCreateService extraPointActivityCreateService,
			LangueProperties langueProperties) {
		this.extraPointActivityCreateService = extraPointActivityCreateService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.extrapoints.create")
	@PostMapping(name = "创建额外积分活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = ExtraPointActivityAdminSupport.mergeInput(request, body);
		Map<String, Object> jwt = ExtraPointActivityAdminSupport.readOperatorJwtMap(request);
		long companyId = ExtraPointActivityAdminSupport.readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = extraPointActivityCreateService.createActivity(merged, langTag, true, false);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.extrapoints.lists")
	@GetMapping(value = "/lists", name = "额外积分列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(HttpServletRequest request) {
		Map<String, Object> jwt = ExtraPointActivityAdminSupport.readOperatorJwtMap(request);
		long companyId = ExtraPointActivityAdminSupport.readCompanyIdFromOperatorJwtMap(jwt);
		String beginTimeRaw = request.getParameter("begin_time");
		String endTimeRaw = request.getParameter("end_time");
		String titleRaw = request.getParameter("title");
		int page = readIntParam(request, "page", 1);
		int pageSize = readIntParam(request, "pageSize", 10);
		Map<String, Object> payload =
				extraPointActivityCreateService.getActivityList(companyId, beginTimeRaw, endTimeRaw, titleRaw, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private static int readIntParam(HttpServletRequest req, String name, int defaultValue) {
		String v = req.getParameter(name);
		if (v == null || !StringUtils.hasText(v.trim())) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Activated(routeAlias = "promotions.extrapoints.info")
	@GetMapping(value = "/{id}", name = "额外积分详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<?>> getActivityInfo(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> jwt = ExtraPointActivityAdminSupport.readOperatorJwtMap(request);
		long companyId = ExtraPointActivityAdminSupport.readCompanyIdFromOperatorJwtMap(jwt);
		String raw = id == null ? "" : id;
		long activityId;
		try {
			activityId = Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			activityId = 0L;
		}
		Map<String, Object> payload = extraPointActivityCreateService.getActivityInfo(companyId, activityId);
		if (payload.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(payload));
	}
}
