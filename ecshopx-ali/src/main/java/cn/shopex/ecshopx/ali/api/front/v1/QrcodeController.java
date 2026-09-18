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

package cn.shopex.ecshopx.ali.api.front.v1;

import cn.shopex.ecshopx.ali.service.h5.H5AlipayMiniQrcodeFacade;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@FrontNoAuth
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO_FIXED,
		badRequest = DingoResponse.BadRequestStyle.DINGO_FIXED)
@RestController("aliFrontV1Qrcode")
@RequestMapping("/api/v1/h5app")
public class QrcodeController {

	private final H5AlipayMiniQrcodeFacade h5AlipayMiniQrcodeFacade;

	public QrcodeController(H5AlipayMiniQrcodeFacade h5AlipayMiniQrcodeFacade) {
		this.h5AlipayMiniQrcodeFacade = h5AlipayMiniQrcodeFacade;
	}

	@GetMapping(value = "/alipaymini/qrcode.png", name = "支付宝小程序码")
	public ResponseEntity<ApiResult<Map<String, String>>> getQrcode(
			@RequestParam(value = "company_id", required = false) String companyId,
			@RequestParam(value = "page", required = false, defaultValue = "pages/index") String page,
			@RequestParam(required = false) String cxdid,
			@RequestParam(required = false) String dtid,
			@RequestParam(required = false) String smid,
			@RequestParam(required = false) String uid,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Map<String, String> data =
				h5AlipayMiniQrcodeFacade.createQrcodeUrl(companyId, page, cxdid, dtid, smid, uid, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
