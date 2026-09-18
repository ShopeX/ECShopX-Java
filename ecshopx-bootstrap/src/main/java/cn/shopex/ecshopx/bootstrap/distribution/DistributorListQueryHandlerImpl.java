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

package cn.shopex.ecshopx.bootstrap.distribution;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.repository.AdapayMemberDistributorListRepository;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListContext;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListCoreService;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListResult;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryHandler;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.distributor.DistributorShowItemsQueryService;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.kaquan.service.DiscountCardOngoingListBySourceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorListQueryHandlerImpl implements DistributorListQueryHandler {

	private final DistributorAdminListCoreService distributorAdminListCoreService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final AdapayMemberDistributorListRepository adapayMemberDistributorListRepository;
	private final DiscountCardOngoingListBySourceService discountCardOngoingListBySourceService;
	private final DistributorShowItemsQueryService distributorShowItemsQueryService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public DistributorListQueryHandlerImpl(
			DistributorAdminListCoreService distributorAdminListCoreService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			AdapayMemberDistributorListRepository adapayMemberDistributorListRepository,
			DiscountCardOngoingListBySourceService discountCardOngoingListBySourceService,
			DistributorShowItemsQueryService distributorShowItemsQueryService,
				ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.distributorAdminListCoreService = distributorAdminListCoreService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.adapayMemberDistributorListRepository = adapayMemberDistributorListRepository;
		this.discountCardOngoingListBySourceService = discountCardOngoingListBySourceService;
		this.distributorShowItemsQueryService = distributorShowItemsQueryService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> handle(
			HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> mergedInput) {
		boolean datapassBlock = DatapassBlockResolver.isBlockedFromQueryParameter(request);
		long companyId = longOf(operatorJwt.get("company_id"));
		String lang = RequestLangTag.current(langueProperties);

		DistributorAdminListContext ctx = new DistributorAdminListContext();
		ctx.setMergedInput(mergedInput);
		ctx.setJwt(operatorJwt);
		ctx.setRequestLang(lang);
		ctx.setProductModel(itemsCategoryDistributorIdResolver.resolveProductModel(companyId));

		DistributorAdminListResult core = distributorAdminListCoreService.buildCore(ctx);
		List<Map<String, Object>> list = new ArrayList<>(core.getList());

		if (!list.isEmpty()) {
			List<Long> shopIds = pageShopIds(list);
			Set<Long> openAccounts =
					shopIds.isEmpty()
							? Set.of()
							: adapayMemberDistributorListRepository.listDistributorIdsWithOpenAccount(
									companyId, shopIds);
			Map<Long, List<Map<String, Object>>> coupons =
					discountCardOngoingListBySourceService.mapOngoingByDistributorIds(companyId, shopIds);
			for (Map<String, Object> row : list) {
				Long did = longOrNull(row.get("distributor_id"));
				// Head-office placeholder row: skip link, open-account flag, coupons, and contact masking.
				if (isHeadOfficePlaceholderRow(did)) {
					continue;
				}
				row.put("is_openAccount", openAccounts.contains(did));
				row.put("link", "pages/index?dtid=" + did);
			}
			if (datapassBlock) {
				for (Map<String, Object> row : list) {
					Long did = longOrNull(row.get("distributor_id"));
					if (isHeadOfficePlaceholderRow(did)) {
						continue;
					}
					Object mob = row.get("mobile");
					if (mob != null) {
						row.put("mobile", DataMasking.maskMobile(mob.toString()));
					}
					Object ct = row.get("contact");
					if (ct != null) {
						row.put("contact", DataMasking.maskTruename(ct.toString()));
					}
				}
			}
			for (Map<String, Object> row : list) {
				Long did = longOrNull(row.get("distributor_id"));
				if (isHeadOfficePlaceholderRow(did)) {
					continue;
				}
				row.put("discountCardList", coupons.getOrDefault(did, List.of()));
			}
			int showItems = parseNonNegativeInt(mergedInput.get("show_items"), 0);
			if (showItems != 0) {
				List<Long> itemTagIds = parseLongList(mergedInput.get("item_tag_id"));
				distributorShowItemsQueryService.appendItemListPerDistributor(companyId, list, itemTagIds, 10);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", core.getTotalCount());
		data.put("tagList", core.getTopTagList());
		data.put("distributor_self", core.getDistributorSelf());
		data.put("datapass_block", datapassBlock ? 1 : 0);
		return data;
	}

	/** True when {@code distributor_id} is missing or zero (head-office list placeholder, not a shop row). */
	private static boolean isHeadOfficePlaceholderRow(Long distributorId) {
		return distributorId == null || distributorId == 0L;
	}

	private static List<Long> pageShopIds(List<Map<String, Object>> list) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did != null && did > 0) {
				ids.add(did);
			}
		}
		return ids;
	}

	private List<Long> parseLongList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				Long v = longOrNull(o);
				if (v != null) {
					out.add(v);
				}
			}
			return out;
		}
		if (raw instanceof String s && s.trim().startsWith("[")) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node.isArray()) {
					List<Long> out = new ArrayList<>();
					for (JsonNode n : node) {
						if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							Long v = longOrNull(n.asText());
							if (v != null) {
								out.add(v);
							}
						}
					}
					return out;
				}
			} catch (Exception ignored) {
				return List.of();
			}
		}
		Long single = longOrNull(raw);
		return single != null ? List.of(single) : List.of();
	}

	private static int parseNonNegativeInt(Object v, int def) {
		Long n = longOrNull(v);
		if (n == null || n < 0) {
			return def;
		}
		if (n > Integer.MAX_VALUE) {
			return def;
		}
		return n.intValue();
	}

	private static long longOf(Object o) {
		Long v = longOrNull(o);
		return v != null ? v : 0L;
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
