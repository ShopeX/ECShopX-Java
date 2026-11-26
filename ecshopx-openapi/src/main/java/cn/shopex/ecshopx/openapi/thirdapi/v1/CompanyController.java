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
import cn.shopex.ecshopx.common.openapi.OpenapiCompanyInfoPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCompanyInfoBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCompanyInfoSuccessEnvelopeDoc;
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
@Tag(name = "开放接口-云店")
@OpenapiResponse
@RestController("openapiV1Company")
@RequestMapping("/api/openapi/internal/v1")
public class CompanyController extends OpenapiBaseController {

	private final OpenapiCompanyInfoPort companyInfoPort;

	public CompanyController(OpenapiCompanyInfoPort companyInfoPort) {
		this.companyInfoPort = companyInfoPort;
	}

	@Operation(
			summary = "开放接口获取云店基本信息",
			description =
					"按签名鉴权得到的租户 company_id 返回当前语言的门店展示配置（logo、intro、brand_name、background）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.company.info, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 路径无鉴权上下文时返回 E4004 未授权。"
							+ " country_code 双通道：body 含该键时以 body 为准（null/空白不回退 query），"
							+ " body 无该键时读 query；均缺省 zh-CN。"
							+ " 只读（仅读 Redis wxShopsSetting），无写库/异步/第三方副作用。",
			operationId = "ecx.company.info")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=success，data 见 EcxCompanyInfoDataDoc"
								+ "（Redis 未命中或无该语言切片时仅含 company_id）；"
								+ " 鉴权失败：status=fail, code=E4004, data=null，见 EcxCompanyInfoFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxCompanyInfoSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "成功（语言切片存在）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"logo\":\"/fixtures/logo.png\",\"intro\":\"hi\","
															+ "\"brand_name\":\"FixtureBrand\",\"background\":\"/bg.png\","
															+ "\"company_id\":88001}}"),
									@ExampleObject(
											name = "success-empty-setting",
											summary = "成功（未配置门店展示或无该语言切片）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"company_id\":88001}}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.company.info", name = "开放接口获取云店基本信息")
	public OpenapiEnvelope getInfo(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"语言/国家码，默认 zh-CN；body 含 country_code 键时以 body 为准，"
											+ " body 无该键时才使用本 query 参数",
							example = "zh-CN")
					@RequestParam(name = "country_code", required = false)
					String countryCodeParam,
			@RequestBody(
							description =
									"可选 JSON body；仅读取 country_code。含该键时优先于 query"
											+ "（值为 null/空白视为未提供且不回退 query，最终默认 zh-CN）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxCompanyInfoBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String countryCode = OpenapiPaginationParams.mergeCountryCode(countryCodeParam, body);
		if (countryCode == null) {
			countryCode = "zh-CN";
		}
		Map<String, Object> data = companyInfoPort.getCompanyInfo(companyId, countryCode);
		return new OpenapiEnvelope("success", "E0000", "success", data);
	}
}
