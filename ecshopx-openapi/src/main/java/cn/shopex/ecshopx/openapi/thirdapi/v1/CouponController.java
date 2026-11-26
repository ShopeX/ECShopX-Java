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
import cn.shopex.ecshopx.common.openapi.OpenapiCouponCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponVerifyPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCouponCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCouponCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCouponListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxCouponListSuccessEnvelopeDoc;
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
@Tag(name = "开放接口-优惠券")
@OpenapiResponse
@RestController("openapiV1Coupon")
@RequestMapping("/api/openapi/internal/v1")
public class CouponController extends OpenapiBaseController {

	private final OpenapiCouponCreatePort couponCreatePort;
	private final OpenapiCouponVerifyPort couponVerifyPort;
	private final OpenapiCouponUpdatePort couponUpdatePort;
	private final OpenapiCouponListPort couponListPort;

	public CouponController(
			OpenapiCouponCreatePort couponCreatePort,
			OpenapiCouponVerifyPort couponVerifyPort,
			OpenapiCouponUpdatePort couponUpdatePort,
			OpenapiCouponListPort couponListPort) {
		this.couponCreatePort = couponCreatePort;
		this.couponVerifyPort = couponVerifyPort;
		this.couponUpdatePort = couponUpdatePort;
		this.couponListPort = couponListPort;
	}

	@Operation(
			summary = "开放接口云店优惠券发放",
			description =
					"按签名鉴权得到的租户 company_id，向会员发放一张第三方来源优惠券（写入 user_discount，"
							+ "source_type=第三方发放，status=1，事务内执行）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.coupon.create, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 全部参数双通道：body 含同名键时以 body 为准（null/空白视为未提供且不回退 query），"
							+ " body 无该键时读 query。会员定位：outer_crm_userid（third_data）优先于 user_id（手机号），"
							+ " 均缺省时券归属 user_id=0。校验：券模板存在、end_time 未过期、库存充足、code 未被领取；"
							+ " 重复领取返回 status=fail 且 code=E0000（PHP 旧契约）。"
							+ " 副作用：写库 user_discount 一条；无异步/第三方调用。",
			operationId = "ecx.coupon.create")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=发放成功，data 仅含 status=true；"
								+ " 业务失败：status=fail, code=E0001（缺参/时间格式/用户不存在/该优惠券已失效/"
								+ "领取优惠券失败，优惠券已过期/领取的优惠券失败，库存不足了），data=null；"
								+ " 重复领取：status=fail 且 code=E0000（您已经领取该卡券/领取该卡券失败），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxCouponCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxCouponCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "发放成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"发放成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-card-invalid",
											summary = "业务失败（券模板不存在）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"该优惠券已失效\",\"data\":null}"),
									@ExampleObject(
											name = "fail-already-received",
											summary = "重复领取（PHP 旧契约：code=E0000 但 status=fail）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\",\"message\":\"您已经领取该卡券\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.coupon.create", name = "开放接口云店优惠券发放")
	public OpenapiEnvelope userGetCard(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "优惠券模板ID（第三方编码，对应 discount_cards.card_code）；必填（body 同名键优先）",
							required = true)
					@RequestParam(name = "template", required = false)
					String templateParam,
			@Parameter(description = "优惠券规则ID（第三方编码，对应 discount_cards.card_rule_code）；可选，缺省按空串匹配")
					@RequestParam(name = "rule", required = false)
					String ruleParam,
			@Parameter(
							description = "优惠券编码（写入 user_discount.code，同券模板下唯一）；必填（body 同名键优先）",
							required = true,
							example = "CODE-1")
					@RequestParam(name = "code", required = false)
					String codeParam,
			@Parameter(
							description = "优惠券生效时间，Unix 秒级时间戳；必填，非整数报「优惠券生效时间必填」（body 同名键优先）",
							required = true,
							example = "1764115200")
					@RequestParam(name = "start_time", required = false)
					String startTimeParam,
			@Parameter(
							description =
									"优惠券失效时间，Unix 秒级时间戳；必填，非整数报「优惠券失效时间必填」，"
											+ "大于 0 且早于等于当前时间报已过期（body 同名键优先）",
							required = true,
							example = "1766707200")
					@RequestParam(name = "end_time", required = false)
					String endTimeParam,
			@Parameter(description = "第三方 CRM 用户标识，按会员 third_data 定位；查无会员报「用户不存在」；优先于 user_id")
					@RequestParam(name = "outer_crm_userid", required = false)
					String outerCrmUseridParam,
			@Parameter(
							description =
									"会员手机号，按手机号定位；查无会员报「用户不存在」；"
											+ "outer_crm_userid 未提供时生效，两者均缺省时券归属 user_id=0")
					@RequestParam(name = "user_id", required = false)
					String userIdParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先"
											+ "（null/空白视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxCouponCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String template =
				OpenapiRequestParams.mergeRequiredString(templateParam, body, "template", "优惠券模板ID必填");
		String rule = OpenapiRequestParams.mergeString(ruleParam, body, "rule");
		if (rule == null) {
			rule = "";
		}
		String code = OpenapiRequestParams.mergeRequiredString(codeParam, body, "code", "优惠券编码必填");
		String startTimeRaw =
				OpenapiRequestParams.mergeRequiredString(startTimeParam, body, "start_time", "优惠券生效时间必填");
		String endTimeRaw =
				OpenapiRequestParams.mergeRequiredString(endTimeParam, body, "end_time", "优惠券失效时间必填");
		String outerCrmUserid = OpenapiRequestParams.mergeString(outerCrmUseridParam, body, "outer_crm_userid");
		String mobileUserId = OpenapiRequestParams.mergeString(userIdParam, body, "user_id");

		long startTimeEpoch = parseRequiredEpoch(startTimeRaw, "优惠券生效时间必填");
		long endTimeEpoch = parseRequiredEpoch(endTimeRaw, "优惠券失效时间必填");

		Map<String, Object> data =
				couponCreatePort.userGetCard(
						companyId,
						template,
						rule,
						code,
						startTimeEpoch,
						endTimeEpoch,
						outerCrmUserid,
						mobileUserId);
		return new OpenapiEnvelope("success", "E0000", "发放成功", data);
	}

	@PostMapping(value = "/ecx.coupon.update", name = "开放接口优惠券有效期更新")
	public OpenapiEnvelope updateUserCard(
			HttpServletRequest request,
			@RequestParam(name = "template", required = false) String templateParam,
			@RequestParam(name = "rule", required = false) String ruleParam,
			@RequestParam(name = "code", required = false) String codeParam,
			@RequestParam(name = "start_time", required = false) String startTimeParam,
			@RequestParam(name = "end_time", required = false) String endTimeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String template =
				OpenapiRequestParams.mergeRequiredString(templateParam, body, "template", "优惠券模板ID必填");
		String rule = OpenapiRequestParams.mergeString(ruleParam, body, "rule");
		if (rule == null) {
			rule = "";
		}
		String code = OpenapiRequestParams.mergeRequiredString(codeParam, body, "code", "优惠券编码必填");
		String startTimeRaw =
				OpenapiRequestParams.mergeRequiredString(startTimeParam, body, "start_time", "优惠券生效时间必填");
		String endTimeRaw =
				OpenapiRequestParams.mergeRequiredString(endTimeParam, body, "end_time", "优惠券失效时间必填");

		Map<String, Object> data =
				couponUpdatePort.updateUserCard(companyId, template, rule, code, startTimeRaw, endTimeRaw);
		return new OpenapiEnvelope("success", "E0000", "修改成功", data);
	}

	@Operation(
			summary = "开放接口优惠券列表",
			description =
					"按签名鉴权得到的租户 company_id 查询优惠券模板列表（discount_cards），按 created 倒序。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.coupon.list, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 全部参数双通道：body 含同名键时以 body 为准（null/空白视为未提供且不回退 query），"
							+ " body 无该键时读 query。distributor_id 非空且非 \"0\" 时按逗号分隔多店铺过滤"
							+ "（含全部店铺的券）；page_no 与 page_size 键成对存在才分页，否则返回全部；"
							+ " 分页参数非整数返回 status=fail 且 code=E0000（PHP 旧契约）。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.coupon.list")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功，data 仅含 list（无总数字段），"
								+ " 每条 6 字段见 EcxCouponListItemDoc；"
								+ " 分页参数非整数：status=fail 且 code=E0000（A non-numeric value encountered），data=null；"
								+ " 查询异常：status=fail, code=E0001，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxCouponListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxCouponListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"list\":[{\"card_id\":3,\"title\":\"满减券\","
															+ "\"description\":\"\",\"discount\":0,\"distributor_id\":\",\","
															+ "\"card_type\":\"cash\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "查询成功（无匹配券模板）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"list\":[]}}"),
									@ExampleObject(
											name = "fail-non-numeric-page",
											summary = "分页参数非整数（PHP 旧契约：code=E0000 但 status=fail）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\","
															+ "\"message\":\"A non-numeric value encountered\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.coupon.list", name = "开放接口优惠券列表")
	public OpenapiEnvelope getCouponList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"店铺 ID 筛选，支持逗号分隔多个；非空且非 \"0\" 时生效"
											+ "（含全部店铺的券）；缺省不过滤（body 同名键优先）",
							example = "5")
					@RequestParam(name = "distributor_id", required = false)
					String distributorIdParam,
			@Parameter(
							description = "页码，从 1 起；与 page_size 键成对存在才分页，否则返回全部；非整数报错（body 同名键优先）",
							example = "1")
					@RequestParam(name = "page_no", required = false)
					String pageNoParam,
			@Parameter(description = "每页条数；与 page_no 键成对存在才分页；非整数报错（body 同名键优先）", example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先"
											+ "（null/空白视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxCouponListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String distributorId = OpenapiRequestParams.mergeString(distributorIdParam, body, "distributor_id");
		boolean applyDistributorFilter = OpenapiCouponListParams.isPhpTruthyDistributorId(distributorId);
		boolean pageNoPresent = OpenapiCouponListParams.isParamPresent(pageNoParam, body, "page_no");
		boolean pageSizePresent = OpenapiCouponListParams.isParamPresent(pageSizeParam, body, "page_size");
		String pageNoRaw = OpenapiRequestParams.mergeString(pageNoParam, body, "page_no");
		String pageSizeRaw = OpenapiRequestParams.mergeString(pageSizeParam, body, "page_size");
		Map<String, Object> data =
				couponListPort.getCouponList(
						companyId,
						applyDistributorFilter,
						distributorId,
						pageNoPresent,
						pageNoRaw,
						pageSizePresent,
						pageSizeRaw);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.coupon.verify", name = "开放接口优惠券状态更新")
	public OpenapiEnvelope userConsumeCard(
			HttpServletRequest request,
			@RequestParam(name = "coupon_code", required = false) String couponCodeParam,
			@RequestParam(name = "coupon_status", required = false) String couponStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String couponCode =
				OpenapiRequestParams.mergeRequiredString(couponCodeParam, body, "coupon_code", "优惠券编码必填");
		String couponStatus =
				OpenapiRequestParams.mergeRequiredString(couponStatusParam, body, "coupon_status", "状态必填");
		if (!"1".equals(couponStatus) && !"2".equals(couponStatus)) {
			throw new ResourceException("不支持的核销状态");
		}
		Map<String, Object> data = couponVerifyPort.userConsumeCard(companyId, couponCode, couponStatus);
		return new OpenapiEnvelope("success", "E0000", "修改成功", data);
	}

	private static long parseRequiredEpoch(String raw, String requiredMessage) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(requiredMessage);
		}
	}
}
