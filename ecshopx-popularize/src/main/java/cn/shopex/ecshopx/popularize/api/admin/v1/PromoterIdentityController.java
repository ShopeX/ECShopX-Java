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

package cn.shopex.ecshopx.popularize.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.popularize.service.PromoterIdentityQueryService;
import cn.shopex.ecshopx.popularize.service.PromoterIdentitySaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("popularizePromoterIdentityAdminV1")
@RequestMapping("/api/v1")
public class PromoterIdentityController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PromoterIdentitySaveService promoterIdentitySaveService;
	private final PromoterIdentityQueryService promoterIdentityQueryService;

	public PromoterIdentityController(
			PromoterIdentitySaveService promoterIdentitySaveService,
			PromoterIdentityQueryService promoterIdentityQueryService) {
		this.promoterIdentitySaveService = promoterIdentitySaveService;
		this.promoterIdentityQueryService = promoterIdentityQueryService;
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.identity.list")
	@GetMapping(value = "/popularize/promoter/identity/list", name = "获取推广员身份列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromoteridentityList(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String pageQuery = request.getParameter("page");
		String pageSizeQuery = request.getParameter("pageSize");
		Map<String, Object> data =
				promoterIdentityQueryService.getPromoteridentityList(companyId, pageQuery, pageSizeQuery);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.identity.info")
	@GetMapping(value = "/popularize/promoter/identity/info", name = "获取推广员身份详情")
	public ResponseEntity<ApiResult<Object>> getPromoteridentityInfo(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String idRaw = mergeWhitelistField(idQuery, body, "id");
		if (idRaw == null || !StringUtils.hasText(idRaw.trim())) {
			throw new BadRequestException("ID错误");
		}
		long idParsed;
		try {
			idParsed = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("ID错误");
		}
		if (idParsed < 1L) {
			throw new BadRequestException("ID错误");
		}
		Map<String, Object> data = promoterIdentityQueryService.getPromoteridentityInfo(companyId, idParsed);
		if (data == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.identity.save")
	@PostMapping(value = "/popularize/promoter/identity", name = "保存推广员身份")
	public ResponseEntity<ApiResult<Map<String, Object>>> savePromoteridentity(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idQuery,
			@RequestParam(value = "name", required = false) String nameQuery,
			@RequestParam(value = "is_subordinates", required = false) String isSubordinatesQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String idRaw = mergeWhitelistField(idQuery, body, "id");
		String nameTrimmed = mergeWhitelistField(nameQuery, body, "name");
		String subRaw = mergeWhitelistField(isSubordinatesQuery, body, "is_subordinates");
		if (!StringUtils.hasText(nameTrimmed)) {
			throw new BadRequestException("推广员身份名称错误");
		}
		promoterIdentitySaveService.savePromoteridentity(companyId, idRaw, nameTrimmed, subRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.identity.delete")
	@DeleteMapping(value = "/popularize/promoter/identity", name = "删除推广员身份")
	public ResponseEntity<ApiResult<Map<String, Object>>> deletePromoteridentity(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String idRaw = mergeWhitelistField(idQuery, body, "id");
		if (idRaw == null || !StringUtils.hasText(idRaw.trim())) {
			throw new BadRequestException("ID错误");
		}
		long idParsed;
		try {
			idParsed = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("ID错误");
		}
		if (idParsed < 1L) {
			throw new BadRequestException("ID错误");
		}
		promoterIdentitySaveService.deletePromoteridentity(companyId, idParsed);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.identity.default")
	@PostMapping(value = "/popularize/promoter/identity/default", name = "默认推广员身份")
	public ResponseEntity<ApiResult<Map<String, Object>>> defaultPromoteridentity(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String idRaw = mergeWhitelistField(idQuery, body, "id");
		if (idRaw == null || !StringUtils.hasText(idRaw.trim())) {
			throw new BadRequestException("ID错误");
		}
		long idParsed;
		try {
			idParsed = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("ID错误");
		}
		if (idParsed < 1L) {
			throw new BadRequestException("ID错误");
		}
		promoterIdentitySaveService.defaultPromoteridentity(companyId, idParsed);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String mergeWhitelistField(String requestParam, Map<String, Object> body, String key) {
		if (requestParam != null && StringUtils.hasText(requestParam.trim())) {
			return requestParam.trim();
		}
		if (body != null && body.get(key) != null) {
			return String.valueOf(body.get(key)).trim();
		}
		return null;
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}
}
