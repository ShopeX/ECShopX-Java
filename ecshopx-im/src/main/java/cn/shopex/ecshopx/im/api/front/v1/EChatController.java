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

package cn.shopex.ecshopx.im.api.front.v1;

import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.im.service.EChatConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FrontNoAuth
@RestController("imEChatFrontV1")
@RequestMapping("/api/v1/h5app")
public class EChatController {

	private final EChatConfigService eChatConfigService;

	public EChatController(EChatConfigService eChatConfigService) {
		this.eChatConfigService = eChatConfigService;
	}

	@GetMapping(value = "/wxapp/im/echat", name = "获取一洽配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(HttpServletRequest request) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (!(attr instanceof Number n)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long companyId = n.longValue();
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		Map<String, Object> result = eChatConfigService.getInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}
}
