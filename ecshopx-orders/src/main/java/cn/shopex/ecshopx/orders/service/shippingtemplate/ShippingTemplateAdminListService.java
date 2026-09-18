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

package cn.shopex.ecshopx.orders.service.shippingtemplate;

import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ShippingTemplateAdminListService {

	private final ShopMenuService shopMenuService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ShippingTemplateAdminCreateService shippingTemplateAdminCreateService;
	private final CommonLangModReadService commonLangModReadService;

	public ShippingTemplateAdminListService(
			ShopMenuService shopMenuService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ShippingTemplateAdminCreateService shippingTemplateAdminCreateService,
			CommonLangModReadService commonLangModReadService) {
		this.shopMenuService = shopMenuService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.shippingTemplateAdminCreateService = shippingTemplateAdminCreateService;
		this.commonLangModReadService = commonLangModReadService;
	}

	public Map<String, Object> getShippingTemplatesList(
			long companyId,
			Map<String, Object> operatorJwt,
			String rawStatusParamOrNull,
			String rawIsFreeParamOrNull,
			String rawValuationParamOrNull,
			String rawPageParamOrNull,
			String rawPageSizeParamOrNull,
			String acceptLanguageHeader) {
		boolean restrictStatusEnabled = queryFlagTruthyForStatus(rawStatusParamOrNull);
		Optional<String> isFreeEq = Optional.empty();
		if (isFreeLooseNotEqualMinusOne(rawIsFreeParamOrNull)) {
			String t = rawIsFreeParamOrNull == null ? "" : rawIsFreeParamOrNull.trim();
			isFreeEq = Optional.of(t);
		}
		Optional<String> valuationEq = Optional.empty();
		if (valuationTruthy(rawValuationParamOrNull)) {
			String t = rawValuationParamOrNull == null ? "" : rawValuationParamOrNull.trim();
			valuationEq = Optional.of(t);
		}

		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(operatorJwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(operatorJwt.get("distributor_id"));
		long distributorFromJwt = distributorIdOrNull != null ? distributorIdOrNull : 0L;

		long supplierId;
		boolean applyDistributorEq;
		long distributorEq;
		if ("supplier".equals(operatorType)) {
			Long sid = EspierAdminJwtControllerSupport.parseLongOrNull(operatorJwt.get("operator_id"));
			supplierId = sid != null ? sid : 0L;
			applyDistributorEq = false;
			distributorEq = 0L;
		} else if ("distributor".equals(operatorType)) {
			supplierId = 0L;
			applyDistributorEq = true;
			distributorEq = distributorFromJwt;
		} else {
			supplierId = 0L;
			applyDistributorEq = false;
			distributorEq = 0L;
		}

		String pm = shopMenuService.resolveProductModelKeyForCompany(companyId);
		if ("platform".equalsIgnoreCase(pm)) {
			applyDistributorEq = true;
			distributorEq = distributorFromJwt;
		} else if ("standard".equalsIgnoreCase(pm)) {
			applyDistributorEq = true;
			distributorEq = 0L;
		}

		ShippingTemplateAdminListCriteria criteria =
				new ShippingTemplateAdminListCriteria(
						companyId,
						restrictStatusEnabled,
						isFreeEq,
						valuationEq,
						supplierId,
						applyDistributorEq,
						distributorEq);

		int page = parsePositiveIntWithDefault(rawPageParamOrNull, 1);
		int pageSize = parsePositiveIntWithDefault(rawPageSizeParamOrNull, 50);
		pageSize = Math.min(pageSize, 200);
		long offset = (long) (page - 1) * (long) pageSize;

		long total = shippingTemplatesQueryRepository.countByCriteria(criteria);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}

		List<ShippingTemplates> entities =
				shippingTemplatesQueryRepository.selectPageByCriteria(criteria, offset, pageSize);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ShippingTemplates e : entities) {
			rows.add(shippingTemplateAdminCreateService.toTemplateDetailRow(e));
		}
		commonLangModReadService.mergeShippingTemplateListNamesForLocale(companyId, rows, acceptLanguageHeader);
		out.put("list", rows);
		return out;
	}

	/**
	 * Treats the {@code status} query flag as set only when the value is non-blank after trim and not the literal {@code "0"}.
	 * When unset, the list does not add an extra enabled-status filter from this parameter; when set, only enabled rows match.
	 */
	private static boolean queryFlagTruthyForStatus(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	private static boolean isFreeLooseNotEqualMinusOne(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return true;
		}
		if ("-1".equals(t)) {
			return false;
		}
		try {
			BigDecimal bd = new BigDecimal(t);
			if (bd.compareTo(new BigDecimal("-1")) == 0) {
				return false;
			}
		} catch (NumberFormatException ignored) {
			// non-numeric: not loosely -1
		}
		return true;
	}

	/**
	 * Whether {@code valuation} should narrow the query: non-blank after trim and not the literal {@code "0"}.
	 */
	private static boolean valuationTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	private static int parsePositiveIntWithDefault(String raw, int def) {
		if (raw == null) {
			return def;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return def;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 1L || v > Integer.MAX_VALUE) {
				return def;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
