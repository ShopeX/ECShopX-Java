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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.admin.TradeRateAdminDeleteService;
import cn.shopex.ecshopx.orders.service.admin.TradeRateAdminDetailService;
import cn.shopex.ecshopx.orders.service.admin.TradeRateAdminListService;
import cn.shopex.ecshopx.orders.service.admin.TradeRateAdminReplyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1TradeRate")
@RequestMapping("/api/v1/trade")
public class TradeRateController {

	private final TradeRateAdminReplyService tradeRateAdminReplyService;
	private final TradeRateAdminListService tradeRateAdminListService;
	private final TradeRateAdminDetailService tradeRateAdminDetailService;
	private final TradeRateAdminDeleteService tradeRateAdminDeleteService;

	public TradeRateController(
			TradeRateAdminReplyService tradeRateAdminReplyService,
			TradeRateAdminListService tradeRateAdminListService,
			TradeRateAdminDetailService tradeRateAdminDetailService,
			TradeRateAdminDeleteService tradeRateAdminDeleteService) {
		this.tradeRateAdminReplyService = tradeRateAdminReplyService;
		this.tradeRateAdminListService = tradeRateAdminListService;
		this.tradeRateAdminDetailService = tradeRateAdminDetailService;
		this.tradeRateAdminDeleteService = tradeRateAdminDeleteService;
	}

	@Activated(routeAlias = "traderate.list.get")
	@GetMapping(value = "/rate", name = "评价列表")
	public ApiResult<Map<String, Object>> getTradeRateList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		return ApiResult.ok(tradeRateAdminListService.getTradeRateList(companyId, jwt, request));
	}

	@Activated(routeAlias = "traderate.reply.put")
	@PutMapping(value = "/rate", name = "回复评价", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> replyTradeRate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		String content = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("content"));
		if (!StringUtils.hasText(content)) {
			throw new BadRequestException("回复必填");
		}

		Object rateRaw = merged.get("rate_id");
		if (rateRaw == null || EspierAdminJwtControllerSupport.optionalTrimmedString(rateRaw) == null) {
			throw new BadRequestException("评价ID异常");
		}
		Long parsedRateId = EspierAdminJwtControllerSupport.parseLongOrNull(rateRaw);
		if (parsedRateId == null) {
			throw new BadRequestException("评价ID异常");
		}
		long rateId = parsedRateId;

		long companyId = readCompanyIdFromJwt(request);

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		if (operatorIdObj == null) {
			throw new BadRequestException("操作员信息异常");
		}

		Map<String, Object> data =
				tradeRateAdminReplyService.replyTradeRate(companyId, operatorIdObj, rateId, content);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "traderate.details.get")
	@GetMapping(value = "/{rate_id}/rate", name = "评价详情")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	public ApiResult<Map<String, Object>> getTradeRateInfo(
			HttpServletRequest request, @PathVariable("rate_id") String rateId) {
		String trimmed = EspierAdminJwtControllerSupport.optionalTrimmedString(rateId);
		if (rateId != null && trimmed == null) {
			throw new ResourceException("参数缺失");
		}
		if (trimmed == null && rateId == null) {
			throw new BadRequestException("参数格式错误");
		}
		Long parsed = EspierAdminJwtControllerSupport.parseLongOrNull(trimmed);
		if (parsed == null) {
			throw new BadRequestException("参数格式错误");
		}
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(tradeRateAdminDetailService.getTradeRateInfo(companyId, parsed.longValue()));
	}

	@Activated(routeAlias = "traderate.rate.delete")
	@DeleteMapping(value = "/rate/{rate_id}", name = "删除评价")
	public ApiResult<Map<String, Object>> tradeRateDelete(@PathVariable("rate_id") String rateId) {
		String trimmed = EspierAdminJwtControllerSupport.optionalTrimmedString(rateId);
		if (rateId != null && trimmed == null) {
			throw new ResourceException("参数缺失");
		}
		if (trimmed == null && rateId == null) {
			throw new BadRequestException("参数格式错误");
		}

		tradeRateAdminDeleteService.tradeRateDelete(trimmed);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", 1);
		return ApiResult.ok(body);
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
