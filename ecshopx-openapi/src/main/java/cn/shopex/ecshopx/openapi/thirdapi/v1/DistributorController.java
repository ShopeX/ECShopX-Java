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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDistributorDetailV1BodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDistributorDetailV1SuccessEnvelopeDoc;
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

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-店铺V1")
@OpenapiResponse
@RestController("openapiV1Distributor")
@RequestMapping("/api/openapi/internal/v1")
public class DistributorController extends OpenapiBaseController {

	private final OpenapiDistributorDetailPort distributorDetailPort;

	public DistributorController(OpenapiDistributorDetailPort distributorDetailPort) {
		this.distributorDetailPort = distributorDetailPort;
	}

	@Operation(
			summary = "开放接口获取店铺详情",
			description =
					"按签名鉴权得到的租户 company_id 与店铺号 shop_code 精确查询店铺详情（单对象 23 字段）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor.detail, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " shop_code 双通道：body 含键时取 body 原始值（null 不回退 query），否则用 query 原样（不 trim）。"
							+ " contact/mobile 解密返回；name/contact/logo/省市区地址按默认语言分表覆盖；"
							+ " created/updated 格式化为 yyyy-MM-dd HH:mm:ss。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.distributor.detail.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=成功，data 为单对象，见 EcxDistributorDetailV1DataDoc；"
								+ " 业务失败：status=fail, code=E0001（店铺号必填/店铺找不到），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorDetailV1FailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorDetailV1SuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"distributor_id\":1001,\"shop_code\":\"S001\",\"status\":1,"
															+ "\"distributor_name\":\"示例门店\",\"contact_username\":\"张三\","
															+ "\"contact_mobile\":\"13800000000\",\"province\":\"上海市\","
															+ "\"city\":\"上海市\",\"area\":\"黄浦区\",\"region_codes\":[],"
															+ "\"region_names\":[],\"address\":\"南京东路 1 号\",\"lng\":\"121.47\","
															+ "\"lat\":\"31.23\",\"hour\":\"10:00-22:00\","
															+ "\"logo\":\"/fixtures/logo.png\",\"is_ziti\":0,\"is_delivery\":1,"
															+ "\"is_auto_sync_goods\":0,\"is_dada\":0,\"is_default\":0,"
															+ "\"created\":\"2026-11-26 10:00:00\",\"updated\":\"2026-11-26 10:00:00\"}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "业务失败（店铺不存在）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"店铺找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-shop-code",
											summary = "业务失败（店铺号缺失）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"店铺号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.distributor.detail", name = "开放接口获取店铺详情")
	public OpenapiEnvelope detail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "店铺号（distribution_distributor.shop_code），精确匹配；必填非空（body 同名键优先，原样不 trim）",
							required = true,
							example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@RequestBody(
							description =
									"可选 JSON body；shop_code 与 query 同名双通道，body 含键时取原始值"
											+ "（null 视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxDistributorDetailV1BodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		if (shopCodeRaw == null || shopCodeRaw.isEmpty()) {
			throw new ResourceException("店铺号必填");
		}

		Map<String, Object> data =
				distributorDetailPort.getDistributorDetail(companyId, shopCodeRaw);
		return new OpenapiEnvelope("success", "E0000", "成功", data);
	}
}
