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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorDownloadPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorDetailBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorDetailSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc.EcxDistributorListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-店铺")
@OpenapiResponse
@RestController("openapiV2Distributor")
@RequestMapping("/api/openapi/internal/v2")
public class DistributorController extends OpenapiBaseController {

	private final OpenapiDistributorListPort distributorListPort;
	private final OpenapiDistributorCreatePort distributorCreatePort;
	private final OpenapiDistributorUpdatePort distributorUpdatePort;
	private final OpenapiDistributorDetailPort distributorDetailPort;
	private final OpenapiDistributorDownloadPort distributorDownloadPort;

	public DistributorController(
			OpenapiDistributorListPort distributorListPort,
			OpenapiDistributorCreatePort distributorCreatePort,
			OpenapiDistributorUpdatePort distributorUpdatePort,
			@Qualifier("openapiDistributorV2DetailPortImpl")
					OpenapiDistributorDetailPort distributorDetailPort,
			OpenapiDistributorDownloadPort distributorDownloadPort) {
		this.distributorListPort = distributorListPort;
		this.distributorCreatePort = distributorCreatePort;
		this.distributorUpdatePort = distributorUpdatePort;
		this.distributorDetailPort = distributorDetailPort;
		this.distributorDownloadPort = distributorDownloadPort;
	}

	@Operation(
			summary = "开放接口查询店铺列表",
			description =
					"按租户 company_id 查询店铺（分销商）列表，支持多条件筛选。"
							+ " query 与 JSON body 同名参数等价，body 中 key 存在时优先（OpenapiRequestParams.originalString）。"
							+ " 只读，无写库副作用。",
			operationId = "ecx.distributor.list")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000，data 见 EcxDistributorListDataDoc；"
								+ " 参数/业务校验失败：status=fail（如 E0001 page_size 超范围），见 EcxDistributorListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorListSuccessEnvelopeDoc.class)))
	})
	@GetMapping(value = "/ecx.distributor.list", name = "开放接口查询店铺列表")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码，默认 1，最小 1", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(description = "每页条数，默认 20，范围 1–500", example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(description = "店铺号，精确匹配", example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@Parameter(description = "店铺名称，模糊匹配")
					@RequestParam(name = "distributor_name", required = false)
					String distributorNameParam,
			@Parameter(description = "状态筛选：0 已删除，1 启用，2 禁用", example = "1")
					@RequestParam(name = "status", required = false)
					String statusParam,
			@Parameter(description = "省，模糊匹配")
					@RequestParam(name = "province", required = false)
					String provinceParam,
			@Parameter(description = "市，模糊匹配") @RequestParam(name = "city", required = false)
					String cityParam,
			@Parameter(description = "区，模糊匹配") @RequestParam(name = "area", required = false)
					String areaParam,
			@Parameter(description = "联系人姓名，模糊匹配")
					@RequestParam(name = "contact_username", required = false)
					String contactUsernameParam,
			@Parameter(description = "联系人手机，精确匹配")
					@RequestParam(name = "contact_mobile", required = false)
					String contactMobileParam,
			@Parameter(description = "店铺 ID，精确匹配")
					@RequestParam(name = "distributor_id", required = false)
					String distributorIdParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名等价，body 优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxDistributorListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		return distributorListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.originalString(distributorNameParam, body, "distributor_name"),
				OpenapiRequestParams.originalString(statusParam, body, "status"),
				OpenapiRequestParams.originalString(provinceParam, body, "province"),
				OpenapiRequestParams.originalString(cityParam, body, "city"),
				OpenapiRequestParams.originalString(areaParam, body, "area"),
				OpenapiRequestParams.originalString(contactUsernameParam, body, "contact_username"),
				OpenapiRequestParams.originalString(contactMobileParam, body, "contact_mobile"),
				OpenapiRequestParams.originalString(distributorIdParam, body, "distributor_id"));
	}

	@Operation(
			summary = "开放接口创建店铺",
			description =
					"按签名鉴权租户 company_id 创建店铺（distribution_distributor）："
							+ " shop_code（^[A-Za-z0-9-]+$）、distributor_name、contact_username、"
							+ " contact_mobile（^1[3456789]\\d{9}$）必填，缺失/非法 E5001 对应文案；"
							+ " 五个 0/1 开关可选（is_delivery 缺省 1，其余缺省 0；空串/非 0/1 报 E5001）；hour/logo 可选无格式校验。"
							+ " 租户内店铺号/名称/手机号（密文）重复分别 E5402。"
							+ " is_dada=1 时校验同城配已开启（未开启 E5300）并远程建店，shop_code 被 originShopId 覆盖"
							+ "（覆盖后重复再报 E5402；建店失败 E0001）。"
							+ " 新建恒禁用态（is_valid=false → status=2）、source_from=3，省市区/地址/坐标/regions 为空，"
							+ " contact/mobile 加密存储。成功 data 为新建店铺 23 键行（同列表行结构），Advice 包装 message 固定「成功」。"
							+ " 参数双通道：多数 originalString（body 含键取原始值），hour/logo mergeString。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor.create, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 INSERT distribution_distributor + 默认语言分表写；afterCommit 发布"
							+ " 创建店铺慢队列任务 + distribution.add / distributor.create 事件；is_dada=1 时第三方同城配建店 HTTP。",
			operationId = "ecx.distributor.create.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxDistributorListItemDoc（23 键，新建恒 status=2）；"
								+ " 业务失败：status=fail, code=E5001 参数错误 / E5402 店铺号·名称·手机号已存在 /"
								+ " E5300 该商户未开启达达同城配 / E0001 同城配建店失败文案，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "创建成功（禁用态，地址字段为空）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"distributor_id\":1001,\"shop_code\":\"S001\",\"status\":2,"
															+ "\"distributor_name\":\"新门店名\",\"contact_username\":\"王五\","
															+ "\"contact_mobile\":\"13912345678\",\"province\":\"\",\"city\":\"\","
															+ "\"area\":\"\",\"region_codes\":[],\"region_names\":[],\"address\":\"\","
															+ "\"lng\":\"\",\"lat\":\"\",\"hour\":\"08:30 - 21:30\",\"logo\":\"/x/logo.jpg\","
															+ "\"is_ziti\":0,\"is_delivery\":1,\"is_auto_sync_goods\":0,\"is_dada\":0,"
															+ "\"is_default\":0,\"created\":\"2023-11-15 06:13:21\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}}"),
									@ExampleObject(
											name = "fail-shop-code-exists",
											summary = "店铺号重复",
											value =
													"{\"status\":\"fail\",\"code\":\"E5402\",\"message\":\"店铺号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "联系方式缺失/格式非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"联系方式参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-dada-not-open",
											summary = "is_dada=1 但未开启同城配",
											value =
													"{\"status\":\"fail\",\"code\":\"E5300\","
															+ "\"message\":\"该商户未开启达达同城配\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.distributor.create", name = "开放接口创建店铺")
	public Map<String, Object> create(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "店铺号；必填，^[A-Za-z0-9-]+$；is_dada=1 时被 originShopId 覆盖（body 同名键优先）",
							required = true,
							example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@Parameter(description = "店铺名称；必填非空，租户内不可重名（body 同名键优先）", required = true, example = "新门店名")
					@RequestParam(name = "distributor_name", required = false)
					String distributorNameParam,
			@Parameter(description = "联系人姓名；必填非空（body 同名键优先）", required = true, example = "王五")
					@RequestParam(name = "contact_username", required = false)
					String contactUsernameParam,
			@Parameter(
							description = "联系人手机；必填 ^1[3456789]\\d{9}$，租户内不可重复（body 同名键优先）",
							required = true,
							example = "13912345678")
					@RequestParam(name = "contact_mobile", required = false)
					String contactMobileParam,
			@Parameter(description = "营业时间描述；可选缺省空串（body 同名键优先，mergeString）", example = "08:30 - 21:30")
					@RequestParam(name = "hour", required = false)
					String hourParam,
			@Parameter(
							description = "是否支持自提；0/1，缺省 0（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "0")
					@RequestParam(name = "is_ziti", required = false)
					String isZitiParam,
			@Parameter(
							description = "是否支持快递配送；0/1，缺省 1（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "1")
					@RequestParam(name = "is_delivery", required = false)
					String isDeliveryParam,
			@Parameter(
							description = "是否自动同步总部商品；0/1，缺省 0（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "0")
					@RequestParam(name = "is_auto_sync_goods", required = false)
					String isAutoSyncGoodsParam,
			@Parameter(
							description = "是否开启同城配；0/1，缺省 0；=1 须已开启达达/闪送并远程建店（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "0")
					@RequestParam(name = "is_dada", required = false)
					String isDadaParam,
			@Parameter(
							description = "是否默认店铺；0/1，缺省 0（body 同名键优先）",
							schema = @Schema(allowableValues = {"0", "1"}),
							example = "0")
					@RequestParam(name = "is_default", required = false)
					String isDefaultParam,
			@Parameter(description = "店铺 Logo URL；可选缺省空串（body 同名键优先，mergeString）", example = "/x/logo.jpg")
					@RequestParam(name = "logo", required = false)
					String logoParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道；校验与默认值见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDistributorCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return distributorCreatePort.create(
				companyId,
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.originalString(distributorNameParam, body, "distributor_name"),
				OpenapiRequestParams.originalString(contactUsernameParam, body, "contact_username"),
				OpenapiRequestParams.originalString(contactMobileParam, body, "contact_mobile"),
				OpenapiRequestParams.mergeString(hourParam, body, "hour"),
				OpenapiRequestParams.originalString(isZitiParam, body, "is_ziti"),
				OpenapiRequestParams.originalString(isDeliveryParam, body, "is_delivery"),
				OpenapiRequestParams.originalString(isAutoSyncGoodsParam, body, "is_auto_sync_goods"),
				OpenapiRequestParams.originalString(isDadaParam, body, "is_dada"),
				OpenapiRequestParams.originalString(isDefaultParam, body, "is_default"),
				OpenapiRequestParams.mergeString(logoParam, body, "logo"));
	}

	@GetMapping(value = "/ecx.distributor.download", name = "开放接口生成店铺码")
	public Map<String, Object> download(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "distributor_name", required = false) String distributorNameParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "template_name", required = false) String templateNameParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return distributorDownloadPort.download(
				companyId,
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.originalString(distributorNameParam, body, "distributor_name"),
				OpenapiRequestParams.originalString(statusParam, body, "status"),
				OpenapiRequestParams.mergeString(templateNameParam, body, "template_name"));
	}

	@Operation(
			summary = "开放接口查询店铺详情",
			description =
					"按签名鉴权租户 company_id 查询单个店铺详情：shop_code 必填非空（originalString 双通道，"
							+ " body 含键取原始值不 trim），缺失 E5001「店铺号必填」；租户内精确匹配 LIMIT 1"
							+ "（不过滤删除态），查无 E5401「店铺找不到」。"
							+ " data 为 23 键店铺行（与列表/创建同构）：distributor_id 整型、status（0 已删除/1 启用/2 禁用/-1 未知）、"
							+ " contact_username/contact_mobile 解密明文、region_codes/region_names 字符串数组、"
							+ " 五个 0/1 开关、created/updated yyyy-MM-dd HH:mm:ss；"
							+ " name/logo/省市区/address 等按默认语言分表非空覆盖。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.distributor.detail, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.distributor.detail.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxDistributorListItemDoc（23 键）；"
								+ " 业务失败：status=fail, code=E5001 店铺号必填 / E5401 店铺找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDistributorDetailFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDistributorDetailSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（启用店铺）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"distributor_id\":1001,\"shop_code\":\"S001\",\"status\":1,"
															+ "\"distributor_name\":\"新门店名\",\"contact_username\":\"王五\","
															+ "\"contact_mobile\":\"13912345678\",\"province\":\"北京市\",\"city\":\"北京市\","
															+ "\"area\":\"海淀区\",\"region_codes\":[],\"region_names\":[],"
															+ "\"address\":\"中关村大街1号\",\"lng\":\"116.310003\",\"lat\":\"39.992806\","
															+ "\"hour\":\"08:30 - 21:30\",\"logo\":\"/x/logo.jpg\","
															+ "\"is_ziti\":1,\"is_delivery\":1,\"is_auto_sync_goods\":1,\"is_dada\":0,"
															+ "\"is_default\":0,\"created\":\"2020-09-13 20:26:40\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}}"),
									@ExampleObject(
											name = "fail-missing-shop-code",
											summary = "shop_code 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"店铺号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "shop_code 查无店铺",
											value =
													"{\"status\":\"fail\",\"code\":\"E5401\",\"message\":\"店铺找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.distributor.detail", name = "开放接口查询店铺详情")
	public Map<String, Object> detail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "店铺号；必填非空，租户内精确匹配（body 同名键优先，原始值不 trim）",
							required = true,
							example = "S001")
					@RequestParam(name = "shop_code", required = false)
					String shopCodeParam,
			@RequestBody(
							description = "可选 JSON body；shop_code 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDistributorDetailBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		if (shopCodeRaw == null || shopCodeRaw.isEmpty()) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号必填");
		}

		return distributorDetailPort.getDistributorDetail(companyId, shopCodeRaw);
	}

	@PatchMapping(value = "/ecx.distributor.update", name = "开放接口更新店铺")
	public Map<String, Object> update(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "distributor_name", required = false) String distributorNameParam,
			@RequestParam(name = "contact_username", required = false) String contactUsernameParam,
			@RequestParam(name = "contact_mobile", required = false) String contactMobileParam,
			@RequestParam(name = "hour", required = false) String hourParam,
			@RequestParam(name = "is_ziti", required = false) String isZitiParam,
			@RequestParam(name = "is_delivery", required = false) String isDeliveryParam,
			@RequestParam(name = "is_auto_sync_goods", required = false) String isAutoSyncGoodsParam,
			@RequestParam(name = "is_dada", required = false) String isDadaParam,
			@RequestParam(name = "is_default", required = false) String isDefaultParam,
			@RequestParam(name = "logo", required = false) String logoParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "province", required = false) String provinceParam,
			@RequestParam(name = "city", required = false) String cityParam,
			@RequestParam(name = "area", required = false) String areaParam,
			@RequestParam(name = "address", required = false) String addressParam,
			@RequestParam(name = "lng", required = false) String lngParam,
			@RequestParam(name = "lat", required = false) String latParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return distributorUpdatePort.update(
				companyId,
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.presentOptionalString(distributorNameParam, body, "distributor_name"),
				OpenapiRequestParams.presentOptionalString(contactUsernameParam, body, "contact_username"),
				OpenapiRequestParams.presentOptionalString(contactMobileParam, body, "contact_mobile"),
				OpenapiRequestParams.presentOptionalString(hourParam, body, "hour"),
				OpenapiRequestParams.presentOptionalString(isZitiParam, body, "is_ziti"),
				OpenapiRequestParams.presentOptionalString(isDeliveryParam, body, "is_delivery"),
				OpenapiRequestParams.presentOptionalString(isAutoSyncGoodsParam, body, "is_auto_sync_goods"),
				OpenapiRequestParams.presentOptionalString(isDadaParam, body, "is_dada"),
				OpenapiRequestParams.presentOptionalString(isDefaultParam, body, "is_default"),
				OpenapiRequestParams.presentOptionalString(logoParam, body, "logo"),
				OpenapiRequestParams.presentOptionalString(statusParam, body, "status"),
				OpenapiRequestParams.presentOptionalString(provinceParam, body, "province"),
				OpenapiRequestParams.presentOptionalString(cityParam, body, "city"),
				OpenapiRequestParams.presentOptionalString(areaParam, body, "area"),
				OpenapiRequestParams.presentOptionalString(addressParam, body, "address"),
				OpenapiRequestParams.presentOptionalString(lngParam, body, "lng"),
				OpenapiRequestParams.presentOptionalString(latParam, body, "lat"));
	}
}
