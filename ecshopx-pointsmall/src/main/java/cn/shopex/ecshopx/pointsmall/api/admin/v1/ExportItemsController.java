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

package cn.shopex.ecshopx.pointsmall.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.pointsmall.service.PointsmallExportItemsDataService;
import cn.shopex.ecshopx.pointsmall.service.dto.PointsmallExportItemsDataResult;
import cn.shopex.ecshopx.pointsmall.web.PointsmallAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO, badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true, notFound = true)
@AdminAuth
@ShopLog
@RestController("pointsmallExportItemsAdminV1")
@RequestMapping("/api/v1/pointsmall")
public class ExportItemsController {

	private final PointsmallExportItemsDataService pointsmallExportItemsDataService;

	public ExportItemsController(PointsmallExportItemsDataService pointsmallExportItemsDataService) {
		this.pointsmallExportItemsDataService = pointsmallExportItemsDataService;
	}

	@Activated(routeAlias = "pointsmall.goods.export")
	@PostMapping(value = "/goods/export", name = "导出商品信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportItemsData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = readOperatorContextLong(ud, "company_id");
		long operatorId = readOperatorContextLong(ud, "operator_id");
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		PointsmallExportItemsDataResult r = pointsmallExportItemsDataService.submit(companyId, operatorId, merged);
		if (r instanceof PointsmallExportItemsDataResult.EmptyPreview) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readOperatorContextLong(Map<String, Object> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException("缺少企业或操作员上下文");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("缺少企业或操作员上下文");
		}
	}
}
