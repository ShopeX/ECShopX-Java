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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityCreateService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityDeleteService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityDetailService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityFinishService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityListService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityUpdateService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamInfoService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1PromotionGroupsActivity")
@RequestMapping("/api/v1/promotions/groups")
public class PromotionGroupsActivityController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PromotionGroupsActivityCreateService promotionGroupsActivityCreateService;
	private final PromotionGroupsActivityDeleteService promotionGroupsActivityDeleteService;
	private final PromotionGroupsActivityFinishService promotionGroupsActivityFinishService;
	private final PromotionGroupsActivityListService promotionGroupsActivityListService;
	private final PromotionGroupsActivityDetailService promotionGroupsActivityDetailService;
	private final PromotionGroupsActivityUpdateService promotionGroupsActivityUpdateService;
	private final PromotionGroupsTeamInfoService promotionGroupsTeamInfoService;
	private final PromotionGroupsTeamListService promotionGroupsTeamListService;
	private final LangueProperties langueProperties;

	public PromotionGroupsActivityController(
			PromotionGroupsActivityCreateService promotionGroupsActivityCreateService,
			PromotionGroupsActivityDeleteService promotionGroupsActivityDeleteService,
			PromotionGroupsActivityFinishService promotionGroupsActivityFinishService,
			PromotionGroupsActivityListService promotionGroupsActivityListService,
			PromotionGroupsActivityDetailService promotionGroupsActivityDetailService,
			PromotionGroupsActivityUpdateService promotionGroupsActivityUpdateService,
			PromotionGroupsTeamInfoService promotionGroupsTeamInfoService,
			PromotionGroupsTeamListService promotionGroupsTeamListService,
			LangueProperties langueProperties) {
		this.promotionGroupsActivityCreateService = promotionGroupsActivityCreateService;
		this.promotionGroupsActivityDeleteService = promotionGroupsActivityDeleteService;
		this.promotionGroupsActivityFinishService = promotionGroupsActivityFinishService;
		this.promotionGroupsActivityListService = promotionGroupsActivityListService;
		this.promotionGroupsActivityDetailService = promotionGroupsActivityDetailService;
		this.promotionGroupsActivityUpdateService = promotionGroupsActivityUpdateService;
		this.promotionGroupsTeamInfoService = promotionGroupsTeamInfoService;
		this.promotionGroupsTeamListService = promotionGroupsTeamListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.groups.list")
	@GetMapping(name = "拼团列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromotionGroupsActivityList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "keywords", required = false) String keywords,
			@RequestParam(value = "view", required = false) Integer view) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				promotionGroupsActivityListService.getPromotionGroupsActivityList(
						companyId, pageRaw, pageSizeRaw, keywords, view, null, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.groups.detail")
	@GetMapping(value = "/{groupId}", name = "拼团详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromotionGroupsActivityDetail(
			HttpServletRequest request, @PathVariable("groupId") String groupId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String requestLangTag = RequestLangTag.current(langueProperties);
		long groupsActivityId;
		try {
			groupsActivityId = Long.parseLong(groupId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数格式错误");
		}
		Map<String, Object> data =
				promotionGroupsActivityDetailService.getPromotionGroupsActivityDetail(
						companyId, groupsActivityId, requestLangTag, request, jwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.groups.teamlist")
	@GetMapping(value = "/{groupId}/team", name = "获取拼团数据详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromotionGroupsTeamList(
			HttpServletRequest request,
			@PathVariable("groupId") String groupId,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "view", required = false) Integer view,
			@RequestParam(value = "start_time", required = false) Long startTime,
			@RequestParam(value = "end_time", required = false) Long endTime) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String requestLangTag = RequestLangTag.current(langueProperties);
		long actId = LeadingNumberParser.parseAsLong(groupId != null ? groupId : "");
		Map<String, Object> data =
				promotionGroupsTeamListService.getPromotionGroupsTeamList(
						companyId, actId, pageRaw, pageSizeRaw, view, startTime, endTime, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.groups.teaminfo")
	@GetMapping(value = "/team/{teamId}", name = "拼团成员")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromotionGroupsTeamInfo(
			HttpServletRequest request,
			@PathVariable("teamId") String teamId,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "start_time", required = false) Long startTime,
			@RequestParam(value = "end_time", required = false) Long endTime,
			@RequestParam(value = "order_id", required = false) String orderId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> data =
				promotionGroupsTeamInfoService.getPromotionGroupsTeamInfo(
						companyId, teamId, pageRaw, pageSizeRaw, startTime, endTime, orderId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.groups.create")
	@PostMapping(name = "创建拼团")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPromotionGroupsActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = java.util.Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = promotionGroupsActivityCreateService.createPromotionGroupsActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								input.put(k, v[0]);
							}
						});
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	@Activated(routeAlias = "promotions.groups.update")
	@PutMapping(
			value = "/{groupId}",
			name = "更新拼团",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePromotionGroupsActivity(
			@PathVariable("groupId") String groupId,
			@FlexibleBody Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = java.util.Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row =
				promotionGroupsActivityUpdateService.updatePromotionGroupsActivity(merged, groupId, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.groups.finish")
	@PutMapping(
			value = "/finish/{groupId}",
			name = "结束拼团",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> finishPromotionGroupsActivity(
			HttpServletRequest request, @PathVariable("groupId") String groupId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> row = promotionGroupsActivityFinishService.finishPromotionGroupsActivity(companyId, groupId);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.groups.delete")
	@DeleteMapping(value = "/{groupId}", name = "删除拼团")
	public ResponseEntity<Void> deletePromotionGroupsActivity(
			HttpServletRequest request, @PathVariable("groupId") String groupId) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		promotionGroupsActivityDeleteService.deletePromotionGroupsActivity(companyId, groupId);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).build();
	}
}
