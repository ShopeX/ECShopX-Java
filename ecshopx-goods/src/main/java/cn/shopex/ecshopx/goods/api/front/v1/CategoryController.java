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

package cn.shopex.ecshopx.goods.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.goods.service.ItemsCategoryQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@FrontNoAuth
@RestController("goodsFrontV1Category")
@RequestMapping("/api/v1/h5app/wxapp/goods")
public class CategoryController {

	private final ItemsCategoryQueryService itemsCategoryQueryService;

	public CategoryController(ItemsCategoryQueryService itemsCategoryQueryService) {
		this.itemsCategoryQueryService = itemsCategoryQueryService;
	}

	@GetMapping(value = "/category", name = "商品分类")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getCategoryList(HttpServletRequest request,
			@RequestParam(name = "is_main_category", required = false) String isMainCategory,
			@RequestParam(name = "distributor_id", required = false) Long distributorId,
			@RequestParam(name = "only_top", required = false, defaultValue = "false") String onlyTop,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		long companyId = parseCompanyIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		List<Map<String, Object>> data = itemsCategoryQueryService.getWxappCategoryList(companyId, isMainCategory,
				distributorId, onlyTop, countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static long parseJwtDistributorIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return 0L;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object v = claims.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@GetMapping(value = "/category/{cat_id}", name = "分类子分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> getChildrenCategorys(HttpServletRequest request,
			@PathVariable("cat_id") String catId,
			@RequestParam(name = "distributor_id", required = false) Long distributorId,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		long companyId = parseCompanyIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		Map<String, Object> data = itemsCategoryQueryService.getWxappChildrenCategorys(companyId, catId, distributorId,
				countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/categorylevel", name = "指定等级分类列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLevelCategoryList(HttpServletRequest request,
			@RequestParam(name = "is_main_category", required = false) String isMainCategory,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode,
			@RequestParam(name = "category_level", required = false) String categoryLevelIgnored) {
		long companyId = parseCompanyIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		Map<String, Object> data = itemsCategoryQueryService.getWxappLevelCategoryList(companyId, isMainCategory,
				distributorIdRaw, countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/shopcategorylevel", name = "小店上架分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShopShelvesCategoryList(HttpServletRequest request,
			@RequestParam(name = "shop_user_id", required = false) String shopUserIdRaw,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "is_main_category", required = false) String isMainCategory,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		long companyId = parseCompanyIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		long authUserId = parseOptionalUserIdFromRequest(request);
		Map<String, Object> data = itemsCategoryQueryService.getWxappShopShelvesCategoryList(companyId, shopUserIdRaw,
				authUserId, distributorIdRaw, isMainCategory, countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/categoryinfo", name = "商品分类信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCategoryInfo(HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdRaw,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		long companyId = parseCompanyIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		long categoryId = parseRequiredCategoryId(categoryIdRaw);
		Long distributorIdParam = parseOptionalWxappDistributorId(distributorIdRaw);
		Map<String, Object> data = itemsCategoryQueryService.getWxappCategoryInfo(companyId, categoryId, distributorIdParam,
				countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseRequiredCategoryId(String categoryIdRaw) {
		if (categoryIdRaw == null || !StringUtils.hasText(categoryIdRaw.trim())) {
			throw new BadRequestException("请传递商品分类",
					Map.of("category_id", List.of("validation.required")), 422);
		}
		try {
			long v = Long.parseLong(categoryIdRaw.trim());
			if (v < 1L) {
				throw new BadRequestException("请传递商品分类");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请传递商品分类");
		}
	}

	/**
	 * 与现网一致：空白视为未传；非数字或 {@code <=0} 视为未传；不抛参数异常。
	 */
	private static Long parseOptionalWxappDistributorId(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@GetMapping(value = "/promoter/category", name = "店铺推广分类")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getPromoterCategoryList(HttpServletRequest request,
			@RequestParam(name = "is_main_category", required = false) String isMainCategory,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "isSalesmanPage", required = false) String isSalesmanPage,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		long jwtDistributorId = parseJwtDistributorIdFromRequest(request);
		List<Map<String, Object>> data = itemsCategoryQueryService.getWxappPromoterCategoryList(companyId, userId,
				isMainCategory, distributorIdRaw, isSalesmanPage, countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@SuppressWarnings("unchecked")
	private static long parseOptionalUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return 0L;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			return 0L;
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
