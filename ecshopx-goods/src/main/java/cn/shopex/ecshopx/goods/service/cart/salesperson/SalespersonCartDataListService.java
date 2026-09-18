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

package cn.shopex.ecshopx.goods.service.cart.salesperson;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.service.salesperson.SalespersonCartDataListFacade;
import cn.shopex.ecshopx.salesperson.repository.SalespersonCartRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartDataListService implements SalespersonCartDataListFacade {

	private final SalespersonCartRepository salespersonCartRepository;
	private final SalespersonCartSkuLoadService salespersonCartSkuLoadService;
	private final SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService;
	private final SalespersonCartHandleValidCartService salespersonCartHandleValidCartService;
	private final SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService;
	private final LangueProperties langueProperties;

	public SalespersonCartDataListService(SalespersonCartRepository salespersonCartRepository,
			SalespersonCartSkuLoadService salespersonCartSkuLoadService,
			SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService,
			SalespersonCartHandleValidCartService salespersonCartHandleValidCartService,
			SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService,
			LangueProperties langueProperties) {
		this.salespersonCartRepository = salespersonCartRepository;
		this.salespersonCartSkuLoadService = salespersonCartSkuLoadService;
		this.salespersonCartDistributorSkuReplaceService = salespersonCartDistributorSkuReplaceService;
		this.salespersonCartHandleValidCartService = salespersonCartHandleValidCartService;
		this.salespersonDistributorCartFormatAndTotalService = salespersonDistributorCartFormatAndTotalService;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> getCartdataList(long userId, Map<String, Object> filter, HttpServletRequest request) {
		return buildCartdataList(userId, filter, request, false);
	}

	@Override
	public Map<String, Object> getCartdataListForCheckout(long userId, Map<String, Object> filter, HttpServletRequest request) {
		return buildCartdataList(userId, filter, request, true);
	}

	private Map<String, Object> buildCartdataList(
			long userId, Map<String, Object> filter, HttpServletRequest request, boolean isSubmit) {
		long companyId = longFrom(filter.get("company_id"));
		long salespersonId = longFrom(filter.get("salesperson_id"));
		long distributorId = longFrom(filter.get("distributor_id"));

		List<Map<String, Object>> cartRows =
				salespersonCartRepository.listForGetCartdataList(companyId, salespersonId, distributorId, isSubmit);
		if (cartRows == null || cartRows.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("购物车为空");
			}
			return emptyBody();
		}

		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : cartRows) {
			long iid = longFrom(row.get("item_id"));
			if (iid > 0L && !itemIds.contains(iid)) {
				itemIds.add(iid);
			}
		}

		Map<String, Object> itemPack =
				salespersonCartSkuLoadService.loadSkuItemsList(companyId, distributorId, salespersonId, userId, itemIds, request);
		int totalCount = toInt(itemPack.get("total_count"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) itemPack.get("list");
		if (totalCount <= 0 || skuList == null || skuList.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("商品已失效");
			}
			return emptyBody();
		}

		if (distributorId != 0L) {
			salespersonCartDistributorSkuReplaceService.apply(companyId, distributorId, skuList);
		}

		Map<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		for (Map<String, Object> row : skuList) {
			long iid = longFrom(row.get("item_id"));
			if (iid > 0L) {
				itemByItemId.put(iid, row);
			}
		}
		if (itemByItemId.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("购物车商品已失效或不存在");
			}
			return emptyBody();
		}

		List<Map<String, Object>> cartList =
				cartRows.stream().map(m -> new LinkedHashMap<>(m)).collect(Collectors.toCollection(ArrayList::new));

		Map<String, Object> handled =
				salespersonCartHandleValidCartService.handle(companyId, userId, cartList, itemByItemId);
		handled.put("is_check_store", false);

		String acceptLang = resolveAcceptLanguage(request);
		Map<String, Object> cartData =
				salespersonDistributorCartFormatAndTotalService.apply(
						companyId, userId, handled, isSubmit, distributorId, "distributor", acceptLang);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invalidAfter = (List<Map<String, Object>>) cartData.get("invalid_cart");
		if (invalidAfter != null && !invalidAfter.isEmpty()) {
			List<Long> cartIds = new ArrayList<>();
			for (Map<String, Object> row : invalidAfter) {
				long cid = longFrom(row.get("cart_id"));
				if (cid > 0L) {
					cartIds.add(cid);
				}
			}
			if (!cartIds.isEmpty()) {
				salespersonCartRepository.updateIsCheckedByCompanyAndCartIds(companyId, cartIds, false);
			}
		}

		return cartData;
	}

	private static Map<String, Object> emptyBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("invalid_cart", List.of());
		m.put("valid_cart", List.of());
		return m;
	}

	private String resolveAcceptLanguage(HttpServletRequest request) {
		if (request == null) {
			return "zh-CN";
		}
		return RequestLangTag.current(langueProperties);
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
