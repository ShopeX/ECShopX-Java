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
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemMaincategoryDetailGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemMaincategoryDetailGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemMaincategoryGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemCategoryUpdateSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口查询商品分类列表",
			description =
					"按签名鉴权租户 company_id 返回全量商品分类树（不分页）："
							+ " items_category 过滤 is_main_category=0 + distributor_id=0，zh-CN 分表覆盖 category_name"
							+ " 后组树（根 parent_id=0）。data 为一级节点数组，节点 9 固定键 + 条件键 children（递归同构，"
							+ " 仅有子级时出现）：category_id/category_level/parent_id/sort 字符串化（null→空串）、"
							+ " category_name/path/image_url null→空串、created/updated yyyy-MM-dd HH:mm:ss（库值 null 为 null）。"
							+ " 无请求参数（query/body 均不读取）。DB 访问异常报 E5000（底层信息透传）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.category.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.category.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为分类树数组（见 EcxItemCategoryGetNodeDoc；"
								+ "无分类时 data=[]）；"
								+ " 失败：status=fail, code=E5000 DB 异常 / E4004 未授权，data=null。见 EcxItemCategoryGetFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemCategoryGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（一级节点含子级）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":[{\"category_id\":\"1\",\"category_name\":\"聚合测试类目\","
															+ "\"category_level\":\"1\",\"parent_id\":\"0\",\"path\":\"1\",\"sort\":\"1\","
															+ "\"image_url\":\"/fixtures/logo.png\","
															+ "\"created\":\"2023-11-15 06:13:21\",\"updated\":\"2023-11-15 06:13:21\","
															+ "\"children\":[{\"category_id\":\"2\",\"category_name\":\"聚合测试类目\","
															+ "\"category_level\":\"2\",\"parent_id\":\"1\",\"path\":\"1,2\",\"sort\":\"0\","
															+ "\"image_url\":\"\",\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}]}]}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无分类",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.item.category.get", name = "开放接口查询商品分类列表")
	public List<Map<String, Object>> getItemCategoryList(
			@Parameter(hidden = true) HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemCategoryList(companyId);
	}

	@Operation(
			summary = "开放接口查询商品主类目列表",
			description =
					"按签名鉴权租户 company_id 返回全量商品主类目树（不分页）："
							+ " items_category 过滤 is_main_category=1 + distributor_id=0，zh-CN 分表覆盖 category_name"
							+ " 后组树（根 parent_id=0）。与 ecx.item.category.get 共用同一服务，仅主类目过滤值不同。"
							+ " data 为一级节点数组，节点 9 固定键 + 条件键 children（递归同构，仅有子级时出现）："
							+ " category_id/category_level/parent_id/sort 字符串化（null→空串）、"
							+ " category_name/path/image_url null→空串、created/updated yyyy-MM-dd HH:mm:ss（库值 null 为 null）。"
							+ " 无请求参数（query/body 均不读取）。DB 访问异常报 E5000（底层信息透传）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.maincategory.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.maincategory.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为主类目树数组（见 EcxItemCategoryGetNodeDoc；"
								+ "无主类目时 data=[]）；"
								+ " 失败：status=fail, code=E5000 DB 异常 / E4004 未授权，data=null。见 EcxItemCategoryGetFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemMaincategoryGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（一级主类目含子级）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":[{\"category_id\":\"1\",\"category_name\":\"HP\","
															+ "\"category_level\":\"1\",\"parent_id\":\"0\",\"path\":\"1\",\"sort\":\"1\","
															+ "\"image_url\":\"/fixtures/logo.png\","
															+ "\"created\":\"2023-11-15 06:13:21\",\"updated\":\"2023-11-15 06:13:21\","
															+ "\"children\":[{\"category_id\":\"2\",\"category_name\":\"上衣\","
															+ "\"category_level\":\"2\",\"parent_id\":\"1\",\"path\":\"1,2\",\"sort\":\"0\","
															+ "\"image_url\":\"\",\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}]}]}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无主类目",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.item.maincategory.get", name = "开放接口查询商品主类目列表")
	public List<Map<String, Object>> getItemMainCategoryList(
			@Parameter(hidden = true) HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemMainCategoryList(companyId);
	}

	@Operation(
			summary = "开放接口获取商品主类目详情",
			description =
					"按签名鉴权租户 company_id 查询三级主类目详情：category 为「一级->二级->三级」名称路径"
							+ "（必填非空否则 E5001「主类目名称必填」；恰好 3 段否则 E5130「主类目名称格式错误」）。"
							+ " 逐级按名称查（is_main_category=1、distributor_id=0），任一级查无报 E5131 对应文案；"
							+ " 三级命中后复核失败/详情为空返回 data=[]（PHP 旧契约空数组，不报错）。"
							+ " data 8 固定键（category_id/sort/category_level 字符串化、created/updated 格式化可 null）+"
							+ " 条件键 goods_params（attribute_type=item_params）/goods_spec（其余类型，attribute_id 倒序），"
							+ " 属性行 8 键 + attribute_values 嵌套（total_count 字符串 + list 7 键/值，sort 升序 LIMIT 100）。"
							+ " DB 异常 E5000 透传。category originalString 双通道。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.maincategory.detail.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.maincategory.detail.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemMaincategoryDetailGetDataDoc"
								+ "（复核失败/详情为空时 data=[]）；"
								+ " 业务失败：status=fail, code=E5001 主类目名称必填 / E5130 主类目名称格式错误 /"
								+ " E5131 各级类目名称未查询到相关数据 / E5000 DB 异常，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemMaincategoryDetailGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemMaincategoryDetailGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（含规格属性与值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"category_id\":\"1\",\"category_name\":\"HP\",\"sort\":\"1\","
															+ "\"category_level\":\"3\",\"path\":\"1\",\"image_url\":\"/fixtures/logo.png\","
															+ "\"created\":\"2023-11-15 06:13:21\",\"updated\":\"2023-11-15 06:13:21\","
															+ "\"goods_spec\":[{\"attribute_id\":\"1\",\"attribute_name\":\"尺码\","
															+ "\"attribute_memo\":\"\",\"attribute_sort\":\"1\",\"is_show\":\"true\","
															+ "\"is_image\":\"false\",\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\","
															+ "\"attribute_values\":{\"total_count\":\"1\",\"list\":["
															+ "{\"attribute_value_id\":\"100\",\"attribute_id\":\"1\","
															+ "\"attribute_value\":\"XXL\",\"sort\":\"0\",\"image_url\":null,"
															+ "\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}]}}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "复核失败/详情为空（PHP 旧契约空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-format",
											summary = "路径段数≠3",
											value =
													"{\"status\":\"fail\",\"code\":\"E5130\","
															+ "\"message\":\"主类目名称格式错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-level3-not-found",
											summary = "三级名称查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5131\","
															+ "\"message\":\"三级类目名称未查询到相关数据\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.item.maincategory.detail.get", name = "开放接口获取商品主类目详情")
	public Object getItemMainCategoryDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "三级主类目名称路径（一级->二级->三级）；必填（body 同名键优先，原始值不 trim）",
							required = true,
							example = "HP->上衣->HP")
					@RequestParam(name = "category", required = false)
					String categoryParam,
			@RequestBody(
							description = "可选 JSON body；category 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemMaincategoryDetailGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口更新商品分类",
			description =
					"按签名鉴权租户 company_id 更新商品分类（items_category）："
							+ " category_id、category_name 必填非空（缺失依次 E5001「分类ID必填」「分类名称必填」）；"
							+ " category_id trim 后非整数/租户内查无 E5120「更新的分类不存在」。"
							+ " category_name 全量覆盖（trim 落库 + zh-CN 分表 upsert）；改名时同父级/同店铺范围重名"
							+ "（排除自身）E5120「分类名称不能重复」（与原名相同跳过查重）。"
							+ " sort 可选（非整数 E5120「排序必须大于等于0」）；image_url 可选键不存在不改动；"
							+ " 服务端固定附带 customize_page_id=0 重置。四参数 originalString 双通道（body 含键取原始值）。"
							+ " 更新后回读分类详情校验存在性，并触发开放平台类目同步 dispatch（授权允许时异步外发）。"
							+ " 其他异常统一包装 E5120（message 透传）。成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.category.update, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：UPDATE items_category（部分字段）+ zh-CN 分表 upsert + 条件性开放平台类目同步事件。",
			operationId = "ecx.item.category.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 分类ID必填·分类名称必填 /"
								+ " E5120 更新的分类不存在·分类名称不能重复·排序必须大于等于0，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemCategoryUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemCategoryUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "category_id 非整数/查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5120\",\"message\":\"更新的分类不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-name-duplicate",
											summary = "改名后与同父级分类重名",
											value =
													"{\"status\":\"fail\",\"code\":\"E5120\",\"message\":\"分类名称不能重复\",\"data\":null}"),
									@ExampleObject(
											name = "fail-name-required",
											summary = "category_name 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"分类名称必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.item.category.update", name = "开放接口更新商品分类")
	public Map<String, Object> updateItemCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类 ID；必填非空（body 同名键优先，原始值不 trim）",
							required = true,
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@Parameter(
							description = "分类名称；必填非空，全量覆盖，改名不可与同父级重名（body 同名键优先）",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "category_name", required = false)
					String categoryNameParam,
			@Parameter(description = "排序值；可选，非整数报错（body 同名键优先）", example = "1")
					@RequestParam(name = "sort", required = false)
					String sortParam,
			@Parameter(description = "分类图片 URL；可选，键不存在不改动（body 同名键优先）", example = "/fixtures/logo.png")
					@RequestParam(name = "image_url", required = false)
					String imageUrlParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；校验见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemCategoryUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		String categoryNameRaw = OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemCategoryUpdatePort.updateItemCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, imageUrlRaw);
	}

	@Operation(
			summary = "开放接口删除商品分类",
			description =
					"按签名鉴权租户 company_id 删除商品分类（含子级/孙级级联）："
							+ " category_id 必填非空（缺失 E5001「分类ID必填」）；trim 后非整数/空白视为无操作直接成功"
							+ "（PHP 旧契约）。删除守卫：分类树（自身+子+孙）经 items_rel_cats 关联的商品存在报"
							+ " E5123「分类下存在商品」；主类目引用（items.item_main_cat_id 含子孙）存在报"
							+ " E5123「类目下存在商品」。通过后删关联行、自身分类、直接子级与孙级分类。"
							+ " 查无该 ID 不报错（DELETE 0 行仍成功）；其他异常统一 E5123（如「删除失败」）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " category_id originalString 双通道（body 含键取原始值不 trim）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.category.delete, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：DELETE items_rel_cats 关联行 + items_category（自身/子/孙）；无异步、无第三方调用。",
			operationId = "ecx.item.category.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}（含非整数无操作/查无场景）；"
								+ " 业务失败：status=fail, code=E5001 分类ID必填 / E5123 分类下存在商品·类目下存在商品·删除失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemCategoryDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemCategoryDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功（非整数 ID 亦同形无操作成功）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-id-required",
											summary = "category_id 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"分类ID必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-items-in-category",
											summary = "分类树下有商品在用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5123\",\"message\":\"分类下存在商品\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.item.category.delete", name = "开放接口删除商品分类")
	public Map<String, Object> deleteItemCategory(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "分类 ID；必填非空；非整数视为无操作成功；删除级联子/孙级（body 同名键优先）",
							required = true,
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@RequestBody(
							description = "可选 JSON body；category_id 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemCategoryDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		return itemCategoryDeletePort.deleteItemCategory(companyId, categoryIdRaw);
	}
}
