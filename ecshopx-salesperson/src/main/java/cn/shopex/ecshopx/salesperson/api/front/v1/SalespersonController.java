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

package cn.shopex.ecshopx.salesperson.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.annotation.QywxSalespersonAuth;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.BindUserSalespersonRelationshipService;
import cn.shopex.ecshopx.salesperson.service.MemberH5SalespersonInfoService;
import cn.shopex.ecshopx.salesperson.service.SalespersonBaInfoUpdateService;
import cn.shopex.ecshopx.salesperson.service.SalespersonSigninQrcodeService;
import cn.shopex.ecshopx.salesperson.service.SalespersonSigninValidService;
import cn.shopex.ecshopx.salesperson.service.SalespersonComplaintsDetailService;
import cn.shopex.ecshopx.salesperson.service.SalespersonComplaintsListService;
import cn.shopex.ecshopx.salesperson.service.SendSalespersonComplaintsService;
import cn.shopex.ecshopx.salesperson.service.UserSalespersonRelationshipReadService;
import cn.shopex.ecshopx.salesperson.service.dto.BindUserSalespersonRelationshipOutcome;
import cn.shopex.ecshopx.salesperson.web.ShopSalespersonFrontRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
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

@RestController("salespersonBaFrontV1")
@RequestMapping("/api/v1/h5app")
public class SalespersonController {

	private final BindUserSalespersonRelationshipService bindUserSalespersonRelationshipService;

	private final SalespersonBaInfoUpdateService salespersonBaInfoUpdateService;

	private final SendSalespersonComplaintsService sendSalespersonComplaintsService;

	private final SalespersonSigninValidService salespersonSigninValidService;

	private final SalespersonSigninQrcodeService salespersonSigninQrcodeService;

	private final MemberH5SalespersonInfoService memberH5SalespersonInfoService;

	private final SalespersonComplaintsDetailService salespersonComplaintsDetailService;

	private final SalespersonComplaintsListService salespersonComplaintsListService;

	private final UserSalespersonRelationshipReadService userSalespersonRelationshipReadService;

	public SalespersonController(
			BindUserSalespersonRelationshipService bindUserSalespersonRelationshipService,
			SalespersonBaInfoUpdateService salespersonBaInfoUpdateService,
			SendSalespersonComplaintsService sendSalespersonComplaintsService,
			SalespersonSigninValidService salespersonSigninValidService,
			SalespersonSigninQrcodeService salespersonSigninQrcodeService,
			MemberH5SalespersonInfoService memberH5SalespersonInfoService,
			SalespersonComplaintsDetailService salespersonComplaintsDetailService,
			SalespersonComplaintsListService salespersonComplaintsListService,
			UserSalespersonRelationshipReadService userSalespersonRelationshipReadService) {
		this.bindUserSalespersonRelationshipService = bindUserSalespersonRelationshipService;
		this.salespersonBaInfoUpdateService = salespersonBaInfoUpdateService;
		this.sendSalespersonComplaintsService = sendSalespersonComplaintsService;
		this.salespersonSigninValidService = salespersonSigninValidService;
		this.salespersonSigninQrcodeService = salespersonSigninQrcodeService;
		this.memberH5SalespersonInfoService = memberH5SalespersonInfoService;
		this.salespersonComplaintsDetailService = salespersonComplaintsDetailService;
		this.salespersonComplaintsListService = salespersonComplaintsListService;
		this.userSalespersonRelationshipReadService = userSalespersonRelationshipReadService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@QywxSalespersonAuth
	@PostMapping(value = "/wxapp/salesperson/bainfo", name = "更新导购敏感信息")
	public ResponseEntity<?> updateBaInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long salespersonId = parseQywxSalespersonIdFromRequest(request);
		Map<String, Object> data = salespersonBaInfoUpdateService.updateBaInfo(salespersonId, merged);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@GetMapping(value = "/wxapp/salesperson", name = "获取导购员信息")
	public ResponseEntity<Map<String, Object>> getSalespersonInfo(HttpServletRequest request) {
		long userId = parseMemberUserIdFromRequest(request);
		Map<String, Object> data = memberH5SalespersonInfoService.getSalespersonInfo(userId);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@PostMapping(value = "/wxapp/salesperson/complaints", name = "提交投诉导购员")
	public ResponseEntity<Map<String, Object>> sendSalespersonComplaints(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object rawContent = merged.get("complaints_content");
		String content = rawContent == null ? "" : String.valueOf(rawContent).trim();
		Object rawImages = merged.get("complaints_images");
		String images = rawImages == null ? "" : String.valueOf(rawImages).trim();

		long userId = parseMemberUserIdFromRequest(request);
		long companyId = parseCompanyIdFromRequest(request);

		Object claimsRaw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(claimsRaw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		String username = firstNonBlankText(claims.get("username"), claims.get("user_name"), claims.get("name"));
		String mobile = firstNonBlankText(claims.get("mobile"), claims.get("phone"), claims.get("cellphone"));
		if (username == null) {
			username = "";
		}
		if (mobile == null) {
			mobile = "";
		}

		if (content.codePointCount(0, content.length()) > 255) {
			throw new BadRequestException("投诉内容不能超过255个字符");
		}
		if (!StringUtils.hasText(content)) {
			return dingoValidation422("投诉内容不能为空");
		}

		Map<String, Object> row = sendSalespersonComplaintsService.sendSalespersonComplaints(
				userId, companyId, username, mobile, content, images);
		return ResponseEntity.ok(Map.of("data", row));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@GetMapping(value = "/wxapp/salesperson/complaintsList", name = "已投诉导购员列表")
	public ResponseEntity<Map<String, Object>> getSalespersonComplaintsList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSize) {
		long userId = parseMemberUserIdFromRequest(request);
		long companyId = parseCompanyIdFromRequest(request);
		if (userId <= 0L || userId > (long) Integer.MAX_VALUE) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (companyId <= 0L || companyId > (long) Integer.MAX_VALUE) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int pageVal = parseComplaintsListPageParam(page);
		int pageSizeVal = parseComplaintsListPageSizeParam(pageSize);
		Map<String, Object> data =
				salespersonComplaintsListService.getSalespersonComplaintsList(userId, companyId, pageVal, pageSizeVal);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@GetMapping(value = "/wxapp/salesperson/complaintsDetail/{id}", name = "投诉详情")
	public ResponseEntity<?> getSalespersonComplaintsDetail(
			HttpServletRequest request, @PathVariable("id") String id) {
		long userId = parseMemberUserIdFromRequest(request);
		long companyId = parseCompanyIdFromRequest(request);
		if (userId <= 0L || userId > (long) Integer.MAX_VALUE) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (companyId <= 0L || companyId > (long) Integer.MAX_VALUE) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (isComplaintDetailPathIdAbsent(id)) {
			return ResponseEntity.ok(Map.of("data", List.of()));
		}
		String t = id.trim();
		String digits = LeadingNumberParser.parseAsString(t);
		int complaintId;
		try {
			complaintId = Integer.parseInt(digits);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		Map<String, Object> row =
				salespersonComplaintsDetailService.getSalespersonComplaintsDetail(userId, companyId, complaintId);
		if (row == null) {
			return ResponseEntity.ok(Map.of("data", List.of()));
		}
		return ResponseEntity.ok(Map.of("data", row));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@GetMapping(value = "/wxapp/usersalespersonrel", name = "用户与导购员关系")
	public ResponseEntity<Map<String, Object>> userSalespersonRelationship(
			HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false, defaultValue = "0") String salespersonIdRaw) {
		long userId = parseMemberUserIdFromRequest(request);
		long companyId = parseCompanyIdFromRequest(request);
		long salespersonId = parseSalespersonIdLoose(salespersonIdRaw);
		Map<String, Object> data =
				userSalespersonRelationshipReadService.userSalespersonRelationship(userId, companyId, salespersonId);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@PostMapping(value = "/wxapp/usersalespersonrel", name = "绑定用户导购关系")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<?> bindUserSalespersonRelationship(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long userId = parseMemberUserIdFromRequest(request);
		long companyId = parseCompanyIdFromRequest(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long salespersonId = parseSalespersonIdLoose(merged.get("salesperson_id"));
		String unionid = resolveUnionIdFromClaimsOnly(request);
		BindUserSalespersonRelationshipOutcome out =
				bindUserSalespersonRelationshipService.bindUserSalespersonRelationship(
						userId, companyId, salespersonId, unionid);
		if (out.shortCircuit()) {
			return ResponseEntity.ok(Map.of("data", Map.of("success", false)));
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("success", out.successInData());
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/salesperson/nologin", name = "导购员信息免登")
	public ResponseEntity<Map<String, Object>> getSalespersonInfoNologin(
			@RequestParam(value = "salesperson_id", required = false) String salespersonIdRaw) {
		if (!ValuePresence.hasEffectiveValue(salespersonIdRaw)) {
			return dingoValidation422("请输入导购员id");
		}
		long sid = LeadingNumberParser.parseAsLong(String.valueOf(salespersonIdRaw).trim());
		if (sid <= 0L) {
			return dingoValidation422("请输入导购员id");
		}
		Map<String, Object> data = memberH5SalespersonInfoService.getSalespersonInfoNologin(sid);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/salesperson/signinQrcode", name = "门店签到码")
	public ResponseEntity<Map<String, Object>> getSigninQrcode(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "salesperson_id", required = false) String salespersonId) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> data =
				salespersonSigninQrcodeService.getSigninQrcode(companyId, type, distributorId, salespersonId);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/salesperson/signinValid", name = "签到校验")
	public ResponseEntity<Map<String, Object>> validSignin(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object tokenRaw = merged.get("token");
		if (tokenRaw == null || !StringUtils.hasText(String.valueOf(tokenRaw).trim())) {
			Map<String, Object> errData = new LinkedHashMap<>();
			errData.put("message", "参数错误.");
			errData.put("errors", Map.of("token", List.of("validation.required")));
			errData.put("status_code", 422);
			return ResponseEntity.ok(Map.of("data", errData));
		}
		long companyId = parseCompanyIdFromRequest(request);
		String token = String.valueOf(tokenRaw).trim();
		Map<String, Object> data = salespersonSigninValidService.validSignin(companyId, token);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@SuppressWarnings("unchecked")
	private static long parseQywxSalespersonIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购身份无效");
		}
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object sid = auth.get("salesperson_id");
		if (sid == null) {
			throw new BadRequestException("导购身份无效");
		}
		if (sid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		}
		String s = sid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购身份无效");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购身份无效");
		}
	}

	@SuppressWarnings("unchecked")
	static long parseMemberUserIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return v;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购更新用户信息错误");
		}
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

	private static int parseComplaintsListPageParam(String raw) {
		try {
			String t = raw == null ? "" : raw.trim();
			int v = Integer.parseInt(t);
			return v >= 1 ? v : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseComplaintsListPageSizeParam(String raw) {
		try {
			String t = raw == null ? "" : raw.trim();
			int v = Integer.parseInt(t);
			return v >= 1 ? v : 20;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static boolean isComplaintDetailPathIdAbsent(String raw) {
		return !ValuePresence.hasEffectiveValue(raw);
	}

	private static long parseSalespersonIdLoose(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return LeadingNumberParser.parseAsLong(raw.toString());
	}

	@SuppressWarnings("unchecked")
	private static String resolveUnionIdFromClaimsOnly(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			return null;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		String u = firstNonBlankText(claims.get("unionid"), claims.get("union_id"));
		return StringUtils.hasText(u) ? u.trim() : null;
	}

	private static String firstNonBlankText(Object... candidates) {
		if (candidates == null) {
			return null;
		}
		for (Object v : candidates) {
			if (v != null) {
				String s = v.toString().trim();
				if (StringUtils.hasText(s)) {
					return s;
				}
			}
		}
		return null;
	}

	/** Compatible validation envelope: HTTP 200 with {@code data.message} + integer {@code data.status_code=422}. */
	private static ResponseEntity<Map<String, Object>> dingoValidation422(String message) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", 422);
		return ResponseEntity.ok(Map.of("data", data));
	}
}
