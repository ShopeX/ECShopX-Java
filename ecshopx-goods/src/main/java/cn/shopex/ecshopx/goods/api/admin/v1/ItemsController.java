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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsBatchApproveStatusService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsByCouponService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsCreateParamValidator;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsCreateRequestGate;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsDetailFacadeService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import cn.shopex.ecshopx.goods.service.items.ItemsAuditUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsCreateOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemsUpdateOrchestrator;
import cn.shopex.ecshopx.goods.service.ItemsAttributesCreateService;
import cn.shopex.ecshopx.goods.service.ItemsCommissionSaveService;
import cn.shopex.ecshopx.goods.service.items.ItemsDeleteOrchestrator;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDeleteService;
import cn.shopex.ecshopx.goods.service.items.ItemsIsGiftBatchUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsRebateConfUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsRelCatsWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsSortUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemWarningStoreWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsBatchStoreRequestParser;
import cn.shopex.ecshopx.goods.service.items.ItemsPriceStoreStatusWriteService;
import cn.shopex.ecshopx.goods.service.items.DistributionGoodsWxaCodeStreamService;
import cn.shopex.ecshopx.goods.service.items.ItemsBatchUpdateStoreOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemStoreBatchRow;
import cn.shopex.ecshopx.goods.service.items.ItemsTemplateWriteService;
import cn.shopex.ecshopx.goods.service.keywords.GoodsKeywordsQueryService;
import cn.shopex.ecshopx.goods.service.keywords.GoodsKeywordsWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("goodsAdminV1Items")
@RequestMapping("/api/v1/goods")
public class ItemsController {

	private final ItemsCreateOrchestrator itemsCreateOrchestrator;
	private final ItemsUpdateOrchestrator itemsUpdateOrchestrator;
	private final GoodsItemsCreateRequestGate goodsItemsCreateRequestGate;
	private final ItemsAuditUpdateService itemsAuditUpdateService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final GoodsItemsListFacadeService goodsItemsListFacadeService;
	private final GoodsItemsDetailFacadeService goodsItemsDetailFacadeService;
	private final CompanysActivationService companysActivationService;
	private final ItemsDeleteOrchestrator itemsDeleteOrchestrator;
	private final PlatformItemsDeleteService platformItemsDeleteService;
	private final ItemsAttributesCreateService itemsAttributesCreateService;
	private final GoodsKeywordsWriteService goodsKeywordsWriteService;
	private final GoodsKeywordsQueryService goodsKeywordsQueryService;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemsRebateConfUpdateService itemsRebateConfUpdateService;
	private final ItemsRelCatsWriteService itemsRelCatsWriteService;
	private final ItemsSortUpdateService itemsSortUpdateService;
	private final ItemsTemplateWriteService itemsTemplateWriteService;
	private final ItemsCommissionSaveService itemsCommissionSaveService;
	private final ItemWarningStoreWriteService itemWarningStoreWriteService;
	private final ItemsIsGiftBatchUpdateService itemsIsGiftBatchUpdateService;
	private final GoodsItemsBatchApproveStatusService goodsItemsBatchApproveStatusService;
	private final ItemsBatchStoreRequestParser itemsBatchStoreRequestParser;
	private final ItemsBatchUpdateStoreOrchestrator itemsBatchUpdateStoreOrchestrator;
	private final ItemsPriceStoreStatusWriteService itemsPriceStoreStatusWriteService;
	private final DistributionGoodsWxaCodeStreamService distributionGoodsWxaCodeStreamService;
	private final GoodsItemsByCouponService goodsItemsByCouponService;
	private final LangueProperties langueProperties;

	private static final String KEYWORDS_VALIDATION_MSG = "参数必填.";

	public ItemsController(
			ItemsCreateOrchestrator itemsCreateOrchestrator,
			ItemsUpdateOrchestrator itemsUpdateOrchestrator,
			GoodsItemsCreateRequestGate goodsItemsCreateRequestGate,
			ItemsAuditUpdateService itemsAuditUpdateService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			GoodsItemsListFacadeService goodsItemsListFacadeService,
			GoodsItemsDetailFacadeService goodsItemsDetailFacadeService,
			CompanysActivationService companysActivationService,
			ItemsDeleteOrchestrator itemsDeleteOrchestrator,
			PlatformItemsDeleteService platformItemsDeleteService,
			ItemsAttributesCreateService itemsAttributesCreateService,
			GoodsKeywordsWriteService goodsKeywordsWriteService,
			GoodsKeywordsQueryService goodsKeywordsQueryService,
			ItemsMedicineService itemsMedicineService,
			ItemsRebateConfUpdateService itemsRebateConfUpdateService,
			ItemsRelCatsWriteService itemsRelCatsWriteService,
			ItemsSortUpdateService itemsSortUpdateService,
			ItemsTemplateWriteService itemsTemplateWriteService,
			ItemsCommissionSaveService itemsCommissionSaveService,
			ItemWarningStoreWriteService itemWarningStoreWriteService,
			ItemsIsGiftBatchUpdateService itemsIsGiftBatchUpdateService,
			GoodsItemsBatchApproveStatusService goodsItemsBatchApproveStatusService,
			ItemsBatchStoreRequestParser itemsBatchStoreRequestParser,
			ItemsBatchUpdateStoreOrchestrator itemsBatchUpdateStoreOrchestrator,
			ItemsPriceStoreStatusWriteService itemsPriceStoreStatusWriteService,
			DistributionGoodsWxaCodeStreamService distributionGoodsWxaCodeStreamService,
			GoodsItemsByCouponService goodsItemsByCouponService,
			LangueProperties langueProperties) {
		this.itemsCreateOrchestrator = itemsCreateOrchestrator;
		this.itemsUpdateOrchestrator = itemsUpdateOrchestrator;
		this.goodsItemsCreateRequestGate = goodsItemsCreateRequestGate;
		this.itemsAuditUpdateService = itemsAuditUpdateService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.goodsItemsListFacadeService = goodsItemsListFacadeService;
		this.goodsItemsDetailFacadeService = goodsItemsDetailFacadeService;
		this.companysActivationService = companysActivationService;
		this.itemsDeleteOrchestrator = itemsDeleteOrchestrator;
		this.platformItemsDeleteService = platformItemsDeleteService;
		this.itemsAttributesCreateService = itemsAttributesCreateService;
		this.goodsKeywordsWriteService = goodsKeywordsWriteService;
		this.goodsKeywordsQueryService = goodsKeywordsQueryService;
		this.itemsMedicineService = itemsMedicineService;
		this.itemsRebateConfUpdateService = itemsRebateConfUpdateService;
		this.itemsRelCatsWriteService = itemsRelCatsWriteService;
		this.itemsSortUpdateService = itemsSortUpdateService;
		this.itemsTemplateWriteService = itemsTemplateWriteService;
		this.itemsCommissionSaveService = itemsCommissionSaveService;
		this.itemWarningStoreWriteService = itemWarningStoreWriteService;
		this.itemsIsGiftBatchUpdateService = itemsIsGiftBatchUpdateService;
		this.goodsItemsBatchApproveStatusService = goodsItemsBatchApproveStatusService;
		this.itemsBatchStoreRequestParser = itemsBatchStoreRequestParser;
		this.itemsBatchUpdateStoreOrchestrator = itemsBatchUpdateStoreOrchestrator;
		this.itemsPriceStoreStatusWriteService = itemsPriceStoreStatusWriteService;
		this.distributionGoodsWxaCodeStreamService = distributionGoodsWxaCodeStreamService;
		this.goodsItemsByCouponService = goodsItemsByCouponService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.items.set_commission_ratio")
	@PostMapping(value = "/set_commission_ratio", name = "佣金费率")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setCommissionRatio(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyId = readRequiredLong(ud, "company_id");
		long goodsId = parseGoodsIdForSetCommissionRatio(merged.get("goods_id"));
		BigDecimal commissionRatio = parseCommissionRatioForSetCommissionRatio(merged.get("commission_ratio"));
		if (commissionRatio.compareTo(BigDecimal.ZERO) < 0
				|| commissionRatio.compareTo(new BigDecimal("100")) > 0) {
			throw new BadRequestException("佣金比例必须大于0小于100");
		}
		BigDecimal scaled = commissionRatio.setScale(2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
		itemsCommissionSaveService.saveSpuCommissionRatioFromPercentScaled(companyId, goodsId, scaled);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.audit")
	@PutMapping(value = "/audit/items", name = "商品审核")
	public ResponseEntity<ApiResult<Map<String, Object>>> auditItems(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		goodsItemsCreateRequestGate.validateBeforeAudit(request, merged);
		long companyId = readRequiredLong(ud, "company_id");
		itemsAuditUpdateService.auditItems(companyId, merged);

		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/goods/audit/items");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "商品审核");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.create")
	@PostMapping(value = "/items", name = "添加商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> createItems(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		if (!merged.containsKey("origincountry_id")) {
			merged.put("origincountry_id", 0);
		}
		if (!merged.containsKey("taxstrategy_id")) {
			merged.put("taxstrategy_id", 0);
		}
		if (!merged.containsKey("taxation_num")) {
			merged.put("taxation_num", 0);
		}
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		goodsItemsCreateRequestGate.validateBeforeCreate(request, merged);
		GoodsItemsCreateParamValidator.validate(merged);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		merged.put("company_id", companyId);
		merged.put("merchant_id", ud.get("merchant_id"));
		merged.put("supplier_id", 0L);
		merged.remove("item_id");
		String op = merged.get("operator_type") != null ? merged.get("operator_type").toString() : "";
		if ("supplier".equals(op)) {
			merged.put("supplier_id", operatorId);
			Object audit = merged.get("audit_status");
			if (audit == null || !"processing".equals(audit.toString())) {
				merged.put("audit_status", "submitting");
			}
		} else {
			merged.put("audit_status", "approved");
		}

		itemsCreateOrchestrator.createItems(merged);

		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/goods/items");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static long parseGoodsIdForSetCommissionRatio(Object g) {
		if (g == null) {
			return 0L;
		}
		if (g instanceof Number n) {
			return n.longValue();
		}
		String gs = g.toString().trim();
		if (!StringUtils.hasText(gs)) {
			return 0L;
		}
		try {
			return Long.parseLong(gs);
		} catch (NumberFormatException e) {
			throw new BadRequestException("goods_id 无效");
		}
	}

	private static BigDecimal parseCommissionRatioForSetCommissionRatio(Object cr) {
		if (cr == null) {
			return BigDecimal.ZERO;
		}
		if (cr instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		String s = cr.toString().trim();
		if (!StringUtils.hasText(s)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("commission_ratio 格式错误");
		}
	}

	private static long parseDistributorIdForWarningStore(Map<String, Object> merged) {
		if (!merged.containsKey("distributor_id")) {
			return 0L;
		}
		Object v = merged.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 格式无效");
			}
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		throw new BadRequestException("distributor_id 格式无效");
	}

	private static int parseRequiredStoreForSetItemWarningStore(Object storeRaw) {
		String msg = "预警库存最少为1";
		if (storeRaw == null) {
			throw new BadRequestException(msg);
		}
		if (storeRaw instanceof BigDecimal bd) {
			long sv;
			try {
				sv = bd.longValueExact();
			} catch (ArithmeticException e) {
				throw new BadRequestException(msg);
			}
			if (sv < 1L || sv > Integer.MAX_VALUE) {
				throw new BadRequestException(msg);
			}
			return (int) sv;
		}
		if (storeRaw instanceof Number n) {
			long sv = n.longValue();
			if (sv < 1L || sv > (long) Integer.MAX_VALUE) {
				throw new BadRequestException(msg);
			}
			if (n instanceof Double d && d != sv) {
				throw new BadRequestException(msg);
			}
			if (n instanceof Float f && f != sv) {
				throw new BadRequestException(msg);
			}
			return (int) sv;
		}
		if (storeRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(msg);
			}
			long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(msg);
			}
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException(msg);
			}
			return (int) v;
		}
		throw new BadRequestException(msg);
	}

	private static int parseRequiredSort(Object sortRaw) {
		if (sortRaw == null) {
			throw new BadRequestException("请填写排序编号");
		}
		if (sortRaw instanceof Number n) {
			long sv = n.longValue();
			if (sv != (long) (int) sv || sv < 0) {
				throw new BadRequestException("请填写排序编号");
			}
			if (n instanceof Double d && d != sv) {
				throw new BadRequestException("请填写排序编号");
			}
			if (n instanceof Float f && f != sv) {
				throw new BadRequestException("请填写排序编号");
			}
			return (int) sv;
		}
		if (sortRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("请填写排序编号");
			}
			long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("请填写排序编号");
			}
			if (v < 0 || v > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请填写排序编号");
			}
			return (int) v;
		}
		throw new BadRequestException("请填写排序编号");
	}

	private static List<Long> parseRequiredGoodsIdList(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择要同步的商品");
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择要同步的商品");
			}
			String[] parts = s.split(",");
			List<Long> out = new ArrayList<>();
			for (String part : parts) {
				String t = part.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException e) {
					throw new BadRequestException("goods_id 格式错误");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择要同步的商品");
			}
			return out;
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("请选择要同步的商品");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : col) {
				try {
					if (el instanceof Number num) {
						out.add(num.longValue());
					} else if (el != null && StringUtils.hasText(el.toString().trim())) {
						out.add(Long.parseLong(el.toString().trim()));
					} else {
						throw new BadRequestException("goods_id 格式错误");
					}
				} catch (NumberFormatException e) {
					throw new BadRequestException("goods_id 格式错误");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择要同步的商品");
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new BadRequestException("请选择要同步的商品");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : arr) {
				try {
					if (el instanceof Number num) {
						out.add(num.longValue());
					} else if (el != null && StringUtils.hasText(el.toString().trim())) {
						out.add(Long.parseLong(el.toString().trim()));
					} else {
						throw new BadRequestException("goods_id 格式错误");
					}
				} catch (NumberFormatException e) {
					throw new BadRequestException("goods_id 格式错误");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择要同步的商品");
			}
			return out;
		}
		throw new BadRequestException("goods_id 格式错误");
	}

	private static List<Long> parseRequiredItemOrCategoryIdsForSetItemsCategory(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择商品和分类的数据");
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			try {
				return List.of(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : col) {
				if (el instanceof Number num) {
					out.add(num.longValue());
				} else if (el instanceof String es) {
					if (!StringUtils.hasText(es.trim())) {
						throw new BadRequestException("请选择商品和分类的数据");
					}
					try {
						out.add(Long.parseLong(es.trim()));
					} catch (NumberFormatException e) {
						throw new BadRequestException("请选择商品和分类的数据");
					}
				} else {
					throw new BadRequestException("请选择商品和分类的数据");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			return out;
		}
		throw new BadRequestException("请选择商品和分类的数据");
	}

	private static int parseRequiredTemplatesIdForSetItemsTemplate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择运费模板");
		}
		if (raw instanceof Number n) {
			long l = n.longValue();
			if (l < 1L || l > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请选择运费模板");
			}
			return (int) l;
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择运费模板");
			}
			long l;
			try {
				l = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择运费模板");
			}
			if (l < 1L || l > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请选择运费模板");
			}
			return (int) l;
		}
		throw new BadRequestException("请选择运费模板");
	}

	private static List<Long> parseRequiredItemIdsForSetItemsTemplate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择商品");
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择商品");
			}
			try {
				return List.of(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择商品");
			}
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("请选择商品");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : col) {
				if (el instanceof Number num) {
					out.add(num.longValue());
				} else if (el instanceof String es) {
					if (!StringUtils.hasText(es.trim())) {
						throw new BadRequestException("请选择商品");
					}
					try {
						out.add(Long.parseLong(es.trim()));
					} catch (NumberFormatException e) {
						throw new BadRequestException("请选择商品");
					}
				} else {
					throw new BadRequestException("请选择商品");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择商品");
			}
			return out;
		}
		throw new BadRequestException("请选择商品");
	}

	private static BadRequestException keywordsValidationFailed(String field, String validationRuleCode) {
		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		errors.put(field, List.of(validationRuleCode));
		return new BadRequestException(KEYWORDS_VALIDATION_MSG, errors);
	}

	private static void validateKeywordsMerged(Map<String, Object> merged) {
		Object contentObj = merged.get("content");
		if (contentObj == null) {
			throw keywordsValidationFailed("content", "validation.required");
		}
		if (!(contentObj instanceof String)) {
			throw keywordsValidationFailed("content", "validation.string");
		}
		if (!StringUtils.hasText(((String) contentObj).trim())) {
			throw keywordsValidationFailed("content", "validation.required");
		}
		parseOptionalDistributorId(merged);
		parseOptionalKeywordId(merged);
	}

	private static long parseOptionalDistributorId(Map<String, Object> merged) {
		if (!merged.containsKey("distributor_id")) {
			return 0L;
		}
		Object v = merged.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			return 0L;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw keywordsValidationFailed("distributor_id", "validation.integer");
		}
	}

	private static Long parseOptionalKeywordId(Map<String, Object> merged) {
		if (!merged.containsKey("id")) {
			return null;
		}
		Object v = merged.get("id");
		if (v == null) {
			return null;
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			return null;
		}
		long parsed;
		try {
			if (v instanceof Number n) {
				parsed = n.longValue();
			} else {
				parsed = Long.parseLong(v.toString().trim());
			}
		} catch (NumberFormatException e) {
			throw keywordsValidationFailed("id", "validation.integer");
		}
		if (parsed < 1L) {
			throw keywordsValidationFailed("id", "validation.min.numeric");
		}
		return parsed;
	}

	private static long parseKeywordsDetailQueryId(String rawId) {
		if (rawId == null || !StringUtils.hasText(rawId.trim())) {
			throw keywordsValidationFailed("id", "validation.required");
		}
		String t = rawId.trim();
		long parsed;
		try {
			parsed = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw keywordsValidationFailed("id", "validation.integer");
		}
		if (parsed < 1L) {
			throw keywordsValidationFailed("id", "validation.min.numeric");
		}
		return parsed;
	}

	/**
	 * 路径 id 按前导数字解析；无数字或空视为 0，删除 0 行仍成功（幂等）。
	 */
	private static long parseKeywordDeletePathId(String rawId) {
		return LeadingNumberParser.parseAsLong(rawId == null ? "" : rawId.trim());
	}

	@Activated(routeAlias = "goods.items.templates_change")
	@PostMapping(value = "/setItemsTemplate", name = "运费模板")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsTemplate(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyId = readRequiredLong(ud, "company_id");
		int templatesId = parseRequiredTemplatesIdForSetItemsTemplate(merged.get("templates_id"));
		List<Long> itemIds = parseRequiredItemIdsForSetItemsTemplate(merged.get("item_id"));
		Object op = ud.get("operator_type");
		boolean supplierOperator = "supplier".equals(op != null ? op.toString() : "");
		itemsTemplateWriteService.setItemsTemplate(companyId, supplierOperator, templatesId, itemIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.category_change")
	@PostMapping(value = "/setItemsCategory", name = "商品分类")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsCategory(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyId = readRequiredLong(ud, "company_id");
		List<Long> itemIds = parseRequiredItemOrCategoryIdsForSetItemsCategory(merged.get("item_id"));
		List<Long> categoryIds = parseRequiredItemOrCategoryIdsForSetItemsCategory(merged.get("category_id"));
		itemsRelCatsWriteService.setItemsCategoryBatch(companyId, itemIds, categoryIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.lists")
	@GetMapping(value = "/items", name = "商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> data = goodsItemsListFacadeService.list(request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.items.onsale.lists")
	@GetMapping(value = "/items/onsale", name = "可售商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOnsaleItemsList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		validateOnsaleDistributorQueryParam(request);
		Object companyObj = ud.get("company_id");
		if (companyObj == null) {
			throw new ResourceException("公司信息缺失");
		}
		long companyId = companyObj instanceof Number n ? n.longValue() : Long.parseLong(companyObj.toString().trim());
		Object sourceObj = ud.get("source");
		String source = sourceObj != null ? sourceObj.toString() : "";
		if (!"salesperson".equals(source)) {
			companysActivationService.assertShopOperatorCompanyActive(companyId);
		}
		Map<String, Object> data = goodsItemsListFacadeService.listOnsaleItems(request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void validateOnsaleDistributorQueryParam(HttpServletRequest request) {
		String did = request.getParameter("distributor_id");
		if (did == null || !StringUtils.hasText(did.trim())) {
			return;
		}
		if ("all_distributor".equalsIgnoreCase(did.trim())) {
			return;
		}
		try {
			for (String part : did.split(",")) {
				if (StringUtils.hasText(part)) {
					Long.parseLong(part.trim());
				}
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式无效");
		}
	}

	@Activated(routeAlias = "goods.sku.lists")
	@GetMapping(value = "/sku", name = "SKU列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSkuList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> data = goodsItemsListFacadeService.listSku(request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Query {@code operate_source} is accepted for client compatibility; code generation uses a single path
	 * regardless of its value.
	 */
	@Activated(routeAlias = "goods.items.distributiongoodswxacode")
	@GetMapping(value = "/distributionGoodsWxaCodeStream", name = "分销二维码")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	@SuppressWarnings("unused")
	public ResponseEntity<byte[]> getDistributionGoodsWxaCodeStream(
			HttpServletRequest request,
			@RequestParam(value = "item_id", required = false) String itemId,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "operate_source", required = false) String operateSource) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		byte[] body = distributionGoodsWxaCodeStreamService.buildJpegBytes(companyId, itemId, distributorId);
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(body);
	}

	@Activated(routeAlias = "goods.items.warning_store")
	@PostMapping(value = "/warning_store", name = "预警库存")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemWarningStore(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyId = readRequiredLong(ud, "company_id");
		int store = parseRequiredStoreForSetItemWarningStore(merged.get("store"));
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		if ("supplier".equals(operatorType.trim())) {
			long operatorId = readRequiredLong(ud, "operator_id");
			itemWarningStoreWriteService.applyWarningStore(companyId, store, operatorType, operatorId, 0L);
		} else {
			long distributorId = parseDistributorIdForWarningStore(merged);
			itemWarningStoreWriteService.applyWarningStore(companyId, store, operatorType, 0L, distributorId);
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.sort")
	@PostMapping(value = "/setItemsSort", name = "商品排序")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsSort(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long itemId = readRequiredLong(merged, "item_id");
		int sort = parseRequiredSort(merged.get("sort"));
		Object osRaw = merged.get("operate_source");
		String operateSource = (osRaw == null) ? "" : osRaw.toString().trim();
		itemsSortUpdateService.setItemsSort(companyId, itemId, sort, operateSource);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.store.upate")
	@PutMapping(value = "/itemstoreupdate", name = "批量库存")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchUpdateItemStore(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyId = readRequiredLong(ud, "company_id");
		Object opRaw = ud.get("operator_type");
		String operatorType = opRaw == null ? "" : opRaw.toString();
		Object osRaw = merged.get("operate_source");
		if (osRaw != null && "supplier".equals(String.valueOf(osRaw).trim())) {
			operatorType = "supplier";
		}
		Object itemsRaw = merged.get("items");
		List<ItemStoreBatchRow> rows = itemsBatchStoreRequestParser.parseAndValidateRows(itemsRaw);
		itemsBatchUpdateStoreOrchestrator.batchUpdate(companyId, operatorType, rows);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.status.upate")
	@PutMapping(value = "/itemstatusupdate", name = "批量状态")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchUpdateItemStatus(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		decodeItemsStringForBatchItemStatus(merged);
		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		String operatorType = opTypeJwt != null ? opTypeJwt.toString() : "";
		Long merchantId = parseMerchantIdFromJwt(ud);
		goodsItemsBatchApproveStatusService.batchUpdate(request, companyId, operatorId, operatorType, merchantId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private void decodeItemsStringForBatchItemStatus(Map<String, Object> merged) {
		Object it = merged.get("items");
		if (it instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				merged.put("items", List.of());
				return;
			}
			try {
				merged.put("items", objectMapper.readValue(s, Object.class));
			} catch (Exception e) {
				throw new BadRequestException("items 格式错误");
			}
		}
	}

	private static Long parseMerchantIdFromJwt(Map<?, ?> ud) {
		Object mid = ud.get("merchant_id");
		if (mid == null) {
			return null;
		}
		if (mid instanceof Number n) {
			return n.longValue();
		}
		String t = mid.toString().trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Activated(routeAlias = "goods.itemsupdate")
	@PutMapping(value = "/itemsupdate", name = "价格库存上下架")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItemsPriceStoreStatus(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		String operatorType = ud.get("operator_type") == null ? "" : ud.get("operator_type").toString().trim();
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		itemsPriceStoreStatusWriteService.updateFromRequest(companyId, operatorType, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.rebateconf")
	@PostMapping(value = "/rebateconf", name = "返利配置")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItemsRebateConf(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object rebateConfRaw = merged.get("rebateConf");
		String rebateTypeStringOrNull = null;
		Object rt = merged.get("rebate_type");
		if (rt != null) {
			String ts = rt.toString().trim();
			if (StringUtils.hasText(ts)) {
				rebateTypeStringOrNull = ts;
			}
		}
		itemsRebateConfUpdateService.updateRebateConf(companyId, rebateTypeStringOrNull, rebateConfRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "getGoodsByCoupon.get")
	@GetMapping(value = "/goodsbycoupon/{coupon_id}", name = "优惠券商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> getGoodsByCoupon(HttpServletRequest request,
			@PathVariable("coupon_id") String couponId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> data = goodsItemsByCouponService.getGoodsByCoupon(companyId, couponId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.Keywords.set")
	@PostMapping(value = "/keywords", name = "设置关键词")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> setKeywords(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		validateKeywordsMerged(merged);
		long companyId = readRequiredLong(ud, "company_id");
		long distributorId = parseOptionalDistributorId(merged);
		Long idOrNull = parseOptionalKeywordId(merged);
		String content = ((String) merged.get("content")).trim();
		Map<String, Object> row = goodsKeywordsWriteService.addKeywords(companyId, idOrNull, distributorId, content);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "goods.Keywords.delete")
	@DeleteMapping(value = "/keywords/{id}", name = "删除关键词")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> delKeywords(HttpServletRequest request,
			@PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long keywordId = parseKeywordDeletePathId(id);
		goodsKeywordsWriteService.deleteByCompanyAndKeywordId(companyId, keywordId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "goods.Keywords.get")
	@GetMapping(value = "/keywords", name = "获取关键词")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> getKeywords(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Long distributorId,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "page", defaultValue = "1") long page,
			@RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Long distributorIdEqOrNull = (distributorId != null && distributorId != 0L) ? distributorId : null;
		Map<String, Object> data = goodsKeywordsQueryService.listKeywords(
				companyId, distributorIdEqOrNull, content, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.Keywords.getByShop")
	@GetMapping(value = "/keywordsDetail", name = "关键词详情")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Object>> getKeyWordsDetail(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		readRequiredLong(ud, "company_id");
		long idVal = parseKeywordsDetailQueryId(id);
		Map<String, Object> row = goodsKeywordsQueryService.getInfoById(idVal);
		if (row == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "goods.isgift.upate")
	@PutMapping(value = "/itemsisgiftupdate", name = "批量赠品")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchUpdateItemIsgift(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object itemIdObj = merged.get("item_id");
		if (itemIdObj == null) {
			throw new BadRequestException("未指定商品");
		}
		String itemIdStr = itemIdObj.toString().trim();
		if (!StringUtils.hasText(itemIdStr)) {
			throw new BadRequestException("未指定商品");
		}
		long goodsId;
		try {
			goodsId = Long.parseLong(itemIdStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品id无效");
		}
		Object st = merged.get("status");
		if (st == null) {
			throw new BadRequestException("状态必填,且必须是 true 或 false");
		}
		String statusString = st.toString().trim();
		if (!StringUtils.hasText(statusString) || (!"true".equals(statusString) && !"false".equals(statusString))) {
			throw new BadRequestException("状态必填,且必须是 true 或 false");
		}
		itemsIsGiftBatchUpdateService.batchUpdateItemGift(companyId, operatorType, goodsId, statusString);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.medicineItems.sync")
	@PostMapping(value = "/medicineItems/sync", name = "同步药品")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> syncMedicine(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		List<Long> goodsIds = parseRequiredGoodsIdList(merged.get("goods_id"));
		itemsMedicineService.syncMedicine(request, companyId, goodsIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.params.create")
	@PostMapping(value = "/items/params", name = "商品参数")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> createItemsParams(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Object attributeIdObj = merged.get("attribute_id");
		if (attributeIdObj == null || !StringUtils.hasText(attributeIdObj.toString().trim())) {
			throw new BadRequestException("必须填写attribute_id");
		}
		Object attributeValueObj = merged.get("attribute_value");
		if (attributeValueObj == null || !StringUtils.hasText(attributeValueObj.toString().trim())) {
			throw new BadRequestException("必须填写attribute_value");
		}

		String countryCode = RequestCountryCode.resolve(langueProperties, merged);

		merged.put("company_id", companyId);
		boolean status = itemsAttributesCreateService.createItemsParamsAttributeValue(merged, countryCode);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@Activated(routeAlias = "goods.items.delete.response")
	@DeleteMapping(value = "/items/{item_id}/response", name = "删除商品响应数据")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteItemsResponseData(HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) jwtRaw;

		long parsedItemId;
		try {
			parsedItemId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"删除商品出错.",
					Map.of("item_id", List.of("validation.integer")))));
		}
		if (parsedItemId < 1) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"删除商品出错.",
					Map.of("item_id", List.of("validation.min.numeric")))));
		}

		long distributorIdQuery = 0L;
		if (StringUtils.hasText(distributorIdParam.trim())) {
			distributorIdQuery = LeadingNumberParser.parseAsLong(distributorIdParam.trim());
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", readRequiredLong(ud, "company_id"));

		platformItemsDeleteService.deletePlatformItems(merged, parsedItemId, distributorIdQuery);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/goods/items/" + parsedItemId + "/response");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("item_id", parsedItemId, "distributor_id", distributorIdQuery)));
		} catch (Exception e) {
			logCtx.put("params", "item_id=" + parsedItemId);
		}
		logCtx.put("operator_name", "删除商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.detail")
	@GetMapping(value = "/items/{item_id}", name = "商品详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetail(@PathVariable("item_id") String itemId, HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) jwtRaw;
		long parsedId;
		try {
			parsedId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"获取商品详情出错.",
					Map.of("item_id", List.of("validation.integer")))));
		}
		if (parsedId < 1) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"获取商品详情出错.",
					Map.of("item_id", List.of("validation.min.numeric")))));
		}
		Object aid = jwt.get("authorizer_appid");
		String authorizerAppId = aid != null ? aid.toString() : null;
		Map<String, Object> data = goodsItemsDetailFacadeService.getDetail(request, jwt, parsedId, authorizerAppId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> validation422Body(String message, Map<String, List<String>> errors) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("message", message);
		m.put("status_code", 422);
		m.put("errors", errors);
		return m;
	}

	@Activated(routeAlias = "goods.items.delete")
	@DeleteMapping(value = "/items/{item_id}", name = "删除商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteItems(HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) jwtRaw;

		long parsedItemId;
		try {
			parsedItemId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"删除商品出错.",
					Map.of("item_id", List.of("validation.integer")))));
		}
		if (parsedItemId < 1) {
			return ResponseEntity.ok(ApiResult.ok(validation422Body(
					"删除商品出错.",
					Map.of("item_id", List.of("validation.min.numeric")))));
		}

		long distributorIdQuery = 0L;
		if (StringUtils.hasText(distributorIdParam.trim())) {
			distributorIdQuery = LeadingNumberParser.parseAsLong(distributorIdParam.trim());
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", readRequiredLong(ud, "company_id"));
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		merged.put("distributor_id", distributorIdQuery);

		goodsItemsCreateRequestGate.validateBeforeDelete(request, merged);
		long distributorId = parseDistributorIdForWarningStore(merged);
		itemsDeleteOrchestrator.delete(merged, parsedItemId, distributorId);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/goods/items/" + parsedItemId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("item_id", parsedItemId, "distributor_id", distributorId)));
		} catch (Exception e) {
			logCtx.put("params", "item_id=" + parsedItemId);
		}
		logCtx.put("operator_name", "删除商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.items.update")
	@PutMapping(value = "/items/{item_id}", name = "更新商品")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false
	)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItems(HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		if (!merged.containsKey("origincountry_id")) {
			merged.put("origincountry_id", 0);
		}
		if (!merged.containsKey("taxstrategy_id")) {
			merged.put("taxstrategy_id", 0);
		}
		if (!merged.containsKey("taxation_num")) {
			merged.put("taxation_num", 0);
		}
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		long pathItemId;
		try {
			pathItemId = Long.parseLong(itemId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
		if (pathItemId < 1L) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
		long isSupplierGoodsFlag = toLongOrZeroForSupplierFlag(merged.get("supplier_id"));
		boolean isSupplierGoods = isSupplierGoodsFlag > 0;
		merged.put("company_id", readRequiredLong(ud, "company_id"));
		merged.put("merchant_id", ud.get("merchant_id"));
		merged.put("operator_id", readRequiredLong(ud, "operator_id"));
		Object aid = ud.get("authorizer_appid");
		merged.put("authorizer_appid", aid != null ? aid.toString() : "");

		goodsItemsCreateRequestGate.validateBeforeUpdate(request, merged);
		GoodsItemsCreateParamValidator.validateForUpdate(merged, pathItemId);
		merged.put("item_id", pathItemId);
		itemsUpdateOrchestrator.updateItems(merged, pathItemId, isSupplierGoods);

		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/goods/items/" + pathItemId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "更新商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long toLongOrZeroForSupplierFlag(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
