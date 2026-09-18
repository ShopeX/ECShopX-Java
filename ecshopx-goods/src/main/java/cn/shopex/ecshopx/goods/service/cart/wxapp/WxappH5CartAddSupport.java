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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
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
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.orders.service.front.wxapp.OrdersCartColumnNamesMapSupport;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.promotions.service.CartMemberpreferenceValidationService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappH5CartAddSupport {

	private final ItemsRepository itemsRepository;
	private final ItemsMedicineRepository itemsMedicineRepository;
	private final DistributorMapper distributorMapper;
	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final WxappH5PackageCartSupport wxappH5PackageCartSupport;
	private final PointsmallCartSkuLoadService pointsmallCartSkuLoadService;
	private final CartMemberpreferenceValidationService cartMemberpreferenceValidationService;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	private final WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService;

	public WxappH5CartAddSupport(
			ItemsRepository itemsRepository,
			ItemsMedicineRepository itemsMedicineRepository,
			DistributorMapper distributorMapper,
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService,
			WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			WxappH5PackageCartSupport wxappH5PackageCartSupport,
			PointsmallCartSkuLoadService pointsmallCartSkuLoadService,
			CartMemberpreferenceValidationService cartMemberpreferenceValidationService,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher,
			WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService) {
		this.itemsRepository = itemsRepository;
		this.itemsMedicineRepository = itemsMedicineRepository;
		this.distributorMapper = distributorMapper;
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.wxappH5PackageCartSupport = wxappH5PackageCartSupport;
		this.pointsmallCartSkuLoadService = pointsmallCartSkuLoadService;
		this.cartMemberpreferenceValidationService = cartMemberpreferenceValidationService;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
		this.wxappH5DistributorCartPromotionService = wxappH5DistributorCartPromotionService;
	}

	public Map<String, Object> formatAddCartData(Map<String, Object> params, Map<String, Object> cartInfo) {
		long companyId = longVal(params.get("company_id"));
		long itemId = parseRequiredPositiveItemId(params.get("item_id"));
		long shopId = longVal(params.get("shop_id"));
		String shopType = stringVal(params.get("shop_type"));
		int num = parseRequiredNonNegativeNum(params.get("num"));
		if (cartInfo != null && !cartInfo.isEmpty() && isAccumulateMode(params)) {
			num += intVal(cartInfo.get("num"));
		}
		if ("package".equals(stringVal(params.get("activity_type")))) {
			wxappH5PackageCartSupport.assertPackageAddParams(params);
		}

		if ("pointsmall".equals(shopType)) {
			return formatPointsmallAddCartData(params, cartInfo, companyId, itemId, shopId, shopType, num);
		}

		Items baseItem = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (baseItem == null) {
			throw new ResourceException("商品不存在");
		}
		checkStartNum(baseItem, num);
		ResolvedSku resolved = resolveSkuForCart(companyId, itemId, shopId, shopType, baseItem);
		assertSellable(resolved);
		assertMedicineMaxIfNeeded(companyId, itemId, num, baseItem);
		Map<String, Object> activity =
				wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, itemId, shopId);
		assertStockWithActivity(resolved, num, activity, itemId);
		long userId = longVal(params.get("user_id"));
		if (userId > 0L && !"shop_offline".equals(shopType)) {
			wxappH5DistributorCartPromotionService.assertLimitedBuyOnAdd(
					companyId, userId, itemId, shopId, num, activity);
		}
		if (userId > 0L) {
			wxappH5DistributorCartPromotionService.assertLimitedTimeSaleOnAdd(
					companyId, userId, itemId, num, activity);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		if (cartInfo != null) {
			out.putAll(cartInfo);
		}
		out.put("company_id", companyId);
		out.put("user_id", userId);
		out.put("item_id", itemId);
		out.put("shop_id", shopId);
		out.put("shop_type", shopType);
		out.put("item_name", resolved.itemName());
		out.put("pics", resolved.pics());
		out.put("price", resolved.price());
		out.put("num", num);
		out.put("activity_type", stringVal(params.get("activity_type")));
		Object aid = params.get("activity_id");
		if (aid != null) {
			out.put("activity_id", longVal(aid));
		}
		Object itemsId = params.get("items_id");
		if (itemsId != null) {
			out.put("items_id", OrdersCartColumnNamesMapSupport.joinItemsId(itemsId));
		}
		String wxapp = stringVal(params.get("wxapp_appid"));
		if (StringUtils.hasText(wxapp)) {
			out.put("wxapp_appid", wxapp);
		}
		applyActivityFields(out, activity);
		List<Map<String, Object>> oneRow = List.of(out);
		goodsItemsListPromotionEnrichmentService.enrich(oneRow);
		return out;
	}

	private Map<String, Object> formatPointsmallAddCartData(
			Map<String, Object> params,
			Map<String, Object> cartInfo,
			long companyId,
			long itemId,
			long shopId,
			String shopType,
			int num) {
		if (cartInfo != null && !cartInfo.isEmpty() && isAccumulateMode(params)) {
			num += intVal(cartInfo.get("num"));
		}
		PointsmallItems item = pointsmallCartSkuLoadService.requireSellableSku(companyId, itemId);
		if (item == null) {
			throw new ResourceException("无效商品");
		}
		if (!"drug".equals(item.getSpecialType()) && "drug".equals(shopType)) {
			throw new ResourceException("药品清单只支持处方药");
		}
		int store = item.getStore() != null ? item.getStore() : 0;
		int logisticsStore = 0;
		if ("package".equals(stringVal(params.get("activity_type")))) {
			if (store < num) {
				throw new ResourceException("库存不足");
			}
		} else if (store + logisticsStore < num) {
			throw new ResourceException("库存不足");
		}

		String itemName = item.getItemName() != null ? item.getItemName() : "";
		String pics = pointsmallCartSkuLoadService.firstPicUrl(item);
		int price = item.getPrice() != null ? item.getPrice() : 0;
		int point = item.getPoint() != null ? item.getPoint() : 0;

		Map<String, Object> out = new LinkedHashMap<>();
		if (cartInfo != null) {
			out.putAll(cartInfo);
		}
		out.put("cart_id", cartInfo != null && cartInfo.get("cart_id") != null ? cartInfo.get("cart_id") : 0L);
		out.put("company_id", companyId);
		out.put("user_id", longVal(params.get("user_id")));
		out.put("shop_type", shopType);
		out.put("shop_id", shopId);
		out.put("activity_type", stringVal(params.get("activity_type")));
		Object aid = params.get("activity_id");
		if (aid != null) {
			out.put("activity_id", longVal(aid));
		}
		out.put("item_id", itemId);
		Object itemsId = params.get("items_id");
		if (itemsId != null) {
			out.put("items_id", OrdersCartColumnNamesMapSupport.joinItemsId(itemsId));
		}
		out.put("is_checked", Boolean.TRUE);
		out.put("item_name", itemName);
		out.put("pics", pics);
		out.put("num", num);
		out.put("price", price);
		out.put("point", point);
		String wxapp = stringVal(params.get("wxapp_appid"));
		if (StringUtils.hasText(wxapp)) {
			out.put("wxapp_appid", wxapp);
		}
		out.put("is_plus_buy", Boolean.FALSE);
		out.put("isAccumulate", Boolean.FALSE);
		return out;
	}

	public Map<String, Object> checkItemParamsForShopType(String shopType, Map<String, Object> params) {
		String st = stringVal(shopType);
		if ("distributor".equals(st) || "drug".equals(st)) {
			long shopId = longVal(params.get("shop_id"));
			if (shopId > 0L) {
				Distributor d = distributorMapper.selectById(shopId);
				if (d == null || !"true".equals(d.getIsValid())) {
					throw new ResourceException("当前店铺已失效");
				}
			}
			long companyId = longVal(params.get("company_id"));
			long userId = longVal(params.get("user_id"));
			long itemId = longVal(params.get("item_id"));
			cartMemberpreferenceValidationService.assertEligibleForCartAdd(companyId, userId, itemId);
			return params;
		}
		if ("pointsmall".equals(st)) {
			return params;
		}
		return params;
	}

	public Map<String, Object> checkCartInfoPromotionForShopType(
			String shopType, Map<String, Object> cartInfo, boolean isCheckout) {
		if (!isCheckout) {
			return cartInfo;
		}
		String st = stringVal(shopType);
		if (!"distributor".equals(st) && !"drug".equals(st)) {
			return cartInfo;
		}
		long companyId = longVal(cartInfo.get("company_id"));
		long itemId = longVal(cartInfo.get("item_id"));
		if (companyId <= 0L || itemId <= 0L) {
			return cartInfo;
		}
		long lineShopId = longVal(cartInfo.get("shop_id"));
		int num = intVal(cartInfo.get("num"));
		String cartMarketingType = stringVal(cartInfo.get("marketing_type"));
		long cartMarketingId = longVal(cartInfo.get("marketing_id"));
		boolean cartClaimsPromotion = StringUtils.hasText(cartMarketingType) || cartMarketingId > 0L;

		Map<String, Object> live =
				wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, itemId, lineShopId);

		if (!cartClaimsPromotion) {
			if (live != null && !live.isEmpty()) {
				assertActivityLimitNum(live, itemId, num);
				wxappH5DistributorCartPromotionService.applyLimitedTimeSaleCheckoutMoneyOnCartInfo(
						cartInfo, live, isCheckout);
			}
			return cartInfo;
		}
		if (live == null || live.isEmpty()) {
			throw new ResourceException("活动已结束或商品活动已变更");
		}
		String liveType = stringVal(live.get("activity_type"));
		if (StringUtils.hasText(cartMarketingType) && !cartMarketingType.equals(liveType)) {
			throw new ResourceException("活动已结束或商品活动已变更");
		}
		long liveMarketingId = resolveLiveMarketingId(liveType, live);
		if (cartMarketingId > 0L && liveMarketingId > 0L && cartMarketingId != liveMarketingId) {
			throw new ResourceException("活动已结束或商品活动已变更");
		}
		assertActivityLimitNum(live, itemId, num);
		wxappH5DistributorCartPromotionService.applyLimitedTimeSaleCheckoutMoneyOnCartInfo(
				cartInfo, live, isCheckout);
		return cartInfo;
	}

	private static void applyActivityFields(Map<String, Object> out, Map<String, Object> activity) {
		if (activity == null || activity.isEmpty()) {
			return;
		}
		Object at = activity.get("activity_type");
		if (at != null) {
			out.put("marketing_type", at.toString());
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> info = activity.get("info") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
		if (info != null) {
			Object sid = info.get("seckill_id");
			if (sid == null) {
				sid = info.get("groups_activity_id");
			}
			if (sid instanceof Number n) {
				out.put("marketing_id", n.longValue());
			}
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

	private static void assertStockWithActivity(
			ResolvedSku r, int num, Map<String, Object> activity, long itemId) {
		if (activity != null && activity.get("list") instanceof Map<?, ?> listMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cell = (Map<String, Object>) listMap.get(String.valueOf(itemId));
			if (cell != null && cell.get("store") instanceof Number n) {
				int actStore = n.intValue();
				if (actStore < num) {
					throw new ResourceException("库存不足");
				}
				return;
			}
		}
		if (r.effectiveStore() < num) {
			throw new ResourceException("库存不足");
		}
	}

	private static void checkStartNum(Items item, int num) {
		int start = item.getStartNum() == null ? 0 : item.getStartNum();
		if (start > 0 && num < start) {
			throw new BadRequestException("数量不能少于起订量");
		}
	}

	private static boolean isAccumulateMode(Map<String, Object> params) {
		Object raw = params.get("isAccumulate");
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof String s && "false".equals(s)) {
			return false;
		}
		return true;
	}

	private static long parseRequiredPositiveItemId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("提交购物车数据有误");
		}
		long v = longVal(raw);
		if (v <= 0L) {
			throw new BadRequestException("提交购物车数据有误");
		}
		return v;
	}

	private static int parseRequiredNonNegativeNum(Object raw) {
		if (raw == null) {
			throw new BadRequestException("提交购物车数据有误");
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数非法");
		}
	}

	private static String firstPic(String pics) {
		if (pics == null || pics.isEmpty()) {
			return "";
		}
		int comma = pics.indexOf(',');
		return comma > 0 ? pics.substring(0, comma).trim() : pics.trim();
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long resolveLiveMarketingId(String liveType, Map<String, Object> live) {
		@SuppressWarnings("unchecked")
		Map<String, Object> info = live.get("info") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
		if (info == null) {
			return 0L;
		}
		if ("seckill".equals(liveType) || "limited_time_sale".equals(liveType)) {
			Object sid = info.get("seckill_id");
			return sid instanceof Number n ? n.longValue() : 0L;
		}
		if ("group".equals(liveType)) {
			Object gid = info.get("groups_activity_id");
			return gid instanceof Number n ? n.longValue() : 0L;
		}
		if ("limited_buy".equals(liveType)) {
			Object lid = info.get("limit_id");
			return lid instanceof Number n ? n.longValue() : 0L;
		}
		return 0L;
	}

	private static void assertActivityLimitNum(Map<String, Object> live, long itemId, int num) {
		if (live.get("list") instanceof Map<?, ?> listMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cell = (Map<String, Object>) listMap.get(String.valueOf(itemId));
			if (cell != null && cell.get("limit_num") instanceof Number ln) {
				int limit = ln.intValue();
				if (limit > 0 && num > limit) {
					throw new ResourceException("超出活动限购数量");
				}
			}
		}
	}

	private record ResolvedSku(String itemName, String pics, int price, String approveStatus, int effectiveStore) {}
}
