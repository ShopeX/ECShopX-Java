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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDetailListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("kaquanDiscountCardDetailAdminV1")
@RequestMapping("/api/v1")
public class DiscountCardDetailController {

	private static final String NONE_DISPLAY = "无";

	private final DiscountCardDetailListService discountCardDetailListService;

	public DiscountCardDetailController(DiscountCardDetailListService discountCardDetailListService) {
		this.discountCardDetailListService = discountCardDetailListService;
	}

	@DataPass
	@Activated(routeAlias = "card.detail.list")
	@GetMapping(value = "/discountcard/detail/list", name = "获取卡券领取列表以及使用明细")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDiscountCardDetail(
			HttpServletRequest request,
			@RequestParam(value = "card_id", required = false) String cardId,
			@RequestParam(value = "is_use", required = false) String isUse,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "activity_name", required = false) String activityName,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> result =
				discountCardDetailListService.query(
						companyId,
						cardId,
						isUse,
						truthyQueryParam(mobile),
						truthyQueryParam(activityName),
						truthyQueryParam(status),
						page,
						pageSize);

		boolean datapassBlock = isDatapassBlock(request);
		if (datapassBlock) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
			if (list != null) {
				for (Map<String, Object> row : list) {
					Object u = row.get("username");
					if (u != null && !NONE_DISPLAY.contentEquals(u.toString())) {
						row.put("username", DataMasking.maskTruename(u.toString()));
					}
					Object mob = row.get("mobile");
					if (mob != null && !NONE_DISPLAY.contentEquals(mob.toString())) {
						row.put("mobile", DataMasking.maskMobile(mob.toString()));
					}
				}
			}
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static boolean isDatapassBlock(HttpServletRequest request) {
		String h = request.getHeader("x-datapass-block");
		if (StringUtils.hasText(h)) {
			return true;
		}
		String p = request.getParameter("x-datapass-block");
		return StringUtils.hasText(p);
	}

	/** Optional list filters: empty or {@code "0"} are treated as absent (same as legacy API). */
	private static String truthyQueryParam(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		if ("0".equals(raw.trim())) {
			return null;
		}
		return raw;
	}
}
