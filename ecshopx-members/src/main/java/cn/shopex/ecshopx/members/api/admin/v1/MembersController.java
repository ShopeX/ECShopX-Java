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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.admin.AdminBindUserSalespersonRelService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberBatchUpdateGradeService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberCreateMemberService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberGetMemberInfoService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberGetMemberListService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberMembersInfoUpdateService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberOperateLogListService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberReChangeRegDistributorService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRegisterSettingService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberSmsCodeSendService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberSetMemberSalesmanService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberUpdateGradeByIdService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberUpdateMemberInfoService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberUpdateMobileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("membersAdminV1Members")
@RequestMapping("/api/v1")
public class MembersController {

	private static final Logger log = LoggerFactory.getLogger(MembersController.class);

	/**
	 * Sentinel used when {@code distributor_id} is supplied but cannot be parsed as a base-10 {@code long}
	 * (non-numeric text, fractional number, or overflow). The request is not rejected as a client parameter
	 * format error; the value is passed through so an equality condition on distributor id matches no row,
	 * producing the same business outcome as when no qualifying data exists for update. This value must not
	 * correspond to a real distributor identifier in persisted data.
	 */
	private static final long NON_NUMERIC_DISTRIBUTOR_ID_QUERY_PLACEHOLDER = Long.MIN_VALUE;

	private final AdminMemberCreateMemberService adminMemberCreateMemberService;
	private final AdminMemberUpdateMemberInfoService adminMemberUpdateMemberInfoService;
	private final AdminBindUserSalespersonRelService adminBindUserSalespersonRelService;
	private final AdminMemberBatchUpdateGradeService adminMemberBatchUpdateGradeService;
	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;
	private final AdminMemberSmsCodeSendService adminMemberSmsCodeSendService;
	private final AdminMemberReChangeRegDistributorService adminMemberReChangeRegDistributorService;
	private final AdminMemberUpdateMobileService adminMemberUpdateMobileService;
	private final AdminMemberUpdateGradeByIdService adminMemberUpdateGradeByIdService;
	private final AdminMemberSetMemberSalesmanService adminMemberSetMemberSalesmanService;
	private final AdminMemberMembersInfoUpdateService adminMemberMembersInfoUpdateService;
	private final AdminMemberGetMemberInfoService adminMemberGetMemberInfoService;
	private final AdminMemberGetMemberListService adminMemberGetMemberListService;
	private final AdminMemberOperateLogListService adminMemberOperateLogListService;
	private final ObjectMapper objectMapper;

	public MembersController(
			AdminMemberCreateMemberService adminMemberCreateMemberService,
			AdminMemberUpdateMemberInfoService adminMemberUpdateMemberInfoService,
			AdminBindUserSalespersonRelService adminBindUserSalespersonRelService,
			AdminMemberBatchUpdateGradeService adminMemberBatchUpdateGradeService,
			AdminMemberRegisterSettingService adminMemberRegisterSettingService,
			AdminMemberSmsCodeSendService adminMemberSmsCodeSendService,
			AdminMemberReChangeRegDistributorService adminMemberReChangeRegDistributorService,
			AdminMemberUpdateMobileService adminMemberUpdateMobileService,
			AdminMemberUpdateGradeByIdService adminMemberUpdateGradeByIdService,
			AdminMemberSetMemberSalesmanService adminMemberSetMemberSalesmanService,
			AdminMemberMembersInfoUpdateService adminMemberMembersInfoUpdateService,
			AdminMemberGetMemberInfoService adminMemberGetMemberInfoService,
			AdminMemberGetMemberListService adminMemberGetMemberListService,
			AdminMemberOperateLogListService adminMemberOperateLogListService,
			ObjectMapper objectMapper) {
		this.adminMemberCreateMemberService = adminMemberCreateMemberService;
		this.adminMemberUpdateMemberInfoService = adminMemberUpdateMemberInfoService;
		this.adminBindUserSalespersonRelService = adminBindUserSalespersonRelService;
		this.adminMemberBatchUpdateGradeService = adminMemberBatchUpdateGradeService;
		this.adminMemberRegisterSettingService = adminMemberRegisterSettingService;
		this.adminMemberSmsCodeSendService = adminMemberSmsCodeSendService;
		this.adminMemberReChangeRegDistributorService = adminMemberReChangeRegDistributorService;
		this.adminMemberUpdateMobileService = adminMemberUpdateMobileService;
		this.adminMemberUpdateGradeByIdService = adminMemberUpdateGradeByIdService;
		this.adminMemberSetMemberSalesmanService = adminMemberSetMemberSalesmanService;
		this.adminMemberMembersInfoUpdateService = adminMemberMembersInfoUpdateService;
		this.adminMemberGetMemberInfoService = adminMemberGetMemberInfoService;
		this.adminMemberGetMemberListService = adminMemberGetMemberListService;
		this.adminMemberOperateLogListService = adminMemberOperateLogListService;
		this.objectMapper = objectMapper;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.register.set")
	@PostMapping(
			value = "/members/register/setting",
			name = "设置会员注册项",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setMemberRegItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		adminMemberRegisterSettingService.setMemberRegItems(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.register.get")
	@GetMapping(
			value = "/members/register/setting",
			name = "获取会员注册项",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getMemberRegItems(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> payload = adminMemberRegisterSettingService.getMemberRegItems(companyId, request);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "member.list")
	@GetMapping(value = "/members", name = "会员列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getMemberList(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Object payload = adminMemberGetMemberListService.getMemberList(companyId, merged, request);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "member.info")
	@GetMapping(value = "/member", name = "会员信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getMemberInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Object payload = adminMemberGetMemberInfoService.getMemberInfo(companyId, merged, request);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/member/sms/code", name = "短信验证码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsCode(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		String mobile = request.getParameter("mobile");
		String type = request.getParameter("type");
		String token = request.getParameter("token");
		String yzm = request.getParameter("yzm");
		adminMemberSmsCodeSendService.getSmsCode(companyId, mobile, type, token, yzm);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/member/image/code", name = "图片验证码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, String>>> getImageVcode(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		String raw = request.getParameter("type");
		String type = raw == null ? "sign" : raw;
		Map<String, String> data = adminMemberRegisterSettingService.generateImageVcode(companyId, type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.create")
	@PostMapping(value = "/member", name = "新增会员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		return ResponseEntity.ok(ApiResult.ok(adminMemberCreateMemberService.createMember(companyId, merged)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.update")
	@PatchMapping(value = "/member", name = "更新会员信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMemberInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		if (!merged.containsKey("user_id")) {
			throw new BadRequestException("缺少会员id");
		}
		Object userIdObj = merged.get("user_id");
		if (userIdObj == null) {
			throw new BadRequestException("缺少会员id");
		}
		String userIdRaw = String.valueOf(userIdObj).trim();
		if (userIdRaw.isEmpty()) {
			throw new BadRequestException("缺少会员id");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("会员id格式错误");
		}
		if (userId <= 0L) {
			throw new BadRequestException("会员id格式错误");
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		if (merged.containsKey("disabled")) {
			payload.put("disabled", merged.get("disabled"));
		}
		if (merged.containsKey("remarks")) {
			payload.put("remarks", merged.get("remarks"));
		}
		if (merged.containsKey("name")) {
			payload.put("name", merged.get("name"));
		}
		Map<String, Object> data =
				adminMemberUpdateMemberInfoService.updateMemberInfo(companyId, userId, payload);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.upate.mobile")
	@PutMapping(value = "/member", name = "更新会员手机", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMobileById(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> data =
				adminMemberUpdateMobileService.updateMobileById(companyId, map, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.upate.salesman")
	@PutMapping(value = "/member/salesman", name = "设置导购员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setMemberSalesman(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		String distParam = request.getParameter("distributor_id");
		Long distributorId;
		if (distParam == null || !StringUtils.hasText(distParam.trim())) {
			distributorId = null;
		} else {
			try {
				distributorId = Long.parseLong(distParam.trim());
			} catch (NumberFormatException e) {
				distributorId = NON_NUMERIC_DISTRIBUTOR_ID_QUERY_PLACEHOLDER;
			}
		}
		adminMemberSetMemberSalesmanService.setMemberSalesman(companyId, distributorId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.upate.grade_id")
	@PutMapping(value = "/member/grade", name = "更新等级", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateGradeById(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> data =
				adminMemberUpdateGradeByIdService.updateGradeById(companyId, jwtMap, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.upate.grade_ids")
	@PatchMapping(value = "/member/grade", name = "批量更新等级", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateGrade(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> echo = adminMemberBatchUpdateGradeService.updateGrade(companyId, jwtMap, merged);
		return ResponseEntity.ok(ApiResult.ok(echo));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.update.reg_distributor")
	@PutMapping(value = "/member/regDistributor", name = "批量更新注册分销商", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> reChangeRegDistributor(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		return ResponseEntity.ok(ApiResult.ok(adminMemberReChangeRegDistributorService.reChangeRegDistributor(companyId, merged)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.operate.logs")
	@GetMapping(
			value = "/operate/loglist",
			name = "会员操作日志",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> gerMemberOperateLogList(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userId) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> payload = adminMemberOperateLogListService.gerMemberOperateLogList(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.bindusersalesperson.update")
	@PostMapping(
			value = "/member/bindusersalespersonrel",
			name = "绑定导购关系",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> bindUserSalespersonRel(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		merged.put("company_id", companyId);
		if (merged.get("company_id") == null) {
			throw new BadRequestException("企业id必填");
		}
		Object usersObj = merged.get("users");
		if (usersObj == null || !StringUtils.hasText(String.valueOf(usersObj).trim())) {
			throw new BadRequestException("用户必选");
		}
		Object spRequired = merged.get("salesperson_id");
		if (spRequired == null || !StringUtils.hasText(String.valueOf(spRequired).trim())) {
			throw new BadRequestException("导购员必选");
		}
		String usersRaw = String.valueOf(merged.get("users")).trim();
		JsonNode root;
		try {
			root = objectMapper.readTree(usersRaw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("用户id格式错误");
		}
		if (!root.isArray()) {
			throw new BadRequestException("用户id格式错误");
		}
		List<Long> userIds = new ArrayList<>();
		for (JsonNode elem : root) {
			if (elem.isIntegralNumber()) {
				userIds.add(elem.longValue());
			} else if (elem.isTextual()) {
				try {
					userIds.add(Long.parseLong(elem.asText().trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("用户id格式错误");
				}
			} else {
				throw new BadRequestException("用户id格式错误");
			}
		}
		Object sp = merged.get("salesperson_id");
		long salespersonId;
		try {
			salespersonId = Long.parseLong(String.valueOf(sp).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购员id格式错误");
		}
		Long distributorId = null;
		String q = request.getParameter("distributor_id");
		if (StringUtils.hasText(q) && unsetOrEmptyMergedParam(merged.get("distributor_id"))) {
			try {
				distributorId = Long.parseLong(q.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("分销商id格式错误");
			}
		}
		Map<String, Object> data =
				adminBindUserSalespersonRelService.bindUserSalespersonRel(
						companyId, userIds, salespersonId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Key is present and not JSON null (client omitted vs explicit null).
	 */
	private static boolean issetLikeNonNullValue(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return false;
		}
		Object v = merged.get(key);
		if (v == null) {
			return false;
		}
		if (v instanceof JsonNode j) {
			return !j.isNull();
		}
		return true;
	}

	private static boolean unsetOrEmptyMergedParam(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			return "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		return Boolean.FALSE.equals(v);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.update")
	@PutMapping(
			value = "/member/update",
			name = "更新会员基础信息",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		log.info("updateMember");
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		if (!merged.containsKey("user_id")
				|| merged.get("user_id") == null
				|| String.valueOf(merged.get("user_id")).trim().isEmpty()) {
			throw new ResourceException("用户ID必传");
		}
		String userIdTrimmed = String.valueOf(merged.get("user_id")).trim();
		long userId;
		try {
			userId = Long.parseLong(userIdTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("用户ID格式错误");
		}

		LinkedHashMap<String, Object> postdata = new LinkedHashMap<>();
		if (merged.containsKey("username") && ValuePresence.hasEffectiveValue(merged.get("username"))) {
			postdata.put("username", String.valueOf(merged.get("username")).trim());
		}
		if (merged.containsKey("sex")) {
			postdata.put("sex", normalizeSexValue(merged.get("sex")));
		}
		if (merged.containsKey("birthday") && ValuePresence.hasEffectiveValue(merged.get("birthday"))) {
			String trimmed = String.valueOf(merged.get("birthday")).trim();
			LocalDate localDate = parseBirthdayToLocalDate(trimmed);
			postdata.put("birthday", DateTimeFormatter.ISO_LOCAL_DATE.format(localDate));
		}
		if (issetLikeNonNullValue(merged, "address")) {
			postdata.put("address", String.valueOf(merged.get("address")));
		}
		if (issetLikeNonNullValue(merged, "email")) {
			postdata.put("email", String.valueOf(merged.get("email")));
		}
		if (issetLikeNonNullValue(merged, "industry")) {
			postdata.put("industry", String.valueOf(merged.get("industry")));
		}
		if (issetLikeNonNullValue(merged, "income")) {
			postdata.put("income", String.valueOf(merged.get("income")));
		}
		if (issetLikeNonNullValue(merged, "edu_background")) {
			postdata.put("edu_background", String.valueOf(merged.get("edu_background")));
		}
		if (merged.containsKey("habbit")) {
			List<String> selectedNames = parseHabbitSelectedNames(merged.get("habbit"));
			List<Map<String, Object>> habbitRows =
					buildHabbitRowsForPostdata(companyId, selectedNames);
			postdata.put("habbit", habbitRows);
		}
		if (merged.containsKey("other_params")) {
			postdata.put("other_params", merged.get("other_params"));
		}

		if (postdata.isEmpty()) {
			throw new ResourceException("请填写数据!");
		}
		log.info("{}", postdata);
		Map<String, Object> data =
				adminMemberMembersInfoUpdateService.updateMember(companyId, userId, postdata);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private List<String> parseHabbitSelectedNames(Object raw) {
		if (raw instanceof Collection<?> c) {
			List<String> out = new ArrayList<>(c.size());
			for (Object o : c) {
				out.add(String.valueOf(o).trim());
			}
			return out;
		}
		if (raw instanceof String s) {
			try {
				JsonNode n = objectMapper.readTree(s);
				if (!n.isArray()) {
					throw new ResourceException("爱好参数格式错误");
				}
				List<String> out = new ArrayList<>();
				for (JsonNode elem : n) {
					out.add(elem.asText("").trim());
				}
				return out;
			} catch (JsonProcessingException e) {
				throw new ResourceException("爱好参数格式错误");
			}
		}
		throw new ResourceException("爱好参数格式错误");
	}

	private List<Map<String, Object>> buildHabbitRowsForPostdata(long companyId, List<String> selectedNames) {
		Optional<Map<String, Object>> rootOpt =
				adminMemberRegisterSettingService.readMemberRegSettingRoot(companyId);
		List<LinkedHashMap<String, Object>> templateRows = new ArrayList<>();
		if (rootOpt.isPresent()) {
			templateRows.addAll(extractHabbitTemplateRows(rootOpt.get()));
		}
		List<Map<String, Object>> outList = new ArrayList<>();
		for (LinkedHashMap<String, Object> templateRow : templateRows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			Object nameObj = templateRow.get("name");
			String name = nameObj == null ? "" : String.valueOf(nameObj).trim();
			row.put("name", name);
			boolean checked = selectedNames.contains(name);
			row.put("ischecked", checked ? "true" : "false");
			outList.add(row);
		}
		return outList;
	}

	private static List<LinkedHashMap<String, Object>> extractHabbitTemplateRows(Map<String, Object> root) {
		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		Object settingObj = root.get("setting");
		if (!(settingObj instanceof Map<?, ?> setting)) {
			return rows;
		}
		Object habbitObj = setting.get("habbit");
		if (!(habbitObj instanceof Map<?, ?> habbitMap)) {
			return rows;
		}
		Object itemsObj = habbitMap.get("items");
		if (itemsObj instanceof List<?> list) {
			for (Object elem : list) {
				if (elem instanceof Map<?, ?> m) {
					rows.add(copyStringKeyMap(m));
				}
			}
		} else if (itemsObj instanceof Map<?, ?> m) {
			for (Object elem : m.values()) {
				if (elem instanceof Map<?, ?> im) {
					rows.add(copyStringKeyMap(im));
				}
			}
		}
		return rows;
	}

	private static LinkedHashMap<String, Object> copyStringKeyMap(Map<?, ?> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static Integer normalizeSexValue(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static LocalDate parseBirthdayToLocalDate(String trimmed) {
		try {
			return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException ignored) {
		}
		try {
			return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
		} catch (DateTimeParseException ignored) {
		}
		try {
			return ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDate();
		} catch (DateTimeParseException ignored) {
		}
		throw new ResourceException("生日格式错误");
	}
}
