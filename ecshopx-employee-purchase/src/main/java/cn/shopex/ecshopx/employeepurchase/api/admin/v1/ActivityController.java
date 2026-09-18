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

package cn.shopex.ecshopx.employeepurchase.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.service.ActivityCreateService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityInfoService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemDeleteService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityUsersService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseAdminActivityListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsExportFacadeService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityQrcodeExportFacadeService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityScanStatsExportFacadeService;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityAdminExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityEnterpriseBehaviorStatsService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@RestController("employeepurchaseActivityAdminV1")
@RequestMapping("/api/v1")
public class ActivityController {

	private final ActivityCreateService activityCreateService;
	private final EmployeePurchaseActivityItemListService employeePurchaseActivityItemListService;
	private final EmployeePurchaseActivityUsersService employeePurchaseActivityUsersService;
	private final EmployeePurchaseActivityInfoService employeePurchaseActivityInfoService;
	private final EmployeePurchaseActivityItemDeleteService employeePurchaseActivityItemDeleteService;
	private final EmployeePurchaseAdminActivityListService employeePurchaseAdminActivityListService;
	private final ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService;
	private final EmployeePurchaseActivityScanStatsExportFacadeService scanStatsExportFacadeService;
	private final EmployeePurchaseActivityQrcodeExportFacadeService qrcodeExportFacadeService;
	private final EmployeePurchaseActivityItemsExportFacadeService activityItemsExportFacadeService;

	public ActivityController(
			ActivityCreateService activityCreateService,
			EmployeePurchaseActivityItemListService employeePurchaseActivityItemListService,
			EmployeePurchaseActivityUsersService employeePurchaseActivityUsersService,
			EmployeePurchaseActivityInfoService employeePurchaseActivityInfoService,
			EmployeePurchaseActivityItemDeleteService employeePurchaseActivityItemDeleteService,
			EmployeePurchaseAdminActivityListService employeePurchaseAdminActivityListService,
			ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService,
			EmployeePurchaseActivityScanStatsExportFacadeService scanStatsExportFacadeService,
			EmployeePurchaseActivityQrcodeExportFacadeService qrcodeExportFacadeService,
			EmployeePurchaseActivityItemsExportFacadeService activityItemsExportFacadeService) {
		this.activityCreateService = activityCreateService;
		this.employeePurchaseActivityItemListService = employeePurchaseActivityItemListService;
		this.employeePurchaseActivityUsersService = employeePurchaseActivityUsersService;
		this.employeePurchaseActivityInfoService = employeePurchaseActivityInfoService;
		this.employeePurchaseActivityItemDeleteService = employeePurchaseActivityItemDeleteService;
		this.employeePurchaseAdminActivityListService = employeePurchaseAdminActivityListService;
		this.activityEnterpriseBehaviorStatsService = activityEnterpriseBehaviorStatsService;
		this.scanStatsExportFacadeService = scanStatsExportFacadeService;
		this.qrcodeExportFacadeService = qrcodeExportFacadeService;
		this.activityItemsExportFacadeService = activityItemsExportFacadeService;
	}

	@Activated(routeAlias = "employeepurchase.activity.items.export")
	@GetMapping(value = "/employeepurchase/activity/items/export", name = "导出活动商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportActivityItems(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) Long activityId,
			@RequestParam(value = "main_cat_id", required = false) List<Long> mainCatId,
			@RequestParam(value = "category", required = false) Long category,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "item_id", required = false) List<Long> itemId,
			@RequestParam(value = "shelf_status", required = false) Integer shelfStatus,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorId) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		if (activityId == null) {
			throw new BadRequestException("活动ID必填");
		}
		if (itemId != null) {
			for (Long id : itemId) {
				if (id == null || id < 1L) {
					throw new BadRequestException("商品ID格式错误");
				}
			}
		}
		long resolvedDistributorId = distributorId;
		Integer distributorScope = null;
		if ("distributor".equals(stringOrEmpty(operatorJwt.get("operator_type")))) {
			resolvedDistributorId = readDistributorIdFromOperatorJwt(operatorJwt);
			distributorScope = resolvedDistributorId > 0L ? (int) resolvedDistributorId : null;
		}
		Long mainCatIdSingle = null;
		if (mainCatId != null && !mainCatId.isEmpty()) {
			mainCatIdSingle = mainCatId.get(mainCatId.size() - 1);
		}
		ActivityItemsExportQuery query =
				new ActivityItemsExportQuery(
						companyId,
						activityId,
						resolvedDistributorId,
						readOperatorId(operatorJwt),
						distributorScope,
						mainCatIdSingle,
						category,
						StringUtils.hasText(itemName) ? itemName.trim() : null,
						StringUtils.hasText(itemBn) ? itemBn.trim() : null,
						shelfStatus,
						itemId);
		activityItemsExportFacadeService.submitExport(query);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "employeepurchase.activity.items.list")
	@GetMapping(value = "/employeepurchase/activity/items", name = "获取活动商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityItemList(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) Long activityId,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "main_cat_id", required = false) List<Long> mainCatId,
			@RequestParam(value = "category", required = false) Long category,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "shelf_status", required = false) Integer shelfStatus,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);

		Long mainCatIdSingle = null;
		if (mainCatId != null && !mainCatId.isEmpty()) {
			mainCatIdSingle = mainCatId.get(mainCatId.size() - 1);
		}

		if (activityId == null) {
			throw new BadRequestException("活动ID必填");
		}
		int[] pagination = requirePageAndPageSizeForAdminList(pageParam, pageSizeParam);
		int page = pagination[0];
		int pageSize = pagination[1];

		Map<String, Object> data = employeePurchaseActivityItemListService.getActivityItemList(
				companyId, distributorId, activityId.longValue(), page, pageSize,
				mainCatIdSingle, category, itemName, itemBn, shelfStatus);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.items.add")
	@PostMapping(value = "/employeepurchase/activity/items", name = "添加活动商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> addActivityItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.addActivityItems(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.spec_items.update")
	@PostMapping(value = "/employeepurchase/activity/specitems", name = "选择活动商品规格")
	public ResponseEntity<ApiResult<Map<String, Object>>> selectActivitySpecItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.selectActivitySpecItems(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.items.update")
	@PutMapping(value = "/employeepurchase/activity/items", name = "更新活动商品价格库存等")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivityItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.updateActivityItems(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.items.delete")
	@DeleteMapping(value = "/employeepurchase/activity/{activityId}/item/{itemId}", name = "删除活动商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteActivityItems(
			HttpServletRequest request,
			@PathVariable("activityId") String activityId,
			@PathVariable("itemId") String itemId,
			@RequestParam(value = "all", required = false, defaultValue = "0") String all) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		long jwtDistributorId = readDistributorIdFromOperatorJwt(operatorJwt);

		long actId;
		long skuId;
		try {
			actId = Long.parseLong(activityId.trim());
			skuId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
		}

		String allNorm = all == null ? "0" : all.trim();
		boolean allSpec = "1".equals(allNorm) || "true".equalsIgnoreCase(allNorm);

		employeePurchaseActivityItemDeleteService.deleteActivityItems(
				companyId, actId, skuId, allSpec, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "employeepurchase.activity.list")
	@GetMapping(value = "/employeepurchase/activities", name = "获取员工内购活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(required = false) String name,
			@RequestParam(value = "display_time_begin", required = false) Integer displayTimeBegin,
			@RequestParam(value = "buy_time_begin", required = false) Integer buyTimeBegin,
			@RequestParam(value = "buy_time_end", required = false) Integer buyTimeEnd,
			@RequestParam(value = "enterprise_id", required = false) Long enterpriseId,
			@RequestParam(required = false) String status,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);

		int[] pagination = requirePageAndPageSizeForAdminList(pageParam, pageSizeParam);
		int page = pagination[0];
		int pageSize = pagination[1];

		Map<String, Object> data =
				employeePurchaseAdminActivityListService.getActivityList(
						companyId,
						operatorJwt,
						page,
						pageSize,
						name,
						displayTimeBegin,
						buyTimeBegin,
						buyTimeEnd,
						enterpriseId,
						status,
						distributorIdParam);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.users")
	@GetMapping(value = "/employeepurchase/activity/users", name = "获取员工内购活动亲友列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityUsers(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) Long activityId,
			@RequestParam(value = "employee_mobile", required = false) String employeeMobile,
			@RequestParam(value = "relative_mobile", required = false) String relativeMobile,
			@RequestParam(value = "page", required = false, defaultValue = "0") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "0") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);

		if (activityId == null) {
			throw new BadRequestException("活动ID必填");
		}

		Map<String, Object> data = employeePurchaseActivityUsersService.getActivityUsers(
				companyId, activityId.longValue(), employeeMobile, relativeMobile, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.info")
	@GetMapping(value = "/employeepurchase/activity/{activityId}", name = "获取员工内购活动详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityInfo(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		Map<String, Object> data = employeePurchaseActivityInfoService.getActivityInfo(companyId, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.enterprise_behavior_stats")
	@GetMapping(
			value = "/employeepurchase/activity/{activityId}/enterprise-behavior-stats",
			name = "获取活动企业行为统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityEnterpriseBehaviorStats(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		long activityPk;
		try {
			activityPk = Long.parseLong(activityId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID无效");
		}
		List<Map<String, Object>> list =
				activityEnterpriseBehaviorStatsService.loadEnterpriseStatsForActivity(companyId, activityPk);
		Map<String, Object> data = Map.of("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.enterprise_behavior_stats.download")
	@GetMapping(
			value = "/employeepurchase/activity/{activityId}/enterprise-behavior-stats/download",
			name = "下载活动企业行为统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> downloadActivityEnterpriseBehaviorStats(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		long activityPk = parseActivityId(activityId);
		ActivityAdminExportQuery query =
				new ActivityAdminExportQuery(
						companyId, activityPk, resolveDistributorScope(operatorJwt), readOperatorId(operatorJwt));
		scanStatsExportFacadeService.submitExport(query);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "employeepurchase.activity.download_qrcode")
	@GetMapping(value = "/employeepurchase/activity/{activityId}/download-qrcode", name = "下载活动企业小程序码")
	public ResponseEntity<ApiResult<Map<String, Object>>> downloadActivityQrcode(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyIdFromOperatorJwt(operatorJwt);
		long activityPk = parseActivityId(activityId);
		ActivityAdminExportQuery query =
				new ActivityAdminExportQuery(
						companyId, activityPk, resolveDistributorScope(operatorJwt), readOperatorId(operatorJwt));
		qrcodeExportFacadeService.submitExport(query);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "employeepurchase.activity.create")
	@PostMapping(value = "/employeepurchase/activity", name = "创建员工内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.create(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.update")
	@PutMapping(value = "/employeepurchase/activity/{activityId}", name = "更新员工内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivity(HttpServletRequest request,
			@PathVariable("activityId") String activityId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.updateActivity(activityId, merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.if_share_store.set")
	@PostMapping(value = "/employeepurchase/activity/if_share_store", name = "设置活动是否共享库存")
	public ResponseEntity<ApiResult<Map<String, Object>>> seIfShareStore(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = activityCreateService.seIfShareStore(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.cancel")
	@PostMapping(value = "/employeepurchase/activity/cancel/{activityId}", name = "取消内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelActivity(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = activityCreateService.cancelActivity(activityId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.suspend")
	@PostMapping(value = "/employeepurchase/activity/suspend/{activityId}", name = "暂停内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> suspendActivity(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = activityCreateService.suspendActivity(activityId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.active")
	@PostMapping(value = "/employeepurchase/activity/active/{activityId}", name = "重新开始暂停的内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> activeActivity(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = activityCreateService.activeActivity(activityId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.end")
	@PostMapping(value = "/employeepurchase/activity/end/{activityId}", name = "结束内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> endActivity(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = activityCreateService.endActivity(activityId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.activity.ahead")
	@PostMapping(value = "/employeepurchase/activity/ahead/{activityId}", name = "提前开始内购活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> aheadActivity(
			HttpServletRequest request, @PathVariable("activityId") String activityId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = activityCreateService.aheadActivity(activityId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int[] requirePageAndPageSizeForAdminList(String pageParam, String pageSizeParam) {
		if (!StringUtils.hasText(pageParam) || !StringUtils.hasText(pageSizeParam)) {
			throw new BadRequestException("分页参数错误");
		}
		int page;
		int pageSize;
		try {
			page = Integer.parseInt(pageParam.trim());
			pageSize = Integer.parseInt(pageSizeParam.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		return new int[] {page, pageSize};
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static long readCompanyIdFromOperatorJwt(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLongCompany(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLongCompany(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readDistributorIdFromOperatorJwt(Map<String, Object> operatorJwt) {
		Object v = operatorJwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	private static Map<String, Object> requireOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		return operatorJwt;
	}

	private static long parseActivityId(String activityId) {
		try {
			return Long.parseLong(activityId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID无效");
		}
	}

	private static Integer resolveDistributorScope(Map<String, Object> operatorJwt) {
		if (!"distributor".equals(stringOrEmpty(operatorJwt.get("operator_type")))) {
			return null;
		}
		long distributorId = readDistributorIdFromOperatorJwt(operatorJwt);
		return distributorId > 0L ? (int) distributorId : null;
	}

	private static long readOperatorId(Map<String, Object> operatorJwt) {
		Object v = operatorJwt.get("operator_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrEmpty(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}
}
