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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.TagsCategoryCreateService;
import cn.shopex.ecshopx.members.service.TagsCategoryListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
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
@RestController("membersAdminV1TagsCategory")
@RequestMapping("/api/v1/member/tagcategory")
public class TagsCategoryController {

	private final TagsCategoryCreateService tagsCategoryCreateService;
	private final TagsCategoryListService tagsCategoryListService;

	public TagsCategoryController(
			TagsCategoryCreateService tagsCategoryCreateService,
			TagsCategoryListService tagsCategoryListService) {
		this.tagsCategoryCreateService = tagsCategoryCreateService;
		this.tagsCategoryListService = tagsCategoryListService;
	}

	@Activated(routeAlias = "member.tagcategory.add")
	@PostMapping(name = "新增标签分类", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createTagsCategory(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		long companyId = ctx[0];

		String categoryName =
				merged.get("category_name") == null ? "" : merged.get("category_name").toString().trim();

		Long sortResolved = null;
		if (merged.containsKey("sort") && merged.get("sort") != null) {
			String sortStr = String.valueOf(merged.get("sort")).trim();
			if (StringUtils.hasText(sortStr)) {
				try {
					sortResolved = Long.parseLong(sortStr);
				} catch (NumberFormatException e) {
					sortResolved = null;
				}
			}
		}

		expandRelTagIdsCommaSeparatedInMerged(merged);
		Object relRaw = merged.get("relTagIds");
		List<Long> relTagIds = tagsCategoryCreateService.normalizeRelTagIds(relRaw);

		if (companyId <= 0L) {
			throw new ResourceException("缺少企业id");
		}
		if (!StringUtils.hasText(categoryName)) {
			throw new ResourceException("分类名称必填");
		}

		Map<String, Object> row =
				tagsCategoryCreateService.createTagsCategory(companyId, categoryName, sortResolved, relTagIds);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	private static long parseLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private long[] readCompanyAndDistributorFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		long distributorId = parseLongOrZero(jwt.get("distributor_id"));
		return new long[] {companyId, distributorId};
	}

	private static boolean isTagIdsCommaSplitCandidate(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		if (v instanceof long[] arr) {
			return arr.length > 0;
		}
		if (v instanceof int[] arr) {
			return arr.length > 0;
		}
		if (v instanceof Number) {
			return true;
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		return false;
	}

	private static boolean isListCollectionOrArray(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Collection<?>) {
			return true;
		}
		return v.getClass().isArray();
	}

	private static void expandRelTagIdsCommaSeparatedInMerged(Map<String, Object> merged) {
		if (!merged.containsKey("relTagIds")) {
			return;
		}
		Object tagIdsVal = merged.get("relTagIds");
		if (tagIdsVal != null
				&& isTagIdsCommaSplitCandidate(tagIdsVal)
				&& !isListCollectionOrArray(tagIdsVal)) {
			String raw = tagIdsVal.toString();
			merged.put(
					"relTagIds",
					Arrays.stream(raw.split(","))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.toList());
		}
	}

	private static boolean relTagIdsLookPresent(Object relRaw) {
		if (relRaw == null) {
			return false;
		}
		if (relRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			return true;
		}
		if (relRaw instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (relRaw.getClass().isArray()) {
			if (relRaw instanceof Object[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof long[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof int[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof short[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof byte[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof char[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof double[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof float[] arr) {
				return arr.length > 0;
			}
			if (relRaw instanceof boolean[] arr) {
				return arr.length > 0;
			}
			return true;
		}
		if (relRaw instanceof Number) {
			return true;
		}
		return true;
	}

	@Activated(routeAlias = "member.tagcategory.delete")
	@DeleteMapping(
			value = "/{category_id}",
			name = "删除标签分类",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTagsCategory(
			HttpServletRequest request, @PathVariable("category_id") String categoryId) {
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		if (companyId <= 0L) {
			throw new ResourceException("缺少企业id");
		}
		if (!StringUtils.hasText(categoryId == null ? "" : categoryId.trim())) {
			throw new ResourceException("分类Id必填");
		}
		long categoryIdLong = LeadingNumberParser.parseAsLong(categoryId);
		tagsCategoryCreateService.deleteTagsCategory(companyId, categoryIdLong);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "member.tagcategory.update")
	@PutMapping(value = "/{category_id}", name = "更新标签分类", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateTagsCategory(
			HttpServletRequest request,
			@PathVariable("category_id") String categoryId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		expandRelTagIdsCommaSeparatedInMerged(merged);
		Object relRaw = merged.get("relTagIds");
		List<Long> relTagIds = tagsCategoryCreateService.normalizeRelTagIds(relRaw);

		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		if (companyId <= 0L) {
			throw new ResourceException("缺少企业id");
		}

		String categoryName =
				merged.get("category_name") == null ? "" : merged.get("category_name").toString().trim();
		if (!StringUtils.hasText(categoryName)) {
			throw new ResourceException("分类名称必填");
		}

		if (!StringUtils.hasText(categoryId == null ? "" : categoryId.trim())) {
			throw new ResourceException("分类Id必填");
		}
		long categoryIdLong;
		try {
			categoryIdLong = Long.parseLong(categoryId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}

		boolean sortColumnRequested = merged.containsKey("sort") && merged.get("sort") != null;
		Long sortResolved = null;
		if (sortColumnRequested) {
			String sortStr = String.valueOf(merged.get("sort")).trim();
			if (StringUtils.hasText(sortStr)) {
				try {
					sortResolved = Long.parseLong(sortStr);
				} catch (NumberFormatException e) {
					sortResolved = null;
				}
			}
		}

		boolean relExplicitPresent = relTagIdsLookPresent(relRaw);
		Map<String, Object> row =
				tagsCategoryCreateService.updateTagsCategory(
						companyId,
						categoryIdLong,
						categoryName,
						sortColumnRequested,
						sortResolved,
						relExplicitPresent,
						relTagIds);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "member.tagcategory.list")
	@GetMapping(name = "标签分类列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagsCategoryList(HttpServletRequest request) {
		int page = parseTagCategoryListPage(request.getParameter("page"));
		int pageSize = parseTagCategoryListPageSize(request.getParameter("pageSize"));
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		Map<String, Object> data = tagsCategoryListService.getTagsCategoryList(companyId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseTagCategoryListPage(String raw) {
		if (!StringUtils.hasText(raw)) {
			return Math.max(1, 1);
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return Math.max(1, 1);
		}
		if (t.matches("^-?\\d+$")) {
			try {
				return Math.max(1, Integer.parseInt(t));
			} catch (NumberFormatException e) {
				return 1;
			}
		}
		return 1;
	}

	private static int parseTagCategoryListPageSize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return -1;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return -1;
		}
		if (t.matches("^-?\\d+$")) {
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}

	@Activated(routeAlias = "member.tagcategory.get")
	@GetMapping(
			value = "/{category_id}",
			name = "标签分类详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getTagsCategoryInfo(
			HttpServletRequest request, @PathVariable("category_id") String categoryId) {
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		Object payload = tagsCategoryListService.getTagsCategoryInfo(companyId, categoryId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}
}
