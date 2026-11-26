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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemMainCategoryDetailPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryAddSuccessEnvelopeDoc;
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
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-商品分类V2")
@OpenapiResponse
@RestController("openapiV2ItemsCategory")
@RequestMapping("/api/openapi/internal/v2")
public class ItemsCategoryController extends OpenapiBaseController {

	private final OpenapiItemCategoryCreatePort itemCategoryCreatePort;
	private final OpenapiItemCategoryDeletePort itemCategoryDeletePort;
	private final OpenapiItemCategoryUpdatePort itemCategoryUpdatePort;
	private final OpenapiItemCategoryListPort itemCategoryListPort;
	private final OpenapiItemMainCategoryDetailPort mainCategoryDetailPort;

	public ItemsCategoryController(
			OpenapiItemCategoryCreatePort itemCategoryCreatePort,
			OpenapiItemCategoryDeletePort itemCategoryDeletePort,
			OpenapiItemCategoryUpdatePort itemCategoryUpdatePort,
			OpenapiItemCategoryListPort itemCategoryListPort,
			OpenapiItemMainCategoryDetailPort mainCategoryDetailPort) {
		this.itemCategoryCreatePort = itemCategoryCreatePort;
		this.itemCategoryDeletePort = itemCategoryDeletePort;
		this.itemCategoryUpdatePort = itemCategoryUpdatePort;
		this.itemCategoryListPort = itemCategoryListPort;
		this.mainCategoryDetailPort = mainCategoryDetailPort;
	}

	@GetMapping(value = "/ecx.item.category.get", name = "开放接口查询商品分类列表")
	public List<Map<String, Object>> getItemCategoryList(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemCategoryList(companyId);
	}

	@GetMapping(value = "/ecx.item.maincategory.get", name = "开放接口查询商品主类目列表")
	public List<Map<String, Object>> getItemMainCategoryList(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemMainCategoryList(companyId);
	}

	@GetMapping(value = "/ecx.item.maincategory.detail.get", name = "开放接口获取商品主类目详情")
	public Object getItemMainCategoryDetail(
			HttpServletRequest request,
			@RequestParam(name = "category", required = false) String categoryParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryRaw = OpenapiRequestParams.originalString(categoryParam, body, "category");
		return mainCategoryDetailPort.getItemMainCategoryDetail(companyId, categoryRaw);
	}

	@Operation(
			summary = "开放接口新增商品分类",
			description =
					"按签名鉴权租户 company_id 新增商品分类（items_category，非主类目）："
							+ " category_name、parent_id 必填非空（缺失依次 E5001「分类名称必填」「上级分类ID必填」）。"
							+ " parent_id PHP intval 前导数字解析：0（含非数字）建一级分类（parent_id=0、level=1、path=自身 ID）；"
							+ ">0 须父级存在（查无 E5120「父级分类不存在」），level=父级+1、path=父 path,自身 ID。"
							+ " 同父级重名 E5120「分类名称已存在」；sort 可选缺省 0（非整数 E5120「排序必须大于等于0」）；"
							+ " image_url 可选无格式校验。四参数 originalString 双通道（body 含键取原始值不 trim）。"
							+ " 名称 trim 落库，zh-CN 分表同步；createClassification 其他异常统一包装 E5120（message 透传）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.category.add, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional 内 INSERT items_category + path 回填 + zh-CN 分表写；无异步、无第三方。",
			operationId = "ecx.item.category.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 分类名称必填·上级分类ID必填 /"
								+ " E5120 分类名称已存在·父级分类不存在·排序必须大于等于0·保存失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemCategoryAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemCategoryAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-name-duplicate",
											summary = "同父级分类重名",
											value =
													"{\"status\":\"fail\",\"code\":\"E5120\",\"message\":\"分类名称已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-parent-not-found",
											summary = "parent_id>0 但父级查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5120\",\"message\":\"父级分类不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-parent-required",
											summary = "parent_id 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"上级分类ID必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.item.category.add", name = "开放接口新增商品分类")
	public Map<String, Object> createItemCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类名称；必填非空，同父级不可重名（body 同名键优先，原始值不 trim）",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "category_name", required = false)
					String categoryNameParam,
			@Parameter(
							description = "排序值；可选缺省 0，非整数报错（body 同名键优先）",
							example = "1")
					@RequestParam(name = "sort", required = false)
					String sortParam,
			@Parameter(description = "分类图片 URL；可选（body 同名键优先）", example = "/fixtures/logo.png")
					@RequestParam(name = "image_url", required = false)
					String imageUrlParam,
			@Parameter(
							description = "上级分类 ID；必填非空；0 建一级分类，>0 须父级存在（body 同名键优先）",
							required = true,
							example = "0")
					@RequestParam(name = "parent_id", required = false)
					String parentIdParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；层级语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemCategoryAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryNameRaw = OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		String parentIdRaw = OpenapiRequestParams.originalString(parentIdParam, body, "parent_id");
		return itemCategoryCreatePort.createItemCategory(
				companyId, categoryNameRaw, sortRaw, imageUrlRaw, parentIdRaw);
	}

	@PostMapping(value = "/ecx.item.category.update", name = "开放接口更新商品分类")
	public Map<String, Object> updateItemCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@RequestParam(name = "sort", required = false) String sortParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		String categoryNameRaw = OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemCategoryUpdatePort.updateItemCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, imageUrlRaw);
	}

	@DeleteMapping(value = "/ecx.item.category.delete", name = "开放接口删除商品分类")
	public Map<String, Object> deleteItemCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		return itemCategoryDeletePort.deleteItemCategory(companyId, categoryIdRaw);
	}
}
