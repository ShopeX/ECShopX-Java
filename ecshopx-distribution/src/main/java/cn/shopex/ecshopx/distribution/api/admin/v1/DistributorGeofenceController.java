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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.distribution.service.DistributorGeofenceDeleteService;
import cn.shopex.ecshopx.distribution.service.DistributorGeofenceListService;
import cn.shopex.ecshopx.distribution.service.DistributorGeofenceSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("distributionAdminV1DistributorGeofence")
@RequestMapping("/api/v1/distributor/geofence")
public class DistributorGeofenceController {

	private final DistributorGeofenceSaveService distributorGeofenceSaveService;
	private final DistributorGeofenceListService distributorGeofenceListService;
	private final DistributorGeofenceDeleteService distributorGeofenceDeleteService;

	public DistributorGeofenceController(
			DistributorGeofenceSaveService distributorGeofenceSaveService,
			DistributorGeofenceListService distributorGeofenceListService,
			DistributorGeofenceDeleteService distributorGeofenceDeleteService) {
		this.distributorGeofenceSaveService = distributorGeofenceSaveService;
		this.distributorGeofenceListService = distributorGeofenceListService;
		this.distributorGeofenceDeleteService = distributorGeofenceDeleteService;
	}

	@GetMapping(name = "获取店铺围栏")
	public ResponseEntity<ApiResult<Object>> get(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "page", required = false) Integer pageParam,
			@RequestParam(name = "page_size", required = false) Integer pageSizeParam) {
		long effectiveDistributorId = 0L;
		if (StringUtils.hasText(distributorIdRaw)) {
			try {
				effectiveDistributorId = Long.parseLong(distributorIdRaw.trim());
			} catch (NumberFormatException ex) {
				effectiveDistributorId = 0L;
			}
		}
		if (effectiveDistributorId == 0L) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		int page = pageParam == null ? 1 : pageParam;
		int pageSize = pageSizeParam == null ? 10 : pageSizeParam;
		boolean filterByGeofenceId = false;
		Long geofenceId = null;
		if (request.getParameterMap().containsKey("distributor_geofence_id")
				&& request.getParameter("distributor_geofence_id") != null) {
			String v = request.getParameter("distributor_geofence_id").trim();
			if (StringUtils.hasText(v)) {
				try {
					geofenceId = Long.parseLong(v);
					filterByGeofenceId = true;
				} catch (NumberFormatException ex) {
					filterByGeofenceId = false;
				}
			}
		}
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> body = distributorGeofenceListService.get(
				companyId, effectiveDistributorId, page, pageSize, filterByGeofenceId, geofenceId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@PostMapping(name = "添加或更新店铺围栏")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> b = body != null ? body : Map.of();

		long distributorId = parseRequiredPositiveDistributorId(b);
		List<?> dataList = parseRequiredDataList(b);
		Long distributorGeofenceIdOrNull = parseOptionalFenceId(b);
		String type = parseOptionalType(b);

		Map<String, Object> result = distributorGeofenceSaveService.save(
				companyId, distributorId, distributorGeofenceIdOrNull, dataList, type);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@DeleteMapping(name = "删除店铺围栏")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Map<String, Object> merged = body != null ? new LinkedHashMap<>(body) : new LinkedHashMap<>();
		mergeRequestParamIfPresent(request, merged, "distributor_id");
		mergeRequestParamIfPresent(request, merged, "distributor_geofence_id");

		long distributorId = parseRequiredPositiveDistributorId(merged);
		Long distributorGeofenceIdOrNull = parseOptionalFenceId(merged);
		Map<String, Object> result =
				distributorGeofenceDeleteService.delete(companyId, distributorId, distributorGeofenceIdOrNull);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static void mergeRequestParamIfPresent(HttpServletRequest request, Map<String, Object> merged, String key) {
		if (!request.getParameterMap().containsKey(key)) {
			return;
		}
		String v = request.getParameter(key);
		if (v != null) {
			merged.put(key, v);
		}
	}

	private static long parseRequiredPositiveDistributorId(Map<String, Object> body) {
		Object did = body.get("distributor_id");
		if (did == null) {
			throw new BadRequestException("参数有误！");
		}
		long distributorId;
		if (did instanceof Number n) {
			distributorId = n.longValue();
		} else if (did instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("参数有误！");
			}
			try {
				distributorId = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("参数有误！");
			}
		} else {
			try {
				distributorId = Long.parseLong(String.valueOf(did).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("参数有误！");
			}
		}
		if (distributorId <= 0L) {
			throw new BadRequestException("参数有误！");
		}
		return distributorId;
	}

	private static List<?> parseRequiredDataList(Map<String, Object> body) {
		if (!body.containsKey("data")) {
			throw new BadRequestException("参数有误！");
		}
		Object dataObj = body.get("data");
		if (!(dataObj instanceof List<?> list) || list.isEmpty()) {
			throw new BadRequestException("参数有误！");
		}
		return list;
	}

	private static Long parseOptionalFenceId(Map<String, Object> body) {
		if (!body.containsKey("distributor_geofence_id")) {
			return null;
		}
		Object v = body.get("distributor_geofence_id");
		if (v == null) {
			return null;
		}
		if (v instanceof String s && !StringUtils.hasText(s)) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("参数有误！");
		}
	}

	private static String parseOptionalType(Map<String, Object> body) {
		Object t = body.get("type");
		if (t == null) {
			return null;
		}
		String s = String.valueOf(t).trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		return toStringKeyMap(m);
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}
}
