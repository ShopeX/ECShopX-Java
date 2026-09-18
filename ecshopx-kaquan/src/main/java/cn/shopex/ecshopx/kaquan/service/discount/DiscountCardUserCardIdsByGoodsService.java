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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountListFilterParams;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardUserCardIdsByGoodsService {

	private static final int MAX_ITEM_TAGS = 100;

	private final UserDiscountMapper userDiscountMapper;
	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;

	public DiscountCardUserCardIdsByGoodsService(UserDiscountMapper userDiscountMapper,
			OperatorCartSkuLoadFacade operatorCartSkuLoadFacade) {
		this.userDiscountMapper = userDiscountMapper;
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
	}

	@SuppressWarnings("unchecked")
	public List<Long> resolveUserDiscountIds(Map<String, Object> filter) {
		long companyId = toLong(filter.get("company_id"));
		long userId = toLong(filter.get("user_id"));
		List<Long> cartItemIds = extractItemIds(filter.get("item_id"));
		if (cartItemIds.isEmpty()) {
			return List.of();
		}
		Long cardId = parseOptionalLong(filter.get("card_id"));
		UserDiscountListFilterParams p = new UserDiscountListFilterParams();
		p.setCompanyId(companyId);
		p.setUserId(userId);
		p.setCardId(cardId);
		Set<Long> acc = new LinkedHashSet<>();
		addAll(acc, userDiscountMapper.selectIdsUseBoundAll(p));
		p.setCardId(null);
		p.setGoodsItemIds(cartItemIds);
		addAll(acc, userDiscountMapper.selectIdsUseBoundNormal(p));
		addAll(acc, userDiscountMapper.selectIdsUseBoundNormalNeq(p));
		operatorCartSkuLoadFacade.fillCouponGoodsFilterLists(companyId, cartItemIds, filter);
		List<Long> mainCats = castLongList(filter.get("item_main_cat_id"));
		if (mainCats != null && !mainCats.isEmpty()) {
			p.setGoodsItemIds(mainCats);
			addAll(acc, userDiscountMapper.selectIdsUseBoundCategory(p));
		}
		filter.put("tag_ids", new ArrayList<Long>());
		List<Long> defaultItemIds = castLongList(filter.get("default_item_id"));
		if (defaultItemIds != null && !defaultItemIds.isEmpty()) {
			List<Long> tagIds = operatorCartSkuLoadFacade.listTagIdsByItemIds(companyId, defaultItemIds);
			if (tagIds.size() > MAX_ITEM_TAGS) {
				tagIds = tagIds.subList(0, MAX_ITEM_TAGS);
			}
			filter.put("tag_ids", new ArrayList<>(tagIds));
			if (!tagIds.isEmpty()) {
				p.setGoodsItemIds(tagIds);
				addAll(acc, userDiscountMapper.selectIdsUseBoundTag(p));
			}
		}
		List<Integer> brands = castIntList(filter.get("brand_id"));
		if (brands != null && !brands.isEmpty()) {
			List<Long> brandAsLong = brands.stream().map(Integer::longValue).toList();
			p.setGoodsItemIds(brandAsLong);
			addAll(acc, userDiscountMapper.selectIdsUseBoundBrand(p));
		}
		return new ArrayList<>(acc);
	}

	private static void addAll(Set<Long> acc, List<Long> ids) {
		if (ids != null) {
			acc.addAll(ids);
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			return Long.parseLong(s.trim());
		}
		return 0L;
	}

	private static Long parseOptionalLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Long> extractItemIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null) {
					try {
						out.add(Long.parseLong(String.valueOf(o).trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return out;
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	private static List<Long> castLongList(Object o) {
		if (!(o instanceof List<?> list)) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		for (Object x : list) {
			if (x instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out.isEmpty() ? null : out;
	}

	@SuppressWarnings("unchecked")
	private static List<Integer> castIntList(Object o) {
		if (!(o instanceof List<?> list)) {
			return null;
		}
		List<Integer> out = new ArrayList<>();
		for (Object x : list) {
			if (x instanceof Number n) {
				out.add(n.intValue());
			}
		}
		return out.isEmpty() ? null : out;
	}
}
