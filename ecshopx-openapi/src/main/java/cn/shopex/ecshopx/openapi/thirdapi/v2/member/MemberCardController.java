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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardUpdateSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口修改会员卡基础设置",
			description =
					"按签名鉴权租户 company_id 修改会员卡基础设置（5 键双通道，按键存在性 PATCH："
							+ " body 含键即生效——值 null 转空串、非字符串 String.valueOf；query 非 null 即生效，原样不 trim）。"
							+ " 全部键缺席 → 无操作成功；任一键存在即 upsert membercard 行"
							+ "（无行 INSERT 新行含 created/updated；有行仅更新 present 键 + updated）。"
							+ " 与 admin 端 PUT /api/v1/membercard 不同：本接口不校验必填/枚举，"
							+ " 空串（含 body null 转空串）可写入清空；code_type 不可通过本接口修改。"
							+ " 成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_card.update,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：INSERT 或 UPDATE membercard 单行（无显式事务）；无异步、无第三方调用。",
			operationId = "ecx.member_card.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含全键缺席无操作场景）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member_card.update", name = "开放接口修改会员卡基础设置")
	public Void update(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "商户名称；键存在即更新（body null 转空串可清空；不校验必填）",
							example = "FixtureBrand")
					@RequestParam(name = "brand_name", required = false)
					String brandNameParam,
			@Parameter(
							description = "商户 logo URL；键存在即更新（不校验必填；示例为格式演示值）",
							example = "https://example.test/key.jpg")
					@RequestParam(name = "logo_url", required = false)
					String logoUrlParam,
			@Parameter(
							description = "会员卡名称；键存在即更新（不校验必填）",
							example = "Gold")
					@RequestParam(name = "title", required = false)
					String titleParam,
			@Parameter(
							description = "会员卡背景颜色（微信卡券色值枚举，如 Color010）；键存在即更新，不校验枚举",
							example = "Color010")
					@RequestParam(name = "color", required = false)
					String colorParam,
			@Parameter(
							description = "会员卡背景图 URL；键存在即更新（示例为格式演示值）",
							example = "https://example.test/key.jpg")
					@RequestParam(name = "background_pic_url", required = false)
					String backgroundPicUrlParam,
			@RequestBody(
							description =
									"可选 JSON body；5 键与 query 同名双通道（body 含键优先，键存在性 PATCH，"
											+ "null 转空串可清空）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCardUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
