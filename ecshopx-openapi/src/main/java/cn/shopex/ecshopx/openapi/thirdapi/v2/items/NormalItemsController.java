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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityAddPort;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityStatusUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityAddSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityStatusUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityStatusUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemEntityUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemsEntityListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemsEntityListSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-平台商品V2")
@OpenapiResponse
@RestController("openapiV2NormalItems")
@RequestMapping("/api/openapi/internal/v2")
public class NormalItemsController extends OpenapiBaseController {

	private final OpenapiNormalItemsEntityListPort normalItemsEntityListPort;
	private final OpenapiNormalItemsEntityDetailPort normalItemsEntityDetailPort;
	private final OpenapiNormalItemsEntityStatusUpdatePort entityStatusUpdatePort;
	private final OpenapiNormalItemsEntityDeletePort entityDeletePort;
	private final OpenapiNormalItemsEntityAddPort entityAddPort;
	private final OpenapiNormalItemsEntityUpdatePort entityUpdatePort;

	public NormalItemsController(
			OpenapiNormalItemsEntityListPort normalItemsEntityListPort,
			OpenapiNormalItemsEntityDetailPort normalItemsEntityDetailPort,
			OpenapiNormalItemsEntityStatusUpdatePort entityStatusUpdatePort,
			OpenapiNormalItemsEntityDeletePort entityDeletePort,
			OpenapiNormalItemsEntityAddPort entityAddPort,
			OpenapiNormalItemsEntityUpdatePort entityUpdatePort) {
		this.normalItemsEntityListPort = normalItemsEntityListPort;
		this.normalItemsEntityDetailPort = normalItemsEntityDetailPort;
		this.entityStatusUpdatePort = entityStatusUpdatePort;
		this.entityDeletePort = entityDeletePort;
		this.entityAddPort = entityAddPort;
		this.entityUpdatePort = entityUpdatePort;
	}

	@Operation(
			summary = "查询平台商品列表",
			description =
					"按签名鉴权租户 company_id 分页查询平台商品 SPU 列表：8 键与 query 同名双通道"
							+ "（body 含键优先、值 null 视为未传、不 trim）。固定过滤 item_type=normal 且 is_default=1。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析：非法→0、小数截断）；"
							+ " page_size 原始解析值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ " max() 钳制后实际 page≥1、page_size∈[20,499]（E5001 两个分页文案分支不可达）。"
							+ " approve_status truthy 时 EXISTS 同 goods_id 任一 SKU 状态等值；brand_id 须可 parseInt"
							+ "（非数字→E5000 解析异常透传）；category_id 为 PHP 遗留缺陷：truthy 时过滤不存在列"
							+ " items.category_id → 必触发 SQL 错误 E5000；time_begin/time_end 按 yyyy-MM-dd[ HH:mm:ss]"
							+ " 系统时区解析为 updated 区间（非法按 0；同时 truthy 且开始>结束 E5320）；"
							+ " is_self 按键存在性生效：值恰为 \"true\" 过滤自营（distributor_id=0），否则店铺（>0）。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list（item_id 降序，10 键/行，"
							+ " item_name zh-CN 分表覆盖，category_name 为 \"[分类名]\" 数组）。"
							+ " DB 异常 E5000 message 透传。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.items.entity.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。summary 为语义命名（注册表未提供中文接口名）。",
			operationId = "ecx.items.entity.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemsEntityListDataDoc；"
								+ " 业务失败：status=fail, code=E5002 每页的大小最大为500！ / E5320 开始时间不能大于结束时间 /"
								+ " E5000 SQL/解析异常 message 透传，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemsEntityListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemsEntityListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "列表查询成功（示例值取自 Wire 测试夹具；update_time 为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"item_id\":200,\"item_bn\":\"SKU200\","
															+ "\"item_name\":\"Wire test goods\",\"price\":10,\"store\":10,"
															+ "\"approve_status\":\"onsale\",\"nospec\":false,"
															+ "\"category_name\":[\"[聚合测试类目]\"],\"is_self\":true,"
															+ "\"update_time\":1735689600}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无匹配结果（list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-time-range",
											summary = "time_begin > time_end（两者均 truthy）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5320\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping("/ecx.items.entity.get")
	public Map<String, Object> getList(
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
							description = "商品状态过滤（onsale/instock/offline_sale/only_show）；EXISTS 同 SPU 任一 SKU 等值；空串/\"0\" 忽略",
							example = "onsale")
					@RequestParam(name = "approve_status", required = false)
					String approveStatusParam,
			@Parameter(
							description = "品牌 ID（整数字符串）等值过滤；非数字触发 E5000；空串/\"0\" 忽略",
							example = "3")
					@RequestParam(name = "brand_id", required = false)
					String brandIdParam,
			@Parameter(
							description = "分类 ID；PHP 遗留缺陷：truthy 时过滤不存在列 items.category_id，必触发 SQL 错误 E5000；空串/\"0\" 忽略",
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@Parameter(
							description = "更新时间下界；yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd，系统时区；非法按 0；空串/\"0\" 忽略（格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "time_begin", required = false)
					String timeBeginParam,
			@Parameter(
							description = "更新时间上界；格式同 time_begin；与 time_begin 同传且开始>结束报 E5320（格式演示值）",
							example = "2026-12-31 23:59:59")
					@RequestParam(name = "time_end", required = false)
					String timeEndParam,
			@Parameter(
							description = "自营/店铺过滤；键存在即生效：值恰为 \"true\" → 自营（distributor_id=0），其他值 → 店铺（>0）",
							example = "true")
					@RequestParam(name = "is_self", required = false)
					String isSelfParam,
			@RequestBody(
							description = "可选 JSON body；8 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemsEntityListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2NormalItemsEntityListParams.PageSpec pageSpec =
				OpenapiThirdApiV2NormalItemsEntityListParams.resolve(pageParam, pageSizeParam, body);

		return normalItemsEntityListPort.getEntityList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiRequestParams.originalString(approveStatusParam, body, "approve_status"),
				OpenapiRequestParams.originalString(brandIdParam, body, "brand_id"),
				OpenapiRequestParams.originalString(categoryIdParam, body, "category_id"),
				OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin"),
				OpenapiRequestParams.originalString(timeEndParam, body, "time_end"),
				OpenapiMemberQueryParams.isParamPresent(isSelfParam, body, "is_self"),
				OpenapiRequestParams.originalString(isSelfParam, body, "is_self"));
	}

	@Operation(
			summary = "查询平台商品详情",
			description =
					"按签名鉴权租户 company_id 查询平台商品 SPU 详情：item_bn 必填非空白（缺失 E5001「货号必填」），"
							+ " trim 后取 is_default=true 默认 SKU（查无/详情为空 E5101「商品找不到」；构建异常 E5000 透传）。"
							+ " data 23 固定键：goods_id/brand_id/templates_id/sort/price/store 等整型（金额分）、"
							+ " nospec/is_gift/is_show_specimg 布尔、weight 十进制字符串、pics 数组、"
							+ " item_main_cat_id 字符串（主类目路径查无为空串）、item_category 销售分类 ID 数组、"
							+ " update_time 取库 updated Unix 秒；item_name/brief zh-CN 分表覆盖。"
							+ " 多规格（nospec=false）追加 spec_items（11 键/SKU 含 item_spec 6 键）、"
							+ " item_spec_desc（4 键含 spec_values 4 键）、spec_images（4 键），"
							+ " 且 approve_status 为 SKU 聚合、store 为 SKU 合计。"
							+ " item_bn originalString 双通道。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.entity.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.entity.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemEntityGetDataDoc；"
								+ " 业务失败：status=fail, code=E5001 货号必填 / E5101 商品找不到 / E5000 详情构建异常，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemEntityGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemEntityGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success-multi-spec",
											summary = "多规格商品详情（SKU 聚合状态/库存）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"goods_id\":200,\"item_bn\":\"SKU200\","
															+ "\"item_name\":\"Wire test goods\",\"brief\":\"\",\"item_unit\":\"\","
															+ "\"sort\":1,\"brand_id\":3,\"templates_id\":1,\"is_gift\":false,"
															+ "\"pics\":[\"https://example.test/key.jpg\"],\"nospec\":false,"
															+ "\"is_show_specimg\":false,\"weight\":\"1\",\"volume\":0.0,"
															+ "\"price\":10,\"market_price\":0,\"cost_price\":0,\"barcode\":\"B200\","
															+ "\"approve_status\":\"onsale\",\"store\":20,\"item_main_cat_id\":\"77\","
															+ "\"item_category\":[1],\"update_time\":1700000001,"
															+ "\"spec_items\":[{\"item_bn\":\"SKU200\",\"is_default\":true,"
															+ "\"approve_status\":\"onsale\",\"weight\":\"1\",\"volume\":0.0,"
															+ "\"price\":10,\"market_price\":0,\"cost_price\":0,\"barcode\":\"B200\","
															+ "\"item_spec\":[{\"spec_id\":1,\"spec_value_id\":1,\"spec_name\":\"500mg\","
															+ "\"spec_custom_value_name\":null,\"spec_value_name\":\"红\","
															+ "\"spec_image_url\":null}],\"store\":10}],"
															+ "\"item_spec_desc\":[{\"spec_id\":1,\"spec_name\":\"500mg\","
															+ "\"is_image\":\"false\",\"spec_values\":[{\"spec_value_id\":1,"
															+ "\"spec_value_name\":\"红\",\"spec_custom_value_name\":null,"
															+ "\"spec_image_url\":null}]}],"
															+ "\"spec_images\":[{\"spec_value_id\":1,\"spec_custom_value_name\":null,"
															+ "\"spec_value_name\":\"红\",\"spec_image_url\":\"https://example.test/key.jpg\"}]}}"),
									@ExampleObject(
											name = "fail-bn-required",
											summary = "item_bn 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"货号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "默认 SKU 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping("/ecx.item.entity.get")
	public Map<String, Object> getItemSpuDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "商品货号（默认 SKU item_bn）；必填非空白（body 同名键优先，原始值不 trim）",
							required = true,
							example = "SKU200")
					@RequestParam(name = "item_bn", required = false)
					String itemBnParam,
			@RequestBody(
							description = "可选 JSON body；item_bn 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemEntityGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String itemBn = OpenapiRequestParams.originalString(itemBnParam, body, "item_bn");
		return normalItemsEntityDetailPort.getEntityDetail(companyId, itemBn);
	}

	@Operation(
			summary = "批量更新商品上下架状态",
			description =
					"按签名鉴权租户 company_id 批量更新商品上下架：item_bn 为货号 JSON 数组字符串"
							+ "（必填非空白否则 E5001「货号必填」；非数组 E5100「请填写正确的货号格式」；"
							+ "超 1000 个 E5100「商品状态可更改的最大数量为1000」）；status 必填仅 onsale/instock"
							+ "（否则 E5001「状态必填,且必须是 onsale 或 instock 」）。"
							+ " 按默认 SKU 货号定位 goods_id（全部查无 E5101「商品找不到」），整组 SKU 更新 approve_status。"
							+ " onsale 守卫（ResourceException 转 E0000+fail PHP 旧契约）：供应商商品不可售 /"
							+ " 处方药商品需要开启医药行业配置才能上架 / 处方药商品【{商品名}】审核通过后才能上架。"
							+ " 两参数 originalString 双通道（body 含键取原始值，解析时 trim）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.entity.status.update, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：逐 goods_id UPDATE items.approve_status（整组 SKU）+ 批量编辑状态事件任务入队；"
							+ " 无第三方调用。",
			operationId = "ecx.item.entity.status.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 货号必填·状态必填 / E5100 货号格式·超 1000 /"
								+ " E5101 商品找不到 / E0000（PHP 旧契约）供应商商品不可售·处方药守卫文案，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemEntityStatusUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemEntityStatusUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "批量更新成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-status-invalid",
											summary = "status 非 onsale/instock",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"状态必填,且必须是 onsale 或 instock \",\"data\":null}"),
									@ExampleObject(
											name = "fail-bn-format",
											summary = "item_bn 非 JSON 数组",
											value =
													"{\"status\":\"fail\",\"code\":\"E5100\","
															+ "\"message\":\"请填写正确的货号格式\",\"data\":null}"),
									@ExampleObject(
											name = "fail-supplier-legacy-zero",
											summary = "上架含不可售供应商商品（PHP 旧契约：fail 但 code=E0000）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\","
															+ "\"message\":\"供应商商品不可售\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping("/ecx.item.entity.status.update")
	public Map<String, Object> batchUpdateItemsStatus(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "货号 JSON 数组字符串；必填，≤1000 个（body 同名键优先，可直接传数组）",
							required = true,
							example = "[\"SKU200\",\"SKU201\"]")
					@RequestParam(name = "item_bn", required = false)
					String itemBnParam,
			@Parameter(
							description = "目标状态；仅 onsale/instock（body 同名键优先）",
							required = true,
							schema = @Schema(allowableValues = {"onsale", "instock"}),
							example = "onsale")
					@RequestParam(name = "status", required = false)
					String statusParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；守卫语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemEntityStatusUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String itemBnRaw = OpenapiRequestParams.originalString(itemBnParam, body, "item_bn");
		String statusRaw = OpenapiRequestParams.originalString(statusParam, body, "status");
		return entityStatusUpdatePort.batchUpdateItemsStatus(companyId, itemBnRaw, statusRaw);
	}

	@Operation(
			summary = "删除平台商品",
			description =
					"按签名鉴权租户 company_id 删除平台商品：item_bn 必填非空白（缺失 E5001「商品货号必填」），"
							+ " trim 后按租户查 is_default=true 默认 SKU（查无 E5103「商品删除错误」）。"
							+ " 多规格商品按 default_item_id 删除整个 SPU 组全部 SKU；单规格删自身。"
							+ " 固定按平台 distributor_id=0 删除（店铺商品不匹配报 E5103「店铺商品信息有误，不可删除」）。"
							+ " 级联清理：购物车、items 行、Redis item_store、属性关联、销售分类关联、店铺商品关联、"
							+ " 会员价、条码；搜索索引删除调度；afterCommit 商品删除事件。"
							+ " 删除链路其他异常统一 E5103（message 透传，空则「商品删除错误」）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " item_bn originalString 双通道（body 含键取原始值不 trim）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.entity.delete, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：多表 DELETE + Redis 库存键删除 + 搜索索引删除调度 + 商品删除事件；无第三方调用。",
			operationId = "ecx.item.entity.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 商品货号必填 / E5103 商品删除错误·删除商品信息有误·"
								+ "店铺商品信息有误，不可删除，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemEntityDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemEntityDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功（多规格删整组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-bn-required",
											summary = "item_bn 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"商品货号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "默认 SKU 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5103\",\"message\":\"商品删除错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-shop-mismatch",
											summary = "非平台商品（distributor_id≠0）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5103\","
															+ "\"message\":\"店铺商品信息有误，不可删除\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping("/ecx.item.entity.delete")
	public Map<String, Object> deleteItems(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "商品货号（默认 SKU item_bn）；必填非空白；多规格删整组（body 同名键优先）",
							required = true,
							example = "SKU200")
					@RequestParam(name = "item_bn", required = false)
					String itemBnParam,
			@RequestBody(
							description = "可选 JSON body；item_bn 与 query 同名双通道（取原始值）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemEntityDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String itemBnRaw = OpenapiRequestParams.originalString(itemBnParam, body, "item_bn");
		return entityDeletePort.deleteItems(companyId, itemBnRaw);
	}

	@Operation(
			summary = "新增平台商品",
			description =
					"按签名鉴权租户 company_id 新增平台商品（items，item_type/special_type 固定 normal、"
							+ " item_source=openapi，剔除 item_id 恒新建）。入参为 24 键白名单"
							+ "（body 含键优先，否则取 query 同名键；白名单外键忽略）。"
							+ " 必填校验依次：item_main_cat_id「管理分类必填」→item_name「商品名称必填」→pics「请上传商品图片」→"
							+ " templates_id「运费模板必填」→brand_id「品牌必填」→item_category「商品分类ID必填」（均 E5001）；"
							+ " pics/item_category 须非空标量 JSON 数组（E5100 格式文案）。"
							+ " nospec 缺省 true 单规格；false/0 多规格（spec_items 必填非空否则 E5100「请填写正确的规格数据」，"
							+ " 行内键覆盖 SPU 字段，未指定默认时取最低价 SKU）。金额单位分；重量/体积非法按 0。"
							+ " 销售分类仅正数 ID 生效（全非正数 E5100「请选择销售分类」）；价格不得小于进行中拼团活动价（E5100）。"
							+ " 落库固定 audit_status=approved（平台路径）、默认 SKU 挂品牌/销售分类/参数关联。"
							+ " 成功 data={status:true}（不返回 item_id），Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.entity.add, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 INSERT items（单/多 SKU）+ 关联表 + 默认标记回填；afterCommit 商品新增事件 +"
							+ " 店铺商品同步 + 搜索索引 upsert 调度；intro 内联 data:image 上传存储。",
			operationId = "ecx.item.entity.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 六必填文案 / E5100 商品图片格式错误·商品分类格式错误·"
								+ "请填写正确的规格数据·请选择销售分类·拼团价守卫及落库异常透传，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemEntityAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemEntityAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功（不返回 item_id）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-name-required",
											summary = "item_name 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"商品名称必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-spec-items",
											summary = "多规格但 spec_items 空/非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5100\","
															+ "\"message\":\"请填写正确的规格数据\",\"data\":null}"),
									@ExampleObject(
											name = "fail-pics-format",
											summary = "pics 非数组/含对象元素",
											value =
													"{\"status\":\"fail\",\"code\":\"E5100\",\"message\":\"商品图片格式错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping("/ecx.item.entity.add")
	public Map<String, Object> createItems(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description = "可选 JSON body；24 键白名单与 query 同名双通道（body 含键优先）；校验与规格语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemEntityAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiThirdApiV2NormalItemsEntityAddParams.buildMergedParams(request, body);
		return entityAddPort.createItems(companyId, merged);
	}

	@Operation(
			summary = "更新平台商品",
			description =
					"按签名鉴权租户 company_id 更新平台商品：入参为新增 24 键白名单 + old_item_bn 定位键"
							+ "（body 含键优先，否则 query 同名键）。必填校验依次：old_item_bn「老的商品货号必填」→"
							+ " item_main_cat_id/item_name/pics/templates_id/brand_id/item_category（同新增六文案，均 E5001）；"
							+ " pics/item_category 须非空标量 JSON 数组（E5100 格式文案）。"
							+ " old_item_bn 定位默认 SKU（查无 E5100「商品错误」兜底）；nospec 须与库中现值字符串完全一致"
							+ "（不一致 E5100「多规格和单规格不能切换」）。定位后注入 item_id 走新增同链路 upsert"
							+ "（字段全量覆盖，item_params JSON 字符串先解码、非法置 null）。"
							+ " 固定 item_type/special_type=normal、item_source=openapi。价格拼团守卫等同新增（E5100 透传）。"
							+ " 成功 data={status:true}，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.entity.update, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 upsert items（单/多 SKU）+ 关联表 + 默认标记；afterCommit 商品事件 +"
							+ " 店铺商品同步 + 搜索索引 upsert 调度；intro 内联图上传。",
			operationId = "ecx.item.entity.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 七必填文案 / E5100 格式·多规格和单规格不能切换·"
								+ "请填写正确的规格数据·商品错误·拼团价守卫及更新异常透传，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemEntityUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemEntityUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-old-bn-required",
											summary = "old_item_bn 缺失/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"老的商品货号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-nospec-switch",
											summary = "nospec 与库中现值不一致",
											value =
													"{\"status\":\"fail\",\"code\":\"E5100\","
															+ "\"message\":\"多规格和单规格不能切换\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "old_item_bn 查无默认 SKU（兜底文案）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5100\",\"message\":\"商品错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping("/ecx.item.entity.update")
	public Map<String, Object> updateItems(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description = "可选 JSON body；25 键白名单（24 新增键 + old_item_bn）与 query 同名双通道；校验与 nospec 约束见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemEntityUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiThirdApiV2NormalItemsEntityUpdateParams.buildMergedParams(request, body);
		return entityUpdatePort.updateItems(companyId, merged);
	}
}
