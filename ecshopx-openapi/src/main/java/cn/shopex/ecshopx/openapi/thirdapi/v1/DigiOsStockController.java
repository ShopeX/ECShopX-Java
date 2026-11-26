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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDigiOsProductStockPushPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDigiosStockPushBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDigiosStockPushSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP {@code OpenapiBundle/Http/ThirdApi/V1/Action/DigiOsStock}。 */
@Tag(name = "开放接口-DigiOS库存")
@OpenapiResponse
@RestController("openapiV1DigiOsStock")
@RequestMapping("/api/openapi/internal/v1")
public class DigiOsStockController extends OpenapiBaseController {

	private final OpenapiDigiOsProductStockPushPort stockPushPort;

	public DigiOsStockController(OpenapiDigiOsProductStockPushPort stockPushPort) {
		this.stockPushPort = stockPushPort;
	}

	@Operation(
			summary = "DigiOS门店库存推送",
			description =
					"DigiOS 向云店批量推送门店库存：按签名鉴权租户 company_id，逐行以 sku_id（item_bn）定位货品、"
							+ " shop_code 定位门店后更新门店/货品库存。单次最多 200 条（可配）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.digios.product.stock_push, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " sku_list 双通道：body 含该键时取原始值（推荐直接传 JSON 数组）；否则读 query 字符串"
							+ "（须为 JSON 数组文本）。整体入参校验失败返回 E0001；行级失败不中断，"
							+ " 明细在成功 data.not_find_sku / update_error 中返回（信封恒 success）。"
							+ " 副作用：逐行写库更新库存（事务内 per-row）；写入站 API 日志 digios_api_log；无异步/第三方调用。",
			operationId = "ecx.digios.product.stock_push")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=更新成功，data 含 not_find_sku、"
								+ "not_update_sku（恒空）、update_error（行级失败明细，含「门店不存在」等）；"
								+ " 整体入参校验失败：status=fail, code=E0001（sku_list 必填且格式正确/库存数据为空/"
								+ "sku_list 单次最多 200 条/stock 必须为 0-999999999 的整数/shop_code 必填），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDigiosStockPushFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDigiosStockPushSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "推送完成（含行级失败明细）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"更新成功\","
															+ "\"data\":{\"not_find_sku\":[\"A2\"],\"not_update_sku\":[],"
															+ "\"update_error\":[{\"sku_id\":\"A1\",\"shop_code\":\"S02\","
															+ "\"error\":\"门店不存在\"}]}}"),
									@ExampleObject(
											name = "success-all-applied",
											summary = "全部行更新成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"更新成功\","
															+ "\"data\":{\"not_find_sku\":[],\"not_update_sku\":[],\"update_error\":[]}}"),
									@ExampleObject(
											name = "fail-invalid-sku-list",
											summary = "整体入参校验失败",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"sku_list 必填且格式正确\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.digios.product.stock_push", name = "DigiOS门店库存推送")
	public OpenapiEnvelope stockPush(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"库存行列表的 JSON 数组字符串，每行 {sku_id, stock, shop_code}；"
											+ "body 含 sku_list 键时忽略本参数（body 优先，可直接传数组）",
							example = "[{\"sku_id\":\"A1\",\"stock\":10,\"shop_code\":\"S01\"}]")
					@RequestParam(name = "sku_list", required = false)
					String skuListParam,
			@RequestBody(
							description =
									"可选 JSON body；含 sku_list 键时取原始值（推荐数组，也接受 JSON 数组字符串），"
											+ "行字段与校验见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxDigiosStockPushBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Object skuListRaw = resolveSkuList(skuListParam, body);
		Map<String, Object> data = stockPushPort.pushStoreStock(companyId, skuListRaw);
		return new OpenapiEnvelope("success", "E0000", "更新成功", data);
	}

	private static Object resolveSkuList(String skuListParam, Map<String, Object> body) {
		if (body != null && body.containsKey("sku_list")) {
			return body.get("sku_list");
		}
		return OpenapiRequestParams.mergeRequiredString(skuListParam, body, "sku_list", "sku_list 必填且格式正确");
	}
}
