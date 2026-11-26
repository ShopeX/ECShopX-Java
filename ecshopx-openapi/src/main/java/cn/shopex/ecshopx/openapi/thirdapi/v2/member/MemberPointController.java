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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointLogV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointV2UpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointDetailBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointDetailSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointLogListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointLogListSuccessEnvelopeDoc;
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

@Tag(name = "开放接口-会员积分V2")
@OpenapiResponse
@RestController("openapiV2MemberPoint")
@RequestMapping("/api/openapi/internal/v2")
public class MemberPointController extends OpenapiBaseController {

	private final OpenapiMemberPointLogV2ListPort memberPointLogV2ListPort;
	private final OpenapiMemberPointV2UpdatePort memberPointV2UpdatePort;
	private final OpenapiMemberPointV2DetailPort memberPointV2DetailPort;

	public MemberPointController(
			OpenapiMemberPointLogV2ListPort memberPointLogV2ListPort,
			OpenapiMemberPointV2UpdatePort memberPointV2UpdatePort,
			OpenapiMemberPointV2DetailPort memberPointV2DetailPort) {
		this.memberPointLogV2ListPort = memberPointLogV2ListPort;
		this.memberPointV2UpdatePort = memberPointV2UpdatePort;
		this.memberPointV2DetailPort = memberPointV2DetailPort;
	}

	@Operation(
			summary = "开放接口查询会员积分历史记录",
			description =
					"按签名鉴权租户 company_id 分页查询会员积分流水（point_member_log，id 降序）："
							+ " 5 键双通道（body 含键优先、值 null 视为未传、不 trim；三过滤键另按存在性判定）。"
							+ " mobile 键存在且非空白时按固定加密等值定位会员——命中按 user_id 过滤，"
							+ " **查无/空白按 user_id=0 过滤（通常空结果，不报错）**。"
							+ " start_date/end_date 键存在且非空非\"0\"时须 yyyy-MM-dd HH:mm:ss（系统时区，"
							+ " created 区间），纯数字时间戳或其他格式报 E4002「时间格式有误」；"
							+ " 缺省不过滤（无默认时间下界，区别于会员订单列表）。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list；行 11 键："
							+ " id/mobile 当前号解密回查/operater/type（journal_type 1-18 枚举，13=开放接口）/"
							+ " description 拼接（**支出分支源码缺陷拼 -income，纯支出显示 -0**）/"
							+ " increase_point/decrease_point/record（type=13 取 operater_remark 否则 point_desc）/"
							+ " order_id/external_id/created 格式化时间。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_point_log.list,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_point_log.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberPointLogListDataDoc；"
								+ " 业务失败：status=fail, code=E4002 时间格式有误 / E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberPointLogListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberPointLogListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（开放接口加 10 分流水；时间/描述内 Unix 秒为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"id\":1,\"mobile\":\"13800138000\","
															+ "\"operater\":\"外部开发者\",\"type\":13,"
															+ "\"description\":\"系统 于1735689600 给 会员(13800138000) 开放接口 +10\","
															+ "\"increase_point\":10,\"decrease_point\":0,\"record\":\"\","
															+ "\"order_id\":\"\",\"external_id\":\"1\","
															+ "\"created\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无积分流水（含 mobile 查无按 user_id=0 过滤场景）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-time-format",
											summary = "start_date/end_date 传时间戳或非法格式",
											value =
													"{\"status\":\"fail\",\"code\":\"E4002\",\"message\":\"时间格式有误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_point_log.list", name = "开放接口查询会员积分历史记录")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号过滤；命中按 user_id 过滤；查无/空白按 user_id=0 过滤（通常空结果，不报错）",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "起始时间 yyyy-MM-dd HH:mm:ss；纯数字/非法格式 E4002；空串/\"0\"/缺省不过滤（格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "start_date", required = false)
					String startDateParam,
			@Parameter(
							description = "截止时间 yyyy-MM-dd HH:mm:ss；解析同 start_date（格式演示值）",
							example = "2026-12-31 23:59:59")
					@RequestParam(name = "end_date", required = false)
					String endDateParam,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E5002；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；5 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberPointLogListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		boolean startDatePresent =
				OpenapiMemberQueryParams.isParamPresent(startDateParam, body, "start_date");
		String startDateRaw = OpenapiRequestParams.originalString(startDateParam, body, "start_date");
		boolean endDatePresent =
				OpenapiMemberQueryParams.isParamPresent(endDateParam, body, "end_date");
		String endDateRaw = OpenapiRequestParams.originalString(endDateParam, body, "end_date");

		return memberPointLogV2ListPort.listMemberPointLogs(
				companyId,
				mobilePresent,
				mobileRaw,
				startDatePresent,
				startDateRaw,
				endDatePresent,
				endDateRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}

	@Operation(
			summary = "开放接口查询会员可用积分",
			description =
					"按签名鉴权租户 company_id 以手机号查询会员可用积分："
							+ " mobile 必填（originalString 双通道，body 含键优先、值 null 视为未传；服务内 trim 后须"
							+ " ^1[3456789][0-9]{9}$；缺失/空白 E5001「手机号必填」，格式错 E5001「手机号填写错误」）。"
							+ " 会员按固定加密 mobile 等值定位（LIMIT 1）；**查无会员不报错**——"
							+ " point_member 按 (company_id, user_id) LIMIT 1 读 point，"
							+ " 会员查无/无积分账户/point null 均返回 point=0（仍 success）。"
							+ " data 单键 {point: 整型}。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_point.detail,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_point.detail.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data={point}（查无会员为 0，仍 success）；"
								+ " 业务失败：status=fail, code=E5001 手机号必填·手机号填写错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberPointDetailFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberPointDetailSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"point\":10}}"),
									@ExampleObject(
											name = "success-no-member",
											summary = "会员查无/无积分账户（point=0，仍 success）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"point\":0}}"),
									@ExampleObject(
											name = "fail-mobile-missing",
											summary = "mobile 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"手机号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "mobile 格式错误",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"手机号填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_point.detail", name = "开放接口查询会员可用积分")
	public Map<String, Object> detail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号（明文）；必填，trim 后须 ^1[3456789][0-9]{9}$；查无会员不报错（point=0）",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@RequestBody(
							description = "可选 JSON body；mobile 与 query 同名双通道（body 含键优先，值 null 视为未传）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberPointDetailBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberPointV2DetailPort.getMemberPointDetail(companyId, mobileRaw);
	}

	@PatchMapping(value = "/ecx.member_point.update", name = "开放接口修改会员积分")
	public Void update(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "increase_point", required = false) String increasePointParam,
			@RequestParam(name = "decrease_point", required = false) String decreasePointParam,
			@RequestParam(name = "record", required = false) String recordParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdatePointHandlerParams.mergeAll(
						body,
						mobileParam,
						increasePointParam,
						decreasePointParam,
						recordParam,
						externalIdParam);
		memberPointV2UpdatePort.updateMemberPoint(companyId, merged);
		return null;
	}
}
