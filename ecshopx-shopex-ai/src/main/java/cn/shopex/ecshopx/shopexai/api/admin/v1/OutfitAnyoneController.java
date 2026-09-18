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

package cn.shopex.ecshopx.shopexai.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.GenerateOutfitRequest;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.OutfitGenerateAcceptedResponse;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.OutfitGenerateDirectResponse;
import cn.shopex.ecshopx.shopexai.service.OutfitAnyoneGenerateOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@RestController("shopexAiOutfitAnyoneAdminV1")
@RequestMapping("/api/v1/outfit")
public class OutfitAnyoneController {

	private final OutfitAnyoneGenerateOrchestratorService outfitAnyoneGenerateOrchestratorService;
	private final boolean useQueue;

	public OutfitAnyoneController(
			OutfitAnyoneGenerateOrchestratorService outfitAnyoneGenerateOrchestratorService,
			@Value("${shopexai.outfit.use-queue:true}") boolean useQueue) {
		this.outfitAnyoneGenerateOrchestratorService = outfitAnyoneGenerateOrchestratorService;
		this.useQueue = useQueue;
	}

	@PostMapping(value = "/generate", name = "穿搭生成")
	public ResponseEntity<?> generate(@RequestBody @Valid GenerateOutfitRequest body) {
		long companyId = 1L;
		long operatorId = 1L;
		long distributorId = 0L;
		if (useQueue) {
			OutfitGenerateAcceptedResponse accepted =
					outfitAnyoneGenerateOrchestratorService.generateOutfitQueued(
							body, companyId, operatorId, distributorId);
			return ResponseEntity.ok(accepted);
		}
		OutfitGenerateDirectResponse direct =
				outfitAnyoneGenerateOrchestratorService.generateOutfitDirect(
						body, companyId, operatorId, distributorId);
		return ResponseEntity.ok(direct);
	}

	@GetMapping(value = "/status/{task_id}", name = "穿搭任务状态")
	public ResponseEntity<Void> checkStatus(@PathVariable("task_id") String taskId) {
		return ResponseEntity.ok().build();
	}
}
