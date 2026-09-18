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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.SupplierItemsListMultiLangApplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsDetailFacadeService {

	private static final Logger log = LoggerFactory.getLogger(GoodsItemsDetailFacadeService.class);

	private static final String MSG_ITEM_NOT_EXIST = "商品信息不存在，请确认商品ID.";
	private static final String MSG_GET_ITEMS_INFO_ERROR = "获取商品信息出错.";

	private final ItemsRepository itemsRepository;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final SupplierItemsDetailCoreService supplierItemsDetailCoreService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final ItemsDetailAttributeIdsEnricher itemsDetailAttributeIdsEnricher;
	private final ItemsDetailTagListEnricher itemsDetailTagListEnricher;
	private final ItemsDetailSpecDisplayService itemsDetailSpecDisplayService;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final SupplierItemsListMultiLangApplier supplierItemsListMultiLangApplier;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public GoodsItemsDetailFacadeService(ItemsRepository itemsRepository, ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService, SupplierItemsDetailCoreService supplierItemsDetailCoreService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService, 			ItemsDetailAttributeIdsEnricher itemsDetailAttributeIdsEnricher,
			ItemsDetailTagListEnricher itemsDetailTagListEnricher,
			ItemsDetailSpecDisplayService itemsDetailSpecDisplayService, ItemsListMultiLangApplier itemsListMultiLangApplier,
			SupplierItemsListMultiLangApplier supplierItemsListMultiLangApplier, ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.itemsRepository = itemsRepository;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.supplierItemsDetailCoreService = supplierItemsDetailCoreService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.itemsDetailAttributeIdsEnricher = itemsDetailAttributeIdsEnricher;
		this.itemsDetailTagListEnricher = itemsDetailTagListEnricher;
		this.itemsDetailSpecDisplayService = itemsDetailSpecDisplayService;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.supplierItemsListMultiLangApplier = supplierItemsListMultiLangApplier;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getDetail(HttpServletRequest request, Map<String, Object> jwt, long itemId, String authorizerAppId) {
		try {
			return buildDetailPayload(request, jwt, itemId, authorizerAppId);
		} catch (Exception e) {
			log.warn("goods items detail failed: companyId={} itemId={}", jwt != null ? jwt.get("company_id") : null, itemId, e);
			throw new ResourceException("商品详情数据不完整");
		}
	}

	public Map<String, Object> getDetailForH5Kujiale(HttpServletRequest request, Long companyId, long itemId, String authorizerAppId) {
		try {
			return buildDetailPayloadH5Kujiale(request, companyId, itemId, authorizerAppId);
		} catch (Exception e) {
			log.warn("goods items detail failed (h5 kujiale): companyId={} itemId={}", companyId, itemId, e);
			throw new ResourceException("商品详情数据不完整");
		}
	}

	private Map<String, Object> buildDetailPayload(HttpServletRequest request, Map<String, Object> jwt, long itemId, String authorizerAppId) {
		long companyId = toLong(jwt.get("company_id"));
		String operatorType = str(jwt.get("operator_type"));
		long jwtDistributorId = toLong(jwt.get("distributor_id"));

		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		long distributorIdQuery = parseDistributorId(request.getParameter("distributor_id"));
		String operateSource = str(request.getParameter("operate_source"));
		if (!StringUtils.hasText(operateSource)) {
			operateSource = "platform";
		}
		String pageFrom = str(request.getParameter("page_from"));

		Map<String, Object> detail;
		boolean branchA = "standard".equals(productModel) && "distributor".equals(operatorType) && distributorIdQuery > 0;
		if (branchA) {
			if (!itemsRepository.existsByItemIdAndCompany(itemId, companyId)) {
				return itemNotExistError();
			}
			detail = distributorItemsDetailMergeService.merge(companyId, itemId, distributorIdQuery, authorizerAppId, productModel);
		} else if ("supplier".equals(operateSource) && !StringUtils.hasText(pageFrom)) {
			detail = supplierItemsDetailCoreService.build(companyId, itemId, authorizerAppId);
		} else {
			if (!itemsRepository.existsByItemIdAndCompany(itemId, companyId)) {
				return itemNotExistError();
			}
			detail = platformItemsDetailCoreService.build(companyId, itemId, authorizerAppId);
		}

		if (detail == null || detail.isEmpty() || detail.get("item_id") == null) {
			return itemNotExistError();
		}
		long rowCompany = toLong(detail.get("company_id"));
		if (rowCompany != companyId) {
			return getItemsInfoError();
		}

		List<Long> attributeIds = parseAttributeIds(request);
		String distributorIdQueryStr = request.getParameter("distributor_id");
		String countryCode = RequestLangTag.current(langueProperties);
		if ("zh-CN".equals(countryCode)) {
			Object cc = jwt.get("country_code");
			if (cc != null && StringUtils.hasText(cc.toString())) {
				countryCode = cc.toString().trim();
			}
		}
		if (attributeIds != null && !attributeIds.isEmpty()) {
			itemsDetailAttributeIdsEnricher.enrich(companyId, jwtDistributorId, detail, attributeIds, distributorIdQueryStr, countryCode);
		}

		if ("platform".equals(operateSource)) {
			itemsDetailTagListEnricher.enrich(companyId, detail);
		}

		itemsDetailSpecDisplayService.apply(detail);

		if ("supplier".equals(operateSource) && !StringUtils.hasText(pageFrom)) {
			supplierItemsListMultiLangApplier.applyToRows(companyId, countryCode, List.of(detail));
			if (!detail.containsKey("item_params_list")) {
				detail.put("item_params_list", new ArrayList<>());
			}
		} else {
			itemsListMultiLangApplier.applyToRows(companyId, countryCode, List.of(detail));
		}

		parseIntroJsonIfNeeded(detail);
		return detail;
	}

	private Map<String, Object> buildDetailPayloadH5Kujiale(HttpServletRequest request, Long companyId, long itemId, String authorizerAppId) {
		Items row = itemsRepository.findByItemId(itemId);
		if (row == null) {
			return itemNotExistError();
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			return getItemsInfoError();
		}
		long effectiveCompanyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
		Map<String, Object> jwt = new LinkedHashMap<>();
		if (row.getCompanyId() != null) {
			jwt.put("company_id", row.getCompanyId());
		}
		String operatorType = str(jwt.get("operator_type"));
		long jwtDistributorId = toLong(jwt.get("distributor_id"));

		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(effectiveCompanyId);
		long distributorIdQuery = parseDistributorId(request.getParameter("distributor_id"));
		String operateSource = str(request.getParameter("operate_source"));
		if (!StringUtils.hasText(operateSource)) {
			operateSource = "platform";
		}
		String pageFrom = str(request.getParameter("page_from"));

		String appId = StringUtils.hasText(authorizerAppId) ? authorizerAppId : "";

		Map<String, Object> detail;
		boolean branchA = "standard".equals(productModel) && "distributor".equals(operatorType) && distributorIdQuery > 0;
		if (branchA) {
			detail = distributorItemsDetailMergeService.merge(effectiveCompanyId, itemId, distributorIdQuery, appId, productModel);
		} else if ("supplier".equals(operateSource) && !StringUtils.hasText(pageFrom)) {
			detail = supplierItemsDetailCoreService.build(effectiveCompanyId, itemId, appId);
		} else {
			detail = platformItemsDetailCoreService.build(effectiveCompanyId, itemId, appId);
		}

		if (detail == null || detail.isEmpty() || detail.get("item_id") == null) {
			return itemNotExistError();
		}
		long rowCompany = toLong(detail.get("company_id"));
		if (rowCompany != effectiveCompanyId) {
			return getItemsInfoError();
		}

		List<Long> attributeIds = parseAttributeIds(request);
		String distributorIdQueryStr = request.getParameter("distributor_id");
		String countryCode = RequestLangTag.current(langueProperties);
		if ("zh-CN".equals(countryCode)) {
			Object cc = jwt.get("country_code");
			if (cc != null && StringUtils.hasText(cc.toString())) {
				countryCode = cc.toString().trim();
			}
		}
		if (attributeIds != null && !attributeIds.isEmpty()) {
			itemsDetailAttributeIdsEnricher.enrich(effectiveCompanyId, jwtDistributorId, detail, attributeIds, distributorIdQueryStr, countryCode);
		}

		if ("platform".equals(operateSource)) {
			itemsDetailTagListEnricher.enrich(effectiveCompanyId, detail);
		}

		itemsDetailSpecDisplayService.apply(detail);

		if ("supplier".equals(operateSource) && !StringUtils.hasText(pageFrom)) {
			supplierItemsListMultiLangApplier.applyToRows(effectiveCompanyId, countryCode, List.of(detail));
		} else {
			itemsListMultiLangApplier.applyToRows(effectiveCompanyId, countryCode, List.of(detail));
		}

		parseIntroJsonIfNeeded(detail);
		return detail;
	}

	private static Map<String, Object> itemNotExistError() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("message", MSG_ITEM_NOT_EXIST);
		m.put("status_code", 422);
		return m;
	}

	private static Map<String, Object> getItemsInfoError() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("message", MSG_GET_ITEMS_INFO_ERROR);
		m.put("status_code", 422);
		return m;
	}

	private void parseIntroJsonIfNeeded(Map<String, Object> detail) {
		Object intro = detail.get("intro");
		if (intro == null || intro instanceof Map<?, ?> || intro instanceof List<?>) {
			return;
		}
		if (!(intro instanceof String s) || !StringUtils.hasText(s)) {
			return;
		}
		try {
			JsonNode n = objectMapper.readTree(s);
			detail.put("intro", objectMapper.convertValue(n, Object.class));
		} catch (Exception ignored) {
		}
	}

	private static List<Long> parseAttributeIds(HttpServletRequest request) {
		String[] vals = request.getParameterValues("attribute_ids");
		if (vals == null || vals.length == 0) {
			String one = request.getParameter("attribute_ids");
			if (!StringUtils.hasText(one)) {
				return List.of();
			}
			vals = one.split(",");
		}
		List<Long> out = new ArrayList<>();
		for (String v : vals) {
			if (!StringUtils.hasText(v)) {
				continue;
			}
			try {
				out.add(Long.parseLong(v.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static long parseDistributorId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
