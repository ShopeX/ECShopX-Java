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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiWeappIdPort;
import cn.shopex.ecshopx.common.openapi.OpenapiWxappQrcodePort;
import cn.shopex.ecshopx.common.openapi.OpenapiWxappShopListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxWeappIdGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxWxappQrcodeBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxWxappQrcodeFailEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxWxappShopListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxWxappShopListSuccessEnvelopeDoc;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-微信小程序V1")
@OpenapiResponse
@RestController("openapiV1Wxapp")
@RequestMapping("/api/openapi/internal/v1")
public class WxappController extends OpenapiBaseController {

	private final OpenapiWxappQrcodePort wxappQrcodePort;
	private final OpenapiWxappShopListPort wxappShopListPort;
	private final OpenapiWeappIdPort weappIdPort;

	public WxappController(
			OpenapiWxappQrcodePort wxappQrcodePort,
			OpenapiWxappShopListPort wxappShopListPort,
			OpenapiWeappIdPort weappIdPort) {
		this.wxappQrcodePort = wxappQrcodePort;
		this.wxappShopListPort = wxappShopListPort;
		this.weappIdPort = weappIdPort;
	}

	@Operation(
			summary = "开放接口获取导购任务小程序码",
			description =
					"按签名鉴权租户 company_id 生成导购任务小程序码，成功直接返回 image/png 二进制（非信封）。"
							+ " path_type 必填且限 index/goods_list/goods_detail/recommend_list/recommend_detail/share_land，"
							+ " 映射对应小程序页面；scene 必填 JSON 对象字符串，goods_detail/recommend_detail 须含整数 id≥1；"
							+ " 任何校验失败统一「参数错误」（E0001）。"
							+ " 租户须已绑定 yykweishop 授权小程序（wechat_weapp），未绑定报「参数错误」但 code=E0000（PHP 旧契约 fail）。"
							+ " scene 全量 JSON 写 Redis 分享参数（key=companyId_shareId，TTL 30 天），"
							+ " 小程序码 scene 仅 [id=...&]share_id=...（share_id 服务端 PhpUniqid 生成）；"
							+ " width 正整数生效，否则微信默认 430px；经微信 getwxacodeunlimit（env=release，auto_color）取码。"
							+ " 三参数双通道（body 含键取原始值不 trim，否则用 query）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.wxapp.qrcode, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：写 Redis 分享参数 + 调用微信第三方接口；无写库、无异步。",
			operationId = "ecx.wxapp.qrcode.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description = "成功：HTTP 200，Content-Type=image/png，body 为小程序码 PNG 二进制（微信 getwxacodeunlimit 返回）。",
				content =
						@Content(
								mediaType = "image/png",
								schema = @Schema(type = "string", format = "binary"))),
		@ApiResponse(
				responseCode = "200",
				description =
						"失败：HTTP 200，Content-Type=application/json，OpenapiEnvelope 形态。"
								+ " 参数校验失败：status=fail, code=E0001, message=参数错误, data=null；"
								+ " 未绑定 yykweishop 小程序：status=fail, code=E0000（PHP 旧契约）, message=参数错误, data=null；"
								+ " 微信生成失败：status=fail, code=E0001（41030 小程序页面尚未发布或未通过审核 /"
								+ " 获取小程序码失败：响应为空 / 微信 errmsg 透传）, data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权, data=null。见 EcxWxappQrcodeFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxWxappQrcodeFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "fail-param",
											summary = "path_type/scene/id 校验失败",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unbound-legacy-zero",
											summary = "未绑定 yykweishop 小程序（PHP 旧契约：fail 但 code=E0000）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\",\"message\":\"参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-unpublished",
											summary = "微信 41030：页面未发布/未过审",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"小程序页面尚未发布或未通过审核\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(
			value = "/ecx.wxapp.qrcode",
			name = "开放接口获取导购任务小程序码",
			produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getWxCode(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"落地页类型；必填，限 index/goods_list/goods_detail/recommend_list/recommend_detail/share_land"
											+ "（body 同名键优先，原始值不 trim）",
							required = true,
							schema =
									@Schema(
											allowableValues = {
												"index", "goods_list", "goods_detail",
												"recommend_list", "recommend_detail", "share_land"
											}),
							example = "goods_detail")
					@RequestParam(name = "path_type", required = false)
					String pathTypeParam,
			@Parameter(
							description =
									"场景参数 JSON 对象字符串；必填；goods_detail/recommend_detail 须含整数 id≥1"
											+ "（body 同名键优先，原始值不 trim）",
							required = true,
							example = "{\"id\":1001}")
					@RequestParam(name = "scene", required = false)
					String sceneParam,
			@Parameter(
							description = "小程序码宽度 px；正整数生效，否则默认 430（body 同名键优先，原始值不 trim）",
							example = "430")
					@RequestParam(name = "width", required = false)
					String widthParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值不 trim）；校验规则见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxWxappQrcodeBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiWxappQrcodeParams.ParseResult parsed =
				OpenapiWxappQrcodeParams.parse(pathTypeParam, sceneParam, widthParam, body);
		if (parsed instanceof OpenapiWxappQrcodeParams.Fail fail) {
			throw new ResourceException(fail.message());
		}
		OpenapiWxappQrcodeParams.Ok ok = (OpenapiWxappQrcodeParams.Ok) parsed;
		byte[] png = wxappQrcodePort.generateSalespersonTaskQrcode(
				companyId, ok.pathType(), ok.scene(), ok.widthRaw());
		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
	}

	@Operation(
			summary = "开放接口获取微信店铺数据",
			description =
					"按签名鉴权租户 company_id 分页返回店铺（distribution_distributor，不过滤启用/停用，created 升序）："
							+ " data 含 total_count 与 list（10 键/条，camelCase PHP 旧契约）——wxShopId（distributor_id 整型）、"
							+ " storeName/shopBn/contractPhone（mobile 解密明文，null 回退空串）、logo/hour/lng/lat（可 null）、"
							+ " address（省市区+详细地址拼接）、is_deleted（is_valid=true→\"0\" 否则 \"1\"）。"
							+ " name/logo/省市区/address 按系统默认语言分表非空覆盖。"
							+ " page 必填整数 ≥1、pageSize 必填整数 1–50（超上限报错不封顶），失败「参数不正确.」"
							+ " 且 code=E0000（PHP 旧契约 fail）；start_time/end_time 可选 PHP-truthy 才生效，"
							+ " yyyy-MM-dd[ HH:mm:ss] 转 Unix 秒做 updated 严格区间（>/<），解析失败按 0 不报错。"
							+ " 四参数双通道（body 含键取原始值不 trim，否则用 query）。message 恒为 \"success\"。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.wxapp.shoplist, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.wxapp.shoplist.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=success，data 见 EcxWxappShopListDataDoc"
								+ "（无数据时 total_count=0、list=[]）；"
								+ " 参数失败：status=fail, code=E0000（PHP 旧契约）, message=参数不正确., data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxWxappShopListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxWxappShopListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"total_count\":1,\"list\":[{\"wxShopId\":1001,"
															+ "\"storeName\":\"新门店名\",\"shopBn\":\"S001\",\"logo\":\"/x/logo.jpg\","
															+ "\"contractPhone\":\"13912345678\",\"hour\":\"08:30 - 21:30\","
															+ "\"lng\":\"116.310003\",\"lat\":\"39.992806\","
															+ "\"address\":\"北京市北京市海淀区中关村大街1号\",\"is_deleted\":\"0\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无店铺",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"total_count\":0,\"list\":[]}}"),
									@ExampleObject(
											name = "fail-param-legacy-zero",
											summary = "page/pageSize 非法（PHP 旧契约：fail 但 code=E0000）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\",\"message\":\"参数不正确.\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.wxapp.shoplist", name = "开放接口获取微信店铺数据")
	public OpenapiEnvelope getWxShopLists(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码；必填整数 ≥1（不含小数点、不 trim），否则「参数不正确.」（body 同名键优先）",
							required = true,
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；必填整数 1–50（超上限报错），否则「参数不正确.」（body 同名键优先）",
							required = true,
							example = "50")
					@RequestParam(name = "pageSize", required = false)
					String pageSizeParam,
			@Parameter(
							description =
									"updated 下界（严格大于）；yyyy-MM-dd[ HH:mm:ss]，PHP-truthy 才生效，解析失败按 0"
											+ "（body 同名键优先）",
							example = "2020-09-13")
					@RequestParam(name = "start_time", required = false)
					String startTimeParam,
			@Parameter(
							description =
									"updated 上界（严格小于）；语义同 start_time（body 同名键优先）",
							example = "2023-11-15")
					@RequestParam(name = "end_time", required = false)
					String endTimeParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值不 trim）；校验与时间语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxWxappShopListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiWxappShopListParams.ParseResult parsed =
				OpenapiWxappShopListParams.parse(
						pageParam, pageSizeParam, startTimeParam, endTimeParam, body);
		if (parsed instanceof OpenapiWxappShopListParams.Fail) {
			throw new OpenapiLegacyZeroCodeFailException("参数不正确.");
		}
		OpenapiWxappShopListParams.Ok ok = (OpenapiWxappShopListParams.Ok) parsed;
		Map<String, Object> data =
				wxappShopListPort.listWxShopLists(
						companyId, ok.page(), ok.pageSize(), ok.updatedGt(), ok.updatedLt());
		return new OpenapiEnvelope("success", "E0000", "success", data);
	}

	@Operation(
			summary = "开放接口获取yykweishop模板appid",
			description =
					"按签名鉴权租户 company_id 查询 yykweishop 模板授权小程序 appid："
							+ " wechat_weapp 按 company_id + template_name=yykweishop + deleted_at IS NULL 取 LIMIT 1；"
							+ " 查无记录或 appid 为空时 weappid=null（仍成功返回，不报错）。"
							+ " data 恒 2 键：weappid（可 null）、template_name（恒 yykweishop）；message 恒为 \"success\"。"
							+ " 无请求参数（query/body 均不读取）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.weappid.get, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用；无业务校验失败分支。",
			operationId = "ecx.weappid.get.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=success，data 见 EcxWeappIdGetDataDoc"
								+ "（未绑定时 weappid=null 仍成功）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxWeappIdGetFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxWeappIdGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "已绑定授权小程序",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"weappid\":\"wxResolved\",\"template_name\":\"yykweishop\"}}"),
									@ExampleObject(
											name = "success-unbound",
											summary = "未绑定/已删除/appid 为空（仍成功）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"weappid\":null,\"template_name\":\"yykweishop\"}}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.weappid.get", name = "开放接口获取yykweishop模板appid")
	public OpenapiEnvelope getWeappId(@Parameter(hidden = true) HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = weappIdPort.getWeappIdByTemplate(companyId, "yykweishop");
		return new OpenapiEnvelope("success", "E0000", "success", data);
	}
}
