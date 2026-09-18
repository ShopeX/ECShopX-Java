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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.members.service.MemberTagsCreateService;
import cn.shopex.ecshopx.members.service.MemberTagsListParams;
import cn.shopex.ecshopx.members.service.MemberTagsListService;
import cn.shopex.ecshopx.members.service.MemberTagsRelUserService;
import cn.shopex.ecshopx.members.service.MemberTagsUpdateService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRelTagsUserIdsQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
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
@RestController("membersAdminV1MemberTags")
@RequestMapping("/api/v1/member")
public class MemberTagsController {

	private final MemberTagsCreateService memberTagsCreateService;
	private final MemberTagsRelUserService memberTagsRelUserService;
	private final MemberTagsUpdateService memberTagsUpdateService;
	private final MemberTagsListService memberTagsListService;
	private final AdminMemberRelTagsUserIdsQueryService adminMemberRelTagsUserIdsQueryService;
	private final LangueProperties langueProperties;

	public MemberTagsController(
			MemberTagsCreateService memberTagsCreateService,
			MemberTagsRelUserService memberTagsRelUserService,
			MemberTagsUpdateService memberTagsUpdateService,
			MemberTagsListService memberTagsListService,
			AdminMemberRelTagsUserIdsQueryService adminMemberRelTagsUserIdsQueryService,
			LangueProperties langueProperties) {
		this.memberTagsCreateService = memberTagsCreateService;
		this.memberTagsRelUserService = memberTagsRelUserService;
		this.memberTagsUpdateService = memberTagsUpdateService;
		this.memberTagsListService = memberTagsListService;
		this.adminMemberRelTagsUserIdsQueryService = adminMemberRelTagsUserIdsQueryService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "member.tag.add")
	@PostMapping(value = "/tag", name = "新增会员标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createTags(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		long companyId = ctx[0];
		long distributorId = ctx[1];

		String tagName = merged.get("tag_name") == null ? "" : merged.get("tag_name").toString().trim();
		if (!StringUtils.hasText(tagName)) {
			throw new BadRequestException("标签名称不能为空");
		}

		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row =
				memberTagsCreateService.createTags(companyId, distributorId, merged, requestLangTag);
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
		if (v instanceof short[] arr) {
			return arr.length > 0;
		}
		if (v instanceof byte[] arr) {
			return arr.length > 0;
		}
		if (v instanceof char[] arr) {
			return arr.length > 0;
		}
		if (v instanceof float[] arr) {
			return arr.length > 0;
		}
		if (v instanceof double[] arr) {
			return arr.length > 0;
		}
		if (v instanceof boolean[] arr) {
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

	@Activated(routeAlias = "member.tag.delete")
	@DeleteMapping(value = "/tag/{tag_id}", name = "删除会员标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTag(
			HttpServletRequest request, @PathVariable("tag_id") String tagId) {
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		memberTagsCreateService.deleteTag(ctx[0], ctx[1], tagId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.tag.update")
	@PutMapping(value = "/tag", name = "更新会员标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateTags(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		Map<String, Object> row = memberTagsUpdateService.updateTags(ctx[0], ctx[1], merged);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "member.tag.list")
	@GetMapping(value = "/tag", name = "会员标签列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagsList(HttpServletRequest request) {
		int page = parseTagsListPage(request.getParameter("page"));
		int pageSize = parseTagsListPageSize(request.getParameter("page_size"));
		String tagNameRaw = request.getParameter("tag_name");
		String tagName = StringUtils.hasText(tagNameRaw) ? tagNameRaw : null;
		String categoryIdRaw = request.getParameter("category_id");
		String categoryId = StringUtils.hasText(categoryIdRaw) ? categoryIdRaw : null;
		String tagStatusRaw = request.getParameter("tag_status");
		String tagStatus = StringUtils.hasText(tagStatusRaw) ? tagStatusRaw : null;
		MemberTagsListParams params = new MemberTagsListParams(page, pageSize, tagName, categoryId, tagStatus);
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data = memberTagsListService.getTagsList(ctx[0], ctx[1], params, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseTagsListPage(String raw) {
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

	private static int parseTagsListPageSize(String raw) {
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

	@Activated(routeAlias = "member.tag.get")
	@GetMapping(value = "/tag/{tag_id}", name = "会员标签详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getTagsInfo(
			HttpServletRequest request, @PathVariable("tag_id") String tagIdStr) {
		long tagId;
		try {
			tagId = tagIdStr == null ? 0L : Long.parseLong(tagIdStr.trim());
		} catch (NumberFormatException e) {
			tagId = 0L;
		}
		if (tagId <= 0) {
			tagId = 0L;
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		String lang = RequestLangTag.current(langueProperties);
		Object data = memberTagsListService.getTagsInfo(ctx[0], ctx[1], tagId, lang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.tag.del")
	@PostMapping(value = "/reltagdel", name = "删除会员标签关联", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> tagsRelUserDel(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		if (isEmptyBulkSelection(merged.get("user_id"))) {
			throw new BadRequestException("请选择会员");
		}
		if (isEmptyBulkSelection(merged.get("tag_id"))) {
			throw new BadRequestException("请选择标签");
		}
		memberTagsRelUserService.tagsRelUserDel(companyId, merged.get("user_id"), merged.get("tag_id"));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "member.tag.rel")
	@PostMapping(value = "/reltag", name = "关联会员标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> tagsRelUser(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		Object rawUserIds = merged.get("user_ids");
		Object rawTagIds = merged.get("tag_ids");
		if (isEmptyBulkSelection(rawUserIds)) {
			throw new BadRequestException("请选择会员");
		}
		if (isEmptyBulkSelection(rawTagIds)) {
			throw new BadRequestException("请选择标签");
		}
		memberTagsRelUserService.tagsRelUser(companyId, rawUserIds, rawTagIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static boolean isEmptyBulkSelection(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof Number n && n.longValue() == 0L) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			if ("0".equals(t)) {
				return true;
			}
			return false;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (v.getClass().isArray()) {
			return isZeroLengthArray(v);
		}
		return false;
	}

	private static boolean isZeroLengthArray(Object v) {
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		if (v instanceof long[] a) {
			return a.length == 0;
		}
		if (v instanceof int[] a) {
			return a.length == 0;
		}
		if (v instanceof short[] a) {
			return a.length == 0;
		}
		if (v instanceof byte[] a) {
			return a.length == 0;
		}
		if (v instanceof char[] a) {
			return a.length == 0;
		}
		if (v instanceof float[] a) {
			return a.length == 0;
		}
		if (v instanceof double[] a) {
			return a.length == 0;
		}
		if (v instanceof boolean[] a) {
			return a.length == 0;
		}
		// JVM array runtimes are only the nine primitive 1D kinds above or reference arrays (Object[] and
		// subtypes). isArray() therefore always matched above; default guards API stability if that ever
		// changes.
		return false;
	}

	@Activated(routeAlias = "member.tagsearch")
	@GetMapping(value = "/tagsearch", name = "标签筛选会员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Long>>> getUserIdsByTagids(HttpServletRequest request) {
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		String tagid = request.getParameter("tagid");
		List<Long> data = adminMemberRelTagsUserIdsQueryService.getUserIdsByTagids(companyId, tagid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.taggroup.add")
	@PostMapping(value = "/tag-group", name = "新增标签组", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createTagGroup(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		long companyId = ctx[0];
		long distributorId = ctx[1];

		Object gn = merged.get("group_name");
		String groupName = gn == null ? "" : gn.toString().trim();
		if (!StringUtils.hasText(groupName)) {
			throw new BadRequestException("标签组名称不能为空");
		}

		if (merged.containsKey("tag_ids")) {
			Object tagIdsVal = merged.get("tag_ids");
			if (tagIdsVal != null
					&& isTagIdsCommaSplitCandidate(tagIdsVal)
					&& !isListCollectionOrArray(tagIdsVal)) {
				String raw = tagIdsVal.toString();
				merged.put(
						"tag_ids",
						Arrays.stream(raw.split(","))
								.map(String::trim)
								.filter(StringUtils::hasText)
								.toList());
			}
		}

		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row =
				memberTagsCreateService.createTagGroup(companyId, distributorId, merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "member.taggroup.delete")
	@DeleteMapping(value = "/tag-group/{group_id}", name = "删除标签组", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTagGroup(
			HttpServletRequest request, @PathVariable("group_id") String groupId) {
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		long companyId = ctx[0];
		long distributorId = ctx[1];
		long gid = parseLongOrZero(groupId);
		memberTagsCreateService.deleteTagGroup(companyId, distributorId, gid);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.taggroup.list")
	@GetMapping(value = "/tag-group", name = "标签组列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagGroupList(HttpServletRequest request) {
		int page = parseTagsListPage(request.getParameter("page"));
		int pageSize = parseTagGroupListPageSize(request);
		String raw = request.getParameter("group_name");
		long companyId = readCompanyAndDistributorFromJwt(request)[0];
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				memberTagsListService.getTagGroupList(companyId, page, pageSize, raw, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseTagGroupListPageSize(HttpServletRequest request) {
		String raw = null;
		if (StringUtils.hasText(request.getParameter("pageSize"))) {
			raw = request.getParameter("pageSize");
		} else if (StringUtils.hasText(request.getParameter("page_size"))) {
			raw = request.getParameter("page_size");
		} else {
			return 20;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 20;
		}
		if (t.matches("^-?\\d+$")) {
			try {
				int v = Integer.parseInt(t);
				return v <= 0 ? 20 : v;
			} catch (NumberFormatException e) {
				return 20;
			}
		}
		return 20;
	}

	@Activated(routeAlias = "member.taggroup.update")
	@PutMapping(value = "/tag-group/{group_id}", name = "编辑标签组", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateTagGroup(
			HttpServletRequest request,
			@PathVariable("group_id") String groupId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long[] ctx = readCompanyAndDistributorFromJwt(request);
		long companyId = ctx[0];
		long distributorId = ctx[1];

		Object gn = merged.get("group_name");
		String groupName = gn == null ? "" : gn.toString().trim();
		if (!StringUtils.hasText(groupName)) {
			throw new BadRequestException("标签组名称不能为空");
		}

		if (merged.containsKey("deleteids")) {
			Object deleteIdsVal = merged.get("deleteids");
			if (deleteIdsVal != null
					&& isTagIdsCommaSplitCandidate(deleteIdsVal)
					&& !isListCollectionOrArray(deleteIdsVal)) {
				String raw = deleteIdsVal.toString();
				merged.put(
						"deleteids",
						Arrays.stream(raw.split(","))
								.map(String::trim)
								.filter(StringUtils::hasText)
								.toList());
			}
		}

		long groupIdParsed = parseLongOrZero(groupId);
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row =
				memberTagsCreateService.updateTagGroup(companyId, distributorId, groupIdParsed, merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}
}
