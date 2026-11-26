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

	@GetMapping(value = "/ecx.distributor_item.list", name = "开放接口查询店铺商品列表")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "item_code", required = false) String itemCodeParam,
			@RequestParam(name = "item_name", required = false) String itemNameParam,
			@RequestParam(name = "goods_can_sale", required = false) String goodsCanSaleParam,
			@RequestParam(name = "is_total_store", required = false) String isTotalStoreParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PatchMapping(value = "/ecx.distributor_item.update", name = "开放接口更新店铺商品")
	public Map<String, Object> update(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "item_code", required = false) String itemCodeParam,
			@RequestParam(name = "is_can_sale", required = false) String isCanSaleParam,
			@RequestParam(name = "is_total_store", required = false) String isTotalStoreParam,
			@RequestParam(name = "store", required = false) String storeParam,
			@RequestParam(name = "price", required = false) String priceParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
