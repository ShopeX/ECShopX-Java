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
import cn.shopex.ecshopx.goods.service.ItemsGroupGetGroupItemsService;
import cn.shopex.ecshopx.goods.service.ItemsGroupSaveGroupItemService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("goodsAdminV1ItemsGroup")
@RequestMapping("/api/v1/goods")
public class ItemsGroupController {

	private final ItemsGroupSaveGroupItemService itemsGroupSaveGroupItemService;
	private final ItemsGroupGetGroupItemsService itemsGroupGetGroupItemsService;

	public ItemsGroupController(
			ItemsGroupSaveGroupItemService itemsGroupSaveGroupItemService,
			ItemsGroupGetGroupItemsService itemsGroupGetGroupItemsService) {
		this.itemsGroupSaveGroupItemService = itemsGroupSaveGroupItemService;
		this.itemsGroupGetGroupItemsService = itemsGroupGetGroupItemsService;
	}

	@Activated(routeAlias = "goods.items.save_group_item")
	@PostMapping(value = "/save_group_item", name = "保存分组商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveGroupItem(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		Map<String, Object> merged = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> itemsGroup = itemsGroupSaveGroupItemService.saveGroupItem(companyId, merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items_group", itemsGroup);
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

	@Activated(routeAlias = "goods.items.get_group_items")
	@GetMapping(value = "/get_group_items", name = "查询分组商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> getGroupItems(
			@RequestParam(value = "group_id", required = false) Long groupId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize) {
		int p = Math.max(1, page);
		int ps = Math.min(Math.max(1, pageSize), 200);
		List<Long> goodsIds = itemsGroupGetGroupItemsService.listGoodsIdsByGroupId(groupId, p, ps);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("goods_id", goodsIds);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
