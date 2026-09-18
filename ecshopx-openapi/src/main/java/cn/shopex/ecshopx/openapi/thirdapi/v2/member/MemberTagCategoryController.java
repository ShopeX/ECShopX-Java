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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2MemberTagCategory")
@RequestMapping("/api/openapi/internal/v2")
public class MemberTagCategoryController extends OpenapiBaseController {

	private final OpenapiMemberTagCategoryCreatePort memberTagCategoryCreatePort;
	private final OpenapiMemberTagCategoryDeletePort memberTagCategoryDeletePort;
	private final OpenapiMemberTagCategoryUpdatePort memberTagCategoryUpdatePort;
	private final OpenapiMemberTagCategoryListPort memberTagCategoryListPort;

	public MemberTagCategoryController(
			OpenapiMemberTagCategoryCreatePort memberTagCategoryCreatePort,
			OpenapiMemberTagCategoryDeletePort memberTagCategoryDeletePort,
			OpenapiMemberTagCategoryUpdatePort memberTagCategoryUpdatePort,
			OpenapiMemberTagCategoryListPort memberTagCategoryListPort) {
		this.memberTagCategoryCreatePort = memberTagCategoryCreatePort;
		this.memberTagCategoryDeletePort = memberTagCategoryDeletePort;
		this.memberTagCategoryUpdatePort = memberTagCategoryUpdatePort;
		this.memberTagCategoryListPort = memberTagCategoryListPort;
	}

	@PostMapping(value = "/ecx.member.tagcategory.add", name = "开放接口新增会员标签分类")
	public Map<String, Object> createTagCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@RequestParam(name = "sort", required = false) String sortParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryNameRaw = OpenapiRequestParams.mergeString(categoryNameParam, body, "category_name");
		boolean sortParamPresent =
				(body != null && body.containsKey("sort")) || sortParam != null;
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		return memberTagCategoryCreatePort.createTagCategory(
				companyId, categoryNameRaw, sortRaw, sortParamPresent);
	}

	@PostMapping(value = "/ecx.member.tagcategory.update", name = "开放接口修改会员标签分类")
	public Map<String, Object> updateTagCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@RequestParam(name = "sort", required = false) String sortParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		String categoryNameRaw = OpenapiRequestParams.mergeString(categoryNameParam, body, "category_name");
		boolean sortParamPresent =
				(body != null && body.containsKey("sort")) || sortParam != null;
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		return memberTagCategoryUpdatePort.updateTagCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, sortParamPresent);
	}

	@GetMapping(value = "/ecx.member.tagcategorys.get", name = "开放接口查询会员标签分类列表")
	public Map<String, Object> getTagCategoryList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		String categoryNameRaw =
				OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		return memberTagCategoryListPort.getTagCategoryList(
				companyId, pageSpec.page(), pageSpec.pageSize(), categoryNameRaw);
	}

	@DeleteMapping(value = "/ecx.member.tagcategory.delete", name = "开放接口删除会员标签分类")
	public Map<String, Object> deleteTagCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		return memberTagCategoryDeletePort.deleteTagCategory(companyId, categoryIdRaw);
	}
}
