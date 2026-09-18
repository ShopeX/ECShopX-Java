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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ItemsProfitQueryService;
import cn.shopex.ecshopx.goods.service.ItemsProfitSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("goodsAdminV1ItemsProfit")
@RequestMapping("/api/v1/goods")
public class ItemsProfitController {

	private final ItemsProfitSaveService itemsProfitSaveService;
	private final ItemsProfitQueryService itemsProfitQueryService;
	private final LangueProperties langueProperties;

	public ItemsProfitController(ItemsProfitSaveService itemsProfitSaveService, ItemsProfitQueryService itemsProfitQueryService,
			LangueProperties langueProperties) {
		this.itemsProfitSaveService = itemsProfitSaveService;
		this.itemsProfitQueryService = itemsProfitQueryService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.profit.info")
	@GetMapping(value = "/profit/{item_id}", name = "导购分润获取")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsProfit(HttpServletRequest request,
			@PathVariable("item_id") String itemIdStr) {
		String t = itemIdStr == null ? "" : itemIdStr.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException("商品不存在");
		}
		long itemId;
		try {
			itemId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品不存在");
		}
		if (itemId < 1L) {
			throw new BadRequestException("商品不存在");
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		Map<String, Object> data = itemsProfitQueryService.getItemsProfit(companyId, itemId, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.profit.save")
	@PostMapping(value = "/profit/save", name = "导购分润保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveGoodsProfit(HttpServletRequest request,
			@FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		itemsProfitSaveService.save(companyId, body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
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
}
