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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryAddSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员标签分类V2")
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

	@Operation(
			summary = "开放接口新增会员标签分类",
			description =
					"按签名鉴权租户 company_id 新增会员标签分类（2 键双通道，body 含键优先、值 null 视为未传）："
							+ " category_name 必填非空白（mergeString trim；E5001「分类名称必填」；"
							+ " 同租户同名已存在 E5572「该分类名已存在」）；"
							+ " sort 键缺席/值 null 默认 1，键存在时 PHP intval（前导数字/负号截断，"
							+ " 空串/非数字→0，允许负数，非法不报错）。"
							+ " 成功 data 5 键：category_id/category_name/sort/created/updated 格式化时间。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tagcategory.add,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：INSERT tags_category 单行（无显式事务、无多语言分表写）；无异步、无第三方调用。",
			operationId = "ecx.member.tagcategory.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberTagCategoryAddDataDoc；"
								+ " 业务失败：status=fail, code=E5001 分类名称必填 / E5572 该分类名已存在，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagCategoryAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagCategoryAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"category_id\":1,\"category_name\":\"聚合测试类目\","
															+ "\"sort\":1,\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-category-exist",
											summary = "同名分类已存在",
											value =
													"{\"status\":\"fail\",\"code\":\"E5572\","
															+ "\"message\":\"该分类名已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-name-missing",
											summary = "category_name 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"分类名称必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.tagcategory.add", name = "开放接口新增会员标签分类")
	public Map<String, Object> createTagCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类名称；必填非空白（E5001）；同名已存在 E5572",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "category_name", required = false)
					String categoryNameParam,
			@Parameter(
							description = "排序值；键缺席/null 默认 1；PHP intval 前导数字（非法归 0，允许负数）",
							example = "1")
					@RequestParam(name = "sort", required = false)
					String sortParam,
			@RequestBody(
							description = "可选 JSON body；2 键与 query 同名双通道（body 含键优先）；sort 存在性语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagCategoryAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
