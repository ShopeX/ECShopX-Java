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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagCategoryUpdateSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口修改会员标签分类",
			description =
					"按签名鉴权租户 company_id 修改会员标签分类（3 键双通道，body 含键优先、值 null 视为未传）："
							+ " category_id/category_name 必填非空白（mergeString trim；"
							+ " E5001「分类ID必填」/「分类名称必填」）；"
							+ " category_id trim+parseLong（非整数/查无/更新后回读为空 E5571「该标签分类不存在」）；"
							+ " category_name 恒写（同名排除自身冲突 E5572「该分类名已存在」，同名改自身放行）。"
							+ " sort 键存在且 PHP truthy（非空非\"0\"）才更新（PHP intval 前导数字截断，"
							+ " 允许负数，非法归 0 不报错）；键缺席/空/\"0\" 保留原排序（区别于新增默认 1）。"
							+ " 成功 data 为更新后回读 5 键行（同新增格式，created 不变）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member.tagcategory.update, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：UPDATE tags_category（category_name/updated + 条件 sort；无显式事务）；"
							+ " 无异步、无第三方调用。",
			operationId = "ecx.member.tagcategory.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为更新后 5 键行；"
								+ " 业务失败：status=fail, code=E5001 分类ID必填·分类名称必填 /"
								+ " E5571 该标签分类不存在 / E5572 该分类名已存在，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagCategoryUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagCategoryUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"category_id\":1,\"category_name\":\"聚合测试类目\","
															+ "\"sort\":1,\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "分类查无/ID 非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5571\","
															+ "\"message\":\"该标签分类不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-category-exist",
											summary = "同名分类冲突（排除自身）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5572\","
															+ "\"message\":\"该分类名已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.tagcategory.update", name = "开放接口修改会员标签分类")
	public Map<String, Object> updateTagCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类 ID；必填（E5001）；非整数/查无 E5571",
							required = true,
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@Parameter(
							description = "分类名称；必填（E5001）；恒写；同名冲突 E5572",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "category_name", required = false)
					String categoryNameParam,
			@Parameter(
							description = "排序值；键存在且非空非\"0\"才更新（PHP intval）；否则保留原值",
							example = "1")
					@RequestParam(name = "sort", required = false)
					String sortParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（body 含键优先）；sort 条件更新语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagCategoryUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		String categoryNameRaw = OpenapiRequestParams.mergeString(categoryNameParam, body, "category_name");
		boolean sortParamPresent =
				(body != null && body.containsKey("sort")) || sortParam != null;
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		return memberTagCategoryUpdatePort.updateTagCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, sortParamPresent);
	}

	@Operation(
			summary = "开放接口查询会员标签分类列表",
			description =
					"按签名鉴权租户 company_id 分页查询会员标签分类（tags_category）："
							+ " 3 键双通道（originalString，body 含键优先、值 null 视为未传、不 trim）。"
							+ " category_name PHP isset 非空语义（null/空串/\"0\" 忽略）时 LIKE %值% 模糊过滤"
							+ "（不 trim 原样进 LIKE）。无显式排序（库默认行序）。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E0001「每页的大小最大为500！」（复用运费模板分页解析器，ResourceException 通道），"
							+ " 钳制后 page≥1、page_size∈[20,499]。"
							+ " data 键序 total_count/is_last_page(0|1)/pager{page,page_size}/list；"
							+ " 行 5 键同新增/修改分类格式（category_id/category_name/sort/created/updated 格式化时间）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tagcategorys.get,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member.tagcategorys.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberTagCategoryListDataDoc；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagCategoryListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagCategoryListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"category_id\":1,\"category_name\":\"聚合测试类目\","
															+ "\"sort\":1,\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无分类（list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500（E0001 通道）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member.tagcategorys.get", name = "开放接口查询会员标签分类列表")
	public Map<String, Object> getTagCategoryList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码，默认 1；PHP 式解析后 max(…,1)", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E0001；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "分类名模糊过滤（LIKE %值%）；null/空串/\"0\" 忽略；不 trim",
							example = "聚合测试类目")
					@RequestParam(name = "category_name", required = false)
					String categoryNameParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagCategoryListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		String categoryNameRaw =
				OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		return memberTagCategoryListPort.getTagCategoryList(
				companyId, pageSpec.page(), pageSpec.pageSize(), categoryNameRaw);
	}

	@Operation(
			summary = "开放接口删除会员标签分类",
			description =
					"按签名鉴权租户 company_id 删除会员标签分类：category_id 必填非空白"
							+ "（mergeString 双通道 trim，body 含键优先、值 null 视为未传；缺失 E5001「分类ID必填」）。"
							+ " trim 后须可 parseLong（非整数按查无处理），company_id+category_id 定位 tags_category"
							+ "（LIMIT 1），非整数/查无/超 int 范围均 E5571「该标签分类不存在」。"
							+ " **执行顺序为先 DELETE 分类行、再检查分类下标签**：members_tags.category_id 计数 >0 时抛"
							+ " E0000+status=fail「删除失败,该分类下已有标签」（PHP 零码契约），"
							+ " @Transactional 回滚使分类恢复——对外表现为删除被拒、分类保留。"
							+ " 成功 data={status:true}（布尔）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member.tagcategory.delete, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional DELETE tags_category 单行（有标签时回滚）；"
							+ " 不删除/迁移分类下标签；无异步、无第三方调用。",
			operationId = "ecx.member.tagcategory.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 分类ID必填 / E5571 该标签分类不存在 /"
								+ " E0000（status=fail，PHP 零码契约）删除失败,该分类下已有标签，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagCategoryDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagCategoryDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "分类查无/ID 非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5571\","
															+ "\"message\":\"该标签分类不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-has-tags-zero-code",
											summary = "分类下已有标签（PHP 零码契约：code=E0000 但 status=fail；事务回滚）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\","
															+ "\"message\":\"删除失败,该分类下已有标签\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.member.tagcategory.delete", name = "开放接口删除会员标签分类")
	public Map<String, Object> deleteTagCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类 ID；必填（E5001）；非整数/查无 E5571；分类下有标签 E0000+fail（回滚保留）",
							required = true,
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@RequestBody(
							description = "可选 JSON body；category_id 与 query 同名双通道（mergeString trim，body 含键优先）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagCategoryDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		return memberTagCategoryDeletePort.deleteTagCategory(companyId, categoryIdRaw);
	}
}
