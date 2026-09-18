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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsByCouponService {

	private static final int GOODS_BY_COUPON_PAGE_SIZE = 1000;

	private static final Logger log = LoggerFactory.getLogger(GoodsItemsByCouponService.class);

	private final RelItemsMapper relItemsMapper;
	private final ItemsRepository itemsRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final LangueProperties langueProperties;

	public GoodsItemsByCouponService(RelItemsMapper relItemsMapper, ItemsRepository itemsRepository,
			ItemsQueryRepository itemsQueryRepository, ItemsListMultiLangApplier itemsListMultiLangApplier,
			ItemsSkuListAssembler itemsSkuListAssembler,
			LangueProperties langueProperties) {
		this.relItemsMapper = relItemsMapper;
		this.itemsRepository = itemsRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getGoodsByCoupon(long companyId, String couponIdRaw, HttpServletRequest request) {
		if (couponIdRaw == null) {
			return emptyCouponList();
		}
		String t = couponIdRaw.trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return emptyCouponList();
		}
		final long cardId;
		try {
			cardId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return emptyCouponList();
		}

		LambdaQueryWrapper<RelItems> w = new LambdaQueryWrapper<>();
		w.eq(RelItems::getCompanyId, companyId).eq(RelItems::getCardId, cardId).orderByAsc(RelItems::getItemId);
		Page<RelItems> relPage = new Page<>(1, GOODS_BY_COUPON_PAGE_SIZE, false);
		List<RelItems> relRows = relItemsMapper.selectPage(relPage, w).getRecords();

		if (relRows.isEmpty()) {
			return emptyCouponList();
		}

		LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
		for (RelItems ri : relRows) {
			Long itemId = ri.getItemId();
			if (itemId != null && itemId > 0) {
				orderedIds.add(itemId);
			}
		}
		if (orderedIds.isEmpty()) {
			long totalCount = itemsQueryRepository.countByCompanyId(companyId);
			log.info("goodsByCoupon company-wide list companyId={} cardId={} totalCount={}", companyId, cardId, totalCount);
			List<Items> entities = itemsQueryRepository.listPageByCompanyIdOrderByItemIdAsc(companyId, 1,
					GOODS_BY_COUPON_PAGE_SIZE);
			return buildCouponResponse(companyId, request, entities, totalCount);
		}

		List<Long> orderedIdList = new ArrayList<>(orderedIds);
		long totalCount = itemsQueryRepository.countByCompanyAndItemIdIn(companyId, orderedIdList);
		log.info("goodsByCoupon items list companyId={} cardId={} totalCount={}", companyId, cardId, totalCount);

		List<Items> entities = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, orderedIdList);
		return buildCouponResponse(companyId, request, entities, totalCount);
	}

	private Map<String, Object> buildCouponResponse(long companyId, HttpServletRequest request, List<Items> entities,
			long totalCount) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Items it : entities) {
			rows.add(GoodsItemsListRowMapper.toRow(it));
		}
		itemsListMultiLangApplier.applyToRows(companyId, RequestLangTag.current(langueProperties), rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", rows);
		return out;
	}

	private static Map<String, Object> emptyCouponList() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0L);
		out.put("list", List.of());
		return out;
	}
}
