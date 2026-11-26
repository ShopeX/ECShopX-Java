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
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandAddSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemBrandGetSuccessEnvelopeDoc;
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

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-商品品牌V2")
@OpenapiResponse
@RestController("openapiV2ItemsAttributes")
@RequestMapping("/api/openapi/internal/v2")
public class ItemsAttributesController extends OpenapiBaseController {

	private final OpenapiItemBrandCreatePort itemBrandCreatePort;
	private final OpenapiItemBrandDeletePort itemBrandDeletePort;
	private final OpenapiItemBrandUpdatePort itemBrandUpdatePort;
	private final OpenapiItemBrandListPort itemBrandListPort;

	public ItemsAttributesController(
			OpenapiItemBrandCreatePort itemBrandCreatePort,
			OpenapiItemBrandDeletePort itemBrandDeletePort,
			OpenapiItemBrandUpdatePort itemBrandUpdatePort,
			OpenapiItemBrandListPort itemBrandListPort) {
		this.itemBrandCreatePort = itemBrandCreatePort;
		this.itemBrandDeletePort = itemBrandDeletePort;
		this.itemBrandUpdatePort = itemBrandUpdatePort;
		this.itemBrandListPort = itemBrandListPort;
	}

	@Operation(
			summary = "开放接口查询商品品牌列表",
			description =
					"按签名鉴权租户 company_id 分页返回商品品牌（items_attributes，attribute_type=brand，created 倒序）："
							+ " data 含 total_count、is_last_page（1/0）、pager 与 list（5 键/行）——"
							+ " brand_id（attribute_id 整型）、brand_name（zh-CN 分表非空覆盖）、image_url（可 null）、"
							+ " created/updated（yyyy-MM-dd HH:mm:ss，库值 null 时为 null）。"
							+ " 分页：page 缺省 1（<1 归一 1）；page_size 缺省 20、<20 归一 20；解析后 ≥500 报"
							+ " E5002「每页的大小最大为500！」（500 本身也报错）。两参数双通道（body 含键取原始值）。"
							+ " DB 访问异常报 E5000（底层异常信息透传）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.brand.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.brand.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemBrandGetDataDoc"
								+ "（无品牌时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E5002 每页的大小最大为500！ / E5000 DB 异常，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemBrandGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemBrandGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"brand_id\":1,\"brand_name\":\"FixtureBrand\","
															+ "\"image_url\":\"/fixtures/logo.png\","
															+ "\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无品牌",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.item.brand.get", name = "开放接口查询商品品牌列表")
	public Map<String, Object> getItemBrandList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemBrandGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ItemBrandListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ItemBrandListParams.resolve(pageParam, pageSizeParam, body);
		return itemBrandListPort.getItemBrandList(
				companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@Operation(
			summary = "开放接口新增商品品牌",
			description =
					"按签名鉴权租户 company_id 新增商品品牌（items_attributes，attribute_type 固定 brand）："
							+ " brand_name 必填非空（缺失 E5001「品牌名称必填」；落库前 trim），租户内重名 E5110「品牌名称不能重复」；"
							+ " image_url 可选无格式校验。两参数 originalString 双通道（body 含键取原始值不 trim）。"
							+ " 落库固定 shop_id=0、distributor_id=0、is_show=true、attribute_sort=1、无属性值；"
							+ " zh-CN 多语言分表同步写。createAttr 其他异常统一包装 E5110（message 透传，如「创建属性失败」）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.brand.add, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional 内 INSERT items_attributes + zh-CN 分表写；无异步、无第三方调用。",
			operationId = "ecx.item.brand.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 品牌名称必填 / E5110 品牌名称不能重复·创建属性失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemBrandAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemBrandAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-name-required",
											summary = "brand_name 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"品牌名称必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-name-duplicate",
											summary = "租户内品牌重名",
											value =
													"{\"status\":\"fail\",\"code\":\"E5110\",\"message\":\"品牌名称不能重复\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.item.brand.add", name = "开放接口新增商品品牌")
	public Map<String, Object> createItemBrand(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "品牌名称；必填非空，租户内不可重名（body 同名键优先，原始值不 trim）",
							required = true,
							example = "FixtureBrand")
					@RequestParam(name = "brand_name", required = false)
					String brandNameParam,
			@Parameter(description = "品牌 Logo URL；可选（body 同名键优先）", example = "/fixtures/logo.png")
					@RequestParam(name = "image_url", required = false)
					String imageUrlParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemBrandAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandNameRaw = OpenapiRequestParams.originalString(brandNameParam, body, "brand_name");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemBrandCreatePort.createItemBrand(companyId, brandNameRaw, imageUrlRaw);
	}

	@Operation(
			summary = "开放接口删除商品品牌",
			description =
					"按签名鉴权租户 company_id 删除商品品牌（items_attributes + items_attribute_values）："
							+ " brand_id 必填非空（缺失 E5001「品牌ID必填」）；trim 后非整数/空白视为无操作直接成功"
							+ "（PHP 旧契约，不报错）。删除前校验：有关联普通商品报 E5113「有关联商品，请先处理关联的商品」；"
							+ " 有关联积分商城商品报 E5113「有关联积分商城商品，请先处理关联的积分商城商品」；"
							+ " 其他删除异常统一 E5113（如「删除失败」）。查无该 ID 不报错（DELETE 0 行仍成功）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " brand_id originalString 双通道（body 含键取原始值不 trim）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.brand.delete, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：DELETE items_attributes / items_attribute_values（按租户+attribute_id）；"
							+ " 无异步、无第三方调用。",
			operationId = "ecx.item.brand.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}（含非整数无操作/查无场景）；"
								+ " 业务失败：status=fail, code=E5001 品牌ID必填 / E5113 有关联商品·有关联积分商城商品·删除失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemBrandDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemBrandDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功（非整数 ID 亦同形无操作成功）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-id-required",
											summary = "brand_id 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"品牌ID必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-associated-items",
											summary = "品牌有关联商品在用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5113\","
															+ "\"message\":\"有关联商品，请先处理关联的商品\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.item.brand.delete", name = "开放接口删除商品品牌")
	public Map<String, Object> deleteItemBrand(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "品牌 ID（attribute_id）；必填非空；非整数视为无操作成功（body 同名键优先）",
							required = true,
							example = "1")
					@RequestParam(name = "brand_id", required = false)
					String brandIdParam,
			@RequestBody(
							description = "可选 JSON body；brand_id 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemBrandDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandIdRaw = OpenapiRequestParams.originalString(brandIdParam, body, "brand_id");
		return itemBrandDeletePort.deleteItemBrand(companyId, brandIdRaw);
	}

	@PostMapping(value = "/ecx.item.brand.update", name = "开放接口更新商品品牌")
	public Map<String, Object> updateItemBrand(
			HttpServletRequest request,
			@RequestParam(name = "brand_id", required = false) String brandIdParam,
			@RequestParam(name = "brand_name", required = false) String brandNameParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandIdRaw = OpenapiRequestParams.originalString(brandIdParam, body, "brand_id");
		String brandNameRaw = OpenapiRequestParams.originalString(brandNameParam, body, "brand_name");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemBrandUpdatePort.updateItemBrand(companyId, brandIdRaw, brandNameRaw, imageUrlRaw);
	}
}
