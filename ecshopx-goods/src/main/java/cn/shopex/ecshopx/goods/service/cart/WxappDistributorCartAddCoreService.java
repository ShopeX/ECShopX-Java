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

package cn.shopex.ecshopx.goods.service.cart;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 分销商店铺场景下写入 {@code orders_cart} 的加车核心逻辑，对齐前台购物车校验与落库字段语义。
 */
@Service
public class WxappDistributorCartAddCoreService {

	private static final Logger log = LoggerFactory.getLogger(WxappDistributorCartAddCoreService.class);

	private final CartMapper cartMapper;
	private final DistributorMapper distributorMapper;
	private final ItemsRepository itemsRepository;
	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	private final ItemsMedicineRepository itemsMedicineRepository;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	public WxappDistributorCartAddCoreService(
			CartMapper cartMapper,
			DistributorMapper distributorMapper,
			ItemsRepository itemsRepository,
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService,
			ItemsMedicineRepository itemsMedicineRepository,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher) {
		this.cartMapper = cartMapper;
		this.distributorMapper = distributorMapper;
		this.itemsRepository = itemsRepository;
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
		this.itemsMedicineRepository = itemsMedicineRepository;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
	}

	public Map<String, Object> addCart(
			long companyId,
			long userId,
			long itemId,
			String shopType,
			String activityType,
			int num,
			boolean isAccumulate,
			long shopId,
			String isShopScreen) {
		validateBaseParams(companyId, itemId, num, shopType);
		Items baseItem = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (baseItem == null) {
			throw new ResourceException("提交的商品数据错误");
		}
		checkStartNum(baseItem, num);
		if (shopId > 0L && "distributor".equals(shopType)) {
			Distributor d = distributorMapper.selectById(shopId);
			if (d == null || !"true".equals(d.getIsValid())) {
				throw new ResourceException("当前店铺已失效");
			}
		}
		ResolvedSku resolved = resolveSkuForCart(companyId, itemId, shopId, shopType, baseItem);
		assertSellable(resolved);
		assertMedicineMaxIfNeeded(companyId, itemId, num, baseItem);
		assertStock(resolved, num);
		if (userId <= 0L) {
			log.info("wxapp scancode addCart skip persist: companyId={} itemId={} userId=0", companyId, itemId);
			return Map.of();
		}

		Cart existing = findMatchingCartRow(companyId, userId, itemId, shopType, shopId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			int existingNum = existing.getNum() == null ? 0 : existing.getNum();
			int newNum = isAccumulate ? existingNum + num : num;
			existing.setNum(newNum);
			existing.setItemName(resolved.itemName());
			existing.setPics(resolved.pics());
			existing.setPrice(resolved.price());
			existing.setActivityType(activityType != null ? activityType : "normal");
			existing.setSourceType("scancode");
			existing.setUpdated(now);
			cartMapper.updateById(existing);
			Cart refreshed = cartMapper.selectById(existing.getCartId());
			return toResultMap(refreshed);
		}
		Cart insert = new Cart();
		insert.setCompanyId(companyId);
		insert.setUserId(userId);
		insert.setShopType(shopType != null ? shopType : "distributor");
		insert.setShopId(shopId);
		insert.setActivityType(activityType != null ? activityType : "normal");
		insert.setActivityId(null);
		insert.setItemType("normal");
		insert.setItemId(itemId);
		insert.setItemName(resolved.itemName());
		insert.setPics(resolved.pics());
		insert.setNum(num);
		insert.setPrice(resolved.price());
		insert.setIsChecked(true);
		insert.setSourceType("scancode");
		insert.setCreated(now);
		insert.setUpdated(now);
		cartMapper.insert(insert);
		Cart saved = cartMapper.selectById(insert.getCartId());
		if (saved == null) {
			return Map.of();
		}
		return toResultMap(saved);
	}

	private static void validateBaseParams(long companyId, long itemId, int num, String shopType) {
		if (companyId <= 0L) {
			throw new ResourceException("提交的用户信息有误");
		}
		if (itemId <= 0L) {
			throw new ResourceException("提交的商品数据错误");
		}
		if (num <= 0) {
			throw new ResourceException("提交的商品数据错误");
		}
		if (!StringUtils.hasText(shopType)) {
			throw new ResourceException("提交购物车数据有误");
		}
	}

	private static void checkStartNum(Items item, int num) {
		int start = item.getStartNum() == null ? 0 : item.getStartNum();
		if (start > 0 && num < start) {
			throw new ResourceException("数量不能少于起订量");
		}
	}

	private ResolvedSku resolveSkuForCart(long companyId, long itemId, long shopId, String shopType, Items baseItem) {
		String itemName = baseItem.getItemName() != null ? baseItem.getItemName() : "";
		String pics = firstPic(baseItem.getPics());
		int price = baseItem.getPrice() != null ? baseItem.getPrice() : 0;
		String approve = baseItem.getApproveStatus() != null ? baseItem.getApproveStatus() : "";
		int hqStore = baseItem.getStore() != null ? baseItem.getStore() : 0;
		int effectiveStore = hqStore;
		if (shopId > 0L && shopType != null && !"community".equals(shopType)) {
			DistributorItems row = distributorItemSkuInfoForStoreService.findRow(companyId, itemId, shopId);
			if (row != null) {
				boolean totalStore = row.getIsTotalStore() == null || Boolean.TRUE.equals(row.getIsTotalStore());
				if (!totalStore) {
					long distStore = row.getStore() == null ? 0L : row.getStore();
					effectiveStore = (int) Math.min(distStore, Integer.MAX_VALUE);
					if (row.getPrice() != null && row.getPrice() > 0L) {
						price = row.getPrice().intValue();
					}
				}
				boolean canSale = row.getIsCanSale() == null || Boolean.TRUE.equals(row.getIsCanSale());
				if (!canSale) {
					approve = "instock";
				} else if (!totalStore) {
					approve = "onsale";
				}
			} else {
				Integer itemDistId = baseItem.getDistributorId();
				long boundDist = itemDistId == null ? 0L : itemDistId.longValue();
				if (boundDist != shopId) {
					throw new ResourceException("商品不存在");
				}
			}
		}
		effectiveStore = itemLogisticsStoreEnricher.combineEffectiveStore(
				companyId, shopId, effectiveStore, baseItem);
		return new ResolvedSku(itemName, pics, price, approve, effectiveStore);
	}

	private static void assertSellable(ResolvedSku r) {
		if (!"onsale".equals(r.approveStatus()) && !"offline_sale".equals(r.approveStatus())) {
			throw new ResourceException("商品未上架");
		}
	}

	private void assertMedicineMaxIfNeeded(long companyId, long itemId, int num, Items baseItem) {
		if (baseItem.getIsMedicine() == null || baseItem.getIsMedicine() != 1) {
			return;
		}
		Map<Long, ItemsMedicine> medMap = itemsMedicineRepository.mapByItemIds(companyId, List.of(itemId));
		ItemsMedicine m = medMap.get(itemId);
		if (m == null || m.getMaxNum() == null || m.getMaxNum() <= 0) {
			return;
		}
		if (num > m.getMaxNum()) {
			throw new ResourceException("超出药品单次最大可购买数量,最大可购买:" + m.getMaxNum() + "个");
		}
	}

	private static void assertStock(ResolvedSku r, int num) {
		if (r.effectiveStore() < num) {
			throw new ResourceException("库存不足");
		}
	}

	private Cart findMatchingCartRow(long companyId, long userId, long itemId, String shopType, long shopId) {
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getItemId, itemId)
				.eq(Cart::getShopType, shopType)
				.eq(Cart::getShopId, shopId)
				.eq(Cart::getActivityType, "normal")
				.isNull(Cart::getActivityId)
				.and(
						q -> q.isNull(Cart::getItemsId).or().eq(Cart::getItemsId, "").or().eq(Cart::getItemsId, "0"));
		w.last("LIMIT 1");
		return cartMapper.selectOne(w);
	}

	private static Map<String, Object> toResultMap(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (c.getCartId() != null) {
			m.put("cart_id", c.getCartId());
		}
		return m;
	}

	private static String firstPic(String pics) {
		if (pics == null || pics.isEmpty()) {
			return "";
		}
		int comma = pics.indexOf(',');
		return comma > 0 ? pics.substring(0, comma).trim() : pics.trim();
	}

	private record ResolvedSku(String itemName, String pics, int price, String approveStatus, int effectiveStore) {}
}
