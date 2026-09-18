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
import cn.shopex.ecshopx.goods.service.ItemsCommissionGetService;
import cn.shopex.ecshopx.goods.service.ItemsCommissionSaveService;
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
@RestController("goodsAdminV1ItemsCommission")
@RequestMapping("/api/v1/goods")
public class ItemsCommissionController {

	private final ItemsCommissionSaveService itemsCommissionSaveService;
	private final ItemsCommissionGetService itemsCommissionGetService;
	private final LangueProperties langueProperties;

	public ItemsCommissionController(ItemsCommissionSaveService itemsCommissionSaveService,
			ItemsCommissionGetService itemsCommissionGetService,
			LangueProperties langueProperties) {
		this.itemsCommissionSaveService = itemsCommissionSaveService;
		this.itemsCommissionGetService = itemsCommissionGetService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.commission.info")
	@GetMapping(value = "/commission/{item_id}", name = "佣金配置获取")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsCommission(
			@PathVariable("item_id") String itemId, HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Object aid = ud.get("authorizer_appid");
		String authorizerAppId = aid != null ? aid.toString() : null;
		String countryCode = RequestLangTag.current(langueProperties);
		if ("zh-CN".equals(countryCode)) {
			Object cc = ud.get("country_code");
			if (cc != null && StringUtils.hasText(cc.toString())) {
				countryCode = cc.toString().trim();
			}
		}

		if (itemId == null || itemId.isBlank()) {
			throw new BadRequestException("商品不存在");
		}
		long parsedItemId;
		try {
			parsedItemId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品不存在");
		}
		if (parsedItemId < 1) {
			throw new BadRequestException("商品不存在");
		}

		Map<String, Object> data = itemsCommissionGetService.getItemsCommission(companyId, parsedItemId,
				authorizerAppId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.commission.save")
	@PostMapping(value = "/commission/save", name = "佣金配置保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveGoodsCommission(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = AdminGoodsUploadRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		validateSaveGoodsCommissionInput(input);
		itemsCommissionSaveService.saveItemsCommission(companyId, input);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static void validateSaveGoodsCommissionInput(Map<String, Object> input) {
		requireNonBlankString(input, "item_id", "商品不存在");
		requireNonBlankString(input, "goods_id", "产品不存在");
		Object ct = input.get("commission_type");
		if (ct == null) {
			throw new BadRequestException("佣金类型错误");
		}
		String cts = ct.toString().trim();
		if (cts.isEmpty() || (!"1".equals(cts) && !"2".equals(cts))) {
			throw new BadRequestException("佣金类型错误");
		}
		Object commission = input.get("commission");
		if (commission == null) {
			throw new BadRequestException("SPU结算佣金不能为空");
		}
		if (commission.toString().trim().isEmpty()) {
			throw new BadRequestException("SPU结算佣金不能为空");
		}
		parseLongOrBadRequest(input.get("item_id"), "商品不存在");
		parseLongOrBadRequest(input.get("goods_id"), "产品不存在");
	}

	private static void requireNonBlankString(Map<String, Object> input, String key, String err) {
		Object v = input.get(key);
		if (v == null) {
			throw new BadRequestException(err);
		}
		if (v.toString().trim().isEmpty()) {
			throw new BadRequestException(err);
		}
	}

	private static void parseLongOrBadRequest(Object v, String err) {
		try {
			Long.parseLong(v.toString().trim());
		} catch (Exception e) {
			throw new BadRequestException(err);
		}
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
