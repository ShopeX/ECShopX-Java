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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberOperateLogV2ListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberOperateLogListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberOperateLogListSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员操作日志V2")
@OpenapiResponse
@RestController("openapiV2MemberOperateLog")
@RequestMapping("/api/openapi/internal/v2")
public class MemberOperateLogController extends OpenapiBaseController {

	private final OpenapiMemberOperateLogV2ListPort memberOperateLogV2ListPort;

	public MemberOperateLogController(OpenapiMemberOperateLogV2ListPort memberOperateLogV2ListPort) {
		this.memberOperateLogV2ListPort = memberOperateLogV2ListPort;
	}

	@Operation(
			summary = "开放接口查询会员信息操作日志",
			description =
					"按签名鉴权租户 company_id 分页查询会员操作日志（member_operate_log，created 降序）："
							+ " 5 键双通道（body 含键优先、值 null 视为未传、不 trim；mobile/start_date/end_date"
							+ " 另按键存在性判定）。mobile 键存在且非空白时按固定加密等值定位会员——"
							+ " 命中按 user_id 过滤，**查无/空白不报错且不过滤**（回落全租户日志）。"
							+ " start_date/end_date 键存在且非空非\"0\"时须 yyyy-MM-dd HH:mm:ss（系统时区，"
							+ " created 区间），纯数字时间戳或其他格式报 E4002「时间格式有误」；空串/\"0\" 忽略。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list；行 8 键："
							+ " id、operate_type（1=info/2=mobile/3=grade_id，未知 0）、old_data/new_data"
							+ "（mobile 类型兼容 {\"mobile\":…} JSON 取键，否则原样明文；info/grade 为 JSON 差异串）、"
							+ " operater、created 格式化时间、description 服务端拼接描述、mobile 当前号解密回查。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_operate_log.list,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_operate_log.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberOperateLogListDataDoc；"
								+ " 业务失败：status=fail, code=E4002 时间格式有误 / E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberOperateLogListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberOperateLogListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（修改手机号日志；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"id\":1,\"operate_type\":2,"
															+ "\"old_data\":\"13800138000\",\"new_data\":\"13800000000\","
															+ "\"operater\":\"外部开发者\",\"created\":\"2026-01-01 00:00:00\","
															+ "\"description\":\"外部开发者 于2026-01-01 00:00:00进行了修改手机号的操作, "
															+ "将 13800138000 改为 13800000000\",\"mobile\":\"13800000000\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无日志（list 空数组）",
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
	@GetMapping(value = "/ecx.member_operate_log.list", name = "开放接口查询会员信息操作日志")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号过滤；命中按 user_id 过滤；查无/空白不报错且不过滤（回落全租户日志）",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "起始时间 yyyy-MM-dd HH:mm:ss；纯数字/非法格式 E4002「时间格式有误」；空串/\"0\" 忽略（格式演示值）",
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
																	EcxMemberOperateLogListBodyDoc.class)))
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

		return memberOperateLogV2ListPort.listMemberOperateLogs(
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
}
