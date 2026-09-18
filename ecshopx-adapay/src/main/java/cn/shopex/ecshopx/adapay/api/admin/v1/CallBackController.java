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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.service.callback.AdapayCallbackOrchestratorService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.PLAIN,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = false,
		notFound = false)
@RestController("adapayCallBackAdminV1")
@RequestMapping("/api/v1/adapay")
public class CallBackController {

	private final AdapayCallbackOrchestratorService adapayCallbackOrchestratorService;

	public CallBackController(AdapayCallbackOrchestratorService adapayCallbackOrchestratorService) {
		this.adapayCallbackOrchestratorService = adapayCallbackOrchestratorService;
	}

	@PostMapping(value = "/callback")
	public ResponseEntity<Map<String, Object>> handle(@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(adapayCallbackOrchestratorService.handle(body));
	}
}
