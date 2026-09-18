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
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
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

	@GetMapping(value = "/ecx.distributor.list", name = "开放接口查询店铺列表")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "distributor_name", required = false) String distributorNameParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "province", required = false) String provinceParam,
			@RequestParam(name = "city", required = false) String cityParam,
			@RequestParam(name = "area", required = false) String areaParam,
			@RequestParam(name = "contact_username", required = false) String contactUsernameParam,
			@RequestParam(name = "contact_mobile", required = false) String contactMobileParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PostMapping(value = "/ecx.distributor.create", name = "开放接口创建店铺")
	public Map<String, Object> create(
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
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@GetMapping(value = "/ecx.distributor.detail", name = "开放接口查询店铺详情")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
