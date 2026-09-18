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

package cn.shopex.ecshopx.bspay.api.admin.v1;

import cn.shopex.ecshopx.bspay.service.callback.BsPayCallbackOrchestratorService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = false,
		notFound = false)
@RestController("bspayCallBackAdminV1")
@RequestMapping("/api/v1/bspay")
public class CallBackController {

	private final BsPayCallbackOrchestratorService orchestrator;

	public CallBackController(BsPayCallbackOrchestratorService orchestrator) {
		this.orchestrator = orchestrator;
	}

	@Activated(routeAlias = "bspay.callback")
	@PostMapping("/callback/{eventType}")
	public ResponseEntity<Object> handle(
			@PathVariable("eventType") String eventType,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object inner = orchestrator.handle(eventType, body == null ? Map.of() : body);
		return ResponseEntity.ok(Map.of("data", inner));
	}
}
