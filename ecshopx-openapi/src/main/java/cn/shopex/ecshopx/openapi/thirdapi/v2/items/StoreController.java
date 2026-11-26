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
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreGetPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreSyncPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreSyncBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreSyncSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemStoreUpdateSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-商品库存V2")
@OpenapiResponse
@RestController("openapiV2Store")
@RequestMapping("/api/openapi/internal/v2")
public class StoreController extends OpenapiBaseController {

	private final OpenapiItemStoreSyncPort itemStoreSyncPort;
	private final OpenapiItemStoreUpdatePort itemStoreUpdatePort;
	private final OpenapiItemStoreGetPort itemStoreGetPort;

	public StoreController(
			OpenapiItemStoreSyncPort itemStoreSyncPort,
			OpenapiItemStoreUpdatePort itemStoreUpdatePort,
			OpenapiItemStoreGetPort itemStoreGetPort) {
		this.itemStoreSyncPort = itemStoreSyncPort;
		this.itemStoreUpdatePort = itemStoreUpdatePort;
		this.itemStoreGetPort = itemStoreGetPort;
	}

	@Operation(
			summary = "同步商品库存",
			description =
					"按签名鉴权租户 company_id 同步商品库存（3 键白名单，body 含键优先否则 query）："
							+ " item_code 必填非空白（E5001「商品货号参数错误」）；store 必填非负整数"
							+ "（字符串 ^[0-9]+$ 且 ≤Integer.MAX，否则 E5001「同步库存数参数错误」）；"
							+ " distributor_code 键存在须标量（E5001「店铺ID参数错误」）。"
							+ " 总部分支（platform 或未传 distributor_code）：按 item_bn 定位（查无 E5101），"
							+ " 与现值相同跳过写库；否则更新 items.store + 守卫写 Redis 库存（不抬高现值）+ 库存变更事件。"
							+ " standard 店铺分支：店铺查无 E5401；店铺行/SPU 行 is_total_store=true 报 E5410；"
							+ " 已有店铺行覆盖 store，无则新建（price 取总部、is_can_sale=false）；均守卫写店铺维度 Redis 库存。"
							+ " 其他异常 E5000（空则「未知错误」）。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.store.sync, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：UPDATE items.store 或 @Transactional UPDATE/INSERT"
							+ " distribution_distributor_items.store + Redis item_store 守卫写 + 库存变更事件；无第三方调用。",
			operationId = "ecx.item.store.sync.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含与现值相同跳过场景）；"
								+ " 业务失败：status=fail, code=E5001 商品货号·同步库存数·店铺ID参数错误 / E5101 商品找不到 /"
								+ " E5401 店铺找不到 / E5410 商品总部发货无法更新库存和价格 / E5000 未知错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemStoreSyncFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemStoreSyncSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "同步成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-store-invalid",
											summary = "store 缺失/负数/非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"同步库存数参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-total-store",
											summary = "店铺行总部发货禁改库存",
											value =
													"{\"status\":\"fail\",\"code\":\"E5410\","
															+ "\"message\":\"商品总部发货无法更新库存和价格\",\"data\":null}"),
									@ExampleObject(
											name = "fail-goods-not-found",
											summary = "item_code 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PutMapping("/ecx.item.store.sync")
	public Void sync(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description = "可选 JSON body；3 键白名单与 query 同名双通道（body 含键优先）；双分支语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemStoreSyncBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreSyncParams.buildMergedParams(request, body);
		itemStoreSyncPort.syncStore(companyId, merged);
		return null;
	}

	@Operation(
			summary = "增量更新商品库存",
			description =
					"按签名鉴权租户 company_id 对商品库存做加/减增量（4 键白名单，body 含键优先否则 query）："
							+ " item_code 必填非空白（E5001「商品货号参数错误」）；"
							+ " increase_store 与 decrease_store 恰一必填（都缺 E5001「增加库存或减去库存二选一必填」，"
							+ " 同时存在 E5001「增加库存或减去库存不能同时存在！」），非负整数"
							+ "（字符串 ^[0-9]+$ 且 ≤Integer.MAX，否则 E5001「增加库存数参数错误」/「减去库存数参数错误」）；"
							+ " distributor_code 键存在须标量（E5001「店铺ID参数错误」）。"
							+ " productModel=standard 且 distributor_code 非空白：直接无操作成功（不更新店铺库存）。"
							+ " 否则总部增量分支：按 item_bn 定位（查无 E5101），delta=0 不写库；"
							+ " SQL 原子加/减 items.store（减量带 store ≥ 扣减数守卫，不足 affected=0 静默成功）；"
							+ " 扣/加成功则回读新库存并权威覆盖 Redis 库存 + 库存变更事件。"
							+ " 其他异常 E5000（空则「未知错误」）。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.store.update, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：UPDATE items.store/updated + Redis item_store 权威覆盖写 + 库存变更事件；无第三方调用。"
							+ " summary 为语义命名（注册表未提供中文接口名）。",
			operationId = "ecx.item.store.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null"
								+ "（含 standard 店铺无操作、delta=0、减量库存不足静默成功场景）；"
								+ " 业务失败：status=fail, code=E5001 商品货号·二选一必填·增加/减去库存数·不能同时存在·店铺ID参数错误 /"
								+ " E5101 商品找不到 / E5000 未知错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemStoreUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemStoreUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "增量更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-delta",
											summary = "increase_store/decrease_store 都缺失",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"增加库存或减去库存二选一必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-both-delta",
											summary = "increase_store 与 decrease_store 同时传",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"增加库存或减去库存不能同时存在！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-decrease-invalid",
											summary = "decrease_store 负数/非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"减去库存数参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-goods-not-found",
											summary = "item_code 查无（总部分支）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PutMapping("/ecx.item.store.update")
	public Void update(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description =
									"可选 JSON body；4 键白名单与 query 同名双通道（body 含键优先）；"
											+ "increase_store/decrease_store 恰一必填，standard 店铺无操作语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemStoreUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreUpdateParams.buildMergedParams(request, body);
		itemStoreUpdatePort.updateStore(companyId, merged);
		return null;
	}

	@Operation(
			summary = "查询商品库存",
			description =
					"按签名鉴权租户 company_id 查询商品库存：入参 2 键白名单（item_code/distributor_code，"
							+ " body 含键优先否则 query 同名键，空串值忽略）。item_code 按 item_bn 定位"
							+ "（item_id 升序 LIMIT 1；缺省取租户第一条商品——PHP 旧契约），查无 E5101「商品找不到」。"
							+ " data 3 固定键：item_code/item_name 字符串化、store 整型（null 按 0）。"
							+ " distributor_code 仅 productModel=standard 生效：查无店铺 E5401「店铺找不到」；"
							+ " 命中追加 distributor_code/distributor_name，且存在 distributor_id IS NULL 的"
							+ " 店铺商品行时用其 store 覆盖；platform 模式忽略。"
							+ " 其他异常 E5000（message 透传，空则「未知错误」）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.store.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.store.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemStoreGetDataDoc；"
								+ " 业务失败：status=fail, code=E5101 商品找不到 / E5401 店铺找不到 / E5000 未知错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemStoreGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemStoreGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（总部库存）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"item_code\":\"SKU200\",\"item_name\":\"Wire test goods\","
															+ "\"store\":10}}"),
									@ExampleObject(
											name = "success-distributor",
											summary = "standard 店铺分支（追加店铺键）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"item_code\":\"SKU200\",\"item_name\":\"Wire test goods\","
															+ "\"store\":10,\"distributor_code\":\"S001\",\"distributor_name\":\"新门店名\"}}"),
									@ExampleObject(
											name = "fail-goods-not-found",
											summary = "item_code 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-shop-not-found",
											summary = "standard 分支店铺查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5401\",\"message\":\"店铺找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping("/ecx.item.store.get")
	public Map<String, Object> getStoreDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description = "可选 JSON body；item_code/distributor_code 与 query 同名双通道（body 含键优先，空串忽略）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemStoreGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreGetParams.buildMergedParams(request, body);
		return itemStoreGetPort.getStoreDetail(companyId, merged);
	}
}
