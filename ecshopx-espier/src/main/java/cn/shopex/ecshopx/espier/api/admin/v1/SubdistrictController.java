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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.service.subdistrict.SubdistrictApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("espierAdminV1Subdistrict")
@RequestMapping("/api/v1/espier/subdistrict")
public class SubdistrictController {

	private final SubdistrictApplicationService subdistrictApplicationService;

	public SubdistrictController(SubdistrictApplicationService subdistrictApplicationService) {
		this.subdistrictApplicationService = subdistrictApplicationService;
	}

	@Activated(routeAlias = "espier.subdistrict.list.get")
	@GetMapping(name = "获取街道社区列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> get(
			HttpServletRequest request,
			@RequestParam(name = "label", required = false) String label) {
		List<Map<String, Object>> data = subdistrictApplicationService.get(request, label);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.subdistrict.get")
	@GetMapping(value = "/{id}", name = "获取街道社区")
	public ResponseEntity<ApiResult<Object>> getInfo(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> map = subdistrictApplicationService.getInfo(request, id);
		if (map == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(map));
	}

	@Activated(routeAlias = "espier.subdistrict.delete")
	@DeleteMapping(value = "/{id}", name = "删除街道社区")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @PathVariable("id") String id) {
		subdistrictApplicationService.delete(request, id);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "espier.subdistrict.save")
	@PutMapping(name = "保存街道社区")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		LinkedHashMap<String, Object> onlyMap = new LinkedHashMap<>();
		onlyMap.put("id", merged.get("id"));
		onlyMap.put("parent_id", merged.get("parent_id"));
		onlyMap.put("label", merged.get("label"));
		onlyMap.put("distributor_id", merged.get("distributor_id"));
		if (merged.containsKey("regions_id")) {
			onlyMap.put("regions_id", merged.get("regions_id"));
		}
		onlyMap.put("province", merged.get("province"));
		onlyMap.put("city", merged.get("city"));
		onlyMap.put("area", merged.get("area"));
		Map<String, Object> data = subdistrictApplicationService.save(request, onlyMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
