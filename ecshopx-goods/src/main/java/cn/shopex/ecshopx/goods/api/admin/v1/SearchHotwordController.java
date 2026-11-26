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
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordAdminService;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordChannels;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordImageUploadService;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordInputNormalizer;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordStatus;
import cn.shopex.ecshopx.goods.service.hotword.SearchHotwordTime;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 搜索热词管理端接口（ECX-11390 F-001/F-002/F-003；Teambition ECX-11411、ECX-11413）。
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("goodsAdminV1SearchHotword")
@RequestMapping("/api/v1/goods/hotwords")
public class SearchHotwordController {

	private final SearchHotwordAdminService adminService;
	private final SearchHotwordImageUploadService imageUploadService;

	public SearchHotwordController(
			SearchHotwordAdminService adminService, SearchHotwordImageUploadService imageUploadService) {
		this.adminService = adminService;
		this.imageUploadService = imageUploadService;
	}

	@Activated(routeAlias = "goods.search_hotword.get")
	@GetMapping(name = "热词列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(
			HttpServletRequest request,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "channel", required = false) String channel,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "start_time_begin", required = false) String startTimeBegin,
			@RequestParam(value = "start_time_end", required = false) String startTimeEnd,
			@RequestParam(value = "end_time_begin", required = false) String endTimeBegin,
			@RequestParam(value = "end_time_end", required = false) String endTimeEnd,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeAliasRaw) {
		long companyId = readCompanyId(request);
		String pageSizeInput = StringUtils.hasText(pageSizeRaw) ? pageSizeRaw : pageSizeAliasRaw;
		SearchHotwordAdminService.ListQuery query = new SearchHotwordAdminService.ListQuery(
				content,
				SearchHotwordChannels.optionalFilterChannel(channel),
				SearchHotwordStatus.parseFilter(status),
				SearchHotwordTime.parse(startTimeBegin, "start_time_begin"),
				SearchHotwordTime.parse(startTimeEnd, "start_time_end"),
				SearchHotwordTime.parse(endTimeBegin, "end_time_begin"),
				SearchHotwordTime.parse(endTimeEnd, "end_time_end"),
				parsePositiveLongOrZero(pageRaw, 1L),
				parsePositiveLongOrZero(pageSizeInput, 10L));
		return ResponseEntity.ok(ApiResult.ok(adminService.list(companyId, query, now())));
	}

	@Activated(routeAlias = "goods.search_hotword.get")
	@GetMapping(value = "/{id}", name = "热词详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> detail(
			HttpServletRequest request, @PathVariable("id") String idRaw) {
		long companyId = readCompanyId(request);
		Map<String, Object> row = adminService.detail(companyId, parseRequiredId(idRaw), now());
		return ResponseEntity.ok(ApiResult.ok(row == null ? new LinkedHashMap<>() : row));
	}

	@Activated(routeAlias = "goods.search_hotword.set")
	@PostMapping(name = "保存热词")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		Map<String, Object> merged =
				SearchHotwordInputNormalizer.mergeRequestParams(request.getParameterMap(), body);
		Long id = SearchHotwordInputNormalizer.optionalId(merged);
		SearchHotwordInputNormalizer.Normalized input = SearchHotwordInputNormalizer.normalize(merged);
		return ResponseEntity.ok(ApiResult.ok(adminService.save(companyId, id, input, now())));
	}

	@Activated(routeAlias = "goods.search_hotword.delete")
	@DeleteMapping(value = "/{id}", name = "删除热词")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @PathVariable("id") String idRaw) {
		long companyId = readCompanyId(request);
		boolean deleted = adminService.delete(companyId, parseRequiredId(idRaw));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", deleted)));
	}

	@Activated(routeAlias = "goods.search_hotword.set")
	@PostMapping(
			value = "/image",
			name = "上传热词图片",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadImage(
			HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		long companyId = readCompanyId(request);
		return ResponseEntity.ok(ApiResult.ok(imageUploadService.upload(companyId, file)));
	}

	private static long now() {
		return Instant.now().getEpochSecond();
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> userData)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyId = userData.get("company_id");
		if (companyId instanceof Number number) {
			return number.longValue();
		}
		if (companyId instanceof String text && StringUtils.hasText(text)) {
			try {
				return Long.parseLong(text.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("未登录");
			}
		}
		throw new UnauthorizedException("未登录");
	}

	private static long parseRequiredId(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw new BadRequestException("id 无效", Map.of("id", List.of("validation.required")), 422);
		}
		try {
			long id = Long.parseLong(raw.trim());
			if (id <= 0L) {
				throw new BadRequestException("id 无效", Map.of("id", List.of("validation.min.numeric")), 422);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 无效", Map.of("id", List.of("validation.integer")), 422);
		}
	}

	private static long parsePositiveLongOrZero(String raw, long defaultValue) {
		if (!StringUtils.hasText(raw)) {
			return defaultValue;
		}
		try {
			long value = Long.parseLong(raw.trim());
			return value < 1L ? defaultValue : value;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
