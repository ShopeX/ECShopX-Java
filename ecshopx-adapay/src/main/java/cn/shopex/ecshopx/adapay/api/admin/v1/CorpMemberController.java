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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayAlipayIndustryCatListAuthFailEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayAlipayIndustryCatListForbiddenEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayAlipayIndustryCatListServerErrorEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberCreateBizFailEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberCreateBodyDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberModifyBodyDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCorpMemberModifySuccessEnvelopeDoc;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberCreateService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberModifyService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberReadService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberUpdateService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Adapay 企业用户", description = "汇付 Adapay 企业开户用户对象的创建、修改与查询")
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("corpMemberAdminV1")
@RequestMapping("/api/v1/adapay")
public class CorpMemberController {

	private final AdapayCorpMemberCreateService adapayCorpMemberCreateService;
	private final AdapayCorpMemberModifyService adapayCorpMemberModifyService;
	private final AdapayCorpMemberUpdateService adapayCorpMemberUpdateService;
	private final AdapayCorpMemberReadService adapayCorpMemberReadService;

	public CorpMemberController(
			AdapayCorpMemberCreateService adapayCorpMemberCreateService,
			AdapayCorpMemberModifyService adapayCorpMemberModifyService,
			AdapayCorpMemberUpdateService adapayCorpMemberUpdateService,
			AdapayCorpMemberReadService adapayCorpMemberReadService) {
		this.adapayCorpMemberCreateService = adapayCorpMemberCreateService;
		this.adapayCorpMemberModifyService = adapayCorpMemberModifyService;
		this.adapayCorpMemberUpdateService = adapayCorpMemberUpdateService;
		this.adapayCorpMemberReadService = adapayCorpMemberReadService;
	}

	@Operation(
			summary = "获取企业用户对象",
			description =
					"按 JWT 租户 company_id + operator_id 查询 adapay_corp_member（id 升序取第一条），"
							+ "返回企业开户详情 34 固定键 + 3 条件键：敏感字段 legal_person/legal_cert_id/legal_mp/card_no"
							+ " 解密后明文；operator_id 库值 null/0 → null；prov_code 空白 → null；"
							+ "area_code null → 空串；disabled_type 恒空串；div_fee_mode 恒「内扣」。"
							+ " 条件键：bank_name（bank_code 非空白才出现，反查 adapay_bank_codes，查无空串）、"
							+ "attach_file_url / confirm_letter_file_url（对应路径非空白才出现，"
							+ "私有下载链接 3600 秒，local 驱动为直链）。"
							+ " 无 query/path/body 入参。查无记录报「用户信息不存在」"
							+ "（BadRequest + DINGO_422：HTTP 200 + data.status_code=422）。"
							+ " 鉴权面 admin：Operator JWT；JWT 缺 company_id/operator_id 报 401「未登录」。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "adapay.corp_member.info")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200。成功：ApiResult code=200、msg=success、data 见 AdapayCorpMemberGetData；"
								+ "业务失败也走 HTTP 200：Dingo 信封 data.message=用户信息不存在 + data.status_code=422。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												oneOf = {
													AdapayCorpMemberGetSuccessEnvelopeDoc.class,
													AdapayCorpMemberCreateBizFailEnvelopeDoc.class
												}),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（对公已开户；字段值为按代码规则/种子数据构造的样例）",
											value =
													"{\"code\":200,\"msg\":\"success\",\"data\":{\"id\":1,\"app_id\":\"app-x\","
															+ "\"order_no\":\"20231127120000123456\",\"member_id\":1,"
															+ "\"company_id\":1,\"operator_id\":1,\"name\":\"示例科技有限公司\","
															+ "\"prov_code\":\"0011\",\"area_code\":\"1100\","
															+ "\"social_credit_code\":\"91110000123456789X\","
															+ "\"social_credit_code_expires\":\"20991231\","
															+ "\"business_scope\":\"软件开发与销售\",\"legal_person\":\"李四\","
															+ "\"legal_cert_id\":\"110101199001011234\","
															+ "\"legal_cert_id_expires\":\"20991231\","
															+ "\"legal_mp\":\"13800138000\",\"address\":\"北京市东城区示例路1号\","
															+ "\"zip_code\":\"100000\",\"telphone\":\"010-12345678\","
															+ "\"email\":\"a@b.com\","
															+ "\"attach_file\":\"adapay/1/1700000000/0123456789abcdef0123456789abcdef.zip\","
															+ "\"attach_file_name\":\"attach.zip\",\"confirm_letter_file\":\"\","
															+ "\"confirm_letter_file_name\":\"\",\"bank_code\":\"01020000\","
															+ "\"bank_acct_type\":\"1\",\"card_no\":\"622200\","
															+ "\"card_name\":\"示例科技有限公司\",\"audit_state\":\"E\","
															+ "\"audit_desc\":\"审核通过\",\"create_time\":1700000000,"
															+ "\"update_time\":1700000000,\"disabled_type\":\"\","
															+ "\"bank_name\":\"中国工商银行\","
															+ "\"attach_file_url\":\"https://example.com/storage/adapay/1/1700000000/"
															+ "0123456789abcdef0123456789abcdef.zip\",\"div_fee_mode\":\"内扣\"}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "查无企业用户（HTTP 200，Dingo 422 信封）",
											value = "{\"data\":{\"message\":\"用户信息不存在\",\"status_code\":422}}")
								})),
		@ApiResponse(
				responseCode = "401",
				description =
						"鉴权失败：过滤器/拦截器 401 Dingo 信封（data.message/data.status_code=401，无业务 code）；"
								+ "JWT 缺 company_id/operator_id 时本方法抛「未登录」，经 unauthorized=true 包装。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListAuthFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "missing-credentials",
											summary = "缺失/非法 Authorization 头",
											value =
													"{\"data\":{\"message\":\"Failed to authenticate because of bad"
															+ " credentials or an invalid authorization header.\","
															+ "\"status_code\":401}}"),
									@ExampleObject(
											name = "invalid-token",
											summary = "token 过期/校验失败",
											value = "{\"data\":{\"message\":\"登录验证错误\",\"status_code\":401}}")
								})),
		@ApiResponse(
				responseCode = "403",
				description = "租户/账号校验失败（拦截器 ApiResult.fail(403, msg)，data 省略）。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListForbiddenEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "forbidden",
												value = "{\"code\":403,\"msg\":\"未登录\"}"))),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常（GlobalExceptionHandler.handleException），固定文案。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListServerErrorEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "server-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@Activated(routeAlias = "adapay.corp_member.info")
	@GetMapping(value = "/corp_member/get", name = "获取企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(
			@Parameter(hidden = true) HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Object operatorIdRaw = jwtMap.get("operator_id");
		if (!(operatorIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		int operatorId = ((Number) operatorIdRaw).intValue();

		Map<String, Object> data = adapayCorpMemberReadService.get(companyId, operatorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Operation(
			summary = "创建企业用户对象",
			description =
					"按 JWT 租户 company_id 创建 Adapay 企业开户用户（本地落库，不调 AdaPay 出站）："
							+ " 写 adapay_member（member_type=corp、audit_state=0）、adapay_corp_member（order_no="
							+ "yyyyMMddHHmmss+6位随机）、adapay_settle_account（channel=bank_account、cert_type=00）、"
							+ "adapay_entry_apply（status=WAIT_APPROVE）与操作日志；附件 zip 存文件存储"
							+ " adapay/{company_id}/{ts}/{md5}.zip。"
							+ " 字段双通道：@FlexibleBody（JSON body 或 form/query 参数 Map）18 键 + multipart 文件 part"
							+ " attach_file（必填，仅 .zip，<8.5MiB）与 confirm_letter_file（可选，仅大小校验，"
							+ "创建流程不落库该文件）。所有字段 toString+trim；name≤30、address≤60、business_scope≤500、"
							+ "legal_person≤20、zip_code≤6 码点截断；card_name≤20 码点否则报错。"
							+ " area 须为「省code,市code」两段（adapay_regions 四位码）；bank_code 须存在于 adapay_bank_codes；"
							+ " bank_acct_type=1（对公）时 card_name 必须等于 name。"
							+ " 鉴权面 admin：Operator JWT；JWT 无 company_id 或 ≤0 报 401「未登录」（Dingo 401 信封）。"
							+ " 失败信封：BadRequest/Resource 均 HTTP 200 + data.message/data.status_code=422"
							+ "（DINGO/DINGO_422 风格）。成功 data 固定 {status:true}。",
			operationId = "adapay.corp_member.create")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200。成功：ApiResult code=200、msg=success、data={status:true}；"
								+ "业务/参数失败也走 HTTP 200：Dingo 信封 data.message + data.status_code=422"
								+ "（如 企业名称必填 / 银行账户类型错误 / 法人身份证号码必须是18位 / 附件必须上传 /"
								+ " 附件不能超过8M / 请选择正确的结算银行卡所属银行 / 银行卡对应的户名，必须与企业名称一致 /"
								+ " 地区数据格式错误 / 银行编号不存在 / 地区编号不存在 / 用户信息保存失败 等）。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												oneOf = {
													AdapayCorpMemberCreateSuccessEnvelopeDoc.class,
													AdapayCorpMemberCreateBizFailEnvelopeDoc.class
												}),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "创建成功",
											value = "{\"code\":200,\"msg\":\"success\",\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-name-required",
											summary = "参数失败（HTTP 200，Dingo 422 信封）",
											value = "{\"data\":{\"message\":\"企业名称必填\",\"status_code\":422}}"),
									@ExampleObject(
											name = "fail-card-name",
											summary = "对公户名与企业名称不一致",
											value =
													"{\"data\":{\"message\":\"银行卡对应的户名，必须与企业名称一致\","
															+ "\"status_code\":422}}"),
									@ExampleObject(
											name = "fail-attach",
											summary = "附件缺失",
											value = "{\"data\":{\"message\":\"附件必须上传\",\"status_code\":422}}")
								})),
		@ApiResponse(
				responseCode = "401",
				description =
						"鉴权失败：过滤器/拦截器 401 Dingo 信封（data.message/data.status_code=401，无业务 code）；"
								+ "JWT 缺 company_id 时本方法抛「未登录」，经 unauthorized=true 包装为固定英文句 + code=401001。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListAuthFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "missing-credentials",
											summary = "缺失/非法 Authorization 头",
											value =
													"{\"data\":{\"message\":\"Failed to authenticate because of bad"
															+ " credentials or an invalid authorization header.\","
															+ "\"status_code\":401}}"),
									@ExampleObject(
											name = "invalid-token",
											summary = "token 过期/校验失败",
											value = "{\"data\":{\"message\":\"登录验证错误\",\"status_code\":401}}")
								})),
		@ApiResponse(
				responseCode = "403",
				description = "租户/账号校验失败（拦截器 ApiResult.fail(403, msg)，data 省略）。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListForbiddenEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "forbidden",
												value = "{\"code\":403,\"msg\":\"未登录\"}"))),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常（GlobalExceptionHandler.handleException），固定文案。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListServerErrorEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "server-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@Activated(routeAlias = "adapay.corp_member.create")
	@PostMapping(value = "/corp_member/create", name = "创建企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description =
									"企业用户字段（JSON 或 form 双通道；multipart 时字段放 form part）。"
											+ "18 键语义与校验见 schema；附件走独立文件 part",
							required = true,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	AdapayCorpMemberCreateBodyDoc.class),
											examples =
													@ExampleObject(
															name = "corp-create",
															summary = "对公开户（字段值为按校验规则/种子数据构造的样例）",
															value =
																	"{\"name\":\"示例科技有限公司\",\"area\":\"0011,1100\","
																			+ "\"email\":\"a@b.com\",\"bank_acct_type\":\"1\","
																			+ "\"social_credit_code\":\"91110000123456789X\","
																			+ "\"social_credit_code_expires\":\"20991231\","
																			+ "\"business_scope\":\"软件开发与销售\",\"legal_person\":\"李四\","
																			+ "\"legal_cert_id\":\"110101199001011234\","
																			+ "\"legal_cert_id_expires\":\"20991231\","
																			+ "\"legal_mp\":\"13800138000\",\"address\":\"北京市东城区示例路1号\","
																			+ "\"zip_code\":\"100000\",\"telphone\":\"010-12345678\","
																			+ "\"bank_code\":\"01020000\",\"card_no\":\"622200\","
																			+ "\"card_name\":\"示例科技有限公司\",\"submit_review\":\"\"}")))
					@FlexibleBody Map<String, Object> body,
			@Parameter(
							description =
									"开户附件（multipart 文件 part）；必填；仅 .zip 扩展名（否则「文件类型不符合要求」），"
											+ "大小 <8.5MiB（≥ 报「附件不能超过8M」）；缺失报「附件必须上传」",
							required = true,
							schema = @Schema(type = "string", format = "binary"))
					@RequestPart(value = "attach_file", required = false) MultipartFile attachFile,
			@Parameter(
							description =
									"确认函附件（multipart 文件 part）；可选；仅校验大小 <8.5MiB；"
											+ "创建流程不落库该文件（confirm_letter_file 落库恒空串）",
							schema = @Schema(type = "string", format = "binary"))
					@RequestPart(value = "confirm_letter_file", required = false) MultipartFile confirmLetterFile) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", trimToEmpty(stringVal(effectiveBody.get("area"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));
		formFields.put("submit_review", trimToEmpty(stringVal(effectiveBody.get("submit_review"))));

		if (!adapayCorpMemberCreateService.createFromMultipart(companyId, jwtMap, formFields, attachFile, confirmLetterFile)) {
			throw new ResourceException("用户创建失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Operation(
			summary = "修改企业用户对象(未开户)",
			description =
					"按 JWT 租户 company_id + member_id 修改企业开户资料（本地落库，不调 AdaPay 出站）："
							+ " 更新 adapay_member（audit_state 重置 0、audit_desc 清空；原状态 D/E 时不改 cert_id）、"
							+ "adapay_corp_member、adapay_settle_account（cert_type=00、channel=bank_account），"
							+ "新增 adapay_entry_apply（WAIT_APPROVE）并记操作日志。"
							+ " 字段双通道：@FlexibleBody（JSON body 或 form/query 参数 Map）18 键 + multipart 文件 part"
							+ " attach_file（可选；上传时仅 .zip、<8.5MiB，替换附件路径）与 confirm_letter_file"
							+ "（可选，仅大小校验，本流程不落库）。所有字段 toString+trim；name≤30、address≤60、"
							+ "business_scope≤500、legal_person≤20、zip_code≤6 码点截断；card_name≤20 码点否则报错。"
							+ " member_id 必填正整数；area 须「省code,市code」两段；bank_code 须存在；"
							+ "bank_acct_type=1（对公）时 card_name 必须等于 name。"
							+ " 鉴权面 admin：Operator JWT；JWT 无 company_id 或 ≤0 报 401「未登录」。"
							+ " 失败信封：全部 ResourceException（DINGO 风格）→ HTTP 200 + data.status_code=422；"
							+ "特例「开户信息不存在」embedded 400 → data.status_code=400（HTTP 仍 200）。"
							+ "成功 data 固定 {status:true}。",
			operationId = "adapay.corp_member.modify")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200。成功：ApiResult code=200、msg=success、data={status:true}；"
								+ "失败也走 HTTP 200：Dingo 信封 data.message + data.status_code=422"
								+ "（如 开户id不能为空 / 企业名称必填 / 银行账户类型错误 / 附件不能超过8M /"
								+ " 银行卡对应的户名，必须与企业名称一致 / 用户信息更新失败 / 结算账户更新失败 等）；"
								+ "「开户信息不存在」时 data.status_code=400。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												oneOf = {
													AdapayCorpMemberModifySuccessEnvelopeDoc.class,
													AdapayCorpMemberCreateBizFailEnvelopeDoc.class
												}),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功",
											value = "{\"code\":200,\"msg\":\"success\",\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-member-id",
											summary = "member_id 缺失/非正整数（HTTP 200，Dingo 422 信封）",
											value = "{\"data\":{\"message\":\"开户id不能为空\",\"status_code\":422}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "开户记录查无（embedded 400，HTTP 仍 200）",
											value = "{\"data\":{\"message\":\"开户信息不存在\",\"status_code\":400}}"),
									@ExampleObject(
											name = "fail-attach-type",
											summary = "附件非 zip",
											value = "{\"data\":{\"message\":\"文件类型不符合要求\",\"status_code\":422}}")
								})),
		@ApiResponse(
				responseCode = "401",
				description =
						"鉴权失败：过滤器/拦截器 401 Dingo 信封（data.message/data.status_code=401，无业务 code）；"
								+ "JWT 缺 company_id 时本方法抛「未登录」，经 unauthorized=true 包装。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListAuthFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "missing-credentials",
											summary = "缺失/非法 Authorization 头",
											value =
													"{\"data\":{\"message\":\"Failed to authenticate because of bad"
															+ " credentials or an invalid authorization header.\","
															+ "\"status_code\":401}}"),
									@ExampleObject(
											name = "invalid-token",
											summary = "token 过期/校验失败",
											value = "{\"data\":{\"message\":\"登录验证错误\",\"status_code\":401}}")
								})),
		@ApiResponse(
				responseCode = "403",
				description = "租户/账号校验失败（拦截器 ApiResult.fail(403, msg)，data 省略）。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListForbiddenEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "forbidden",
												value = "{\"code\":403,\"msg\":\"未登录\"}"))),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常（GlobalExceptionHandler.handleException），固定文案。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														AdapayAlipayIndustryCatListServerErrorEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "server-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@Activated(routeAlias = "adapay.corp_member.modify")
	@PostMapping(value = "/corp_member/modify", name = "修改企业用户对象(未开户)")
	public ResponseEntity<ApiResult<Map<String, Object>>> modify(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description =
									"企业用户修改字段（JSON 或 form 双通道；multipart 时字段放 form part）。"
											+ "18 键语义与校验见 schema；附件走独立文件 part",
							required = true,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	AdapayCorpMemberModifyBodyDoc.class),
											examples =
													@ExampleObject(
															name = "corp-modify",
															summary = "对公资料修改（字段值为按校验规则/种子数据构造的样例）",
															value =
																	"{\"member_id\":\"1\",\"name\":\"示例科技有限公司\","
																			+ "\"area\":\"0011,1100\",\"bank_acct_type\":\"1\","
																			+ "\"social_credit_code\":\"91110000123456789X\","
																			+ "\"social_credit_code_expires\":\"20991231\","
																			+ "\"business_scope\":\"软件开发与销售\",\"legal_person\":\"李四\","
																			+ "\"legal_cert_id\":\"110101199001011234\","
																			+ "\"legal_cert_id_expires\":\"20991231\","
																			+ "\"legal_mp\":\"13800138000\",\"address\":\"北京市东城区示例路1号\","
																			+ "\"zip_code\":\"100000\",\"telphone\":\"010-12345678\","
																			+ "\"email\":\"a@b.com\",\"bank_code\":\"01020000\","
																			+ "\"card_no\":\"622200\",\"card_name\":\"示例科技有限公司\"}")))
					@FlexibleBody Map<String, Object> body,
			@Parameter(
							description =
									"开户附件（multipart 文件 part）；可选；上传时仅 .zip 扩展名（否则「文件类型不符合要求」）、"
											+ "大小 <8.5MiB（≥ 报「附件不能超过8M」）；上传成功后替换 attach_file 路径",
							schema = @Schema(type = "string", format = "binary"))
					@RequestPart(value = "attach_file", required = false) MultipartFile attachFile,
			@Parameter(
							description = "确认函附件（multipart 文件 part）；可选；仅校验大小 <8.5MiB；本流程不落库该文件",
							schema = @Schema(type = "string", format = "binary"))
					@RequestPart(value = "confirm_letter_file", required = false) MultipartFile confirmLetterFile) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("member_id", trimToEmpty(stringVal(effectiveBody.get("member_id"))));
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", trimToEmpty(stringVal(effectiveBody.get("area"))));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));

		if (!adapayCorpMemberModifyService.modifyFromMultipart(
				companyId, jwtMap, formFields, attachFile, confirmLetterFile)) {
			throw new ResourceException("用户更新失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.corp_member.update")
	@PostMapping(value = "/corp_member/update", name = "更新企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body,
			@RequestPart(value = "attach_file", required = false) MultipartFile attachFile) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("member_id", trimToEmpty(stringVal(effectiveBody.get("member_id"))));
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", normalizeArea(effectiveBody.get("area")));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));

		adapayCorpMemberUpdateService.update(companyId, jwtMap, formFields, attachFile);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static String normalizeArea(Object areaRaw) {
		if (areaRaw instanceof Collection<?> col) {
			StringJoiner joiner = new StringJoiner(",");
			for (Object el : col) {
				joiner.add(String.valueOf(el));
			}
			return joiner.toString().trim();
		}
		if (areaRaw instanceof String s) {
			return s.trim();
		}
		return trimToEmpty(stringVal(areaRaw));
	}
}
