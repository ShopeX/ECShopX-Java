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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.wdterp.WdtErpUploadItemsOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("goodsAdminV1WdtErp")
@RequestMapping("/api/v1/goods")
public class WdtErpController {

	private final WdtErpUploadItemsOrchestratorService wdtErpUploadItemsOrchestratorService;

	public WdtErpController(WdtErpUploadItemsOrchestratorService wdtErpUploadItemsOrchestratorService) {
		this.wdtErpUploadItemsOrchestratorService = wdtErpUploadItemsOrchestratorService;
	}

	@Activated(routeAlias = "goods.wdterp.items.upload")
	@PostMapping(value = "/upload/wdterp/items", name = "旺店通上传商品")
	public ResponseEntity<Map<String, Object>> uploadItems(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Integer distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		Object opTypeObj = ud.get("operator_type");
		String operatorType = opTypeObj != null ? opTypeObj.toString() : "";

		long distributorId = 0L;
		if (operatorType != null && "distributor".equalsIgnoreCase(operatorType.trim())) {
			String q = request.getParameter("distributor_id");
			if (StringUtils.hasText(q)) {
				try {
					distributorId = Long.parseLong(q.trim());
				} catch (NumberFormatException e) {
					distributorId = 0L;
				}
			} else if (body != null && body.get("distributor_id") != null) {
				try {
					distributorId = Long.parseLong(body.get("distributor_id").toString().trim());
				} catch (NumberFormatException e) {
					distributorId = 0L;
				}
			} else if (distributorIdParam != null && distributorIdParam > 0) {
				distributorId = distributorIdParam.longValue();
			}
		}

		Map<String, Object> merged = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		wdtErpUploadItemsOrchestratorService.run(request, companyId, operatorType, distributorId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}
}
