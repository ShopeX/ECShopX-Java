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
import cn.shopex.ecshopx.goods.service.export.ExportItemsApiFileNameService;
import cn.shopex.ecshopx.goods.service.export.ExportItemsCodeDataOrchestratorService;
import cn.shopex.ecshopx.goods.service.export.ExportItemsDataOrchestratorService;
import cn.shopex.ecshopx.goods.service.export.ExportItemsTagDataOrchestratorService;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsApiFileNameResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeOrchestratorResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsDataOrchestratorResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsTagOrchestratorResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("goodsAdminV1ExportItems")
@RequestMapping("/api/v1/goods")
public class ExportItemsController {

	private final ExportItemsCodeDataOrchestratorService exportItemsCodeDataOrchestratorService;
	private final ExportItemsDataOrchestratorService exportItemsDataOrchestratorService;
	private final ExportItemsTagDataOrchestratorService exportItemsTagDataOrchestratorService;
	private final ExportItemsApiFileNameService exportItemsApiFileNameService;

	public ExportItemsController(ExportItemsCodeDataOrchestratorService exportItemsCodeDataOrchestratorService,
			ExportItemsDataOrchestratorService exportItemsDataOrchestratorService,
			ExportItemsTagDataOrchestratorService exportItemsTagDataOrchestratorService,
			ExportItemsApiFileNameService exportItemsApiFileNameService) {
		this.exportItemsCodeDataOrchestratorService = exportItemsCodeDataOrchestratorService;
		this.exportItemsDataOrchestratorService = exportItemsDataOrchestratorService;
		this.exportItemsTagDataOrchestratorService = exportItemsTagDataOrchestratorService;
		this.exportItemsApiFileNameService = exportItemsApiFileNameService;
	}

	@Activated(routeAlias = "goods.export")
	@PostMapping(value = "/exportApiFileName", name = "导出商品API文件名")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportItemsDataApiReturn(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		normalizeItemSource(merged);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		Long merchantId = readOptionalLong(ud.get("merchant_id"));

		ExportItemsApiFileNameResult r = exportItemsApiFileNameService.writeKeyOrEmptyList(companyId, operatorId,
				operatorType, merchantId, merged);
		if (r instanceof ExportItemsApiFileNameResult.EmptyItemBnList) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
		}
		ExportItemsApiFileNameResult.UrlReady ur = (ExportItemsApiFileNameResult.UrlReady) r;
		return ResponseEntity.ok(ApiResult.ok(Map.of("url", ur.urlSuffix())));
	}

	@Activated(routeAlias = "goods.export")
	@PostMapping(value = "/export", name = "导出商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportItemsData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		normalizeItemSource(merged);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		Long merchantId = readOptionalLong(ud.get("merchant_id"));

		ExportItemsDataOrchestratorResult r = exportItemsDataOrchestratorService.submitOrEmptyList(companyId, operatorId,
				operatorType, merchantId, merged);
		if (r instanceof ExportItemsDataOrchestratorResult.EmptyItemBnList) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.tag.export")
	@PostMapping(value = "/tag/export", name = "导出标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportItemsTagData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		normalizeItemSource(merged);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		Long merchantId = readOptionalLong(ud.get("merchant_id"));

		ExportItemsTagOrchestratorResult r = exportItemsTagDataOrchestratorService.submitOrEmptyList(companyId, operatorId,
				operatorType, merchantId, merged);
		if (r instanceof ExportItemsTagOrchestratorResult.EmptyList) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.tag.export")
	@PostMapping(value = "/code/export", name = "导出商品码")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportItemsCodeData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInput(request, body);
		normalizeItemSource(merged);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		Long merchantId = readOptionalLong(ud.get("merchant_id"));

		ExportItemsCodeOrchestratorResult r = exportItemsCodeDataOrchestratorService.submitOrEmptyList(companyId, operatorId,
				operatorType, merchantId, merged);
		if (r instanceof ExportItemsCodeOrchestratorResult.EmptyItemBnList) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static void normalizeItemSource(Map<String, Object> m) {
		Object holder = m.get("item_holder");
		if (holder != null && StringUtils.hasText(holder.toString())) {
			String h = holder.toString().trim();
			if ("supplier".equalsIgnoreCase(h)) {
				m.put("item_source", "supplier");
				return;
			}
			if ("self".equalsIgnoreCase(h) || "platform".equalsIgnoreCase(h)) {
				m.put("item_source", "platform");
				return;
			}
		}
		if (m.containsKey("item_source")) {
			Object is = m.get("item_source");
			if (is != null && StringUtils.hasText(is.toString())) {
				m.put("item_source", is.toString().trim());
			}
			return;
		}
		Object src = m.get("source");
		if (src != null && StringUtils.hasText(src.toString())) {
			m.put("item_source", src.toString().trim());
		}
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		if (body != null) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>(body);
			parameterMapToMap(request).forEach(m::putIfAbsent);
			return m;
		}
		return new LinkedHashMap<>(parameterMapToMap(request));
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
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

	private static Long readOptionalLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
