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
import cn.shopex.ecshopx.espier.service.ExportCsvDataResult;
import cn.shopex.ecshopx.espier.service.ExportCsvDataService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("espierAdminV1ExportData")
@RequestMapping("/api/v1/espier")
public class ExportDataController {

	private final ExportCsvDataService exportCsvDataService;

	public ExportDataController(ExportCsvDataService exportCsvDataService) {
		this.exportCsvDataService = exportCsvDataService;
	}

	@Activated(routeAlias = "espier.export.file.down")
	@GetMapping(value = "/exportCsvData", name = "导出csv文件")
	public ResponseEntity<?> exportCsvData(
			@RequestParam(value = "page", required = false, defaultValue = "0") String page,
			@RequestParam(value = "key", required = false, defaultValue = "0") String key,
			@RequestParam(value = "total_count", required = false, defaultValue = "0") String totalCount) {
		ExportCsvDataResult r = exportCsvDataService.exportCsvData(page, key, totalCount);
		if (r.isKeyError()) {
			// 导出会话 key 不存在：HTTP 200，错误语义放在 data 内（含 status_code 500）
			return ResponseEntity.ok(
					Map.of("data", Map.of("message", "key error", "status_code", 500)));
		}
		return ResponseEntity.ok(ApiResult.ok(r.getPayload()));
	}
}
