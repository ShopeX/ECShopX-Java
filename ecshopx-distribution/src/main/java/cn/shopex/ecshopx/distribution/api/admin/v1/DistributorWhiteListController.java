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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListAddService;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListDeleteService;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListListService;
import cn.shopex.ecshopx.distribution.service.export.DistributorWhiteListExportService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListAddCommand;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorWhiteList")
@RequestMapping("/api/v1/distributor/whitelist")
public class DistributorWhiteListController {

	private final DistributorWhiteListAddService distributorWhiteListAddService;
	private final DistributorWhiteListExportService distributorWhiteListExportService;
	private final DistributorWhiteListListService distributorWhiteListListService;
	private final DistributorWhiteListDeleteService distributorWhiteListDeleteService;
	private final LangueProperties langueProperties;

	public DistributorWhiteListController(
			DistributorWhiteListAddService distributorWhiteListAddService,
			DistributorWhiteListExportService distributorWhiteListExportService,
			DistributorWhiteListListService distributorWhiteListListService,
			DistributorWhiteListDeleteService distributorWhiteListDeleteService,
			LangueProperties langueProperties) {
		this.distributorWhiteListAddService = distributorWhiteListAddService;
		this.distributorWhiteListExportService = distributorWhiteListExportService;
		this.distributorWhiteListListService = distributorWhiteListListService;
		this.distributorWhiteListDeleteService = distributorWhiteListDeleteService;
		this.langueProperties = langueProperties;
	}

	@PostMapping(
			value = "/add",
			name = "新增白名单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> addWhiteList(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwtMap = readOperatorJwtMap(request);
		long companyId = parseCompanyIdStrict(jwtMap);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		DistributorWhiteListAddCommand command = buildAddCommand(merged);
		distributorWhiteListAddService.addWhiteList(companyId, command, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(
			value = "/get",
			name = "获取店铺白名单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getWhiteList(HttpServletRequest request) {
		Map<String, Object> jwtMap = readOperatorJwtMap(request);
		long companyId = parseCompanyIdStrict(jwtMap);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		String operatorType = jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString();
		Long jwtDistributorId = parseNullablePositiveLong(jwtMap.get("distributor_id"));
		int page = parseListPage(merged);
		int pageSize = parseListPageSize(merged);
		Map<String, Object> data = distributorWhiteListListService.getWhiteList(
				companyId, operatorType, jwtDistributorId, merged, page, pageSize);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/delete",
			name = "删除白名单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteWhiteList(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwtMap = readOperatorJwtMap(request);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object typeValue = merged.get("type");
		boolean deleteByWhiteListId =
				typeValue != null
						&& typeValue.getClass() == String.class
						&& "id".equals((String) typeValue);
		Object idRaw = merged.get("id");
		String operatorType = jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString();
		Long jwtDistributorId = parseNullablePositiveLong(jwtMap.get("distributor_id"));
		distributorWhiteListDeleteService.deleteWhiteList(
				deleteByWhiteListId, idRaw, operatorType, jwtDistributorId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(
			value = "/export",
			name = "导出白名单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> exportWhiteList(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwtMap = readOperatorJwtMap(request);
		long companyId = parseCompanyIdStrict(jwtMap);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long operatorId = parseLongDefault(jwtMap.get("operator_id"), 0L);
		String operatorType = jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString();
		long merchantId =
				"merchant".equalsIgnoreCase(operatorType.trim()) ? parseLongDefault(jwtMap.get("merchant_id"), 0L) : 0L;
		Long jwtDistributorId = parseNullablePositiveLong(jwtMap.get("distributor_id"));
		String datapass = request.getHeader("x-datapass-block");
		distributorWhiteListExportService.exportWhiteList(
				companyId, operatorId, merchantId, operatorType, jwtDistributorId, merged, datapass);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static long parseLongDefault(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			String s = o.toString().trim();
			if (!StringUtils.hasText(s)) {
				return def;
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Long parseNullablePositiveLong(Object o) {
		if (o == null) {
			return null;
		}
		try {
			long v;
			if (o instanceof Number n) {
				v = n.longValue();
			} else {
				String s = o.toString().trim();
				if (!StringUtils.hasText(s)) {
					return null;
				}
				v = Long.parseLong(s);
			}
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static int parseListPage(Map<String, Object> merged) {
		Object raw = merged != null ? merged.get("page") : null;
		if (raw == null) {
			return 1;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 1;
			}
			return parsePageIntAtLeastOne(t);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException("请求参数格式错误");
			}
			return (int) v;
		}
		throw new BadRequestException("请求参数格式错误");
	}

	private static int parseListPageSize(Map<String, Object> merged) {
		Object raw = merged != null ? merged.get("page_size") : null;
		if (raw == null) {
			return 10;
		}
		int v;
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 10;
			}
			v = parsePageIntAtLeastOne(t);
		} else if (raw instanceof Number n) {
			long lv = n.longValue();
			if (lv < 1L || lv > Integer.MAX_VALUE) {
				throw new BadRequestException("请求参数格式错误");
			}
			v = (int) lv;
		} else {
			throw new BadRequestException("请求参数格式错误");
		}
		return Math.min(v, 100);
	}

	private static int parsePageIntAtLeastOne(String t) {
		try {
			long v = Long.parseLong(t);
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException("请求参数格式错误");
			}
			return (int) v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("请求参数格式错误");
		}
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parseCompanyIdStrict(Map<String, Object> jwt) {
		Object cid = jwt.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		try {
			long result;
			if (cid instanceof Number n) {
				result = n.longValue();
			} else if (cid instanceof String s) {
				if (!StringUtils.hasText(s)) {
					throw new UnauthorizedException(
							"Failed to authenticate because of bad credentials or an invalid authorization header.");
				}
				result = Long.parseLong(s.trim());
			} else {
				result = Long.parseLong(String.valueOf(cid).trim());
			}
			if (result <= 0L) {
				throw new UnauthorizedException(
						"Failed to authenticate because of bad credentials or an invalid authorization header.");
			}
			return result;
		} catch (NumberFormatException ex) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
	}

	private static DistributorWhiteListAddCommand buildAddCommand(Map<String, Object> merged) {
		try {
			Long idVal = parseIdValue(merged.get("id"));
			List<Long> distIds = parseDistributorIdField(merged);
			String mobileVal = Objects.toString(merged.get("mobile"), null);
			Object un = merged.get("username");
			String usernameVal = un instanceof String s ? s : null;
			return new DistributorWhiteListAddCommand(idVal, distIds, mobileVal, usernameVal);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("请求参数格式错误");
		}
	}

	private static Long parseIdValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			return Long.parseLong(s.trim());
		}
		return Long.parseLong(raw.toString());
	}

	private static List<Long> parseDistributorIdField(Map<String, Object> merged) {
		if (!merged.containsKey("distributor_id")) {
			return null;
		}
		Object rawDist = merged.get("distributor_id");
		if (rawDist == null) {
			return null;
		}
		if (rawDist instanceof List<?> list) {
			if (list.isEmpty()) {
				return List.of();
			}
			List<Long> out = new ArrayList<>(list.size());
			for (Object o : list) {
				if (o == null) {
					out.add(null);
				} else if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o instanceof String s) {
					out.add(Long.parseLong(s.trim()));
				} else {
					throw new BadRequestException("请求参数格式错误");
				}
			}
			return out;
		}
		if (rawDist instanceof Number n) {
			return List.of(n.longValue());
		}
		throw new BadRequestException("请求参数格式错误");
	}
}
