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

package cn.shopex.ecshopx.members.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.MedicationPersonnelCreateService;
import cn.shopex.ecshopx.members.service.MedicationPersonnelDeleteService;
import cn.shopex.ecshopx.members.service.MedicationPersonnelDetailService;
import cn.shopex.ecshopx.members.service.MedicationPersonnelListService;
import cn.shopex.ecshopx.members.service.MedicationPersonnelUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("membersFrontV1MedicationPersonnel")
@RequestMapping("/api/v1/h5app")
@RequiredArgsConstructor
public class MedicationPersonnelController {

	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MedicationPersonnelCreateService medicationPersonnelCreateService;
	private final MedicationPersonnelUpdateService medicationPersonnelUpdateService;
	private final MedicationPersonnelDetailService medicationPersonnelDetailService;
	private final MedicationPersonnelListService medicationPersonnelListService;
	private final MedicationPersonnelDeleteService medicationPersonnelDeleteService;

	@PostMapping(
			value = "/wxapp/medicationPersonnel",
			name = "用药人创建",
			produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		String userFamilyName = trimToNull(b.get("user_family_name"));
		if (!StringUtils.hasText(userFamilyName)) {
			throw new ResourceException("请填写正确的用药人姓名");
		}
		String userFamilyIdCard = trimToNull(b.get("user_family_id_card"));
		if (!StringUtils.hasText(userFamilyIdCard)) {
			throw new ResourceException("请填写正确的用药人身份证号");
		}
		int userFamilyAge = parseNonNegativeIntRequired(b.get("user_family_age"), "请填写正确的用药人年龄");
		int userFamilyGender = parseGender(b.get("user_family_gender"));
		String userFamilyPhone = trimToNull(b.get("user_family_phone"));
		if (!StringUtils.hasText(userFamilyPhone) || !MOBILE_CN.matcher(userFamilyPhone).matches()) {
			throw new ResourceException("请填写正确的手机号");
		}
		int relationship = parseIntRequired(b.get("relationship"), "请选择正确的与本人关系");

		Map<String, Object> result = medicationPersonnelCreateService.create(
				companyId,
				userId,
				userFamilyName,
				userFamilyIdCard,
				userFamilyAge,
				userFamilyGender,
				userFamilyPhone,
				relationship);
		return ApiResult.ok(result);
	}

	@PutMapping(
			value = "/wxapp/medicationPersonnel",
			name = "用药人更新",
			produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> update(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		long id = parseLongOrResourceException(b.get("id"), "缺少参数");

		String userFamilyName = trimToNull(b.get("user_family_name"));
		if (!StringUtils.hasText(userFamilyName)) {
			throw new BadRequestException("请填写正确的用药人姓名");
		}
		String userFamilyIdCard = trimToNull(b.get("user_family_id_card"));
		if (!StringUtils.hasText(userFamilyIdCard)) {
			throw new BadRequestException("请填写正确的用药人身份证号");
		}
		int userFamilyAge = parseNonNegativeIntBadRequest(b.get("user_family_age"), "请填写正确的用药人年龄");
		int userFamilyGender = parseGenderBadRequest(b.get("user_family_gender"));
		String userFamilyPhone = trimToNull(b.get("user_family_phone"));
		if (!StringUtils.hasText(userFamilyPhone) || !MOBILE_CN.matcher(userFamilyPhone).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
		int relationship = parseIntRequiredBadRequest(b.get("relationship"), "请选择正确的与本人关系");

		Map<String, Object> result = medicationPersonnelUpdateService.update(
				companyId,
				userId,
				id,
				userFamilyName,
				userFamilyIdCard,
				userFamilyAge,
				userFamilyGender,
				userFamilyPhone,
				relationship);
		return ApiResult.ok(result);
	}

	@GetMapping(
			value = "/wxapp/medicationPersonnel/list",
			name = "用药人列表",
			produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam) {
		int page = parseMedicationPersonnelListPage(pageParam);
		Integer pageSize = parseMedicationPersonnelListPageSize(pageSizeParam);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> data = medicationPersonnelListService.getList(companyId, userId, page, pageSize);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/medicationPersonnel/detail",
			name = "用药人详情",
			produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getDetail(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idParam) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		long id = parseDetailIdOrBadRequest(idParam);
		Map<String, Object> data = medicationPersonnelDetailService.getDetail(companyId, userId, id);
		return ApiResult.ok(data);
	}

	@DeleteMapping(
			value = "/wxapp/medicationPersonnel",
			name = "用药人删除",
			produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteMedicationPersonnel(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idParam) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		if (idParam == null) {
			throw new BadRequestException("缺少参数");
		}
		String trim = idParam.trim();
		if (!StringUtils.hasText(trim)) {
			throw new BadRequestException("缺少参数");
		}
		long id;
		try {
			id = Long.parseLong(trim);
		} catch (NumberFormatException e) {
			return ApiResult.ok(Map.of("success", true));
		}
		if (id <= 0L) {
			throw new BadRequestException("缺少参数");
		}
		medicationPersonnelDeleteService.deleteMedicationPersonnel(companyId, userId, id);
		return ApiResult.ok(Map.of("success", true));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
	}

	private static long resolveUserId(Map<String, Object> claims) {
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		try {
			long userId = Long.parseLong(userIdStr);
			if (userId <= 0L) {
				throw new UnauthorizedException("还未授权，请授权手机号");
			}
			return userId;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
	}

	private static int parseMedicationPersonnelListPage(String pageParam) {
		if (pageParam == null || !StringUtils.hasText(pageParam.trim())) {
			return 1;
		}
		String t = pageParam.trim();
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static Integer parseMedicationPersonnelListPageSize(String pageSizeParam) {
		if (pageSizeParam == null || !StringUtils.hasText(pageSizeParam.trim())) {
			return null;
		}
		String t = pageSizeParam.trim();
		try {
			int p = Integer.parseInt(t);
			if (p <= 0) {
				return null;
			}
			return p;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseDetailIdOrBadRequest(String idParam) {
		if (idParam == null) {
			throw new BadRequestException("缺少参数");
		}
		String trim = idParam.trim();
		if (!StringUtils.hasText(trim)) {
			throw new BadRequestException("缺少参数");
		}
		try {
			long id = Long.parseLong(trim);
			if (id <= 0L) {
				throw new BadRequestException("缺少参数");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("用药人信息不存在");
		}
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String trimToNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	private static int parseNonNegativeIntRequired(Object raw, String badRequestMessage) {
		if (raw == null) {
			throw new ResourceException(badRequestMessage);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v < 0L || v > Integer.MAX_VALUE) {
				throw new ResourceException(badRequestMessage);
			}
			return (int) v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException(badRequestMessage);
			}
			try {
				int v = Integer.parseInt(t);
				if (v < 0) {
					throw new ResourceException(badRequestMessage);
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException(badRequestMessage);
			}
		}
		throw new ResourceException(badRequestMessage);
	}

	private static int parseGender(Object raw) {
		if (raw == null) {
			throw new ResourceException("请填写正确的用药人性别");
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new ResourceException("请填写正确的用药人性别");
			}
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new ResourceException("请填写正确的用药人性别");
			}
		}
		throw new ResourceException("请填写正确的用药人性别");
	}

	private static int parseIntRequired(Object raw, String badRequestMessage) {
		if (raw == null) {
			throw new ResourceException(badRequestMessage);
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException(badRequestMessage);
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new ResourceException(badRequestMessage);
			}
		}
		throw new ResourceException(badRequestMessage);
	}

	private static long parseLongOrResourceException(Object raw, String message) {
		if (raw == null) {
			throw new ResourceException(message);
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException(message);
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new ResourceException(message);
			}
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}

	private static int parseNonNegativeIntBadRequest(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v < 0L || v > Integer.MAX_VALUE) {
				throw new BadRequestException(message);
			}
			return (int) v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException(message);
			}
			try {
				int v = Integer.parseInt(t);
				if (v < 0) {
					throw new BadRequestException(message);
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException(message);
			}
		}
		throw new BadRequestException(message);
	}

	private static int parseGenderBadRequest(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请填写正确的用药人性别");
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请填写正确的用药人性别");
			}
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("请填写正确的用药人性别");
			}
		}
		throw new BadRequestException("请填写正确的用药人性别");
	}

	private static int parseIntRequiredBadRequest(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException(message);
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(message);
			}
		}
		throw new BadRequestException(message);
	}
}
