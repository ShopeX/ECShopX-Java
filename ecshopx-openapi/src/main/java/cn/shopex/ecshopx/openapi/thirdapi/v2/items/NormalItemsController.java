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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping("/ecx.item.entity.add")
	public Map<String, Object> createItems(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
