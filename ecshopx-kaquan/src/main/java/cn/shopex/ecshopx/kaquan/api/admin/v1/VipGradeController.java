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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCreateRequestMergeService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBatchActiveDelayService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeCreateService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeGradeInfoParser;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderListQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderReceiveService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserUseListService;
import cn.shopex.ecshopx.kaquan.web.VipGradeOrderDatapassSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("kaquanVipGradeAdminV1")
@RequestMapping("/api/v1")
public class VipGradeController {

	private final DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService;
	private final VipGradeCreateService vipGradeCreateService;
	private final VipGradeOrderReceiveService vipGradeOrderReceiveService;
	private final VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final VipGradeOrderListQueryService vipGradeOrderListQueryService;
	private final VipGradeUserUseListService vipGradeUserUseListService;
	private final ObjectMapper objectMapper;

	public VipGradeController(DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService,
			VipGradeCreateService vipGradeCreateService,
			VipGradeOrderReceiveService vipGradeOrderReceiveService,
			VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService,
			VipGradeListQueryService vipGradeListQueryService,
			VipGradeOrderListQueryService vipGradeOrderListQueryService,
			VipGradeUserUseListService vipGradeUserUseListService,
			ObjectMapper objectMapper) {
		this.discountCardCreateRequestMergeService = discountCardCreateRequestMergeService;
		this.vipGradeCreateService = vipGradeCreateService;
		this.vipGradeOrderReceiveService = vipGradeOrderReceiveService;
		this.vipGradeBatchActiveDelayService = vipGradeBatchActiveDelayService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.vipGradeOrderListQueryService = vipGradeOrderListQueryService;
		this.vipGradeUserUseListService = vipGradeUserUseListService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "membercard.vipgrade.add")
	@PutMapping(value = "/membercard/vipgrade", name = "保存付费会员等级卡")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> addDataVipGrade(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@Deprecated
			@RequestParam(name = "card_id", required = false) String ignoredCardId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		if (!merged.containsKey("grade_info") || merged.get("grade_info") == null) {
			throw new BadRequestException("grade_info 无效");
		}
		List<Map<String, Object>> gradeInfoList =
				VipGradeGradeInfoParser.parseGradeInfoList(merged.get("grade_info"), objectMapper);
		vipGradeCreateService.createVipGrades(companyId, gradeInfoList);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "membercard.vipgrade.list")
	@GetMapping(value = "/membercard/vipgrade", name = "获取付费会员等级卡列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> listDataVipGrade(HttpServletRequest request,
			@RequestParam(name = "is_disabled", required = false) String isDisabled) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		boolean filterOnlyNonDisabled = isDisabled != null && "false".equals(isDisabled);
		List<Map<String, Object>> list = vipGradeListQueryService.listDataVipGrade(companyId, filterOnlyNonDisabled);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@DataPass
	@Activated(routeAlias = "vipgrade.order.list")
	@GetMapping(value = "/vipgrade/order", name = "获取会员卡购买记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataVipGradeOrder(HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "user_id", required = false) String userIdRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		int pageNum = parsePageScalarLoose(pageRaw, 1);
		int pageSizeNum = parsePageScalarLoose(pageSizeRaw, 100);
		Long userIdFilter = null;
		if (StringUtils.hasText(userIdRaw)) {
			try {
				userIdFilter = Long.parseLong(userIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("user_id 无效");
			}
		}
		Map<String, Object> result = new LinkedHashMap<>(
				vipGradeOrderListQueryService.listDoneOrders(companyId, userIdFilter, pageNum, pageSizeNum));
		Object datapassBlock = VipGradeOrderDatapassSupport.resolveXDatapassBlock(request);
		result.put("datapass_block", datapassBlock);
		if (VipGradeOrderDatapassSupport.isTruthyForMasking(datapassBlock)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("list");
			if (rows != null && !rows.isEmpty()) {
				for (Map<String, Object> row : rows) {
					Object m = row.get("mobile");
					if (m != null) {
						row.put("mobile", DataMasking.maskMobile(m.toString()));
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static int parsePageScalarLoose(String raw, int defaultWhenParamAbsent) {
		if (raw == null) {
			return defaultWhenParamAbsent;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			double d = Double.parseDouble(s);
			if (!Double.isFinite(d)) {
				return 0;
			}
			return (int) d;
		} catch (NumberFormatException e) {
			long v = LeadingNumberParser.parseAsLong(s);
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		}
	}

	@Activated(routeAlias = "vipgrade.use.list")
	@GetMapping(value = "/vipgrades/uselist", name = "获取指定用户所有的付费会员等级到期时间")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAllUserVipGrade(HttpServletRequest request,
			@RequestParam(name = "user_id", required = false) String userIdRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Long userId = parseUserIdForUseList(userIdRaw);
		Map<String, Object> data = vipGradeUserUseListService.buildUseList(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * uselist 查询参数：空白为 null（关系条件 IS NULL）；纯非负数字串为 Long；否则为 0L。
	 */
	private static Long parseUserIdForUseList(String userIdRaw) {
		if (userIdRaw == null) {
			return null;
		}
		String t = userIdRaw.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (t.matches("^[0-9]+$")) {
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	@Activated(routeAlias = "vipgrade.use.active.delay")
	@PutMapping(value = "/vipgrades/active_delay", name = "主动延期付费会员")
	public ResponseEntity<ApiResult<Map<String, Object>>> receiveMemberCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		long userId = parseUserId(merged.get("user_id"));
		String mobileRaw = merged.get("mobile") != null ? merged.get("mobile").toString() : "";
		String vipGradeAddDayJson = stringifyVipGradeAddDay(merged.get("vipGradeAddDay"));
		vipGradeOrderReceiveService.receiveMemberCard(companyId, userId, mobileRaw, vipGradeAddDayJson);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private long parseUserId(Object userIdRaw) {
		if (userIdRaw == null) {
			throw new BadRequestException("user_id 无效");
		}
		if (userIdRaw instanceof Number) {
			return ((Number) userIdRaw).longValue();
		}
		if (userIdRaw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("user_id 无效");
			}
		}
		try {
			return Long.parseLong(userIdRaw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("user_id 无效");
		}
	}

	private String stringifyVipGradeAddDay(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("vipGradeAddDay 格式错误");
		}
	}

	@Activated(routeAlias = "vipgrade.use.batch.active.delay")
	@PutMapping(value = "/vipgrades/batch_active_delay", name = "批量主动延期付费会员")
	public ResponseEntity<ApiResult<Map<String, Object>>> batchReceiveMemberCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		long vipGradeId = parseVipGradeId(merged.get("vip_grade_id"));
		int addDay = parseAddDay(merged.get("add_day"));
		String filter = merged.get("filter") == null ? "" : merged.get("filter").toString();
		String usersJson = null;
		if ("users".equals(filter)) {
			Object usersRaw = merged.get("users");
			if (usersRaw != null) {
				if (usersRaw instanceof String s) {
					usersJson = s;
				} else {
					try {
						usersJson = objectMapper.writeValueAsString(usersRaw);
					} catch (JsonProcessingException e) {
						throw new BadRequestException("users 格式错误");
					}
				}
			}
		}
		vipGradeBatchActiveDelayService.processBatchActiveDelay(companyId, vipGradeId, addDay, filter, usersJson);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private long parseVipGradeId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("vip_grade_id 无效");
		}
		if (raw instanceof Number) {
			return ((Number) raw).longValue();
		}
		if (raw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("vip_grade_id 无效");
			}
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("vip_grade_id 无效");
		}
	}

	private int parseAddDay(Object raw) {
		if (raw == null) {
			throw new BadRequestException("延期天数无效");
		}
		int day;
		if (raw instanceof Number n) {
			day = n.intValue();
		} else {
			try {
				day = Integer.parseInt(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("延期天数无效");
			}
		}
		if (day <= 0) {
			throw new BadRequestException("延期天数无效");
		}
		return day;
	}
}
