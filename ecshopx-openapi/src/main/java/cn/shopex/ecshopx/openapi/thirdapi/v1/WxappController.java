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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
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

	@PostMapping(
			value = "/ecx.wxapp.qrcode",
			name = "开放接口获取导购任务小程序码",
			produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getWxCode(
			HttpServletRequest request,
			@RequestParam(name = "path_type", required = false) String pathTypeParam,
			@RequestParam(name = "scene", required = false) String sceneParam,
			@RequestParam(name = "width", required = false) String widthParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PostMapping(value = "/ecx.wxapp.shoplist", name = "开放接口获取微信店铺数据")
	public OpenapiEnvelope getWxShopLists(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "start_time", required = false) String startTimeParam,
			@RequestParam(name = "end_time", required = false) String endTimeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
		return new OpenapiEnvelope("success", "0", "success", data);
	}

	@PostMapping(value = "/ecx.weappid.get", name = "开放接口获取yykweishop模板appid")
	public OpenapiEnvelope getWeappId(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = weappIdPort.getWeappIdByTemplate(companyId, "yykweishop");
		return new OpenapiEnvelope("success", "0", "success", data);
	}
}
