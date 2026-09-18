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

package cn.shopex.ecshopx.community.api.front.v1.chief;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.community.api.admin.v1.CommunityAdminRequestMerge;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.dto.CommunityItemsListQuery;
import cn.shopex.ecshopx.community.dto.chief.ChiefActivityZitiInput;
import cn.shopex.ecshopx.community.dto.chief.CreateChiefActivityInput;
import cn.shopex.ecshopx.community.dto.chief.UpdateChiefActivityInput;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.community.service.CommunityItemsListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("communityFrontChiefV1Activity")
@RequestMapping("/api/v1/h5app")
public class CommunityActivityController {

	private final CommunityActivityService communityActivityService;
	private final CommunityChiefDistributorMapper communityChiefDistributorMapper;
	private final CommunityChiefService communityChiefService;
	private final CommunityItemsListService communityItemsListService;

	public CommunityActivityController(
			CommunityActivityService communityActivityService,
			CommunityChiefDistributorMapper communityChiefDistributorMapper,
			CommunityChiefService communityChiefService,
			CommunityItemsListService communityItemsListService) {
		this.communityActivityService = communityActivityService;
		this.communityChiefDistributorMapper = communityChiefDistributorMapper;
		this.communityChiefService = communityChiefService;
		this.communityItemsListService = communityItemsListService;
	}

	@GetMapping(value = { "/wxapp/community/activity/lists", "/wxapp/community/chief/activity" }, name = "团购活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "activity_status", required = false) String activityStatus,
			@RequestParam(name = "order_by", required = false) String orderBy) {
		page = Math.max(1, page);
		pageSize = Math.max(1, pageSize);
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		Map<String, Object> data =
				communityActivityService.getChiefH5ActivityList(
						companyId, chiefId, activityStatus, orderBy, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/chief/activity/{activity_id}", name = "团长活动详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityDetail(
			HttpServletRequest request, @PathVariable("activity_id") String activityId) {
		long companyId = parseCompanyIdFromRequest(request);
		long viewerChiefId = resolveChiefId(request, companyId);
		long activityIdLong = parseActivityPathId(activityId);
		Map<String, Object> data = communityActivityService.getH5ActivityDetailMap(companyId, activityIdLong);
		long activityChiefId = longFromMapValue(data.get("chief_id"));
		CommunityChiefDistributor dist =
				communityChiefDistributorMapper.selectOne(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, activityChiefId)
								.last("LIMIT 1"));
		data.put(
				"distributor_id",
				dist != null && dist.getDistributorId() != null ? Math.toIntExact(dist.getDistributorId()) : 0);
		data.put("is_activity_author", Boolean.valueOf(activityChiefId == viewerChiefId));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/chief/items", name = "团长商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDisitrbutorItemList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "distributor_id", required = false) Integer distributorIdParam) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		CommunityChiefDistributor dist =
				communityChiefDistributorMapper.selectOne(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, chiefId)
								.last("LIMIT 1"));
		if (dist == null) {
			throw new ResourceException("当前团长没有配置店铺");
		}
		int distributorId =
				dist.getDistributorId() != null ? Math.toIntExact(dist.getDistributorId()) : 0;
		if (distributorIdParam != null && distributorIdParam > 0) {
			distributorId = distributorIdParam;
		}
		CommunityItemsListQuery query =
				CommunityItemsListQuery.builder()
						.companyId(companyId)
						.operatorType("")
						.distributorId(distributorId)
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
		return ResponseEntity.ok(ApiResult.ok(Map.of("total_count", pr.getTotal(), "list", pr.getList())));
	}

	@PostMapping(value = "/wxapp/community/chief/activity", name = "团长创建活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);

		String activityName = stringVal(merged.get("activity_name"));
		if (!StringUtils.hasText(activityName) || activityName.length() > 200) {
			throw new BadRequestException("活动名称不能为空且长度不能超过200");
		}

		int startTime = parseEpochSecondsRequired(merged.get("start_time"), "开始时间格式错误");
		int endTime = parseEpochSecondsRequired(merged.get("end_time"), "结束时间格式错误");

		List<Long> itemIds = parseItemIds(merged.get("items"));
		if (itemIds.isEmpty()) {
			throw new BadRequestException("请选择商品");
		}

		List<ChiefActivityZitiInput> zitiRows = parseZitiRows(merged.get("ziti"));
		if (zitiRows.isEmpty()) {
			throw new BadRequestException("请选择自提点");
		}

		String activityPics = merged.get("activity_pics") != null ? merged.get("activity_pics").toString() : null;
		String activityDesc = merged.get("activity_desc") != null ? merged.get("activity_desc").toString() : null;
		String activityIntro = merged.get("activity_intro") != null ? merged.get("activity_intro").toString() : null;
		String activityStatusRaw = stringVal(merged.get("activity_status"));
		String activityStatus = StringUtils.hasText(activityStatusRaw) ? activityStatusRaw : "public";

		CommunityChiefDistributor distRow =
				communityChiefDistributorMapper.selectOne(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, chiefId)
								.last("LIMIT 1"));
		if (distRow == null || distRow.getDistributorId() == null || distRow.getDistributorId() <= 0L) {
			throw new ResourceException("当前团长没有配置店铺");
		}
		int distributorId = Math.toIntExact(distRow.getDistributorId());

		CreateChiefActivityInput input =
				new CreateChiefActivityInput(
						companyId,
						chiefId,
						distributorId,
						activityName.trim(),
						startTime,
						endTime,
						activityPics,
						activityDesc,
						activityIntro,
						activityStatus,
						itemIds,
						zitiRows);

		Map<String, Object> data = communityActivityService.createChiefActivity(input);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/community/chief/activity/{activity_id}", name = "团长修改活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivity(
			HttpServletRequest request,
			@PathVariable("activity_id") String activityId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		long actId = parseActivityPathId(activityId);
		communityActivityService.loadActivityForChiefUpdate(companyId, chiefId, actId);

		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);

		String activityName = stringVal(merged.get("activity_name"));
		if (!StringUtils.hasText(activityName) || activityName.length() > 200) {
			throw new BadRequestException("活动名称不能为空且长度不能超过200");
		}

		int startTime = parseEpochSecondsRequired(merged.get("start_time"), "开始时间格式错误");
		int endTime = parseEpochSecondsRequired(merged.get("end_time"), "结束时间格式错误");

		List<Long> itemIds = parseItemIds(merged.get("items"));
		if (itemIds.isEmpty()) {
			throw new BadRequestException("请选择商品");
		}

		List<ChiefActivityZitiInput> zitiRows = parseZitiRows(merged.get("ziti"));
		if (zitiRows.isEmpty()) {
			throw new BadRequestException("请选择自提点");
		}

		String activityPics = merged.get("activity_pics") != null ? merged.get("activity_pics").toString() : null;
		String activityDesc = merged.get("activity_desc") != null ? merged.get("activity_desc").toString() : null;
		boolean activityIntroKeyPresent = merged.containsKey("activity_intro");
		String activityIntro =
				merged.get("activity_intro") != null ? merged.get("activity_intro").toString() : null;
		String activityStatusRaw = stringVal(merged.get("activity_status"));
		String activityStatusOrNull = StringUtils.hasText(activityStatusRaw) ? activityStatusRaw : null;

		Integer distributorIdFromBody = parseOptionalDistributorIdFromMerged(merged);

		UpdateChiefActivityInput input =
				new UpdateChiefActivityInput(
						actId,
						companyId,
						chiefId,
						activityName.trim(),
						startTime,
						endTime,
						activityPics,
						activityDesc,
						activityIntroKeyPresent,
						activityIntro,
						activityStatusOrNull,
						distributorIdFromBody,
						itemIds,
						zitiRows);

		Map<String, Object> data = communityActivityService.updateChiefActivity(input);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/community/chief/activity_status/{activity_id}", name = "团长修改活动状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivityStatus(
			HttpServletRequest request,
			@PathVariable("activity_id") String activityId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		long actId = parseActivityPathId(activityId);
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		String activityStatusRaw = stringVal(merged.get("activity_status"));
		if (!StringUtils.hasText(activityStatusRaw)) {
			throw new BadRequestException("活动状态错误");
		}
		Map<String, Object> data =
				communityActivityService.updateChiefActivityStatus(companyId, chiefId, actId, activityStatusRaw, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/community/chief/confirm_delivery/{activity_id}", name = "团长确认收货")
	public ResponseEntity<ApiResult<Map<String, Object>>> confirmDeliveryStatus(
			HttpServletRequest request, @PathVariable("activity_id") String activityId) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		long actId = parseActivityPathId(activityId);
		Map<String, Object> data = communityActivityService.chiefConfirmDelivery(companyId, chiefId, actId);
		return ResponseEntity.ok(ApiResult.ok(data));
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
	private long resolveChiefId(HttpServletRequest request, long companyId) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			throw new ForbiddenException("只有团长才能操作");
		}
		return communityChiefService.resolveChiefIdForH5(companyId, (Map<String, Object>) m);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static int parseEpochSecondsRequired(Object raw, String badMessage) {
		Long sec = DateExpressionParser.parseToEpochSecond(raw, ZoneId.systemDefault());
		if (sec == null) {
			throw new BadRequestException(badMessage);
		}
		if (sec < Integer.MIN_VALUE || sec > Integer.MAX_VALUE) {
			throw new BadRequestException(badMessage);
		}
		return sec.intValue();
	}

	private static List<Long> parseItemIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (raw instanceof List<?> list) {
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					long id = toLongFlexible(m.get("item_id"));
					if (id > 0) {
						out.add(id);
					}
				} else {
					long id = toLongFlexible(el);
					if (id > 0) {
						out.add(id);
					}
				}
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			for (String part : s.split(",")) {
				String t = part.trim();
				if (!StringUtils.hasText(t)) {
					continue;
				}
				try {
					long id = Long.parseLong(t);
					if (id > 0) {
						out.add(id);
					}
				} catch (NumberFormatException ignored) {
					/* skip invalid token */
				}
			}
			return new ArrayList<>(out);
		}
		return List.of();
	}

	private static List<ChiefActivityZitiInput> parseZitiRows(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Number n) {
			long zid = n.longValue();
			return zid > 0 ? List.of(new ChiefActivityZitiInput(zid, 0, "")) : List.of();
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				long zid = Long.parseLong(s.trim());
				return zid > 0 ? List.of(new ChiefActivityZitiInput(zid, 0, "")) : List.of();
			} catch (NumberFormatException e) {
				return List.of();
			}
		}
		if (raw instanceof List<?> list) {
			List<ChiefActivityZitiInput> out = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					long zid = toLongFlexible(m.get("ziti_id"));
					if (zid <= 0) {
						continue;
					}
					Integer cond = null;
					Object c = m.get("condition_num");
					if (c instanceof Number num) {
						cond = num.intValue();
					} else if (c != null && StringUtils.hasText(c.toString())) {
						try {
							cond = Integer.parseInt(c.toString().trim());
						} catch (NumberFormatException ignored) {
							cond = 0;
						}
					}
					String remark = m.get("remark") != null ? m.get("remark").toString() : "";
					out.add(new ChiefActivityZitiInput(zid, cond, remark));
				}
			}
			return out;
		}
		return List.of();
	}

	private static long toLongFlexible(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer parseOptionalDistributorIdFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("distributor_id") || merged.get("distributor_id") == null) {
			return null;
		}
		Object v = merged.get("distributor_id");
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("店铺ID格式错误");
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("店铺ID格式错误");
		}
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
