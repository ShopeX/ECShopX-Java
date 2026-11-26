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

	@GetMapping("/ecx.items.entity.get")
	public Map<String, Object> getList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "approve_status", required = false) String approveStatusParam,
			@RequestParam(name = "brand_id", required = false) String brandIdParam,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@RequestParam(name = "is_self", required = false) String isSelfParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@GetMapping("/ecx.item.entity.get")
	public Map<String, Object> getItemSpuDetail(
			HttpServletRequest request,
			@RequestParam(name = "item_bn", required = false) String itemBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String itemBn = OpenapiRequestParams.originalString(itemBnParam, body, "item_bn");
		return normalItemsEntityDetailPort.getEntityDetail(companyId, itemBn);
	}

	@PostMapping("/ecx.item.entity.status.update")
	public Map<String, Object> batchUpdateItemsStatus(
			HttpServletRequest request,
			@RequestParam(name = "item_bn", required = false) String itemBnParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String itemBnRaw = OpenapiRequestParams.originalString(itemBnParam, body, "item_bn");
		String statusRaw = OpenapiRequestParams.originalString(statusParam, body, "status");
		return entityStatusUpdatePort.batchUpdateItemsStatus(companyId, itemBnRaw, statusRaw);
	}

	@DeleteMapping("/ecx.item.entity.delete")
	public Map<String, Object> deleteItems(
			HttpServletRequest request,
			@RequestParam(name = "item_bn", required = false) String itemBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PostMapping("/ecx.item.entity.update")
	public Map<String, Object> updateItems(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiThirdApiV2NormalItemsEntityUpdateParams.buildMergedParams(request, body);
		return entityUpdatePort.updateItems(companyId, merged);
	}
}
