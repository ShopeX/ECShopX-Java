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

package cn.shopex.ecshopx.community.api.front.v1.member;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.dto.CommunityItemsListQuery;
import cn.shopex.ecshopx.community.dto.MemberH5ActivityListParams;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.community.service.CommunityItemsListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("communityFrontMemberV1Activity")
@RequestMapping("/api/v1/h5app")
public class CommunityActivityController {

	private final CommunityActivityService communityActivityService;
	private final CommunityChiefDistributorMapper communityChiefDistributorMapper;
	private final CommunityItemsListService communityItemsListService;

	public CommunityActivityController(
			CommunityActivityService communityActivityService,
			CommunityChiefDistributorMapper communityChiefDistributorMapper,
			CommunityItemsListService communityItemsListService) {
		this.communityActivityService = communityActivityService;
		this.communityChiefDistributorMapper = communityChiefDistributorMapper;
		this.communityItemsListService = communityItemsListService;
	}

	@GetMapping(value = "/wxapp/community/member/activity", name = "会员端活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "tab_status", required = false, defaultValue = "all") String tabStatus,
			@RequestParam(name = "activity_name", required = false) String activityName,
			@RequestParam(name = "order_by", required = false) String orderBy) {
		page = Math.max(1, page);
		pageSize = Math.max(1, pageSize);
		long companyId = parseCompanyIdFromRequest(request);
		Long optionalChiefId = parseOptionalChiefIdFromClaims(request);
		String tabNorm = normalizeMemberTabStatus(tabStatus);
		String nameContains = null;
		if (StringUtils.hasText(activityName)) {
			String trimmed = activityName.trim();
			nameContains = trimmed.isEmpty() ? null : trimmed;
		}
		boolean orderByOrderNum =
				StringUtils.hasText(orderBy) && "order_num".equals(orderBy.trim());
		MemberH5ActivityListParams params =
				new MemberH5ActivityListParams(tabNorm, nameContains, orderByOrderNum);
		Map<String, Object> data =
				communityActivityService.getMemberH5ActivityList(
						companyId, optionalChiefId, params, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/member/activity/{activity_id}", name = "会员端活动详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityDetail(
			HttpServletRequest request, @PathVariable("activity_id") String activityId) {
		long companyId = parseCompanyIdFromRequest(request);
		long activityIdLong = parseActivityPathId(activityId);
		Map<String, Object> data =
				communityActivityService.getH5ActivityDetailMap(companyId, activityIdLong);
		long activityChiefId = longFromMapValue(data.get("chief_id"));
		CommunityChiefDistributor dist =
				communityChiefDistributorMapper.selectOne(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, activityChiefId)
								.last("LIMIT 1"));
		data.put(
				"distributor_id",
				dist != null && dist.getDistributorId() != null ? Math.toIntExact(dist.getDistributorId()) : 0);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/member/items", name = "会员端团长商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDisitrbutorItemList(
			HttpServletRequest request,
			@RequestParam(name = "chief_id", required = false) String chiefIdRaw,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "distributor_id", required = false) Integer distributorIdParam) {
		long companyId = parseCompanyIdFromRequest(request);
		if (chiefIdRaw == null || !StringUtils.hasText(chiefIdRaw.trim())) {
			throw new ResourceException("必须选择活动的团长信息");
		}
		long chiefId;
		try {
			chiefId = Long.parseLong(chiefIdRaw.trim());
			if (chiefId <= 0L) {
				throw new ResourceException("必须选择活动的团长信息");
			}
		} catch (NumberFormatException e) {
			throw new ResourceException("必须选择活动的团长信息");
		}
		List<CommunityChiefDistributor> chiefRows =
				communityChiefDistributorMapper.selectList(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, chiefId));
		List<Integer> distributorIds =
				chiefRows.stream()
						.map(CommunityChiefDistributor::getDistributorId)
						.filter(Objects::nonNull)
						.map(
								did -> {
									try {
										return Math.toIntExact(did);
									} catch (ArithmeticException e) {
										throw new BadRequestException("参数错误");
									}
								})
						.distinct()
						.toList();
		if (distributorIds.isEmpty()) {
			throw new ResourceException("当前团长没有配置店铺");
		}
		if (distributorIdParam != null && distributorIdParam > 0) {
			distributorIds = List.of(distributorIdParam);
		}
		CommunityItemsListQuery query =
				CommunityItemsListQuery.builder()
						.companyId(companyId)
						.operatorType("")
						.distributorId(0)
						.distributorIds(distributorIds)
						.keywords(null)
						.itemName(null)
						.itemBn(null)
						.barcode(null)
						.approveStatus("onsale")
						.auditStatus("approved")
						.brandIdRaw(null)
						.categoryRaw(null)
						.inActivityParameterPresent(false)
						.inActivity(false)
						.normalizedActivityId(null)
						.page(page)
						.pageSize(pageSize)
						.build();
		PageResult<Map<String, Object>> pr = communityItemsListService.getItemsList(query);
		return ResponseEntity.ok(
				ApiResult.ok(Map.of("total_count", pr.getTotal(), "list", pr.getList())));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static Long parseOptionalChiefIdFromClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?>)) {
			return null;
		}
		Map<String, Object> claims = (Map<String, Object>) raw;
		Object cid = claims.get("chief_id");
		if (cid instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		if (cid != null && StringUtils.hasText(cid.toString())) {
			try {
				long v = Long.parseLong(cid.toString().trim());
				return v > 0L ? v : null;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static String normalizeMemberTabStatus(String tabStatus) {
		if (!StringUtils.hasText(tabStatus)) {
			return "all";
		}
		String t = tabStatus.trim();
		return switch (t) {
			case "all", "waiting", "end", "running" -> t;
			default -> "all";
		};
	}

	private static long parseActivityPathId(String activityId) {
		if (!StringUtils.hasText(activityId)) {
			throw new BadRequestException("活动ID无效");
		}
		try {
			long id = Long.parseLong(activityId.trim());
			if (id <= 0L) {
				throw new BadRequestException("活动ID无效");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID无效");
		}
	}

	private static long longFromMapValue(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v != null && StringUtils.hasText(v.toString())) {
			try {
				return Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
