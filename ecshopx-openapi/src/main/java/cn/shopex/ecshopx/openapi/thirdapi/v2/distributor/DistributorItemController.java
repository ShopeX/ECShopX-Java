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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdateCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemDownloadBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemDownloadSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorItemUpdateSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-店铺商品V2")
@OpenapiResponse
@RestController("openapiV2DistributorItem")
@RequestMapping("/api/openapi/internal/v2")
public class DistributorItemController extends OpenapiBaseController {

	private final OpenapiDistributorItemListPort distributorItemListPort;
	private final OpenapiDistributorItemUpdatePort distributorItemUpdatePort;
	private final OpenapiDistributorItemDownloadPort distributorItemDownloadPort;

	public DistributorItemController(
			OpenapiDistributorItemListPort distributorItemListPort,
			OpenapiDistributorItemUpdatePort distributorItemUpdatePort,
			OpenapiDistributorItemDownloadPort distributorItemDownloadPort) {
		this.distributorItemListPort = distributorItemListPort;
		this.distributorItemUpdatePort = distributorItemUpdatePort;
		this.distributorItemDownloadPort = distributorItemDownloadPort;
	}

	@Operation(
			summary = "开放接口查询店铺商品列表",
			description =
					"按签名鉴权租户 company_id 分页返回店铺商品（SPU 维度，is_default=1 + item_type=normal，item_id 倒序）："
							+ " shop_code 键必须存在（缺失 E5001「店铺号参数错误」）；distributor_id 非空时优先按其定位店铺；"
							+ " 店铺查无/非整数返回空列表不报错。过滤：item_code=item_bn 精确、item_name contains 预查"
							+ "（查无即空列表）、status=approve_status 精确；goods_can_sale/is_total_store 读取但当前不参与过滤。"
							+ " 分页 page 缺省 1、page_size 缺省 20（<20 归一 20；≥500 报 E0001「每页的大小最大为500！」含 500）。"
							+ " data 行 9 键：distributor_id/item_id（整型）、item_code/item_name（默认语言分表覆盖）、"
							+ " store/price（元两位小数字符串；店铺独立库存 is_total_store=0 时取店铺 SPU 行覆盖）、"
							+ " goods_can_sale（任一 SKU 店铺行可售即 1）、is_total_store（无店铺行默认 1）、status。"
							+ " 九参数双通道（body 含键取原始值）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor_item.list, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.distributor_item.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxDistributorItemListDataDoc"
								+ "（店铺查无/无商品时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E5001 店铺号参数错误 / E0001 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorItemListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorItemListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（总部库存商品）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"distributor_id\":1001,\"item_id\":1001,\"item_code\":\"A1\","
															+ "\"item_name\":\"示例商品\",\"store\":10,\"price\":\"99.00\","
															+ "\"goods_can_sale\":1,\"is_total_store\":1,\"status\":\"onsale\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "店铺查无或无商品（不报错）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-missing-shop-code",
											summary = "shop_code 键缺失",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"店铺号参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.distributor_item.list", name = "开放接口查询店铺商品列表")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "店铺号；键必须存在否则「店铺号参数错误」；distributor_id 提供时被忽略（body 同名键优先）",
							required = true,
							example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@Parameter(description = "商品货号（item_bn 精确过滤）；可选（body 同名键优先）", example = "A1")
					@RequestParam(name = "item_code", required = false)
					String itemCodeParam,
			@Parameter(description = "商品名称模糊过滤；可选，查无匹配返回空列表（body 同名键优先）", example = "示例商品")
					@RequestParam(name = "item_name", required = false)
					String itemNameParam,
			@Parameter(description = "店铺可售过滤——当前实现读取但不参与过滤（body 同名键优先）", example = "1")
					@RequestParam(name = "goods_can_sale", required = false)
					String goodsCanSaleParam,
			@Parameter(description = "总部库存过滤——当前实现读取但不参与过滤（body 同名键优先）", example = "1")
					@RequestParam(name = "is_total_store", required = false)
					String isTotalStoreParam,
			@Parameter(
							description = "上下架状态精确过滤（approve_status）；可选（body 同名键优先）",
							schema = @Schema(allowableValues = {"onsale", "instock", "offline_sale", "only_show"}),
							example = "onsale")
					@RequestParam(name = "status", required = false)
					String statusParam,
			@Parameter(
							description = "店铺 ID；可选，非空时优先于 shop_code 定位店铺（body 同名键优先）",
							example = "1001")
					@RequestParam(name = "distributor_id", required = false)
					String distributorIdParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；过滤语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDistributorItemListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		if (OpenapiRequestParams.presentOptionalString(shopCodeParam, body, "shop_code").isEmpty()) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号参数错误");
		}

		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);

		return distributorItemListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.originalString(itemCodeParam, body, "item_code"),
				OpenapiRequestParams.originalString(itemNameParam, body, "item_name"),
				OpenapiRequestParams.originalString(goodsCanSaleParam, body, "goods_can_sale"),
				OpenapiRequestParams.originalString(isTotalStoreParam, body, "is_total_store"),
				OpenapiRequestParams.originalString(statusParam, body, "status"),
				OpenapiRequestParams.originalString(distributorIdParam, body, "distributor_id"));
	}

	@Operation(
			summary = "开放接口更新店铺商品",
			description =
					"按签名鉴权租户 company_id 部分更新店铺商品（distribution_distributor_items）："
							+ " shop_code/item_code 必填（mergeString），缺失依次 E5001「店铺号参数有误」「商品货号参数错误」；"
							+ " is_can_sale/is_total_store 键存在时仅 0/1（否则 E5001 对应文案）；store/price 键存在但非数值静默忽略"
							+ "（price 元 ×100 HALF_UP 转分）。无任何补丁字段直接成功（data=null）。"
							+ " 店铺查无 E5401「店铺找不到」、商品查无 E5101「商品找不到」；"
							+ " is_total_store 仅 SPU 级可改（SKU 行 E5410），改后同步整个 SPU 组；"
							+ " 总部发货生效值=1 时提交 store/price 报 E5410「商品总部发货无法更新库存和价格」；"
							+ " price 不得小于进行中拼团活动价（E0001 拼团守卫文案）。"
							+ " 店铺商品行不存在则新建（is_can_sale 缺省 false、store/price 缺省总部值、goods_can_sale=true）；"
							+ " is_can_sale 改后按 SPU 组合同步 goods_can_sale。成功恒 data=null、message=成功（Advice 包装）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor_item.update, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional 内 UPDATE/INSERT distribution_distributor_items（含 SPU 组同步）+"
							+ " store 提交时覆盖 Redis item_store 并发布库存变更事件；无第三方调用。",
			operationId = "ecx.distributor_item.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null；"
								+ " 业务失败：status=fail, code=E5001 参数错误 / E5401 店铺找不到 / E5101 商品找不到 /"
								+ " E5410 是否总部发货, 当前只支持更新spu级商品·商品总部发货无法更新库存和价格 /"
								+ " E0001 拼团名称【…】的活动价格大于商品价格，请检查后再提交！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorItemUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorItemUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-total-store-limit",
											summary = "总部发货下提交库存/价格",
											value =
													"{\"status\":\"fail\",\"code\":\"E5410\","
															+ "\"message\":\"商品总部发货无法更新库存和价格\",\"data\":null}"),
									@ExampleObject(
											name = "fail-sku-total-store",
											summary = "SKU 行改总部发货",
											value =
													"{\"status\":\"fail\",\"code\":\"E5410\","
															+ "\"message\":\"是否总部发货, 当前只支持更新spu级商品\",\"data\":null}"),
									@ExampleObject(
											name = "fail-shop-not-found",
											summary = "shop_code 查无店铺",
											value =
													"{\"status\":\"fail\",\"code\":\"E5401\",\"message\":\"店铺找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PatchMapping(value = "/ecx.distributor_item.update", name = "开放接口更新店铺商品")
	public Map<String, Object> update(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "店铺号；必填（body 同名键优先）", required = true, example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@Parameter(description = "商品货号（item_bn）；必填（body 同名键优先）", required = true, example = "A1")
					@RequestParam(name = "item_code", required = false)
					String itemCodeParam,
			@Parameter(
							description = "是否上架；键存在时仅 0/1（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "1")
					@RequestParam(name = "is_can_sale", required = false)
					String isCanSaleParam,
			@Parameter(
							description = "是否总部发货；键存在时仅 0/1；仅 SPU 级可改；=1 时禁改库存/价格（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "0")
					@RequestParam(name = "is_total_store", required = false)
					String isTotalStoreParam,
			@Parameter(
							description = "店铺独立库存；数值字符串，非数值静默忽略；仅非总部发货可改（body 同名键优先）",
							example = "10")
					@RequestParam(name = "store", required = false)
					String storeParam,
			@Parameter(
							description = "店铺独立价格（元，×100 转分）；非数值静默忽略；不得小于拼团活动价（body 同名键优先）",
							example = "99.00")
					@RequestParam(name = "price", required = false)
					String priceParam,
			@RequestBody(
							description = "可选 JSON body；部分更新语义与键存在规则见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDistributorItemUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiDistributorItemUpdateCommand command =
				OpenapiThirdApiV2DistributorItemUpdateParams.resolve(
						shopCodeParam, itemCodeParam,
						isCanSaleParam, isTotalStoreParam,
						storeParam, priceParam, body);

		distributorItemUpdatePort.update(companyId, command);
		return null;
	}

	@Operation(
			summary = "开放接口生成店铺商品码",
			description =
					"按签名鉴权租户 company_id 生成/复用店铺商品小程序码图片 URL："
							+ " shop_code、item_code 必填非空白（mergeString 双通道，body 含键优先，null/空白不回退 query），"
							+ " 缺失依次报 E5001「店铺号参数有误」「商品货号参数错误」。"
							+ " 前置：租户须绑定 yykweishop 授权小程序（未绑定 E0001「没有开通此小程序，不能下载」）；"
							+ " shop_code 租户内查店铺（查无 E5401「未查询到该店铺」）；item_code 匹配 items.item_bn"
							+ "（item_id 升序 LIMIT 1，查无 E5101「商品找不到」）。"
							+ " 取码：Redis hash 缓存命中直接用缓存 uri；未命中调微信 getwxacodeunlimit"
							+ "（scene=id={itemId}&dtid={distributorId}，页面 pages/item/espier-detail，失败回退 pages/goodsdetail）"
							+ " 并上传存储（group=distributor_item_qr_code，jpg），上传失败吞掉写空串缓存。"
							+ " 成功 data 仅 url 一键（存储 domain + / + key；uri 空时退化为 domain）；Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor_item.download, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：微信第三方取码 + 图片上传存储 + 写 Redis 缓存（无 expire）；不写业务表。",
			operationId = "ecx.distributor_item.download.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data={url}；"
								+ " 业务失败：status=fail, code=E5001 店铺号参数有误·商品货号参数错误 / E5401 未查询到该店铺 /"
								+ " E5101 商品找不到 / E0001 没有开通此小程序，不能下载·微信取码失败文案，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorItemDownloadFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorItemDownloadSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "生成/复用商品码成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"url\":\"https://example.test/test-project/image/1/2025/01/01/"
															+ "distributor_item_qr_code/abc123test.jpg\"}}"),
									@ExampleObject(
											name = "fail-shop-not-found",
											summary = "shop_code 查无店铺",
											value =
													"{\"status\":\"fail\",\"code\":\"E5401\",\"message\":\"未查询到该店铺\",\"data\":null}"),
									@ExampleObject(
											name = "fail-item-not-found",
											summary = "item_code 查无商品",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-no-wxapp",
											summary = "租户未绑定 yykweishop 小程序",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"没有开通此小程序，不能下载\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.distributor_item.download", name = "开放接口生成店铺商品码")
	public Map<String, Object> download(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "店铺号（shop_code 精确匹配）；必填（body 同名键优先）",
							required = true,
							example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@Parameter(
							description = "商品货号（items.item_bn 精确匹配）；必填（body 同名键优先）",
							required = true,
							example = "A1")
					@RequestParam(name = "item_code", required = false)
					String itemCodeParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDistributorItemDownloadBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiDistributorItemDownloadCommand command =
				OpenapiThirdApiV2DistributorItemDownloadParams.resolve(
						shopCodeParam, itemCodeParam, body);
		return distributorItemDownloadPort.download(companyId, command);
	}
}
