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

package cn.shopex.ecshopx.wsugc.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.wsugc.service.setting.UgcSettingListReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@FrontNoAuth
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true)
@RestController("wsugcFrontV1Setting")
@RequestMapping("/api/v1/h5app")
public class SettingController {

	private final UgcSettingListReadService ugcSettingListReadService;

	public SettingController(UgcSettingListReadService ugcSettingListReadService) {
		this.ugcSettingListReadService = ugcSettingListReadService;
	}

	@GetMapping(value = "/wxapp/ugc/post/setting", name = "UGC设置")
	public ResponseEntity<ApiResult<Object>> getSetting(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false, defaultValue = "") String type) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (cidAttr instanceof Integer i && i > 0) {
			companyId = i.longValue();
		} else if (cidAttr instanceof Long l && l > 0L) {
			companyId = l;
		} else {
			companyId = 1L;
		}

		Map<String, String> setting = ugcSettingListReadService.loadKeyValueByCompanyAndType(companyId, type);
		if (setting.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(setting));
	}

	@GetMapping(value = "/wxapp/mps/pullfeed", name = "pullfeed")
	public ResponseEntity<Void> pullfeed() {
		return ResponseEntity.ok().build();
	}
}
