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
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberBatchCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCreateDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2ListByGradeOrVipGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateCardCodeGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateMobilePort;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberBatchCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberBatchCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardCodeGradeUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardCodeGradeUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberGradeChangeBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberGradeChangeSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardGradeRelListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardGradeRelListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberInfoUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberInfoUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberMobileUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberMobileUpdateSuccessEnvelopeDoc;
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
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员V2")
@OpenapiResponse
@RestController("openapiV2Member")
@RequestMapping("/api/openapi/internal/v2")
public class MemberController extends OpenapiBaseController {

	private final OpenapiMemberCreateDetailPort memberCreateDetailPort;
	private final OpenapiMemberBatchCreatePort memberBatchCreatePort;
	private final OpenapiMemberV2ListPort memberV2ListPort;
	private final OpenapiMemberV2ListByGradeOrVipGradePort memberV2ListByGradeOrVipGradePort;
	private final OpenapiMemberV2DetailPort memberV2DetailPort;
	private final OpenapiMemberV2UpdateDetailPort memberV2UpdateDetailPort;
	private final OpenapiMemberV2UpdateMobilePort memberV2UpdateMobilePort;
	private final OpenapiMemberV2UpdateCardCodeGradePort memberV2UpdateCardCodeGradePort;
	private final OpenapiMemberV2UpdateGradePort memberV2UpdateGradePort;

	public MemberController(
			OpenapiMemberCreateDetailPort memberCreateDetailPort,
			OpenapiMemberBatchCreatePort memberBatchCreatePort,
			OpenapiMemberV2ListPort memberV2ListPort,
			OpenapiMemberV2ListByGradeOrVipGradePort memberV2ListByGradeOrVipGradePort,
			OpenapiMemberV2DetailPort memberV2DetailPort,
			OpenapiMemberV2UpdateDetailPort memberV2UpdateDetailPort,
			OpenapiMemberV2UpdateMobilePort memberV2UpdateMobilePort,
			OpenapiMemberV2UpdateCardCodeGradePort memberV2UpdateCardCodeGradePort,
			OpenapiMemberV2UpdateGradePort memberV2UpdateGradePort) {
		this.memberCreateDetailPort = memberCreateDetailPort;
		this.memberBatchCreatePort = memberBatchCreatePort;
		this.memberV2ListPort = memberV2ListPort;
		this.memberV2ListByGradeOrVipGradePort = memberV2ListByGradeOrVipGradePort;
		this.memberV2DetailPort = memberV2DetailPort;
		this.memberV2UpdateDetailPort = memberV2UpdateDetailPort;
		this.memberV2UpdateMobilePort = memberV2UpdateMobilePort;
		this.memberV2UpdateCardCodeGradePort = memberV2UpdateCardCodeGradePort;
		this.memberV2UpdateGradePort = memberV2UpdateGradePort;
	}

	@Operation(
			summary = "开放接口创建会员单个",
			description =
					"按签名鉴权租户 company_id 同步创建单个会员（21 键：20 键 mergeAll 双通道"
							+ " body 含键取原值否则 query trim；habbit 仅 body）。"
							+ " mobile 必填（格式错/占用统一 E5502「会员手机号已存在」PHP 文案）；"
							+ " source_from 仅 default/openapi（缺省 openapi）；sex 仅 0；status 0|1（缺省 1）；"
							+ " grade_id 非负整数（缺省取租户默认等级，>0 查无 E5531）；card_code 空白自动生成"
							+ "（占用 E5522）；tag_names 逗号分隔且**标签须已存在**（查无 E5561，不自动建）；"
							+ " tag_ids 被接收但忽略；inviter_mobile 查无 E5511；salesperson_mobile 导购查无 E5601、"
							+ " 已绑定 E5612；birthday 实际仅 yyyy-MM-dd 可用（datetime 形态落库重解析失败）；"
							+ " habbit/枚举/email 校验同批量创建元素。"
							+ " 同步执行完整创建链（members 随机 10 位密码/固定加密手机号 + members_info +"
							+ " 标签关联 + 导购绑定（work_wechat_rel）+ unionid 关联 + 新客标签对账），"
							+ " 任一步失败整体回滚（E5000「系统错误」兜底）。"
							+ " 成功 data 为新建会员 29 键行（大量固定初始值见 EcxMemberCreateDataDoc）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.create,"
							+ " version=2.0，另有 routeAlias adapay.member.create）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional INSERT members/members_info/members_rel_tags（计数更新）/"
							+ " work_wechat_rel（导购绑定）/members_associations + 新客系统标签对账 +"
							+ " 空卡号 Redis zset 写 + 导购拉新完成/会员数增加端口调用；无异步。",
			operationId = "ecx.member.create.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberCreateDataDoc；"
								+ " 业务失败：status=fail, code=E5001 各字段填写错误 / E5502 会员手机号已存在 /"
								+ " E5511 推荐人找不到 / E5531 等级找不到 / E5522 卡号已存在 / E5561 标签不存在 /"
								+ " E5601 导购找不到 / E5612 导购已绑定 / E5000 系统错误，data=null（整体回滚）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "创建成功（29 键行；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"user_id\":50,\"company_id\":1,\"grade_id\":1,"
															+ "\"mobile\":\"13800138000\",\"region_mobile\":\"13800138000\","
															+ "\"mobile_country_code\":\"86\",\"user_card_code\":\"card\","
															+ "\"offline_card_code\":\"\",\"inviter_id\":0,"
															+ "\"source_from\":\"openapi\",\"source_id\":0,\"monitor_id\":0,"
															+ "\"latest_source_id\":0,\"latest_monitor_id\":0,"
															+ "\"authorizer_appid\":\"\",\"use_point\":0,\"wxa_appid\":\"\","
															+ "\"alipay_appid\":\"\",\"created\":1735689600,\"updated\":1735689600,"
															+ "\"disabled\":0,\"remarks\":\"\",\"third_data\":null,"
															+ "\"reg_distributor\":0,\"reg_salesperson\":null,"
															+ "\"fp_salesperson\":null,\"has_fp\":0,\"is_become_friend\":0,"
															+ "\"op_distributor\":0}}"),
									@ExampleObject(
											name = "fail-mobile-exist",
											summary = "mobile 格式错或已占用（同文案）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5502\","
															+ "\"message\":\"会员手机号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-tag-not-found",
											summary = "tag_names 标签不存在（不自动建）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5561\","
															+ "\"message\":\"标签名为:聚合测试类目, 标签不存在！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-not-found",
											summary = "指定 grade_id 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5531\","
															+ "\"message\":\"会员等级找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@Activated(routeAlias = "adapay.member.create")
	@PostMapping(value = "/ecx.member.create", name = "开放接口创建会员单个")
	public Map<String, Object> create(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号；必填；格式错/占用统一 E5502「会员手机号已存在」",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "推荐人手机号；非空须格式（E5001）；查无 E5511",
							example = "13800000000")
					@RequestParam(name = "inviter_mobile", required = false)
					String inviterMobileParam,
			@Parameter(
							description = "绑定导购手机号；非空须格式（E5001）；查无 E5601；已绑定 E5612",
							example = "13800000000")
					@RequestParam(name = "salesperson_mobile", required = false)
					String salespersonMobileParam,
			@Parameter(description = "微信 unionid；须字符串（否则 E5001）")
					@RequestParam(name = "union_id", required = false)
					String unionIdParam,
			@Parameter(
							description = "会员状态 0 禁用/1 启用；缺省 1；其他值 E5001",
							example = "1")
					@RequestParam(name = "status", required = false)
					String statusParam,
			@Parameter(
							description = "标签名逗号分隔；标签须已存在（查无 E5561，不自动建）",
							example = "聚合测试类目")
					@RequestParam(name = "tag_names", required = false)
					String tagNamesParam,
			@Parameter(
							description = "遗留参数：被接收但忽略（标签仅按 tag_names 解析）",
							deprecated = true)
					@RequestParam(name = "tag_ids", required = false)
					String tagIdsParam,
			@Parameter(
							description = "会员卡号；空白自动生成；占用 E5522",
							example = "card")
					@RequestParam(name = "card_code", required = false)
					String cardCodeParam,
			@Parameter(
							description = "会员等级 ID（内部）；非负整数；缺省取默认等级；>0 查无 E5531",
							example = "1")
					@RequestParam(name = "grade_id", required = false)
					String gradeIdParam,
			@Parameter(description = "姓名/昵称（缺省空串）", example = "张三")
					@RequestParam(name = "username", required = false)
					String usernameParam,
			@Parameter(
							description = "头像 URL（缺省空串；示例为格式演示值）",
							example = "https://example.test/key.jpg")
					@RequestParam(name = "avatar", required = false)
					String avatarParam,
			@Parameter(
							description = "性别；仅 0 可成功（非 0 E5001）；缺省 0",
							example = "0")
					@RequestParam(name = "sex", required = false)
					String sexParam,
			@Parameter(
							description = "生日 yyyy-MM-dd（datetime 形态落库失败）；可缺省（格式演示值）",
							example = "1990-01-01")
					@RequestParam(name = "birthday", required = false)
					String birthdayParam,
			@Parameter(
							description = "学历枚举 0-4；缺省 4；越界 E5001",
							example = "4")
					@RequestParam(name = "edu_background", required = false)
					String eduBackgroundParam,
			@Parameter(
							description = "年收入枚举 0-4；缺省 4；越界 E5001",
							example = "4")
					@RequestParam(name = "income", required = false)
					String incomeParam,
			@Parameter(
							description = "行业枚举 0-12；缺省 12；越界 E5001",
							example = "12")
					@RequestParam(name = "industry", required = false)
					String industryParam,
			@Parameter(
							description = "邮箱；非空须格式（E5001；示例为格式演示值）",
							example = "zhangsan@example.test")
					@RequestParam(name = "email", required = false)
					String emailParam,
			@Parameter(description = "地址（缺省空串）")
					@RequestParam(name = "address", required = false)
					String addressParam,
			@Parameter(description = "备注（缺省空串）")
					@RequestParam(name = "remarks", required = false)
					String remarksParam,
			@Parameter(
							description = "来源渠道；仅 default/openapi（缺省 openapi）；其他 E5001",
							example = "openapi")
					@RequestParam(name = "source_from", required = false)
					String sourceFromParam,
			@RequestBody(
							description =
									"可选 JSON body；20 键与 query 同名双通道（body 含键取原值）+ habbit 仅 body；"
											+ "同步创建链与忽略键语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> raw =
				OpenapiMemberV2CreateHandlerParams.mergeAll(
						body,
						mobileParam,
						inviterMobileParam,
						salespersonMobileParam,
						unionIdParam,
						statusParam,
						tagNamesParam,
						tagIdsParam,
						cardCodeParam,
						gradeIdParam,
						usernameParam,
						avatarParam,
						sexParam,
						birthdayParam,
						eduBackgroundParam,
						incomeParam,
						industryParam,
						emailParam,
						addressParam,
						remarksParam,
						sourceFromParam);
		if (body != null && body.containsKey("habbit")) {
			raw.put("habbit", body.get("habbit"));
		}
		return memberCreateDetailPort.createDetail(companyId, raw);
	}

	@Operation(
			summary = "开放接口批量创建会员异步",
			description =
					"按签名鉴权租户 company_id 批量创建会员（异步）：仅 data 一键双通道"
							+ "（body 含 data 键或 query 参数表含 data 即受理；值 originalString，body 数组字符串化后 JSON 解析）。"
							+ " data 键缺席 → data.status=0（不报错不入队）；键存在但空白/解析失败/非数组 → 空列表"
							+ " → data.status=1（0 条入队，解析失败静默）；元素非对象静默丢弃；"
							+ " >50 条 E5500「批量创建最多支持50个！」。"
							+ " 逐条**同步校验**（复用单个创建 validateAndNormalize：手机号必填/格式（格式错 E5502"
							+ "「会员手机号已存在」PHP 文案）、source_from 仅 default/openapi、推荐人/导购手机号格式、"
							+ " union_id 须字符串、status 0|1、sex 仅 0、birthday 格式、habbit 结构、"
							+ " edu 0-4/income 0-4/industry 0-12、email 格式、grade_id 非负整数），"
							+ " 任一条失败整批拒绝未入队；全部通过后逐条入队 dispatch job 异步创建，返回 data.status=1。"
							+ " **占用/推荐人查无/导购/等级/卡号类校验在异步 job 内执行**"
							+ "（E5502 占用/E5511/E5601/E5612/E5531/E5522），失败不影响同步响应。"
							+ " tag_ids 不被读取（标签仅 tag_names 逗号分隔字符串）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.batch_create,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：同步阶段无写库；逐条发布 CREATE_MEMBER_JOB 异步任务"
							+ "（job 内 INSERT members/members_info/标签关联/导购绑定/unionid 关联 + 新客标签对账）。",
			operationId = "ecx.member.batch_create.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data={status:0|1}"
								+ "（0=data 键缺席；1=已受理入队，条数可为 0）；"
								+ " 业务失败：status=fail, code=E5500 批量创建最多支持50个！/ E5502 会员手机号已存在（格式）/"
								+ " E5001 各字段填写错误 / E5000 系统错误，data=null（整批拒绝未入队）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberBatchCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberBatchCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success-enqueued",
											summary = "受理入队（异步创建）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":1}}"),
									@ExampleObject(
											name = "success-no-data-key",
											summary = "data 键缺席（未受理，data.status=0）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":0}}"),
									@ExampleObject(
											name = "fail-over-limit",
											summary = "超过 50 条",
											value =
													"{\"status\":\"fail\",\"code\":\"E5500\","
															+ "\"message\":\"批量创建最多支持50个！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "某条 mobile 格式错（整批拒绝；PHP 文案「已存在」）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5502\","
															+ "\"message\":\"会员手机号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-source-from",
											summary = "source_from 非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"来源渠道填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.batch_create", name = "开放接口批量创建会员异步")
	public Map<String, Object> batchCreate(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"会员数组 JSON 字符串（≤50 条，元素字段见 body schema）；键缺席返回 data.status=0；"
											+ "解析失败静默按空数组（data.status=1，0 条入队）",
							example = "[{\"mobile\":\"13800138000\"}]")
					@RequestParam(name = "data", required = false)
					String dataParam,
			@RequestBody(
							description =
									"可选 JSON body；data 与 query 同名双通道（body 含键优先，可直接传数组）；"
											+ "元素 20 键与同步/异步校验分界见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberBatchCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean hasData =
				OpenapiMemberV2BatchCreateParams.hasDataParameter(request, body, dataParam);
		if (!hasData) {
			return memberBatchCreatePort.batchCreate(companyId, false, List.of());
		}
		String dataRaw = OpenapiRequestParams.originalString(dataParam, body, "data");
		List<Map<String, Object>> items =
				OpenapiMemberV2BatchCreateParams.parseDataArray(dataRaw);
		return memberBatchCreatePort.batchCreate(companyId, true, items);
	}

	@Operation(
			summary = "开放接口修改会员等级",
			description =
					"按签名鉴权租户 company_id 修改会员等级（数云对接语义，3 键双通道均必填，body 含键取原值）："
							+ " plat_account 为平台会员 user_id（数值/可 parseDouble 字符串，小数截断；"
							+ " 否则 E5001「商派会员Id必填」；查无 E5501）；"
							+ " **grade_id 为外部等级标识 external_id**（非平台内部等级 ID；数值校验后 trim 字符串化"
							+ " 按 company_id+external_id 查 membercard_grade，查无 E5531「会员等级找不到」；"
							+ " 小数保留字符串形态匹配，\"1.0\"≠\"1\"）；"
							+ " grade_level 须非空白**字符串**（body 直传数字因类型校验报 E5001「等级参数必填」），"
							+ " 仅校验不落库不参与升级判定。"
							+ " 更新 members.grade_id/updated（affected=0 → E5501）；"
							+ " 升级判定按库内新旧等级 promotion_condition.total_consumption（PHP 式取整）比较："
							+ " 新门槛>旧门槛 且 新 grade_id>旧 grade_id 时事务提交后发布 member_upgrade 升级活动"
							+ "（异步 dispatch，载荷含 grade_id/user_id/mobile 明文/grade_name）；同级/降级不触发。"
							+ " 其他异常统一 E5000「系统错误」。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.card_grade.update,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members.grade_id/updated +"
							+ " 条件性 afterCommit 发布升级活动异步任务；无第三方同步调用。",
			operationId = "ecx.member.card_grade.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含同级/降级）；"
								+ " 业务失败：status=fail, code=E5001 商派会员Id必填·等级ID参数必填·等级参数必填 /"
								+ " E5531 会员等级找不到 / E5501 会员找不到 / E5000 系统错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberGradeChangeFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberGradeChangeSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-not-found",
											summary = "external_id 查无等级",
											value =
													"{\"status\":\"fail\",\"code\":\"E5531\","
															+ "\"message\":\"会员等级找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "plat_account 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-level-type",
											summary = "grade_level 传数值（须字符串）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"等级参数必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.card_grade.update", name = "开放接口修改会员等级")
	public Void updateGrade(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "商派会员 Id（user_id）；必填数值（小数截断），否则 E5001「商派会员Id必填」；查无 E5501",
							required = true,
							example = "50")
					@RequestParam(name = "plat_account", required = false)
					String platAccountParam,
			@Parameter(
							description = "外部等级标识（external_id，非内部等级 ID）；必填数值校验后按字符串匹配；查无 E5531",
							required = true,
							example = "1")
					@RequestParam(name = "grade_id", required = false)
					String gradeIdParam,
			@Parameter(
							description = "等级门槛；必填非空白字符串（body 传数字报 E5001）；仅校验不参与业务",
							required = true,
							example = "0")
					@RequestParam(name = "grade_level", required = false)
					String gradeLevelParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（body 含键取原值）；external_id 语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberGradeChangeBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateGradeHandlerParams.mergeAll(
						body, platAccountParam, gradeIdParam, gradeLevelParam);
		memberV2UpdateGradePort.updateGrade(companyId, merged);
		return null;
	}

	@Operation(
			summary = "开放接口修改会员卡号等级",
			description =
					"按签名鉴权租户 company_id 以手机号定位会员，局部更新卡号/等级（3 键双通道，body 含键优先）："
							+ " mobile 必填，trim 后须 ^1[3456789][0-9]{9}$（E5001「手机号必填」/「手机号填写错误」）；"
							+ " user_card_code/grade_id 按键存在性 PATCH——两键均缺席时无操作成功（mobile 仍先校验）。"
							+ " user_card_code：body 值须字符串或 null（否则 E5001「会员卡号参数错误」）；trim 非空直接用，"
							+ " null/空白自动生成 12 位大写随机码（Redis zset usercardcode 去重，3 次冲突→E5000）；"
							+ " 被其他手机号会员占用 E5522「会员卡号已存在」（占用者即本手机号放行）。"
							+ " grade_id：须数值/可 parseDouble 有限数字字符串（否则 E5001「等级ID参数错误」），小数截断；"
							+ " membercard_grade 查无 E5531「会员等级找不到」。"
							+ " 会员按固定加密 mobile 等值定位，查无或 UPDATE affected=0 → E5501「会员找不到」。"
							+ " 注意：query 参数 card_code 被接收但忽略（未并入 mergeAll），改卡号须用 user_card_code。"
							+ " 其他异常统一 E5000「系统错误」。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_code_grade.update, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members.user_card_code/grade_id/updated；"
							+ " 空卡号时 Redis zset usercardcode 写入随机码；无第三方调用。",
			operationId = "ecx.member_card_code_grade.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含两 PATCH 键均缺席的无操作场景）；"
								+ " 业务失败：status=fail, code=E5001 手机号必填·手机号填写错误·会员卡号参数错误·等级ID参数错误 /"
								+ " E5501 会员找不到 / E5522 会员卡号已存在 / E5531 会员等级找不到 / E5000 系统错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardCodeGradeUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardCodeGradeUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "mobile 格式错误",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"手机号填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-card-exist",
											summary = "卡号被其他会员占用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5522\","
															+ "\"message\":\"会员卡号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-not-found",
											summary = "grade_id 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5531\","
															+ "\"message\":\"会员等级找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PatchMapping(value = "/ecx.member_card_code_grade.update", name = "开放接口修改会员卡号等级")
	public Void updateCardCodeAndGrade(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号（明文）；必填，trim 后须 ^1[3456789][0-9]{9}$；查无会员 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "新会员卡号；键存在即更新；空白/null 自动生成 12 位随机码；他人占用 E5522（详见 body schema）",
							example = "card")
					@RequestParam(name = "user_card_code", required = false)
					String userCardCodeParam,
			@Parameter(
							description = "遗留参数：被接收但忽略（未并入 mergeAll），修改卡号请使用 user_card_code",
							deprecated = true)
					@RequestParam(name = "card_code", required = false)
					String cardCodeParam,
			@Parameter(
							description = "新会员等级 ID；键存在即更新；数字（小数截断），membercard_grade 查无 E5531",
							example = "1")
					@RequestParam(name = "grade_id", required = false)
					String gradeIdParam,
			@RequestBody(
							description =
									"可选 JSON body；3 键与 query 同名双通道（body 含键优先，PATCH 键原值保留）；"
											+ "card_code 键不被读取；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCardCodeGradeUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateCardCodeGradeHandlerParams.mergeAll(
						body, mobileParam, userCardCodeParam, gradeIdParam);
		memberV2UpdateCardCodeGradePort.updateCardCodeAndGrade(companyId, merged);
		return null;
	}

	@Operation(
			summary = "开放接口修改会员手机号",
			description =
					"按签名鉴权租户 company_id 修改会员手机号（3 键双通道，body 含键取原值否则 query mergeString trim）："
							+ " mobile 定位旧会员，必填（格式错统一 E5502「会员手机号已存在」——PHP 旧契约文案，"
							+ " 实为格式校验；查无 E5501「会员找不到」）；"
							+ " new_mobile 必填：null/空白/\"0\"/false/数值 0 报 E5001「会员新手机号参数错误」，"
							+ " 归一化（整型数值转长整字符串、trim）后须 ^1[3456789][0-9]{9}$"
							+ "（否则 E5001「新手机号填写错误」）；被其他会员占用报 E5502「会员新手机号已存在」"
							+ "（占用者即本人放行——同号重设幂等成功，仅刷新 updated，不写操作日志）。"
							+ " user_id 被接收但忽略（定位仅按 mobile）。"
							+ " 其他异常统一 E5000「系统错误」。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_mobile.update,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members.mobile（固定加密存储）/updated +"
							+ " 新旧号不同时 INSERT member_operate_log（operate_type=mobile，明文新旧号，"
							+ " operater=外部开发者）；无异步、无第三方调用。",
			operationId = "ecx.member_mobile.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含同号重设幂等场景）；"
								+ " 业务失败：status=fail, code=E5502 会员手机号已存在（格式）·会员新手机号已存在（占用）/"
								+ " E5001 会员新手机号参数错误·新手机号填写错误 / E5501 会员找不到 / E5000 系统错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberMobileUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberMobileUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-new-mobile-exist",
											summary = "新号被其他会员占用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5502\","
															+ "\"message\":\"会员新手机号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "mobile 缺失/格式错（PHP 旧契约文案「已存在」）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5502\","
															+ "\"message\":\"会员手机号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-new-mobile-format",
											summary = "new_mobile 格式错误",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"新手机号填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PatchMapping(value = "/ecx.member_mobile.update", name = "开放接口修改会员手机号")
	public Void updateMobile(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "遗留参数：被接收但服务不消费（定位仅按 mobile）",
							deprecated = true)
					@RequestParam(name = "user_id", required = false)
					String userIdParam,
			@Parameter(
							description = "当前会员手机号（定位键）；必填，格式错统一 E5502「会员手机号已存在」；查无 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "新手机号；必填（空/\"0\"/false E5001 参数错误）；格式错 E5001；他人占用 E5502「会员新手机号已存在」",
							required = true,
							example = "13800000000")
					@RequestParam(name = "new_mobile", required = false)
					String newMobileParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（body 含键取原值）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberMobileUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateMobileHandlerParams.mergeAll(
						body, userIdParam, mobileParam, newMobileParam);
		memberV2UpdateMobilePort.updateMemberMobile(companyId, merged);
		return null;
	}

	@Operation(
			summary = "开放接口修改会员信息",
			description =
					"按签名鉴权租户 company_id 以手机号定位会员并**全量覆盖**更新信息（15 键：14 键双通道"
							+ " body 含键取原值否则 query mergeString trim；habbit 仅 body）。"
							+ " mobile 必填（格式错统一 E5502「会员手机号已存在」——PHP 旧契约文案，实为格式校验；"
							+ " 查无 E5501）；birthday **实际必填**（缺失/空串 E5001「生日格式填写错误」，"
							+ " 仅 yyyy-MM-dd 可用，datetime 形态过输入校验但落库重解析失败）。"
							+ " 全量覆盖：未传键回落默认值——inviter_id 清 0、status=1（会把禁用会员重新启用）、"
							+ " sex=0、username/avatar/email/address 空串、habbit=[]、edu=4/income=4/industry=12，"
							+ " 不保留原值。sex 仅 0 可成功（action 层仅许 0，非 0 报 E5001「性别填写错误」，"
							+ " 服务层 0/1/2 枚举不可达）。user_id/remarks 被接收但忽略（remarks 仍做字符串类型校验）。"
							+ " 推荐人非空非\"0\"查无 E5511；各字段键存在时校验（详见 body schema）。"
							+ " 其他异常统一 E5000「系统错误」（事务回滚）。成功恒 data=null，Advice message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_info.update,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members(inviter_id/disabled/updated) +"
							+ " UPDATE members_info（username 加密/avatar/sex/email/address/edu/income/industry/habbit/"
							+ " birthday+year/month/day/updated）+ 新客系统标签对账（禁用摘除/启用按有效已付单判定）"
							+ " 变更差异写 member_operate_log（operater=外部开发者）；无第三方调用。",
			operationId = "ecx.member_info.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null；"
								+ " 业务失败：status=fail, code=E5502 会员手机号已存在（格式校验）/ E5001 各字段填写错误 /"
								+ " E5501 会员找不到 / E5511 会员的推荐人找不到 / E5000 系统错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberInfoUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberInfoUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "mobile 缺失/格式错（PHP 旧契约文案「已存在」）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5502\","
															+ "\"message\":\"会员手机号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-birthday-missing",
											summary = "birthday 缺失/空串（实际必填）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"生日格式填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-sex-nonzero",
											summary = "sex 传 1/2（action 层仅许 0）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"性别填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-inviter-not-found",
											summary = "推荐人手机号查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5511\","
															+ "\"message\":\"会员的推荐人找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PatchMapping(value = "/ecx.member_info.update", name = "开放接口修改会员信息")
	public Void updateDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "遗留参数：被接收但服务不消费（定位仅按 mobile）",
							deprecated = true)
					@RequestParam(name = "user_id", required = false)
					String userIdParam,
			@Parameter(
							description = "会员手机号（定位键）；必填，格式错统一 E5502「会员手机号已存在」；查无 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "推荐人手机号；非空须格式（E5001）；查无 E5511；未传/\"0\" 清空推荐人（全量覆盖）",
							example = "13800138000")
					@RequestParam(name = "inviter_mobile", required = false)
					String inviterMobileParam,
			@Parameter(
							description = "会员状态 0 禁用/1 启用；未传默认 1（会重新启用已禁用会员）；其他值 E5001",
							example = "1")
					@RequestParam(name = "status", required = false)
					String statusParam,
			@Parameter(
							description = "遗留参数：被接收并做字符串类型校验，但不落库",
							deprecated = true)
					@RequestParam(name = "remarks", required = false)
					String remarksParam,
			@Parameter(
							description = "姓名/昵称；恒写（加密），未传写空串清空；非字符串 E5001",
							example = "张三")
					@RequestParam(name = "username", required = false)
					String usernameParam,
			@Parameter(
							description = "头像 URL；恒写，未传写空串清空；非字符串 E5001（示例为格式演示值）",
							example = "https://example.test/key.jpg")
					@RequestParam(name = "avatar", required = false)
					String avatarParam,
			@Parameter(
							description = "性别；仅 0 可成功（非 0 恒 E5001「性别填写错误」）；未传按 0",
							example = "0")
					@RequestParam(name = "sex", required = false)
					String sexParam,
			@Parameter(
							description = "生日 yyyy-MM-dd；实际必填（缺失/空串 E5001「生日格式填写错误」；示例为格式演示值）",
							required = true,
							example = "1990-01-01")
					@RequestParam(name = "birthday", required = false)
					String birthdayParam,
			@Parameter(
							description = "学历枚举 0-4；未传默认 4（恒写覆盖）；越界 E5001",
							example = "4")
					@RequestParam(name = "edu_background", required = false)
					String eduBackgroundParam,
			@Parameter(
							description = "年收入枚举 0-4；未传默认 4（恒写覆盖）；越界 E5001",
							example = "4")
					@RequestParam(name = "income", required = false)
					String incomeParam,
			@Parameter(
							description = "行业枚举 0-12；未传默认 12（恒写覆盖）；越界 E5001",
							example = "12")
					@RequestParam(name = "industry", required = false)
					String industryParam,
			@Parameter(
							description = "邮箱；非空须格式（E5001）；恒写，未传写空串清空（示例为格式演示值）",
							example = "zhangsan@example.test")
					@RequestParam(name = "email", required = false)
					String emailParam,
			@Parameter(
							description = "地址；恒写，未传写空串清空；非字符串 E5001")
					@RequestParam(name = "address", required = false)
					String addressParam,
			@RequestBody(
							description =
									"可选 JSON body；14 键与 query 同名双通道（body 含键取原值）+ habbit 仅 body；"
											+ "全量覆盖语义与忽略键/校验缺口见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberInfoUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateDetailHandlerParams.mergeAll(
						body,
						userIdParam,
						mobileParam,
						inviterMobileParam,
						statusParam,
						remarksParam,
						usernameParam,
						avatarParam,
						sexParam,
						birthdayParam,
						eduBackgroundParam,
						incomeParam,
						industryParam,
						emailParam,
						addressParam);
		if (body != null && body.containsKey("habbit")) {
			merged.put("habbit", body.get("habbit"));
		}
		Map<String, Object> requestData = OpenapiMemberV2UpdateDetailHandlerParams.buildRequestData(merged);
		memberV2UpdateDetailPort.updateMemberInfo(companyId, merged, requestData);
		return null;
	}

	@GetMapping(value = "/ecx.member.detail", name = "开放接口查询会员详情")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobile = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberV2DetailPort.getMemberDetail(companyId, mobile);
	}

	@GetMapping(value = "/ecx.member.list", name = "开放接口查询会员批量")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "source_from", required = false) String sourceFromParam,
			@RequestParam(name = "inviter_mobile", required = false) String inviterMobileParam,
			@RequestParam(name = "salesperson_mobile", required = false) String salespersonMobileParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "tag_id", required = false) String tagIdParam,
			@RequestParam(name = "tag_name", required = false) String tagNameParam,
			@RequestParam(name = "have_consume", required = false) String haveConsumeParam,
			@RequestParam(name = "card_code", required = false) String cardCodeParam,
			@RequestParam(name = "user_card_code", required = false) String userCardCodeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@RequestParam(name = "vip_grade_name", required = false) String vipGradeNameParam,
			@RequestParam(name = "start_date", required = false) String startDateParam,
			@RequestParam(name = "end_date", required = false) String endDateParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> raw =
				OpenapiMemberV2ListHandlerParams.mergePresentKeys(
						body,
						mobileParam,
						sourceFromParam,
						inviterMobileParam,
						salespersonMobileParam,
						statusParam,
						tagIdParam,
						tagNameParam,
						haveConsumeParam,
						cardCodeParam,
						userCardCodeParam,
						gradeIdParam,
						gradeNameParam,
						vipGradeIdParam,
						vipGradeNameParam,
						startDateParam,
						endDateParam);
		return memberV2ListPort.listMembers(
				companyId, pageSpec.page(), pageSpec.pageSize(), raw);
	}

	@Operation(
			summary = "开放接口会员等级关联会员列表",
			description =
					"按签名鉴权租户 company_id 分页查询等级关联会员：4 键与 query 同名双通道"
							+ "（body 含键优先、值 null 视为未传、不 trim）。"
							+ " grade_id 与 vip_grade_id 恰一键存在（按键存在性判定，与值无关）："
							+ " 都缺 E5001「会员等级ID参数错误」；同传 E5001「会员等级ID与付费会员等级ID不能同时存在」；"
							+ " 值须严格整数 ^-?\\d+$（允许负数，空串/小数/非数字分别报 E5001"
							+ "「会员等级ID参数错误」/「付费会员等级ID参数错误」）；两分支均不校验等级存在。"
							+ " grade 分支：members.grade_id 等值 + DISTINCT，user_id 降序；"
							+ " vip 分支：vip_grade_rel_user(company_id, vip_grade_id) 等值、不过滤 end_date"
							+ "（过期关联仍计入），user_id 降序，total_count 为关联行数（非去重），list 可重复会员。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），page_size 原始解析 ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data 键序 total_count/list/is_last_page(0|1)/pager{page,page_size}；"
							+ " 行仅 mobile/username 两键（密文解密明文，查无空串；user_id 不透出）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_grade_rel.list, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " DB 等未处理异常无本服务兜底 → HTTP 500 ApiResult{code:500,msg:服务器内部错误}。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_card_grade_rel.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberCardGradeRelListDataDoc；"
								+ " 业务失败：status=fail, code=E5001 会员等级ID参数错误·两ID不能同时存在·付费会员等级ID参数错误 /"
								+ " E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardGradeRelListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardGradeRelListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "grade_id 分支查询成功（示例值取自 members 测试夹具）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,"
															+ "\"list\":[{\"mobile\":\"13800138000\",\"username\":\"张三\"}],"
															+ "\"is_last_page\":1,\"pager\":{\"page\":1,\"page_size\":20}}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "等级查无关联会员（list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"list\":[],\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20}}}"),
									@ExampleObject(
											name = "fail-both-ids",
											summary = "grade_id 与 vip_grade_id 同传",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"会员等级ID与付费会员等级ID不能同时存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-id",
											summary = "两 ID 键均缺席",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"会员等级ID参数错误\",\"data\":null}"),
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
	@GetMapping(value = "/ecx.member_card_grade_rel.list", name = "开放接口会员等级关联会员列表")
	public Map<String, Object> listByGradeOrVipGrade(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)，负数/0/非法按 1",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E5002「每页的大小最大为500！」；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "普通会员等级 ID；与 vip_grade_id 互斥恰一（键存在性）；严格整数 ^-?\\d+$，否则 E5001；不校验等级存在",
							example = "1")
					@RequestParam(name = "grade_id", required = false)
					String gradeIdParam,
			@Parameter(
							description = "付费会员等级 ID；与 grade_id 互斥恰一；严格整数，否则 E5001「付费会员等级ID参数错误」；不过滤过期关联",
							example = "2")
					@RequestParam(name = "vip_grade_id", required = false)
					String vipGradeIdParam,
			@RequestBody(
							description = "可选 JSON body；4 键与 query 同名双通道（body 含键优先，值 null 视为未传）；互斥语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCardGradeRelListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean gradeIdPresent =
				OpenapiMemberQueryParams.isParamPresent(gradeIdParam, body, "grade_id");
		String gradeIdRaw = OpenapiRequestParams.originalString(gradeIdParam, body, "grade_id");
		boolean vipGradeIdPresent =
				OpenapiMemberQueryParams.isParamPresent(vipGradeIdParam, body, "vip_grade_id");
		String vipGradeIdRaw =
				OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id");

		return memberV2ListByGradeOrVipGradePort.listByGradeOrVipGrade(
				companyId,
				gradeIdPresent,
				gradeIdRaw,
				vipGradeIdPresent,
				vipGradeIdRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}
}
