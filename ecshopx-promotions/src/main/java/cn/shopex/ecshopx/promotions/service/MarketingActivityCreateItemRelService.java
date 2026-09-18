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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import cn.shopex.ecshopx.promotions.support.PromotionTagTruncate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityCreateItemRelService {

	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;

	public MarketingActivityCreateItemRelService(
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess) {
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
	}

	public void createMarketingItemRel(Map<String, Object> activityRow, Map<String, Object> params) {
		long companyId = readLong(activityRow.get("company_id"));
		long marketingId = readLong(activityRow.get("marketing_id"));
		marketingActivityItemsMapper.delete(new LambdaQueryWrapper<MarketingActivityItems>()
				.eq(MarketingActivityItems::getCompanyId, companyId)
				.eq(MarketingActivityItems::getMarketingId, marketingId));
		List<Long> itemIds = new ArrayList<>(readLongList(params.get("item_ids")));
		String itemTypeForJob = params.get("item_type") != null ? params.get("item_type").toString() : "normal";
		int useBound = toInt(params.get("use_bound"));
		if (useBound == 1) {
			params.put("item_type", "normal");
			itemTypeForJob = "normal";
		}
		String defaultItemName = "";
		if (useBound == 2) {
			itemIds.clear();
			itemIds.addAll(readLongList(params.get("item_category")));
			params.put("item_type", "category");
			itemTypeForJob = "category";
			defaultItemName = "主分类";
		} else if (useBound == 3) {
			itemIds.clear();
			itemIds.addAll(readLongList(params.get("tag_ids")));
			params.put("item_type", "tag");
			itemTypeForJob = "tag";
			defaultItemName = "标签";
		} else if (useBound == 4) {
			itemIds.clear();
			itemIds.addAll(readLongList(params.get("brand_ids")));
			params.put("item_type", "brand");
			itemTypeForJob = "brand";
			defaultItemName = "品牌";
		}
		Map<Long, Map<String, Object>> itemsById = new LinkedHashMap<>();
		if (useBound == 1 && !itemIds.isEmpty()) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>)
					marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds).get("list");
			for (Map<String, Object> row : list) {
				Object id = row.get("item_id");
				if (id instanceof Number n) {
					itemsById.put(n.longValue(), row);
				}
			}
		}
		if (activityRow != null && !activityRow.isEmpty() && useBound != 0 && !itemIds.isEmpty()) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			for (Long itemId : itemIds) {
				MarketingActivityItems row = new MarketingActivityItems();
				row.setMarketingId(marketingId);
				row.setCompanyId(companyId);
				row.setStartTime(toInt(activityRow.get("start_time")));
				row.setEndTime(toInt(activityRow.get("end_time")));
				String pt = stringify(params.get("promotion_tag"));
				row.setPromotionTag(StringUtils.hasText(pt) ? PromotionTagTruncate.toVarchar15(pt) : null);
				row.setMarketingType(String.valueOf(params.get("marketing_type")));
				row.setItemType(String.valueOf(params.get("item_type")));
				row.setItemId(itemId);
				Map<String, Object> sku = itemsById.get(itemId);
				if (sku != null) {
					row.setItemName(stringify(sku.get("item_name")));
					Object pr = sku.get("price");
					row.setPrice(pr instanceof Number n ? n.intValue() : 0);
					row.setPics(stringify(sku.get("pics")));
					Object gid = sku.get("goods_id");
					row.setGoodsId(gid instanceof Number n ? n.longValue() : 0L);
					row.setItemSpecDesc(stringify(sku.get("item_spec_desc")));
				} else {
					row.setItemName(defaultItemName);
					row.setPrice(0);
					row.setPics("");
					row.setGoodsId(0L);
					row.setItemSpecDesc("");
				}
				row.setIsShow(true);
				row.setCreated(now);
				row.setUpdated(now);
				int n = marketingActivityItemsMapper.insert(row);
				if (n <= 0) {
					throw new ResourceException("关联活动商品出错");
				}
			}
		}
		params.put("item_type", itemTypeForJob);
		params.put("_tag_job_item_ids", new ArrayList<>(itemIds));
	}

	public void createMarketingGiftItemRel(Map<String, Object> activityRow, Map<String, Object> params) {
		long companyId = readLong(activityRow.get("company_id"));
		long marketingId = readLong(activityRow.get("marketing_id"));
		marketingGiftItemsMapper.delete(new LambdaQueryWrapper<MarketingGiftItems>()
				.eq(MarketingGiftItems::getCompanyId, companyId)
				.eq(MarketingGiftItems::getMarketingId, marketingId));
		String mt = String.valueOf(params.get("marketing_type"));
		if (!("full_gift".equals(mt) || "plus_price_buy".equals(mt))) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gifts = params.get("gifts") instanceof List<?> l ? (List<Map<String, Object>>) (List<?>) l : List.of();
		if (gifts.isEmpty()) {
			return;
		}
		List<Long> giftItemIds = new ArrayList<>();
		for (Map<String, Object> g : gifts) {
			Object iid = g.get("item_id");
			if (iid instanceof Number n) {
				giftItemIds.add(n.longValue());
			}
		}
		if ("plus_price_buy".equals(mt)) {
			List<Long> mainIds = readLongList(params.get("item_ids"));
			Set<Long> giftSet = new HashSet<>(giftItemIds);
			for (Long mid : mainIds) {
				if (giftSet.contains(mid)) {
					throw new ResourceException("加价购的主商品不能设置为加价购的加价商品");
				}
			}
		}
		Map<String, Object> skuResult = marketingActivityCatalogAccess.loadSkuItemsList(companyId, giftItemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuResult.get("list");
		Map<Long, Map<String, Object>> byId = new java.util.LinkedHashMap<>();
		for (Map<String, Object> row : skuList) {
			Object id = row.get("item_id");
			if (id instanceof Number n) {
				byId.put(n.longValue(), row);
			}
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		String conditionType = String.valueOf(activityRow.get("condition_type"));
		for (Map<String, Object> data : gifts) {
			long itemId = readLong(data.get("item_id"));
			if (!byId.containsKey(itemId)) {
				throw new ResourceException("商品不存在");
			}
			Map<String, Object> sku = byId.get(itemId);
			MarketingGiftItems row = new MarketingGiftItems();
			row.setMarketingId(marketingId);
			row.setCompanyId(companyId);
			row.setItemId(itemId);
			row.setItemType("normal");
			row.setItemName(stringify(sku.get("item_name")));
			row.setPrice(yuanStringToCentsInt(data.get("price")));
			row.setStore(toInt(data.getOrDefault("store", 0)));
			row.setGiftNum(toInt(data.getOrDefault("gift_num", 1)));
			row.setPics(stringify(sku.get("pics")));
			row.setConditionType(conditionType);
			Integer filterFull = resolveFilterFullForGift(data, conditionType);
			if (filterFull != null) {
				row.setFilterFull(filterFull);
			}
			row.setItemSpecDesc(stringify(sku.get("item_spec_desc")));
			row.setCreated(now);
			row.setUpdated(now);
			int n = marketingGiftItemsMapper.insert(row);
			if (n <= 0) {
				throw new ResourceException("赠品保存出错");
			}
		}
	}

	private static int yuanStringToCentsInt(Object priceRaw) {
		double yuan = toDouble(priceRaw);
		return (int) Math.round(yuan * 100.0);
	}

	private static double toDouble(Object v) {
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		return Double.parseDouble(String.valueOf(v).trim());
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static Integer resolveFilterFullForGift(Map<String, Object> data, String conditionType) {
		Object raw = data.get("filter_full");
		if (raw == null) {
			return null;
		}
		String text = String.valueOf(raw).trim();
		if (!StringUtils.hasText(text) || "null".equalsIgnoreCase(text)) {
			return null;
		}
		double val = toDouble(raw);
		if ("totalfee".equals(conditionType)) {
			return (int) Math.round(val * 100.0);
		}
		return (int) val;
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static String stringify(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static List<Long> readLongList(Object v) {
		if (!(v instanceof List<?> l)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}
}
