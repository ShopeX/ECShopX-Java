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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountExportService;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountPasswordService;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountQueryService;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountStatusService;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountWriteService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@RestController("companysAdminV1PosStoreAccount")
@RequestMapping("/api/v1/store/pos-accounts")
public class PosStoreAccountController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PosStoreAccountPasswordService passwordService;
	private final PosStoreAccountQueryService queryService;
	private final PosStoreAccountWriteService writeService;
	private final PosStoreAccountStatusService statusService;
	private final PosStoreAccountExportService exportService;

	public PosStoreAccountController(
			PosStoreAccountPasswordService passwordService,
			PosStoreAccountQueryService queryService,
			PosStoreAccountWriteService writeService,
			PosStoreAccountStatusService statusService,
			PosStoreAccountExportService exportService) {
		this.passwordService = passwordService;
		this.queryService = queryService;
		this.writeService = writeService;
		this.statusService = statusService;
		this.exportService = exportService;
	}

	@DataPass(pathAlias = "store.pos_account")
	@Activated(routeAlias = "store.pos_account")
	@GetMapping(name = "POS账号列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(
			HttpServletRequest request,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "role", required = false) String role,
			@RequestParam(value = "distributor_id", required = false) Long distributorId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) {
		return ResponseEntity.ok(
				ApiResult.ok(
						queryService.list(
								requireJwt(request), keyword, role, distributorId, status, page, pageSize)));
	}

	@DataPass(pathAlias = "store.pos_account")
	@Activated(routeAlias = "store.pos_account")
	@GetMapping(value = "/{id}", name = "POS账号详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> detail(
			HttpServletRequest request, @PathVariable("id") long id) {
		return ResponseEntity.ok(ApiResult.ok(queryService.detail(requireJwt(request), id)));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/export", name = "导出POS账号", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> export(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		exportService.enqueue(requireJwt(request), body);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("data", Map.of("status", true)));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(name = "新建POS账号")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(ApiResult.ok(writeService.create(requireJwt(request), body)));
	}

	@Activated(routeAlias = "store.pos_account")
	@PutMapping(value = "/{id}", name = "编辑POS账号")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("id") long id,
			@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(ApiResult.ok(writeService.update(requireJwt(request), id, body)));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/{id}/reset-password", name = "重置POS账号密码")
	public ResponseEntity<ApiResult<Map<String, Object>>> resetPassword(
			HttpServletRequest request, @PathVariable("id") long id) {
		return ResponseEntity.ok(ApiResult.ok(passwordService.reset(requireJwt(request), id)));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/{id}/status", name = "变更POS账号状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> changeStatus(
			HttpServletRequest request,
			@PathVariable("id") long id,
			@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(
				ApiResult.ok(statusService.change(requireJwt(request), id, str(body, "action"))));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/batch-status", name = "批量变更POS账号状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> batchStatus(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(
				ApiResult.ok(statusService.batch(requireJwt(request), ids(body.get("ids")), str(body, "action"))));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/{id}/role", name = "变更POS账号角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> changeRole(
			HttpServletRequest request,
			@PathVariable("id") long id,
			@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(
				ApiResult.ok(writeService.changeRole(requireJwt(request), id, str(body, "role"))));
	}

	@Activated(routeAlias = "store.pos_account")
	@PostMapping(value = "/{id}/shop", name = "POS账号调店")
	public ResponseEntity<ApiResult<Map<String, Object>>> changeShop(
			HttpServletRequest request,
			@PathVariable("id") long id,
			@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(
				ApiResult.ok(writeService.changeShop(requireJwt(request), id, body.get("distributor_id"))));
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> requireJwt(HttpServletRequest request) {
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		return jwt;
	}

	private static String str(Map<String, Object> body, String key) {
		if (body == null || body.get(key) == null) {
			return "";
		}
		return body.get(key).toString();
	}

	private static List<Long> ids(Object raw) {
		List<Long> out = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				addId(out, o);
			}
		} else {
			addId(out, raw);
		}
		return out;
	}

	private static void addId(List<Long> out, Object o) {
		if (o instanceof Number n && n.longValue() > 0) {
			out.add(n.longValue());
			return;
		}
		if (o == null) {
			return;
		}
		try {
			long v = Long.parseLong(o.toString().trim());
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException ignored) {
		}
	}
}
