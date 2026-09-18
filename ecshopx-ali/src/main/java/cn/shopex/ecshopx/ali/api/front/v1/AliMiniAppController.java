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

import cn.shopex.ecshopx.ali.api.front.v1.dto.AliTemplateMessageResponse;
import cn.shopex.ecshopx.ali.service.alitemplate.AliTemplateMsgService;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FrontNoAuth
@RestController("aliFrontV1AliMiniApp")
@RequestMapping("/api/v1/h5app/wxapp")
public class AliMiniAppController {

	private final AliTemplateMsgService aliTemplateMsgService;

	public AliMiniAppController(AliTemplateMsgService aliTemplateMsgService) {
		this.aliTemplateMsgService = aliTemplateMsgService;
	}

	@GetMapping(value = "/alitemplatemessage", name = "支付宝模板消息")
	public ResponseEntity<ApiResult<AliTemplateMessageResponse>> getTemplateMessage(
			HttpServletRequest request,
			@RequestParam(value = "source_type", required = false) String sourceType) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (!(attr instanceof Integer companyId)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (!StringUtils.hasText(sourceType)) {
			return ResponseEntity.ok(ApiResult.ok(new AliTemplateMessageResponse(List.of())));
		}
		List<String> ids = aliTemplateMsgService.getValidTemplateIds(companyId, sourceType);
		return ResponseEntity.ok(ApiResult.ok(new AliTemplateMessageResponse(ids)));
	}
}
