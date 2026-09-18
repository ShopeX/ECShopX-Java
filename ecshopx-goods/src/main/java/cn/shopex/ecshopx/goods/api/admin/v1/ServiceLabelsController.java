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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ServiceLabelsCreateService;
import cn.shopex.ecshopx.goods.service.ServiceLabelsListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
		notFound = false
)
@AdminAuth
@ShopLog
@RestController("goodsAdminV1ServiceLabels")
@RequestMapping("/api/v1/goods/servicelabels")
public class ServiceLabelsController {

	private static final String SERVICE_LABELS_LIST_VALIDATION_MSG = "查询服务标签列表出错.";

	private final ServiceLabelsCreateService serviceLabelsCreateService;

	private final ServiceLabelsListService serviceLabelsListService;

	public ServiceLabelsController(
			ServiceLabelsCreateService serviceLabelsCreateService,
			ServiceLabelsListService serviceLabelsListService) {
		this.serviceLabelsCreateService = serviceLabelsCreateService;
		this.serviceLabelsListService = serviceLabelsListService;
	}

	@Activated(routeAlias = "goods.servicelabels.create")
	@PostMapping(name = "添加数值属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> createServiceLabels(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = serviceLabelsCreateService.createServiceLabels(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
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

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static BadRequestException validationFailedForLabelId(String validationRuleCode) {
		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		errors.put("label_id", List.of(validationRuleCode));
		return new BadRequestException("更新会员数值属性出错.", errors);
	}

	private static BadRequestException validationFailedForDetailLabelId(String validationRuleCode) {
		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		errors.put("label_id", List.of(validationRuleCode));
		return new BadRequestException("获取会员数值属性出错.", errors);
	}

	private static BadRequestException validationFailedForDeleteLabelId(String validationRuleCode) {
		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		errors.put("label_id", List.of(validationRuleCode));
		return new BadRequestException("删除会员数值属性出错.", errors);
	}

	private static long parseLabelIdPath(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw validationFailedForDetailLabelId("validation.required");
		}
		String trimmed = raw.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw validationFailedForDetailLabelId("validation.integer");
		}
		if (id < 1) {
			throw validationFailedForDetailLabelId("validation.min.numeric");
		}
		return id;
	}

	private static long parsePathLabelId(String labelId) {
		if (!StringUtils.hasText(labelId)) {
			throw validationFailedForLabelId("validation.required");
		}
		String trimmed = labelId.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw validationFailedForLabelId("validation.integer");
		}
		if (id < 1) {
			throw validationFailedForLabelId("validation.min.numeric");
		}
		return id;
	}

	private static long parseLabelIdForDelete(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw validationFailedForDeleteLabelId("validation.required");
		}
		String trimmed = raw.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw validationFailedForDeleteLabelId("validation.integer");
		}
		if (id < 1) {
			throw validationFailedForDeleteLabelId("validation.min.numeric");
		}
		return id;
	}

	@Activated(routeAlias = "goods.servicelabels.lists")
	@GetMapping(name = "数值属性列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getServiceLabelsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageStr,
			@RequestParam(name = "pageSize", required = false) String pageSizeStr,
			@RequestParam(name = "service_type", required = false) String serviceTypeStr,
			@RequestParam(name = "label_name", required = false) String labelName,
			@RequestParam(name = "keywords", required = false) String keywords) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");

		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		appendPageListErrors(pageStr, pageSizeStr, errors);
		appendServiceTypeListErrors(serviceTypeStr, errors);
		if (!errors.isEmpty()) {
			throw new BadRequestException(SERVICE_LABELS_LIST_VALIDATION_MSG, errors);
		}

		long page = Long.parseLong(pageStr.trim());
		long pageSize = Long.parseLong(pageSizeStr.trim());
		String serviceTypeTrimmed = serviceTypeStr.trim();

		String labelNameExact = null;
		String keywordsContains = null;
		if (StringUtils.hasText(keywords)) {
			keywordsContains = keywords.trim();
			labelNameExact = null;
		} else if (StringUtils.hasText(labelName)) {
			labelNameExact = labelName.trim();
		}

		Map<String, Object> data = serviceLabelsListService.getServiceLabelsList(
				companyId, serviceTypeTrimmed, labelNameExact, keywordsContains, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void appendPageListErrors(
			String pageStr, String pageSizeStr, LinkedHashMap<String, List<String>> errors) {
		if (!StringUtils.hasText(pageStr)) {
			errors.put("page", List.of("validation.required"));
		} else {
			String t = pageStr.trim();
			try {
				long p = Long.parseLong(t);
				if (p < 1L) {
					errors.put("page", List.of("validation.min.numeric"));
				}
			} catch (NumberFormatException e) {
				errors.put("page", List.of("validation.integer"));
			}
		}
		if (!StringUtils.hasText(pageSizeStr)) {
			errors.put("pageSize", List.of("validation.required"));
		} else {
			String t = pageSizeStr.trim();
			try {
				long ps = Long.parseLong(t);
				if (ps < 1L) {
					errors.put("pageSize", List.of("validation.min.numeric"));
				} else if (ps > 100L) {
					errors.put("pageSize", List.of("validation.max.numeric"));
				}
			} catch (NumberFormatException e) {
				errors.put("pageSize", List.of("validation.integer"));
			}
		}
	}

	private static void appendServiceTypeListErrors(
			String serviceTypeStr, LinkedHashMap<String, List<String>> errors) {
		if (serviceTypeStr == null || !StringUtils.hasText(serviceTypeStr.trim())) {
			errors.put("service_type", List.of("validation.required"));
			return;
		}
		String st = serviceTypeStr.trim();
		if (!"point".equals(st) && !"deposit".equals(st) && !"timescard".equals(st)) {
			errors.put("service_type", List.of("validation.in"));
		}
	}

	@Activated(routeAlias = "goods.servicelabels.detail")
	@GetMapping(value = "/{label_id}", name = "数值属性详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getServiceLabelsDetail(
			HttpServletRequest request, @PathVariable("label_id") String labelId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long id = parseLabelIdPath(labelId);
		Map<String, Object> data = serviceLabelsCreateService.getServiceLabelsDetail(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.servicelabels.delete")
	@DeleteMapping(value = "/{label_id}", name = "删除数值属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteServiceLabels(
			HttpServletRequest request, @PathVariable("label_id") String labelId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long id = parseLabelIdForDelete(labelId);
		Map<String, Object> data = serviceLabelsCreateService.deleteServiceLabels(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.servicelabels.update")
	@PutMapping(value = "/{label_id}", name = "更新数值属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateServiceLabels(HttpServletRequest request,
			@PathVariable("label_id") String labelId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long pathLabelId = parsePathLabelId(labelId);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("label_id", pathLabelId);
		Map<String, Object> data = serviceLabelsCreateService.updateServiceLabels(companyId, pathLabelId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
