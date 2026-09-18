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

package cn.shopex.ecshopx.superadmin.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.superadmin.service.ShopNoticeListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("superAdminV1ShopNotice")
@RequestMapping("/api/v1/notice")
public class ShopNoticeController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String SHOP_NOTICE_LIST_VALIDATION_MESSAGE = "获取公告列表出错.";

	private static final Pattern SIGNED_INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final ShopNoticeListService shopNoticeListService;

	public ShopNoticeController(ShopNoticeListService shopNoticeListService) {
		this.shopNoticeListService = shopNoticeListService;
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.PLAIN)
	@Activated(routeAlias = "super.notice.list")
	@GetMapping(value = "/list", name = "公告列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShopNoticeList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}

		assertShopNoticeListPagination(pageParam, pageSizeParam);

		long pageVal = Long.parseLong(pageParam.trim());
		int page = pageVal > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pageVal;
		int pageSize = Integer.parseInt(pageSizeParam.trim());

		Map<String, Object> data = shopNoticeListService.list(page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void assertShopNoticeListPagination(String pageParam, String pageSizeParam) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendShopNoticeListPageErrors("page", pageParam, errors);
		appendShopNoticeListPageSizeErrors("pageSize", pageSizeParam, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(SHOP_NOTICE_LIST_VALIDATION_MESSAGE);
		}
	}

	private static void appendShopNoticeListPageErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
	}

	private static void appendShopNoticeListPageSizeErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
		}
	}

	private static boolean isShopNoticeInfoPathNoticeIdInvalid(String noticeIdParam) {
		return noticeIdParam == null || noticeIdParam.isBlank() || "0".equals(noticeIdParam.trim());
	}

	@GetMapping(value = "/{notice_id}", name = "公告详情")
	public ResponseEntity<ApiResult<Object>> getShopNoticeInfo(
			HttpServletRequest request, @PathVariable("notice_id") String noticeIdParam) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}

		if (isShopNoticeInfoPathNoticeIdInvalid(noticeIdParam)) {
			throw new ResourceException("参数错误");
		}

		String t = noticeIdParam.trim();
		if (!SIGNED_INTEGER_STRING.matcher(t).matches()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		try {
			long noticeId = Long.parseLong(t);
			Optional<Map<String, Object>> row = shopNoticeListService.findNoticeRowByNoticeId(noticeId);
			return row.<ResponseEntity<ApiResult<Object>>>map(m -> ResponseEntity.ok(ApiResult.ok(m)))
					.orElseGet(() -> ResponseEntity.ok(ApiResult.ok(Collections.emptyList())));
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
	}
}
