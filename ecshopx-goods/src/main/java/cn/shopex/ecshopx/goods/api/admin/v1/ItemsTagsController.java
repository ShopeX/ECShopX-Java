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
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ItemsTagsCreateService;
import cn.shopex.ecshopx.goods.service.ItemsTagsDeleteService;
import cn.shopex.ecshopx.goods.service.ItemsTagsQueryService;
import cn.shopex.ecshopx.goods.service.ItemsTagsRelService;
import cn.shopex.ecshopx.goods.service.ItemsTagsUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("goodsAdminV1ItemsTags")
@RequestMapping("/api/v1/goods")
public class ItemsTagsController {

	@SuppressWarnings("unused")
	private final ObjectMapper objectMapper;
	private final ItemsTagsCreateService itemsTagsCreateService;
	private final ItemsTagsUpdateService itemsTagsUpdateService;
	private final ItemsTagsDeleteService itemsTagsDeleteService;
	private final ItemsTagsQueryService itemsTagsQueryService;
	private final ItemsTagsRelService itemsTagsRelService;
	private final LangueProperties langueProperties;

	public ItemsTagsController(ObjectMapper objectMapper, ItemsTagsCreateService itemsTagsCreateService,
			ItemsTagsUpdateService itemsTagsUpdateService, ItemsTagsDeleteService itemsTagsDeleteService,
			ItemsTagsQueryService itemsTagsQueryService, ItemsTagsRelService itemsTagsRelService,
			LangueProperties langueProperties) {
		this.objectMapper = objectMapper;
		this.itemsTagsCreateService = itemsTagsCreateService;
		this.itemsTagsUpdateService = itemsTagsUpdateService;
		this.itemsTagsDeleteService = itemsTagsDeleteService;
		this.itemsTagsQueryService = itemsTagsQueryService;
		this.itemsTagsRelService = itemsTagsRelService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.tag.add")
	@PostMapping(value = "/tag", name = "添加商品标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTags(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long distributorId = readDistributorId(ud);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		String countryCode = RequestCountryCode.resolve(langueProperties, merged);

		validateCreateTagParams(merged);

		Map<String, Object> data = itemsTagsCreateService.createTag(companyId, distributorId, merged, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private void validateUpdateTagParams(Map<String, Object> merged) {
		Object tagIdObj = merged.get("tag_id");
		if (tagIdObj == null) {
			throw new ResourceException("tagId不能为空");
		}
		if (!(tagIdObj instanceof Number)) {
			try {
				Long.parseLong(tagIdObj.toString().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("tagId不能为空");
			}
		}
		Object tagNameObj = merged.get("tag_name");
		if (tagNameObj == null || !StringUtils.hasText(tagNameObj.toString().trim())) {
			throw new ResourceException("标签名称不能为空");
		}
		Object tagColorObj = merged.get("tag_color");
		if (tagColorObj == null || !StringUtils.hasText(tagColorObj.toString().trim())) {
			throw new ResourceException("标签颜色");
		}
		Object fontColorObj = merged.get("font_color");
		if (fontColorObj == null || !StringUtils.hasText(fontColorObj.toString().trim())) {
			throw new ResourceException("标签字体颜色");
		}
		assertValidFrontShow(merged.get("front_show"));
	}

	private void validateCreateTagParams(Map<String, Object> merged) {
		Object tagNameObj = merged.get("tag_name");
		if (tagNameObj == null || !StringUtils.hasText(tagNameObj.toString().trim())) {
			throw new ResourceException("标签名称不能为空");
		}
		Object tagColorObj = merged.get("tag_color");
		if (tagColorObj == null || !StringUtils.hasText(tagColorObj.toString().trim())) {
			throw new ResourceException("标签颜色");
		}
		Object fontColorObj = merged.get("font_color");
		if (fontColorObj == null || !StringUtils.hasText(fontColorObj.toString().trim())) {
			throw new ResourceException("标签字体颜色");
		}
		assertValidFrontShow(merged.get("front_show"));
	}

	private static void assertValidFrontShow(Object v) {
		if (v == null) {
			throw new ResourceException("前台显示类型错误");
		}
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
			long lv = ((Number) v).longValue();
			if (lv != 0L && lv != 1L) {
				throw new ResourceException("前台显示类型错误");
			}
			return;
		}
		if (v instanceof Number) {
			throw new ResourceException("前台显示类型错误");
		}
		String s = v.toString().trim();
		if (!"0".equals(s) && !"1".equals(s)) {
			throw new ResourceException("前台显示类型错误");
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
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

	private static long readDistributorId(Map<?, ?> ud) {
		Object v = ud.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	@Activated(routeAlias = "goods.tag.delete")
	@DeleteMapping(value = "/tag/{tag_id}", name = "删除商品标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTag(HttpServletRequest request,
			@PathVariable("tag_id") String tagIdStr) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long tagId;
		try {
			tagId = tagIdStr == null ? 0L : Long.parseLong(tagIdStr.trim());
		} catch (NumberFormatException e) {
			tagId = 0L;
		}
		if (tagId <= 0) {
			tagId = 0L;
		}
		itemsTagsDeleteService.deleteTag(companyId, tagId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.tag.update")
	@PutMapping(value = "/tag", name = "更新商品标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateTags(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long distributorId = readDistributorId(ud);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		String countryCode = RequestCountryCode.resolve(langueProperties, merged);

		validateUpdateTagParams(merged);

		Map<String, Object> data = itemsTagsUpdateService.updateTag(companyId, distributorId, merged, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.tag.list")
	@GetMapping(value = "/tag", name = "商品标签列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagsList(HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageStr,
			@RequestParam(name = "tag_name", required = false) String tagName,
			@RequestParam(name = "country_code", required = false) String countryCode,
			@RequestParam(name = "tag_source", required = false) String tagSource,
			@RequestParam(name = "isPlatform", required = false) String isPlatformStr,
			@RequestParam(name = "distributor_id", required = false) String distributorIdQueryStr) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long jwtDistributorId = readDistributorId(ud);

		if (!StringUtils.hasText(pageStr)) {
			throw new BadRequestException("page 必填");
		}
		int page;
		try {
			page = Integer.parseInt(pageStr.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
		// 分页查询参数仅接受 camelCase 键名 pageSize；不接受 page_size
		String pageSizeRaw = request.getParameter("pageSize");
		if (!StringUtils.hasText(pageSizeRaw)) {
			throw new BadRequestException("pageSize 必填");
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}

		boolean frontShowParameterPresent = request.getParameterMap().containsKey("front_show");
		Integer frontShowValue = null;
		if (frontShowParameterPresent) {
			String rawFront = request.getParameter("front_show");
			assertValidFrontShow(rawFront);
			frontShowValue = frontShowToInt(rawFront);
		}

		Boolean isPlatform = parseIsPlatformFlag(isPlatformStr);

		Map<String, Object> data = itemsTagsQueryService.getTagsList(companyId, jwtDistributorId, page, pageSize, tagName, frontShowParameterPresent,
				frontShowValue, countryCode, tagSource, isPlatform, distributorIdQueryStr);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Integer frontShowToInt(Object v) {
		if (v instanceof Number n) {
			return (int) n.longValue();
		}
		return Integer.parseInt(v.toString().trim());
	}

	private static Boolean parseIsPlatformFlag(String s) {
		if (!StringUtils.hasText(s)) {
			return false;
		}
		String t = s.trim().toLowerCase();
		return "true".equals(t) || "1".equals(t);
	}

	@Activated(routeAlias = "goods.tag.get")
	@GetMapping(value = "/tag/{tag_id}", name = "商品标签详情")
	public ResponseEntity<ApiResult<Object>> getTagsInfo(HttpServletRequest request,
			@PathVariable("tag_id") String tagIdStr) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		long tagId;
		try {
			tagId = tagIdStr == null ? 0L : Long.parseLong(tagIdStr.trim());
		} catch (NumberFormatException e) {
			tagId = 0L;
		}
		if (tagId <= 0) {
			tagId = 0L;
		}
		Object data = itemsTagsQueryService.getTagInfoByTagId(tagId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.tag.rel")
	@PostMapping(value = "/reltag", name = "商品关联标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> tagsRelItem(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		itemsTagsRelService.relateTags(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.tagsearch")
	@GetMapping(value = "/tagsearch", name = "按标签查商品")
	public ResponseEntity<ApiResult<List<String>>> getItemIdsByTagids(HttpServletRequest request,
			@RequestParam(name = "tag_id", required = false) List<String> tagIdParams) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		if (tagIdParams == null || tagIdParams.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		List<Long> tagIds = parsePositiveTagIds(tagIdParams);
		if (tagIds.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		long companyId = readRequiredLong(ud, "company_id");
		List<String> data = itemsTagsQueryService.getItemIdsByTagIds(companyId, tagIds);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static List<Long> parsePositiveTagIds(List<String> tagIdParams) {
		List<Long> out = new ArrayList<>();
		for (String token : tagIdParams) {
			if (token == null) {
				continue;
			}
			String t = token.trim();
			if (t.isEmpty() || "0".equals(t)) {
				continue;
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					continue;
				}
				out.add(v);
			} catch (NumberFormatException ignored) {
				// skip invalid token
			}
		}
		return out;
	}
}
