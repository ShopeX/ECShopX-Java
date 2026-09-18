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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemRowsSortKey;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityItemsListQueryMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import cn.shopex.ecshopx.goods.service.pagestemplate.PagesTemplateDecorationItemsQueryService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityItemListService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateItemsInfoService {

	private final SeckillActivityItemListService seckillActivityItemListService;
	private final PagesTemplateDecorationItemsQueryService pagesTemplateDecorationItemsQueryService;
	private final ActivitiesMapper activitiesMapper;
	private final EmployeePurchaseActivityItemsListQueryMapper employeePurchaseActivityItemsListQueryMapper;
	private final ItemsMapper itemsMapper;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;

	public PagesTemplateItemsInfoService(
			SeckillActivityItemListService seckillActivityItemListService,
			PagesTemplateDecorationItemsQueryService pagesTemplateDecorationItemsQueryService,
			ActivitiesMapper activitiesMapper,
			EmployeePurchaseActivityItemsListQueryMapper employeePurchaseActivityItemsListQueryMapper,
			ItemsMapper itemsMapper,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository) {
		this.seckillActivityItemListService = seckillActivityItemListService;
		this.pagesTemplateDecorationItemsQueryService = pagesTemplateDecorationItemsQueryService;
		this.activitiesMapper = activitiesMapper;
		this.employeePurchaseActivityItemsListQueryMapper = employeePurchaseActivityItemsListQueryMapper;
		this.itemsMapper = itemsMapper;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.itemCrossBorderTaxQueryRepository = itemCrossBorderTaxQueryRepository;
	}

	public Map<Long, Map<String, Object>> getItemsInfo(
			long companyId, String configName, List<Long> itemsId, Map<String, Object> params) {
		if (itemsId == null || itemsId.isEmpty()) {
			return Map.of();
		}
		int userId = intOrZero(params.get("user_id"));
		int activityId = 0;
		Object cfg = params.get("config");
		if (cfg instanceof Map<?, ?> c) {
			Object sid = c.get("seckillId");
			if (sid != null) {
				activityId = intOrZero(sid);
			}
		}
		int distributorId = intOrZero(params.get("distributor_id"));

		List<Map<String, Object>> seckillOverlay = null;
		if ("goodsScroll".equals(configName) && activityId > 0) {
			Map<String, Object> sk =
					seckillActivityItemListService.getSeckillItemList(
							companyId, Long.valueOf(activityId), 1, 10, false);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> skList = (List<Map<String, Object>>) sk.get("list");
			if (skList != null && !skList.isEmpty()) {
				Object cfgObj = params.get("config");
				if (cfgObj instanceof Map<?, ?> c) {
					@SuppressWarnings("unchecked")
					Map<String, Object> cm = (Map<String, Object>) (Map<?, ?>) c;
					Object act = sk.get("activity");
					if (act instanceof Map<?, ?> am) {
						Object st = am.get("status");
						if (st != null) {
							cm.put("status", st);
						}
						Object ls = am.get("last_seconds");
						cm.put("last_seconds", ls != null ? ls : 0);
					}
				}
				seckillOverlay = skList;
			}
		}

		long queryDistributorId = distributorId;
		long eActivityId = longOrZero(params.get("e_activity_id"));
		if (eActivityId > 0L) {
			Activities actRow =
					activitiesMapper.selectOne(
							Wrappers.lambdaQuery(Activities.class)
									.eq(Activities::getCompanyId, companyId)
									.eq(Activities::getId, eActivityId));
			if (actRow != null
					&& actRow.getDistributorId() != null
					&& actRow.getDistributorId() > 0) {
				queryDistributorId = actRow.getDistributorId().longValue();
			}
		}

		List<Map<String, Object>> mainList =
				pagesTemplateDecorationItemsQueryService.queryItemListData(
						companyId, itemsId, queryDistributorId, params);
		Map<Long, Map<String, Object>> keyedByItemId =
				PagesTemplateDecorationItemsQueryService.indexByItemId(mainList);

		if (seckillOverlay != null) {
			for (Map<String, Object> skRow : seckillOverlay) {
				long iid = longOrZero(skRow.get("item_id"));
				if (iid <= 0L) {
					continue;
				}
				Map<String, Object> target = keyedByItemId.computeIfAbsent(iid, k -> new LinkedHashMap<>());
				target.putAll(skRow);
				target.put("item_id", iid);
			}
		}

		if (eActivityId > 0L) {
			List<Long> goodsIds = new ArrayList<>();
			List<Items> batch = itemsMapper.selectBatchIds(itemsId);
			if (batch != null) {
				for (Items it : batch) {
					if (it != null && it.getGoodsId() != null && it.getGoodsId() > 0L) {
						goodsIds.add(it.getGoodsId());
					}
				}
			}
			if (!goodsIds.isEmpty()) {
				List<Map<String, Object>> actRows =
						employeePurchaseActivityItemsListQueryMapper.selectActivityItemRows(
								companyId, eActivityId, goodsIds, ActivityItemRowsSortKey.ITEM_ID_DESC, 1);
				for (Map<String, Object> ar : actRows) {
					long iid = longOrZero(ar.get("item_id"));
					if (iid <= 0L) {
						continue;
					}
					Map<String, Object> row = keyedByItemId.get(iid);
					if (row == null) {
						continue;
					}
					Object ap = ar.get("activity_price");
					if (ap != null) {
						row.put("activity_price", ap);
					}
				}
			}
		}

		List<Map<String, Object>> rowList = new ArrayList<>(keyedByItemId.values());
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, rowList, "");

		List<Items> itemEntities = itemsMapper.selectBatchIds(itemsId);
		Map<Long, Items> byItem =
				itemEntities == null
						? Map.of()
						: itemEntities.stream()
								.filter(Objects::nonNull)
								.filter(it -> it.getItemId() != null)
								.collect(Collectors.toMap(Items::getItemId, it -> it, (a, b) -> a, LinkedHashMap::new));
		for (Map<String, Object> row : rowList) {
			long iid = longOrZero(row.get("item_id"));
			Items it = byItem.get(iid);
			if (it == null) {
				continue;
			}
			if (row.get("type") == null && it.getType() != null) {
				row.put("type", it.getType());
			}
			if (!row.containsKey("brand") || row.get("brand") == null || row.get("brand").toString().isEmpty()) {
				row.put("brand", it.getGoodsBrand() != null ? it.getGoodsBrand() : "");
			}
			if (row.get("special_type") == null && it.getSpecialType() != null) {
				row.put("special_type", it.getSpecialType());
			}
		}

		for (Map<String, Object> row : rowList) {
			applyCrossBorderTax(row, byItem.get(longOrZero(row.get("item_id"))));
		}

		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rowList) {
			long iid = longOrZero(row.get("item_id"));
			if (iid > 0L) {
				out.put(iid, row);
			}
		}
		return out;
	}

	private void applyCrossBorderTax(Map<String, Object> value, Items itemEntity) {
		Object typeObj = value.get("type");
		boolean cross =
				Integer.valueOf(1).equals(typeObj instanceof Number n ? n.intValue() : intOrZero(typeObj));
		if (!cross) {
			value.put("cross_border_tax", 0);
			value.put("cross_border_tax_rate", 0);
			return;
		}
		if (itemEntity == null) {
			value.put("cross_border_tax", 0);
			value.put("cross_border_tax_rate", 0);
			return;
		}
		String taxField = "price";
		int taxBaseFen = intOrZero(value.get("price"));
		if (nonEmptyPrice(value.get("member_price"))) {
			taxField = "member_price";
			taxBaseFen = intOrZero(value.get("member_price"));
		}
		if (nonEmptyPrice(value.get("activity_price"))) {
			taxField = "activity_price";
			taxBaseFen = intOrZero(value.get("activity_price"));
		}
		BigDecimal rateFromRepo =
				itemCrossBorderTaxQueryRepository.resolveEffectiveTaxRatePercent(itemEntity, taxBaseFen);
		if (rateFromRepo == null) {
			rateFromRepo = BigDecimal.ZERO;
		}
		BigDecimal rateRaw = rateFromRepo.setScale(2, RoundingMode.HALF_UP);
		BigDecimal taxBaseBd = BigDecimal.valueOf(taxBaseFen).setScale(2, RoundingMode.HALF_UP);
		BigDecimal rateTimes100 = rateRaw.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
		BigDecimal mul = taxBaseBd.multiply(rateTimes100).setScale(2, RoundingMode.HALF_UP);
		BigDecimal div1 = mul.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		BigDecimal crossBorderTax = div1.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		value.put("cross_border_tax", crossBorderTax.toPlainString());
		value.put("cross_border_tax_rate", rateFromRepo.toPlainString());
		BigDecimal newPriceFen = taxBaseBd.add(crossBorderTax).setScale(0, RoundingMode.HALF_UP);
		int newFen = newPriceFen.intValue();
		value.put(taxField, newFen);
		if ("activity_price".equals(taxField)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> acts = (List<Map<String, Object>>) value.get("promotion_activity");
			if (acts != null && !acts.isEmpty()) {
				Map<String, Object> last = acts.get(acts.size() - 1);
				last.put("activity_price", newFen);
			}
		}
	}

	private static boolean nonEmptyPrice(Object v) {
		return intOrZero(v) > 0;
	}

	private static int intOrZero(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null) {
			return 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
