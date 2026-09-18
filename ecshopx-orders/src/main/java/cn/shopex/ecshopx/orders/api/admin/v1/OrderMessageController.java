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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.workwechat.service.WorkWechatMessageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1OrderMessage")
@RequestMapping("/api/v1/order/message")
public class OrderMessageController {

	private final WorkWechatMessageService workWechatMessageService;

	public OrderMessageController(WorkWechatMessageService workWechatMessageService) {
		this.workWechatMessageService = workWechatMessageService;
	}

	@Activated(routeAlias = "order.message.new")
	@GetMapping(value = "/new", name = "店务未读消息")
	public ApiResult<Map<String, Object>> getNewInfo(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));

		Object distributorObj = merged.get("distributor_id");
		if (distributorObj == null) {
			throw new BadRequestException("店铺编号必填");
		}
		long distributorId = parseLongStrict(distributorObj, "店铺编号类型错误");

		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data = workWechatMessageService.getNewInfo(companyId, operatorId, distributorId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "order.message.list")
	@GetMapping(value = "/list", name = "店务消息列表")
	public ApiResult<Map<String, Object>> getList(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));

		Object msgTypeObj = merged.get("msg_type");
		if (msgTypeObj == null) {
			throw new BadRequestException("消息类型必填");
		}
		int msgType = parseIntStrict(msgTypeObj, "消息类型参数类型错误");

		Object distributorObj = merged.get("distributor_id");
		if (distributorObj == null) {
			throw new BadRequestException("店铺编号必填");
		}
		long distributorId = parseLongStrict(distributorObj, "店铺编号类型错误");

		Object pageObj = merged.get("page");
		if (pageObj == null) {
			throw new BadRequestException("页码必填");
		}
		int page = parseIntStrict(pageObj, "页码类型错误");

		Object pageSizeObj = merged.get("pageSize");
		if (pageSizeObj == null) {
			throw new BadRequestException("每页数量必填");
		}
		int pageSize = parseIntStrict(pageSizeObj, "每页数量类型错误");

		Long cursorLt = null;
		Object idObj = merged.get("id");
		if (!isEmptyForMessageGate(idObj)) {
			long idVal = parseLongStrict(idObj, "消息id类型错误");
			if (idVal > 0) {
				cursorLt = idVal;
			}
		}

		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data = workWechatMessageService.getList(
				companyId, operatorId, msgType, distributorId, cursorLt, page, pageSize);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "order.message.update")
	@PostMapping(value = "/update", name = "更新消息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateMsg(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object msgTypeObj = merged.get("msg_type");
		if (msgTypeObj == null) {
			throw new BadRequestException("消息类型必填");
		}
		int msgType = parseIntStrict(msgTypeObj, "消息类型参数类型错误");

		Object distributorObj = merged.get("distributor_id");
		if (distributorObj == null) {
			throw new BadRequestException("店铺编号必填");
		}
		long distributorId = parseLongStrict(distributorObj, "店铺编号类型错误");

		if (isEmptyForMessageGate(merged.get("order_id"))
				&& isEmptyForMessageGate(merged.get("after_sales_bn"))
				&& isEmptyForMessageGate(merged.get("is_all_read"))) {
			throw new BadRequestException("缺少参数");
		}

		String fragment = null;
		if (!isEmptyForMessageGate(merged.get("order_id"))) {
			fragment = "\"orderId\":\"" + escapeLikeMeta(String.valueOf(merged.get("order_id"))) + "\"";
		}
		if (!isEmptyForMessageGate(merged.get("after_sales_bn"))) {
			fragment = "\"afterSalesBn\":\"" + escapeLikeMeta(String.valueOf(merged.get("after_sales_bn"))) + "\"";
		}

		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		int up = (int) (System.currentTimeMillis() / 1000);
		workWechatMessageService.updateMsg(companyId, operatorId, msgType, distributorId, fragment, up);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("result", "更新成功");
		return ApiResult.ok(data);
	}

	private static int parseIntStrict(Object v, String typeMsg) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(typeMsg);
			}
		}
		throw new BadRequestException(typeMsg);
	}

	private static long parseLongStrict(Object v, String typeMsg) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(typeMsg);
			}
		}
		throw new BadRequestException(typeMsg);
	}

	private static boolean isEmptyForMessageGate(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (value instanceof Number n) {
			return n.intValue() == 0;
		}
		if (value instanceof Boolean b) {
			return !b;
		}
		if (value instanceof Collection<?> c) {
			return c.isEmpty();
		}
		return false;
	}

	private static String escapeLikeMeta(String raw) {
		if (raw == null) {
			return "";
		}
		if (raw.isEmpty()) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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

	private static long readOperatorIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object operatorIdObj = jwt.get("operator_id");
		if (operatorIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
