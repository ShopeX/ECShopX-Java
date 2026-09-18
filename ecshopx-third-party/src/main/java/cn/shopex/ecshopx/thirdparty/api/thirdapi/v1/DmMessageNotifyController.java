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

package cn.shopex.ecshopx.thirdparty.api.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.thirdparty.service.dm.DmMessageNotifyWebhookService;
import cn.shopex.ecshopx.thirdparty.web.ThirdPartyHttpInputMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@ShopLog
@RestController("thirdPartyDmMessageNotifyThirdApiV1")
@RequestMapping("/api/v1")
public class DmMessageNotifyController {

	private final DmMessageNotifyWebhookService dmMessageNotifyWebhookService;

	public DmMessageNotifyController(DmMessageNotifyWebhookService dmMessageNotifyWebhookService) {
		this.dmMessageNotifyWebhookService = dmMessageNotifyWebhookService;
	}

	@Activated(routeAlias = "third.dm.messageNotify")
	@PostMapping(value = "/third/dm/messageNotify/{companyId}", name = "达摩消息订阅回调 messageNotify")
	public ResponseEntity<Map<String, Object>> messageNotify(
			@PathVariable("companyId") long companyId,
			@FlexibleBody(required = false) Map<String, Object> body,
			HttpServletRequest request) {
		if (companyId <= 0) {
			throw new BadRequestException("companyId must be positive");
		}
		Map<String, Object> merged = ThirdPartyHttpInputMerge.mergeJsonLikeBodyWithQuery(request, body);
		Map<String, Object> data = dmMessageNotifyWebhookService.handle(companyId, merged, request);
		LinkedHashMap<String, Object> wrapped = new LinkedHashMap<>();
		wrapped.put("data", data);
		return ResponseEntity.ok(wrapped);
	}
}
