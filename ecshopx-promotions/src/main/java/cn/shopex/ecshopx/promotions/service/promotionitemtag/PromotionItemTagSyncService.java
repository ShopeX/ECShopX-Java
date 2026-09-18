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

package cn.shopex.ecshopx.promotions.service.promotionitemtag;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionItemTagSyncService implements PromotionItemTagSyncPort {

	private static final Logger log = LoggerFactory.getLogger(PromotionItemTagSyncService.class);

	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;

	public PromotionItemTagSyncService(
			PromotionsItemsTagMapper promotionsItemsTagMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess) {
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
	}

	@Override
	public void applySavePromotionItemTagJob(Map<String, Object> payload) {
		try {
			doApply(payload);
		} catch (Exception e) {
			log.debug("promotion item tag sync error: {}", e.getMessage());
		}
	}

	private void doApply(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"));
		long promotionId = readLong(payload.get("promotion_id"));
		String tagType = stringify(payload.get("tag_type"));
		int startTime = readInt(payload.get("start_time"));
		int endTime = readInt(payload.get("end_time"));
		String itemType = stringify(payload.get("item_type"));
		if (!StringUtils.hasText(itemType)) {
			itemType = "normal";
		}
		List<Long> itemIds = readLongList(payload.get("item_ids"));
		Map<Long, BigDecimal> activityPriceByItemId = readPriceMap(payload.get("activity_price_by_item_id"));

		promotionsItemsTagMapper.delete(
				new LambdaQueryWrapper<PromotionsItemsTag>()
						.eq(PromotionsItemsTag::getPromotionId, promotionId)
						.eq(PromotionsItemsTag::getCompanyId, companyId)
						.eq(PromotionsItemsTag::getTagType, tagType));

		if (!itemIds.isEmpty()) {
			Map<String, Object> catalog = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list =
					catalog.get("list") instanceof List<?> raw
							? (List<Map<String, Object>>) (List<?>) raw
							: List.of();
			for (Map<String, Object> row : list) {
				Object idObj = row.get("item_id");
				if (!(idObj instanceof Number)) {
					continue;
				}
				long itemId = ((Number) idObj).longValue();
				PromotionsItemsTag tag = new PromotionsItemsTag();
				tag.setPromotionId(promotionId);
				tag.setCompanyId(companyId);
				tag.setTagType(tagType);
				tag.setItemType(itemType);
				tag.setStartTime(startTime);
				tag.setEndTime(endTime);
				tag.setItemId(itemId);
				Object gid = row.get("goods_id");
				tag.setGoodsId(gid instanceof Number ? ((Number) gid).longValue() : 0L);
				tag.setIsAllItems(2L);
				tag.setActivityPrice(activityPriceCents(activityPriceByItemId.get(itemId)));
				promotionsItemsTagMapper.insert(tag);
			}
		} else {
			PromotionsItemsTag tag = new PromotionsItemsTag();
			tag.setPromotionId(promotionId);
			tag.setCompanyId(companyId);
			tag.setTagType(tagType);
			tag.setItemType(itemType);
			tag.setStartTime(startTime);
			tag.setEndTime(endTime);
			tag.setItemId(0L);
			tag.setGoodsId(0L);
			tag.setIsAllItems(1L);
			tag.setActivityPrice(0L);
			promotionsItemsTagMapper.insert(tag);
		}
	}

	private static Map<Long, BigDecimal> readPriceMap(Object raw) {
		Map<Long, BigDecimal> out = new LinkedHashMap<>();
		if (!(raw instanceof Map<?, ?> m)) {
			return out;
		}
		for (Map.Entry<?, ?> en : m.entrySet()) {
			Long key = null;
			if (en.getKey() instanceof Number n) {
				key = n.longValue();
			} else if (en.getKey() != null) {
				try {
					key = Long.parseLong(en.getKey().toString().trim());
				} catch (NumberFormatException ignored) {
					continue;
				}
			}
			if (key == null) {
				continue;
			}
			Object v = en.getValue();
			if (v instanceof BigDecimal bd) {
				out.put(key, bd);
			} else if (v instanceof Number n) {
				out.put(key, BigDecimal.valueOf(n.doubleValue()));
			} else if (v != null && StringUtils.hasText(v.toString())) {
				try {
					out.put(key, new BigDecimal(v.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}

	private static List<Long> readLongList(Object raw) {
		if (!(raw instanceof List<?> l)) {
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

	private static long activityPriceCents(BigDecimal yuanOrNull) {
		if (yuanOrNull == null) {
			return 0L;
		}
		return yuanOrNull.multiply(BigDecimal.valueOf(100L)).setScale(0, RoundingMode.HALF_UP).longValue();
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(stringify(v).trim());
	}

	private static int readInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = stringify(v).trim();
		if (s.isEmpty()) {
			return 0;
		}
		return (int) Double.parseDouble(s);
	}

	private static String stringify(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
