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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@RestController("shopexAiMemberOutfitAdminV1")
@RequestMapping("/api/v1/member/outfit")
public class MemberOutfitController {

	@PostMapping(value = "/model", name = "创建模特")
	public ResponseEntity<Void> createModel() {
		return ResponseEntity.ok().build();
	}

	@PutMapping(value = "/model/{id}", name = "更新模特")
	public ResponseEntity<Void> updateModel(@PathVariable("id") String id) {
		return ResponseEntity.ok().build();
	}

	@DeleteMapping(value = "/model/{id}", name = "删除模特")
	public ResponseEntity<Void> deleteModel(@PathVariable("id") String id) {
		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/models", name = "模特列表")
	public ResponseEntity<Void> getModels() {
		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/logs", name = "穿搭日志")
	public ResponseEntity<Void> getLogs() {
		return ResponseEntity.ok().build();
	}
}
