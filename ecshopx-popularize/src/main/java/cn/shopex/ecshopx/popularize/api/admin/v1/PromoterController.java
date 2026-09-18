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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.popularize.service.PromoterAdminListOrchestratorService;
import cn.shopex.ecshopx.popularize.service.PromoterAddService;
import cn.shopex.ecshopx.popularize.service.PromoterChildrenListService;
import cn.shopex.ecshopx.popularize.service.PromoterFirstIdentityListService;
import cn.shopex.ecshopx.popularize.service.PromoterDisabledService;
import cn.shopex.ecshopx.popularize.service.PromoterGradeLevelUpdateService;
import cn.shopex.ecshopx.popularize.service.PromoterMemberRelRemoveService;
import cn.shopex.ecshopx.popularize.service.PromoterRelRemoveService;
import cn.shopex.ecshopx.popularize.service.PromoterShopStatusUpdateService;
import cn.shopex.ecshopx.popularize.service.MerchantScopeDistributorIdsReadService;
import cn.shopex.ecshopx.popularize.service.export.PopularizeOrderExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PopularizeStaticExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("popularizePromoterAdminV1")
@RequestMapping("/api/v1")
public class PromoterController {

	private static final Logger log = LoggerFactory.getLogger(PromoterController.class);

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PromoterAddService promoterAddService;
	private final PromoterDisabledService promoterDisabledService;
	private final PromoterGradeLevelUpdateService promoterGradeLevelUpdateService;
	private final PromoterShopStatusUpdateService promoterShopStatusUpdateService;
	private final PromoterChildrenListService promoterChildrenListService;
	private final PromoterFirstIdentityListService promoterFirstIdentityListService;
	private final PromoterExportOrchestratorService promoterExportOrchestratorService;
	private final MerchantScopeDistributorIdsReadService merchantScopeDistributorIdsReadService;
	private final PromoterAdminListOrchestratorService promoterAdminListOrchestratorService;
	private final PromoterRelRemoveService promoterRelRemoveService;
	private final PromoterMemberRelRemoveService promoterMemberRelRemoveService;

	public PromoterController(
			PromoterAddService promoterAddService,
			PromoterDisabledService promoterDisabledService,
			PromoterGradeLevelUpdateService promoterGradeLevelUpdateService,
			PromoterShopStatusUpdateService promoterShopStatusUpdateService,
			PromoterChildrenListService promoterChildrenListService,
			PromoterFirstIdentityListService promoterFirstIdentityListService,
			PromoterExportOrchestratorService promoterExportOrchestratorService,
			MerchantScopeDistributorIdsReadService merchantScopeDistributorIdsReadService,
			PromoterAdminListOrchestratorService promoterAdminListOrchestratorService,
			PromoterRelRemoveService promoterRelRemoveService,
			PromoterMemberRelRemoveService promoterMemberRelRemoveService) {
		this.promoterAddService = promoterAddService;
		this.promoterDisabledService = promoterDisabledService;
		this.promoterGradeLevelUpdateService = promoterGradeLevelUpdateService;
		this.promoterShopStatusUpdateService = promoterShopStatusUpdateService;
		this.promoterChildrenListService = promoterChildrenListService;
		this.promoterFirstIdentityListService = promoterFirstIdentityListService;
		this.promoterExportOrchestratorService = promoterExportOrchestratorService;
		this.merchantScopeDistributorIdsReadService = merchantScopeDistributorIdsReadService;
		this.promoterAdminListOrchestratorService = promoterAdminListOrchestratorService;
		this.promoterRelRemoveService = promoterRelRemoveService;
		this.promoterMemberRelRemoveService = promoterMemberRelRemoveService;
	}

	@Activated(routeAlias = "popularize.promoter.add")
	@PostMapping(value = "/popularize/promoter/add", name = "指定会员成为顶级推广员")
	public ResponseEntity<ApiResult<Map<String, Object>>> addPromoter(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String resolvedMobile = mobile;
		if (!StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim())) {
			if (body != null && body.get("mobile") != null) {
				resolvedMobile = String.valueOf(body.get("mobile"));
			} else {
				resolvedMobile = null;
			}
		}
		String resolvedUserId = userIdParam;
		if (!StringUtils.hasText(resolvedUserId == null ? "" : resolvedUserId.trim())) {
			if (body != null && body.get("user_id") != null) {
				resolvedUserId = String.valueOf(body.get("user_id")).trim();
			} else {
				resolvedUserId = null;
			}
		}
		promoterAddService.addPromoter(
				companyId,
				resolvedMobile != null ? resolvedMobile.trim() : null,
				resolvedUserId,
				body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readOperatorIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			return 0L;
		}
		Object v = ud.get("operator_id");
		if (v == null) {
			return 0L;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readMerchantIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			return 0L;
		}
		Object v = ud.get("merchant_id");
		if (v == null) {
			return 0L;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String readOperatorTypeFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			return "";
		}
		Object v = ud.get("operator_type");
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
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

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "popularize.promoter.children.list")
	@GetMapping(value = "/popularize/promoter/children", name = "获取推广员直属下级列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromoterchildrenList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "promoter_id", required = false, defaultValue = "1") String promoterIdRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		int page = parsePositiveIntParam(pageRaw);
		int pageSize = parsePositiveIntParam(pageSizeRaw);
		long parentPromoterId = parseParentPromoterId(promoterIdRaw);

		boolean block = truthyDatapassBlock(request.getAttribute("x-datapass-block"));
		Map<String, Object> data =
				promoterChildrenListService.getPromoterchildrenList(
						companyId, parentPromoterId, page, pageSize);

		if (block) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					Object mob = row.get("mobile");
					if (mob != null) {
						String plain = String.valueOf(mob).trim();
						if (!plain.isEmpty()) {
							row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
						}
					}
					Object pm = row.get("pmobile");
					if (pm != null) {
						String pplain = String.valueOf(pm).trim();
						if (!pplain.isEmpty()) {
							row.put("pmobile", DataMasking.maskMobile(String.valueOf(pm)));
						}
					}
					Object un = row.get("username");
					if (un != null) {
						row.put(
								"username",
								DataMasking.maskTruenameIfBlocked(String.valueOf(un), 1));
					}
					Object nn = row.get("nickname");
					if (nn != null) {
						row.put(
								"nickname",
								DataMasking.maskTruenameIfBlocked(String.valueOf(nn), 1));
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePositiveIntParam(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 1) {
				throw new BadRequestException("参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	private static Integer tryParseStrictPositiveIntForFirstIdentityList(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 1) {
				return null;
			}
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int[] parseFirstIdentityListPaginationOrNull(String pageRaw, String pageSizeRaw) {
		Integer page = tryParseStrictPositiveIntForFirstIdentityList(pageRaw);
		Integer pageSize = tryParseStrictPositiveIntForFirstIdentityList(pageSizeRaw);
		if (page == null || pageSize == null) {
			return null;
		}
		return new int[] {page, pageSize};
	}

	private static long parseParentPromoterId(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			return 1L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	private static boolean truthyDatapassBlock(Object o) {
		if (Boolean.TRUE.equals(o)) {
			return true;
		}
		if (o instanceof Number n && n.intValue() != 0) {
			return true;
		}
		if (o != null) {
			String t = o.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return true;
			}
		}
		return false;
	}

	@DataPass
	@Activated(routeAlias = "popularize.promoter.list.get")
	@GetMapping(value = "/popularize/promoter/list", name = "获取推广员列表")
	public ResponseEntity<ApiResult<Object>> getPromoterList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "identity_name", required = false) String identityName,
			@RequestParam(value = "store_status", required = false) String storeStatus,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "is_all", required = false, defaultValue = "false") boolean isAll,
			@RequestParam(value = "distributorIds", required = false) String distributorIds,
			@RequestParam(value = "pathSource", required = false) String pathSource,
			@RequestParam(value = "x-datapass-block", required = false) String datapassBlockParam) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long merchantId = readMerchantIdFromOperatorJwt(request);
		String operatorType = readOperatorTypeFromOperatorJwt(request);

		Map<String, Object> orchestration =
				promoterAdminListOrchestratorService.getPromoterList(
						companyId,
						merchantId,
						operatorType,
						page,
						pageSize,
						mobile,
						username,
						identityName,
						storeStatus,
						timeStartBegin,
						timeStartEnd,
						distributorId,
						isAll,
						distributorIds,
						pathSource);

		String shape = String.valueOf(orchestration.get("responseShape"));
		Object body = orchestration.get("body");

		Object datapassAttr = request.getAttribute("x-datapass-block");
		if (datapassAttr == null) {
			datapassAttr = datapassBlockParam;
		}
		boolean datapassBlock = truthyDatapassBlock(datapassAttr);

		switch (shape) {
			case "LIST_ROOT":
			case "LIST_BROKERAGE_ROWS":
				return ResponseEntity.ok(ApiResult.ok(body));
			case "OBJECT_WRAPPER":
				if (body instanceof Map<?, ?> rawMap) {
					@SuppressWarnings("unchecked")
					Map<String, Object> bodyMap = (Map<String, Object>) rawMap;
					bodyMap.putIfAbsent("countDataShopList", Collections.emptyList());
					bodyMap.put("datapass_block", datapassBlock ? 1 : 0);
					if (datapassBlock) {
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> rows =
								(List<Map<String, Object>>) bodyMap.get("list");
						if (rows != null) {
							for (Map<String, Object> row : rows) {
								if (row == null) {
									continue;
								}
								Object mob = row.get("mobile");
								if (mob != null) {
									String plain = String.valueOf(mob).trim();
									if (!plain.isEmpty()) {
										row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
									}
								}
								Object pm = row.get("pmobile");
								if (pm != null) {
									String pplain = String.valueOf(pm).trim();
									if (!pplain.isEmpty()) {
										row.put("pmobile", DataMasking.maskMobile(String.valueOf(pm)));
									}
								}
								Object un = row.get("username");
								if (un != null) {
									row.put(
											"username",
											DataMasking.maskTruenameIfBlocked(String.valueOf(un), 1));
								}
							}
						}
					}
				}
				return ResponseEntity.ok(ApiResult.ok(body));
			default:
				throw new IllegalStateException("unexpected responseShape: " + shape);
		}
	}

	@DataPass
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "popularize.promoter.export")
	@GetMapping(value = "/popularize/promoter/export", name = "导出推广员业绩")
	public ResponseEntity<Map<String, Object>> exportPromoterList(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "x-datapass-block", required = false) String datapassBlockParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long operatorId = readOperatorIdFromOperatorJwt(request);

		String resolvedMobile = mobile;
		if (!StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim())) {
			if (body != null && body.get("mobile") != null) {
				resolvedMobile = String.valueOf(body.get("mobile")).trim();
			} else {
				resolvedMobile = null;
			}
		} else {
			resolvedMobile = resolvedMobile.trim();
		}

		String resolvedUsername = username;
		if (!StringUtils.hasText(resolvedUsername == null ? "" : resolvedUsername.trim())) {
			if (body != null && body.get("username") != null) {
				resolvedUsername = String.valueOf(body.get("username")).trim();
			} else {
				resolvedUsername = null;
			}
		} else {
			resolvedUsername = resolvedUsername.trim();
		}

		Object datapassAttr = request.getAttribute("x-datapass-block");
		if (datapassAttr == null) {
			datapassAttr = datapassBlockParam;
		}
		boolean datapassBlock = truthyDatapassBlock(datapassAttr);

		PromoterExportJobContext ctx =
				new PromoterExportJobContext(companyId, operatorId, resolvedMobile, resolvedUsername, datapassBlock);
		promoterExportOrchestratorService.exportPromoterList(ctx);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	@DataPass
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "popularize.promoter.exportPopularizeOrder")
	@GetMapping(value = "/popularize/promoter/exportPopularizeOrder", name = "入队导出推广员订单佣金CSV")
	public ResponseEntity<Map<String, Object>> exportPopularizeOrder(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(value = "date_start", required = false) String dateStartParam,
			@RequestParam(value = "date_end", required = false) String dateEndParam,
			@RequestParam(value = "x-datapass-block", required = false) String datapassBlockParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long operatorId = readOperatorIdFromOperatorJwt(request);

		String resolvedMobile = mobile;
		if (!StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim())) {
			if (body != null && body.get("mobile") != null) {
				resolvedMobile = String.valueOf(body.get("mobile")).trim();
			} else {
				resolvedMobile = null;
			}
		} else {
			resolvedMobile = resolvedMobile.trim();
		}

		String resolvedUsername = username;
		if (!StringUtils.hasText(resolvedUsername == null ? "" : resolvedUsername.trim())) {
			if (body != null && body.get("username") != null) {
				resolvedUsername = String.valueOf(body.get("username")).trim();
			} else {
				resolvedUsername = null;
			}
		} else {
			resolvedUsername = resolvedUsername.trim();
		}

		String resolvedDistributorIdRaw = distributorIdParam;
		if (!StringUtils.hasText(resolvedDistributorIdRaw == null ? "" : resolvedDistributorIdRaw.trim())) {
			if (body != null && body.get("distributor_id") != null) {
				String fromBody = String.valueOf(body.get("distributor_id")).trim();
				resolvedDistributorIdRaw = StringUtils.hasText(fromBody) ? fromBody : null;
			} else {
				resolvedDistributorIdRaw = null;
			}
		} else {
			resolvedDistributorIdRaw = resolvedDistributorIdRaw.trim();
		}

		String resolvedDateStart = dateStartParam;
		if (!StringUtils.hasText(resolvedDateStart == null ? "" : resolvedDateStart.trim())) {
			if (body != null && body.get("date_start") != null) {
				resolvedDateStart = String.valueOf(body.get("date_start")).trim();
			} else {
				resolvedDateStart = null;
			}
		} else {
			resolvedDateStart = resolvedDateStart.trim();
		}

		String resolvedDateEnd = dateEndParam;
		if (!StringUtils.hasText(resolvedDateEnd == null ? "" : resolvedDateEnd.trim())) {
			if (body != null && body.get("date_end") != null) {
				resolvedDateEnd = String.valueOf(body.get("date_end")).trim();
			} else {
				resolvedDateEnd = null;
			}
		} else {
			resolvedDateEnd = resolvedDateEnd.trim();
		}

		Object datapassAttr = request.getAttribute("x-datapass-block");
		if (datapassAttr == null) {
			datapassAttr = datapassBlockParam;
		}
		boolean datapassBlock = truthyDatapassBlock(datapassAttr);

		List<Long> dIdsForContext = null;
		String operatorType = readOperatorTypeFromOperatorJwt(request);
		if ("merchant".equalsIgnoreCase(operatorType.trim())) {
			long merchantIdForShops = readMerchantIdFromOperatorJwt(request);
			List<Long> fetched =
					merchantScopeDistributorIdsReadService.listDistributorIdsForMerchantShops(
							companyId, merchantIdForShops);
			if (!fetched.isEmpty()) {
				dIdsForContext = fetched;
			}
		}

		String dateStart = resolvedDateStart == null ? "" : resolvedDateStart.trim();
		String dateEnd = resolvedDateEnd == null ? "" : resolvedDateEnd.trim();
		if (!StringUtils.hasText(dateStart) || !StringUtils.hasText(dateEnd)) {
			throw new BadRequestException("请选择【下载日期】开始结束时间");
		}

		long merchantIdJwt = readMerchantIdFromOperatorJwt(request);
		log.info(
				"exportPopularizeOrder companyId={} hasMobile={} hasUsername={} hasDistributorId={} hasDateRange={} operatorType={} merchantId={} dIdsCount={}",
				companyId,
				StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim()),
				StringUtils.hasText(resolvedUsername == null ? "" : resolvedUsername.trim()),
				StringUtils.hasText(resolvedDistributorIdRaw == null ? "" : resolvedDistributorIdRaw.trim()),
				true,
				operatorType,
				merchantIdJwt,
				dIdsForContext == null ? 0 : dIdsForContext.size());

		PopularizeOrderExportJobContext ctx =
				new PopularizeOrderExportJobContext(
						companyId,
						operatorId,
						merchantIdJwt,
						operatorType,
						resolvedMobile,
						resolvedUsername,
						resolvedDistributorIdRaw,
						dIdsForContext,
						dateStart,
						dateEnd,
						datapassBlock);
		promoterExportOrchestratorService.exportPopularizeOrder(ctx);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	@DataPass
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "popularize.promoter.exportPopularizeStatic")
	@GetMapping(value = "/popularize/promoter/exportPopularizeStatic", name = "导出推广员业绩")
	public ResponseEntity<Map<String, Object>> exportPopularizeStatic(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(value = "date_start", required = false) String dateStartParam,
			@RequestParam(value = "date_end", required = false) String dateEndParam,
			@RequestParam(value = "x-datapass-block", required = false) String datapassBlockParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long operatorId = readOperatorIdFromOperatorJwt(request);

		String resolvedMobile = mobile;
		if (!StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim())) {
			if (body != null && body.get("mobile") != null) {
				resolvedMobile = String.valueOf(body.get("mobile")).trim();
			} else {
				resolvedMobile = null;
			}
		} else {
			resolvedMobile = resolvedMobile.trim();
		}

		String resolvedUsername = username;
		if (!StringUtils.hasText(resolvedUsername == null ? "" : resolvedUsername.trim())) {
			if (body != null && body.get("username") != null) {
				resolvedUsername = String.valueOf(body.get("username")).trim();
			} else {
				resolvedUsername = null;
			}
		} else {
			resolvedUsername = resolvedUsername.trim();
		}

		String resolvedDistributorIdRaw = distributorIdParam;
		if (!StringUtils.hasText(resolvedDistributorIdRaw == null ? "" : resolvedDistributorIdRaw.trim())) {
			if (body != null && body.get("distributor_id") != null) {
				String fromBody = String.valueOf(body.get("distributor_id")).trim();
				resolvedDistributorIdRaw = StringUtils.hasText(fromBody) ? fromBody : null;
			} else {
				resolvedDistributorIdRaw = null;
			}
		} else {
			resolvedDistributorIdRaw = resolvedDistributorIdRaw.trim();
		}

		String resolvedDateStart = dateStartParam;
		if (!StringUtils.hasText(resolvedDateStart == null ? "" : resolvedDateStart.trim())) {
			if (body != null && body.get("date_start") != null) {
				resolvedDateStart = String.valueOf(body.get("date_start")).trim();
			} else {
				resolvedDateStart = null;
			}
		} else {
			resolvedDateStart = resolvedDateStart.trim();
		}

		String resolvedDateEnd = dateEndParam;
		if (!StringUtils.hasText(resolvedDateEnd == null ? "" : resolvedDateEnd.trim())) {
			if (body != null && body.get("date_end") != null) {
				resolvedDateEnd = String.valueOf(body.get("date_end")).trim();
			} else {
				resolvedDateEnd = null;
			}
		} else {
			resolvedDateEnd = resolvedDateEnd.trim();
		}

		Object datapassAttr = request.getAttribute("x-datapass-block");
		if (datapassAttr == null) {
			datapassAttr = datapassBlockParam;
		}
		boolean datapassBlock = truthyDatapassBlock(datapassAttr);

		List<Long> dIdsForContext = null;
		String operatorType = readOperatorTypeFromOperatorJwt(request);
		if ("merchant".equalsIgnoreCase(operatorType.trim())) {
			long merchantIdForShops = readMerchantIdFromOperatorJwt(request);
			List<Long> fetched =
					merchantScopeDistributorIdsReadService.listDistributorIdsForMerchantShops(
							companyId, merchantIdForShops);
			if (!fetched.isEmpty()) {
				dIdsForContext = fetched;
			}
		}

		String dateStart = resolvedDateStart == null ? "" : resolvedDateStart.trim();
		String dateEnd = resolvedDateEnd == null ? "" : resolvedDateEnd.trim();
		if (!StringUtils.hasText(dateStart) || !StringUtils.hasText(dateEnd)) {
			throw new BadRequestException("请选择【下载日期】开始结束时间");
		}

		long merchantIdJwt = readMerchantIdFromOperatorJwt(request);
		log.info(
				"exportPopularizeStatic companyId={} hasMobile={} hasUsername={} hasDistributorId={} hasDateRange={} operatorType={} merchantId={} dIdsCount={}",
				companyId,
				StringUtils.hasText(resolvedMobile == null ? "" : resolvedMobile.trim()),
				StringUtils.hasText(resolvedUsername == null ? "" : resolvedUsername.trim()),
				StringUtils.hasText(resolvedDistributorIdRaw == null ? "" : resolvedDistributorIdRaw.trim()),
				true,
				operatorType,
				merchantIdJwt,
				dIdsForContext == null ? 0 : dIdsForContext.size());

		PopularizeStaticExportJobContext ctx =
				new PopularizeStaticExportJobContext(
						companyId,
						operatorId,
						merchantIdJwt,
						operatorType,
						resolvedMobile,
						resolvedUsername,
						resolvedDistributorIdRaw,
						dIdsForContext,
						dateStart,
						dateEnd,
						datapassBlock);
		promoterExportOrchestratorService.exportPopularizeStatic(ctx);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "popularize.promoter.grade.put")
	@PutMapping(value = "/popularize/promoter/grade", name = "推广员等级调整")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePromoterGrade(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@RequestParam(value = "grade_level", required = false) String gradeLevelParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);

		String userIdResolved = userIdParam;
		if (!StringUtils.hasText(userIdResolved == null ? "" : userIdResolved.trim())) {
			if (body != null && body.get("user_id") != null) {
				userIdResolved = String.valueOf(body.get("user_id")).trim();
			} else {
				userIdResolved = null;
			}
		}

		String gradeLevelResolved = gradeLevelParam;
		if (!StringUtils.hasText(gradeLevelResolved == null ? "" : gradeLevelResolved.trim())) {
			if (body != null && body.get("grade_level") != null) {
				gradeLevelResolved = String.valueOf(body.get("grade_level")).trim();
			} else {
				gradeLevelResolved = null;
			}
		}

		promoterGradeLevelUpdateService.updatePromoterGrade(companyId, userIdResolved, gradeLevelResolved);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "popularize.promoter.disabled")
	@PutMapping(value = "/popularize/promoter/disabled", name = "禁用/激活推广员")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePromoterDisabled(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@RequestParam(value = "active", required = false) String activeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);

		String userIdResolved = userIdParam;
		if (!StringUtils.hasText(userIdResolved == null ? "" : userIdResolved.trim())) {
			if (body != null && body.get("user_id") != null) {
				userIdResolved = String.valueOf(body.get("user_id")).trim();
			} else {
				userIdResolved = null;
			}
		}

		String activeResolved = activeParam;
		if (!StringUtils.hasText(activeResolved == null ? "" : activeResolved.trim())) {
			if (body != null && body.get("active") != null) {
				activeResolved = String.valueOf(body.get("active")).trim();
			} else {
				activeResolved = "true";
			}
		}

		promoterDisabledService.updatePromoterDisabled(companyId, userIdResolved, activeResolved);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "popularize.promoter.remove")
	@PutMapping(value = "/popularize/promoter/remove", name = "调整推广员上下级关系")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePromoterRemove(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@RequestParam(value = "new_user_id", required = false) String newUserIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);

		String userIdResolved = userIdParam;
		if (!StringUtils.hasText(userIdResolved == null ? "" : userIdResolved.trim())) {
			if (body != null && body.get("user_id") != null) {
				userIdResolved = String.valueOf(body.get("user_id")).trim();
			} else {
				userIdResolved = null;
			}
		} else {
			userIdResolved = userIdResolved.trim();
		}

		String newUserIdMerged = newUserIdParam;
		if (!StringUtils.hasText(newUserIdParam == null ? "" : newUserIdParam.trim())) {
			if (body != null && body.get("new_user_id") != null) {
				newUserIdMerged = String.valueOf(body.get("new_user_id")).trim();
			} else {
				newUserIdMerged = null;
			}
		} else {
			newUserIdMerged = newUserIdParam.trim();
		}

		if (!StringUtils.hasText(userIdResolved == null ? "" : userIdResolved.trim())) {
			throw new BadRequestException("参数错误");
		}
		long userId = parseRequiredUserIdForPromoterRemove(userIdResolved.trim());
		long newUserId = parseOptionalNewUserIdForRemove(newUserIdMerged == null ? "" : newUserIdMerged);

		promoterRelRemoveService.relRemove(companyId, userId, newUserId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long parseRequiredUserIdForPromoterRemove(String trimmed) {
		try {
			long v = Long.parseLong(trimmed);
			if (v <= 0L) {
				throw new BadRequestException("参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("当前不是推广员-");
		}
	}

	private static long parseOptionalNewUserIdForRemove(String merged) {
		String t = merged == null ? "" : merged.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0L) {
				throw new BadRequestException("参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	@Activated(routeAlias = "popularize.promoter.shop.update")
	@PutMapping(value = "/popularize/promoter/shop", name = "对推广员的店铺状态进行更新")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePromoterShop(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@RequestParam(value = "status", required = false) String statusParam,
			@RequestParam(value = "reason", required = false) String reasonParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);

		String userIdResolved = userIdParam;
		if (!StringUtils.hasText(userIdResolved == null ? "" : userIdResolved.trim())) {
			if (body != null && body.get("user_id") != null) {
				userIdResolved = String.valueOf(body.get("user_id")).trim();
			} else {
				userIdResolved = null;
			}
		}

		String statusResolved = statusParam;
		if (!StringUtils.hasText(statusResolved == null ? "" : statusResolved.trim())) {
			if (body != null && body.get("status") != null) {
				statusResolved = String.valueOf(body.get("status")).trim();
			} else {
				statusResolved = "0";
			}
		}

		String reasonResolved = null;
		if (StringUtils.hasText(reasonParam == null ? "" : reasonParam.trim())) {
			reasonResolved = reasonParam.trim();
		} else if (body != null && body.get("reason") != null) {
			reasonResolved = String.valueOf(body.get("reason")).trim();
		}

		promoterShopStatusUpdateService.updatePromoterShop(
				companyId, userIdResolved, statusResolved, reasonResolved);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "popularize.promoter.firstidentitylist")
	@GetMapping(value = "/popularize/promoter/firstidentitylist", name = "获取推广员一级身份列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getFirstIdentityPromoter(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		int[] pagination = parseFirstIdentityListPaginationOrNull(pageRaw, pageSizeRaw);
		if (pagination == null) {
			return ResponseEntity.ok(
					ApiResult.ok(Map.of("total_count", 0L, "list", Collections.emptyList())));
		}
		Map<String, Object> data =
				promoterFirstIdentityListService.getFirstIdentityPromoter(
						companyId, pagination[0], pagination[1]);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "popularize.promoter.member.remove")
	@PutMapping(value = "/popularize/promoter/member/remove", name = "调整会员上级关系")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMemberPromoterRemove(
			HttpServletRequest request,
			@RequestParam(value = "user_id", required = false) String userIdParam,
			@RequestParam(value = "new_user_id", required = false) String newUserIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String userResolved = userIdParam;
		if (!StringUtils.hasText(userResolved == null ? "" : userResolved.trim())) {
			if (body != null && body.get("user_id") != null) {
				userResolved = String.valueOf(body.get("user_id")).trim();
			} else {
				userResolved = null;
			}
		} else {
			userResolved = userResolved.trim();
		}

		String newUserResolved = newUserIdParam;
		if (!StringUtils.hasText(newUserResolved == null ? "" : newUserResolved.trim())) {
			if (body != null && body.get("new_user_id") != null) {
				newUserResolved = String.valueOf(body.get("new_user_id")).trim();
			} else {
				newUserResolved = null;
			}
		} else {
			newUserResolved = newUserResolved.trim();
		}

		if (!StringUtils.hasText(userResolved == null ? "" : userResolved.trim())) {
			throw new BadRequestException("会员ID错误");
		}
		long userId = parseStrictPositiveLongForMemberRemove(userResolved.trim(), "会员ID错误");
		if (!StringUtils.hasText(newUserResolved == null ? "" : newUserResolved.trim())) {
			throw new BadRequestException("推广员ID错误");
		}
		long newUserId = parseStrictPositiveLongForMemberRemove(newUserResolved.trim(), "推广员ID错误");

		long companyId = readCompanyIdFromOperatorJwt(request);
		promoterMemberRelRemoveService.updateMemberPromoterRemove(companyId, userId, newUserId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long parseStrictPositiveLongForMemberRemove(String trimmed, String errorMessage) {
		try {
			long v = Long.parseLong(trimmed);
			if (v < 1L) {
				throw new BadRequestException(errorMessage);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(errorMessage);
		}
	}
}
