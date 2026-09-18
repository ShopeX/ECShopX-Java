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
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.MemberPrice;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberPriceMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ItemCreatePromotionGuardService {

	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final MemberPriceMapper memberPriceMapper;
	private final ObjectMapper objectMapper;

	public ItemCreatePromotionGuardService(
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			PackagePromotionsMapper packagePromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			BargainPromotionsMapper bargainPromotionsMapper,
			MemberPriceMapper memberPriceMapper,
			ObjectMapper objectMapper) {
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.memberPriceMapper = memberPriceMapper;
		this.objectMapper = objectMapper;
	}

	public void checkNotFinishedActivityValid(long companyId, boolean isGift, List<Long> itemIds, List<Long> goodsIds) {
		if (!isGift || itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		long nowL = now;
		List<Long> normItemIds = distinctPositive(itemIds);
		List<Long> normGoodsIds = distinctPositive(goodsIds == null ? List.of() : goodsIds);

		if (existsSeckillRelNotFinished(companyId, normItemIds, now)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (!normGoodsIds.isEmpty() && existsGroupActivityNotFinished(normGoodsIds, nowL)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (existsLimitActivityNotFinished(normItemIds, now)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (existsMarketingActivityItemsNotFinished(companyId, normItemIds, now)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (!normGoodsIds.isEmpty() && existsPackageMainNotFinished(companyId, normGoodsIds, now)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (existsPackageItemNotFinished(companyId, normItemIds, now)) {
			throw new ResourceException("无法被设置为赠品");
		}
		if (existsBargainNotFinished(companyId, normItemIds, nowL)) {
			throw new ResourceException("无法被设置为赠品");
		}
	}

	public void checkItemPrice(long companyId, List<Long> goodsIds, Map<Long, Long> itemIdToPriceFen) {
		if (itemIdToPriceFen == null || itemIdToPriceFen.isEmpty()) {
			return;
		}
		List<Long> normGoodsIds = distinctPositive(goodsIds == null ? List.of() : goodsIds);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (!normGoodsIds.isEmpty()) {
			List<PromotionGroupsActivity> acts = promotionGroupsActivityMapper.selectList(
					new LambdaQueryWrapper<PromotionGroupsActivity>()
							.in(PromotionGroupsActivity::getGoodsId, normGoodsIds)
							.eq(PromotionGroupsActivity::getDisabled, false)
							.ge(PromotionGroupsActivity::getEndTime, (long) now));
			for (PromotionGroupsActivity act : acts) {
				Long actPrice = act.getActPrice();
				if (actPrice == null) {
					continue;
				}
				Long goodsKey = act.getGoodsId();
				Long sale = goodsKey != null ? itemIdToPriceFen.get(goodsKey) : null;
				if (sale == null) {
					continue;
				}
				if (actPrice > sale) {
					String name = act.getActName() != null ? act.getActName() : "";
					throw new ResourceException("拼团名称【" + name + "】的活动价格大于商品价格，请检查后再提交！");
				}
			}
		}
		assertMemberPricesNotAboveSale(companyId, itemIdToPriceFen);
	}

	private void assertMemberPricesNotAboveSale(long companyId, Map<Long, Long> itemIdToPriceFen) {
		List<Long> keys = new ArrayList<>(itemIdToPriceFen.keySet());
		if (keys.isEmpty()) {
			return;
		}
		List<MemberPrice> rows = memberPriceMapper.selectList(
				new LambdaQueryWrapper<MemberPrice>()
						.eq(MemberPrice::getCompanyId, companyId)
						.in(MemberPrice::getItemId, keys));
		for (MemberPrice row : rows) {
			Long itemId = row.getItemId();
			Long saleFen = itemIdToPriceFen.get(itemId);
			if (saleFen == null) {
				continue;
			}
			String raw = row.getMprice();
			if (raw == null || raw.isBlank()) {
				continue;
			}
			try {
				JsonNode root = objectMapper.readTree(raw);
				if (assertGradeContainerAboveSale(root.get("grade"), saleFen)
						|| assertGradeContainerAboveSale(root.get("vipGrade"), saleFen)) {
					throw new ResourceException("商品会员价大于销售价，请检查后再提交！");
				}
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				continue;
			}
		}
	}

	private boolean assertGradeContainerAboveSale(JsonNode container, long saleFen) {
		if (container == null || container.isNull()) {
			return false;
		}
		if (container.isArray()) {
			for (JsonNode node : container) {
				if (node.isNumber() && node.longValue() > saleFen) {
					return true;
				}
			}
			return false;
		}
		if (!container.isObject()) {
			return false;
		}
		Iterator<JsonNode> it = container.elements();
		while (it.hasNext()) {
			JsonNode node = it.next();
			if (node.isNumber()) {
				if (node.longValue() > saleFen) {
					return true;
				}
			} else if (node.isObject() && node.has("mprice")) {
				JsonNode mp = node.get("mprice");
				if (mp != null && mp.isNumber() && mp.longValue() > saleFen) {
					return true;
				}
				if (mp != null && mp.isTextual()) {
					try {
						long v = Long.parseLong(mp.asText().trim());
						if (v > saleFen) {
							return true;
						}
					} catch (NumberFormatException ignored) {
						// skip
					}
				}
			}
		}
		return false;
	}

	private boolean existsSeckillRelNotFinished(long companyId, List<Long> itemIds, int now) {
		if (itemIds.isEmpty()) {
			return false;
		}
		Long c = seckillRelGoodsMapper.selectCount(
				new LambdaQueryWrapper<SeckillRelGoods>()
						.eq(SeckillRelGoods::getCompanyId, companyId)
						.in(SeckillRelGoods::getItemId, itemIds)
						.ge(SeckillRelGoods::getActivityEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsGroupActivityNotFinished(List<Long> goodsIds, long now) {
		Long c = promotionGroupsActivityMapper.selectCount(
				new LambdaQueryWrapper<PromotionGroupsActivity>()
						.in(PromotionGroupsActivity::getGoodsId, goodsIds)
						.eq(PromotionGroupsActivity::getDisabled, false)
						.ge(PromotionGroupsActivity::getEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsLimitActivityNotFinished(List<Long> itemIds, int now) {
		if (itemIds.isEmpty()) {
			return false;
		}
		Long c = limitItemPromotionsMapper.selectCount(
				new LambdaQueryWrapper<LimitItemPromotions>()
						.in(LimitItemPromotions::getItemId, itemIds)
						.ge(LimitItemPromotions::getEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsMarketingActivityItemsNotFinished(long companyId, List<Long> itemIds, int now) {
		if (itemIds.isEmpty()) {
			return false;
		}
		Long c = marketingActivityItemsMapper.selectCount(
				new LambdaQueryWrapper<MarketingActivityItems>()
						.eq(MarketingActivityItems::getCompanyId, companyId)
						.in(MarketingActivityItems::getItemId, itemIds)
						.ge(MarketingActivityItems::getEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsPackageMainNotFinished(long companyId, List<Long> goodsIds, int now) {
		Long c = packagePromotionsMapper.selectCount(
				new LambdaQueryWrapper<PackagePromotions>()
						.eq(PackagePromotions::getCompanyId, companyId)
						.in(PackagePromotions::getGoodsId, goodsIds)
						.ge(PackagePromotions::getEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsPackageItemNotFinished(long companyId, List<Long> itemIds, int now) {
		if (itemIds.isEmpty()) {
			return false;
		}
		Long c = packageItemPromotionsMapper.selectCount(
				new LambdaQueryWrapper<PackageItemPromotions>()
						.eq(PackageItemPromotions::getCompanyId, companyId)
						.in(PackageItemPromotions::getItemId, itemIds)
						.ge(PackageItemPromotions::getEndTime, now));
		return c != null && c > 0;
	}

	private boolean existsBargainNotFinished(long companyId, List<Long> itemIds, long now) {
		if (itemIds.isEmpty()) {
			return false;
		}
		List<String> asStr = new ArrayList<>(itemIds.size());
		for (Long id : itemIds) {
			asStr.add(String.valueOf(id));
		}
		Long c = bargainPromotionsMapper.selectCount(
				new LambdaQueryWrapper<BargainPromotions>()
						.eq(BargainPromotions::getCompanyId, companyId)
						.in(BargainPromotions::getItemId, asStr)
						.ge(BargainPromotions::getEndTime, now));
		return c != null && c > 0;
	}

	private static List<Long> distinctPositive(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
	}
}
