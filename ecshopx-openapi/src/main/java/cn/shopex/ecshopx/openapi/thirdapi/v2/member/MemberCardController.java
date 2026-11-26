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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardV2UpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardDetailSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-会员卡设置V2")
@OpenapiResponse
@RestController("openapiV2MemberCard")
@RequestMapping("/api/openapi/internal/v2")
public class MemberCardController extends OpenapiBaseController {

	private final OpenapiMemberCardV2DetailPort memberCardV2DetailPort;
	private final OpenapiMemberCardV2UpdatePort memberCardV2UpdatePort;

	public MemberCardController(
			OpenapiMemberCardV2DetailPort memberCardV2DetailPort,
			OpenapiMemberCardV2UpdatePort memberCardV2UpdatePort) {
		this.memberCardV2DetailPort = memberCardV2DetailPort;
		this.memberCardV2UpdatePort = memberCardV2UpdatePort;
	}

	@Operation(
			summary = "开放接口查询会员卡基础设置",
			description =
					"按签名鉴权租户 company_id 查询会员卡基础设置（membercard 表主键查询），无业务入参。"
							+ " data 7 固定键：brand_name/logo_url/title/color/background_pic_url（null 转空串）"
							+ " + created/updated（yyyy-MM-dd HH:mm:ss 系统时区格式化，非数值/缺失为空串）；"
							+ " company_id/code_type 不透出。"
							+ " 无设置行时不报错，7 键全部为空串（仍 success）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_card.detail,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_card.detail.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberCardDetailDataDoc"
								+ "（无设置行为全空串，仍 success）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardDetailFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardDetailSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（URL/时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"brand_name\":\"FixtureBrand\","
															+ "\"logo_url\":\"https://example.test/key.jpg\",\"title\":\"Gold\","
															+ "\"color\":\"Color010\","
															+ "\"background_pic_url\":\"https://example.test/key.jpg\","
															+ "\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "success-unset",
											summary = "租户未设置会员卡（7 键全空串，仍 success）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"brand_name\":\"\",\"logo_url\":\"\",\"title\":\"\","
															+ "\"color\":\"\",\"background_pic_url\":\"\","
															+ "\"created\":\"\",\"updated\":\"\"}}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_card.detail", name = "开放接口查询会员卡基础设置")
	public Map<String, Object> detail(@Parameter(hidden = true) HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		return memberCardV2DetailPort.getMemberCardDetail(companyId);
	}

	@PostMapping(value = "/ecx.member_card.update", name = "开放接口修改会员卡基础设置")
	public Void update(
			HttpServletRequest request,
			@RequestParam(name = "brand_name", required = false) String brandNameParam,
			@RequestParam(name = "logo_url", required = false) String logoUrlParam,
			@RequestParam(name = "title", required = false) String titleParam,
			@RequestParam(name = "color", required = false) String colorParam,
			@RequestParam(name = "background_pic_url", required = false) String backgroundPicUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> params =
				OpenapiMemberCardV2UpdateHandlerParams.collectPresentFields(
						body,
						brandNameParam,
						logoUrlParam,
						titleParam,
						colorParam,
						backgroundPicUrlParam);
		memberCardV2UpdatePort.updateMemberCard(companyId, params);
		return null;
	}
}
