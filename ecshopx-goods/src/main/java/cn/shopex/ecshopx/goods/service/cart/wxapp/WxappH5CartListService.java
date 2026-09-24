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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.crossborder.mapper.CrossBorderSetMapper;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.HandleValidCartValidShopIdsService;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsRelListCoreService;
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.goods.service.order.normal.GiftActivityStoreAdjustService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.orders.service.front.wxapp.OrdersCartColumnNamesMapSupport;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.promotions.service.CartMemberpreferenceValidationService;
import cn.shopex.ecshopx.promotions.service.ItemsTagActivityCheckService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionFrontPackageInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappH5CartListService {

	private static final ObjectMapper OM = new ObjectMapper();

	/**
	 * SKU fields merged into cart lines (narrow per-line column map). Do not
	 * {@code putAll(sku)} — avoids fat DTO keys and wrong {@code item_type} from SKU.
	 */
	private static final String[] SKU_NARROW_KEYS_BASE = {"item_name", "pics", "approve_status", "brief"};

	private static final String[] SKU_NARROW_KEYS_POINTSMALL = {
		"item_name",
		"pics",
		"approve_status",
		"brief",
		"store",
		"market_price",
		"goods_id",
		"item_category",
		"item_bn",
		"templates_id",
		"weight",
		"default_item_id",
		"type",
		"crossborder_tax_rate",
		"taxstrategy_id",
		"taxation_num",
		"origincountry_id",
		"is_medicine",
		"start_num",
		"item_spec_desc"
	};

	private static final String[] SKU_NARROW_KEYS_DISTRIBUTOR = {
		"item_name",
		"pics",
		"approve_status",
		"brief",
		"store",
		"market_price",
		"goods_id",
		"item_category",
		"type",
		"crossborder_tax_rate",
		"taxstrategy_id",
		"taxation_num",
		"origincountry_id",
		"is_medicine",
		"start_num",
		"item_spec_desc",
		"logistics_store",
		"supplier_id",
		"supplier_item_id"
	};

	private final CartMapper cartMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final HandleValidCartValidShopIdsService handleValidCartValidShopIdsService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	private final DistributorItemsRelListCoreService distributorItemsRelListCoreService;
	private final OriginCountryMapper originCountryMapper;
	private final CrossBorderSetMapper crossBorderSetMapper;
	private final PackagePromotionFrontPackageInfoService packagePromotionFrontPackageInfoService;
	private final ItemsTagActivityCheckService itemsTagActivityCheckService;
	private final WxappH5CartMemberPriceEnrichmentService wxappH5CartMemberPriceEnrichmentService;
	private final WxappH5CartListTotalAndPromotionAggregator wxappH5CartListTotalAndPromotionAggregator;
	private final WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService;
	private final WxappH5PackageCartSupport wxappH5PackageCartSupport;
	private final PointsmallCartSkuLoadService pointsmallCartSkuLoadService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final CartMemberpreferenceValidationService cartMemberpreferenceValidationService;
	private final CheckoutCartLogisticsSupplierStoreClampService checkoutCartLogisticsSupplierStoreClampService;
	private final CheckoutCartDistributorStoreOverlayService checkoutCartDistributorStoreOverlayService;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	private final GiftActivityStoreAdjustService giftActivityStoreAdjustService;
	private final ItemAvailableStoreResolver itemAvailableStoreResolver;

	public WxappH5CartListService(
			CartMapper cartMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			HandleValidCartValidShopIdsService handleValidCartValidShopIdsService,
			DistributorListQueryService distributorListQueryService,
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService,
			DistributorItemsRelListCoreService distributorItemsRelListCoreService,
			OriginCountryMapper originCountryMapper,
			CrossBorderSetMapper crossBorderSetMapper,
			PackagePromotionFrontPackageInfoService packagePromotionFrontPackageInfoService,
			ItemsTagActivityCheckService itemsTagActivityCheckService,
			WxappH5CartMemberPriceEnrichmentService wxappH5CartMemberPriceEnrichmentService,
			WxappH5CartListTotalAndPromotionAggregator wxappH5CartListTotalAndPromotionAggregator,
			WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService,
			WxappH5PackageCartSupport wxappH5PackageCartSupport,
			PointsmallCartSkuLoadService pointsmallCartSkuLoadService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			CartMemberpreferenceValidationService cartMemberpreferenceValidationService,
			CheckoutCartLogisticsSupplierStoreClampService checkoutCartLogisticsSupplierStoreClampService,
			CheckoutCartDistributorStoreOverlayService checkoutCartDistributorStoreOverlayService,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher,
			GiftActivityStoreAdjustService giftActivityStoreAdjustService,
			ItemAvailableStoreResolver itemAvailableStoreResolver) {
		this.cartMapper = cartMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.handleValidCartValidShopIdsService = handleValidCartValidShopIdsService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
		this.distributorItemsRelListCoreService = distributorItemsRelListCoreService;
		this.originCountryMapper = originCountryMapper;
		this.crossBorderSetMapper = crossBorderSetMapper;
		this.packagePromotionFrontPackageInfoService = packagePromotionFrontPackageInfoService;
		this.itemsTagActivityCheckService = itemsTagActivityCheckService;
		this.wxappH5CartMemberPriceEnrichmentService = wxappH5CartMemberPriceEnrichmentService;
		this.wxappH5CartListTotalAndPromotionAggregator = wxappH5CartListTotalAndPromotionAggregator;
		this.wxappH5DistributorCartPromotionService = wxappH5DistributorCartPromotionService;
		this.wxappH5PackageCartSupport = wxappH5PackageCartSupport;
		this.pointsmallCartSkuLoadService = pointsmallCartSkuLoadService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.cartMemberpreferenceValidationService = cartMemberpreferenceValidationService;
		this.checkoutCartLogisticsSupplierStoreClampService = checkoutCartLogisticsSupplierStoreClampService;
		this.checkoutCartDistributorStoreOverlayService = checkoutCartDistributorStoreOverlayService;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
		this.giftActivityStoreAdjustService = giftActivityStoreAdjustService;
		this.itemAvailableStoreResolver = itemAvailableStoreResolver;
	}

	public Map<String, Object> getCartList(
			long companyId,
			long userId,
			long shopId,
			String cartType,
			String shopType,
			boolean isCheckout,
			int iscrossborder,
			int isShopScreen) {
		return getCartList(
				companyId,
				userId,
				shopId,
				cartType,
				shopType,
				isCheckout,
				iscrossborder,
				isShopScreen,
				null,
				Collections.emptyList(),
				null);
	}

	public Map<String, Object> getCartList(
			long companyId,
			long userId,
			long shopId,
			String cartType,
			String shopType,
			boolean isCheckout,
			int iscrossborder,
			int isShopScreen,
			String userDevice,
			List<Map<String, Object>> offlineItems,
			Map<String, Object> inputData) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("valid_cart", new ArrayList<>());
		out.put("invalid_cart", new ArrayList<>());
		out.put("is_check_store", Boolean.FALSE);
		out.put("total_count", 0L);
		if ("offline".equals(cartType)) {
			if (offlineItems == null || offlineItems.isEmpty()) {
				return out;
			}
			assertInjected();
			List<Cart> rows = buildOfflineCartsFromItems(offlineItems, companyId, userId, shopType, shopId, inputData);
			out.put("total_count", (long) rows.size());
			assembleCartLinesIntoOut(
					companyId, userId, shopId, shopType, isCheckout, iscrossborder, isShopScreen, userDevice, inputData, rows, out);
			return out;
		}
		if (userId <= 0L) {
			return out;
		}
		assertInjected();
		if ("fastbuy".equals(cartType)) {
			String shopTypeForFastbuy = shopType == null ? "" : shopType.trim();
			List<Cart> fastRows = loadFastBuyCartRows(companyId, userId, shopId, shopTypeForFastbuy);
			out.put("total_count", (long) fastRows.size());
			if (!fastRows.isEmpty()) {
				assembleCartLinesIntoOut(
						companyId,
						userId,
						shopId,
						shopType,
						isCheckout,
						iscrossborder,
						isShopScreen,
						userDevice,
						inputData,
						fastRows,
						out);
				out.put("is_check_store", Boolean.TRUE);
			}
			return out;
		}
		String shopTypeForList = shopType == null ? "" : shopType.trim();
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getShopType, shopType)
				.eq(Cart::getShopId, shopId);
		if (isCheckout && "cart".equals(cartType)) {
			w.eq(Cart::getIsChecked, true);
		}
		long totalCount = cartMapper.selectCount(w);
		out.put("total_count", totalCount);
		List<Cart> rows = cartMapper.selectList(w);
		rows.sort(Comparator.comparing(Cart::getCartId, Comparator.nullsLast(Comparator.reverseOrder())));
		if (rows.isEmpty()) {
			if (isCheckout) {
				throw new ResourceException("购物车选中商品为空");
			}
			wxappH5CartListTotalAndPromotionAggregator.apply(companyId, out);
			return out;
		}
		assembleCartLinesIntoOut(
				companyId, userId, shopId, shopType, isCheckout, iscrossborder, isShopScreen, userDevice, inputData, rows, out);
		if (isCheckout && countLinesInValidCartBlocks(out) == 0) {
			throw new ResourceException("购物车商品不是来自同一个店铺");
		}
		return out;
	}

	public Map<String, Object> getCartItemCount(
			long companyId,
			long authUserId,
			long shopId,
			String cartType,
			String shopType,
			int iscrossborder,
			int isShopScreen,
			long promoterUserId,
			long buyUserId) {
		assertInjected();
		long effectiveUserId = authUserId;
		if (promoterUserId != 0L && buyUserId != 0L) {
			effectiveUserId = buyUserId;
		}
		assertShopTypeSupportedForList(shopType == null ? "" : shopType.trim());

		LambdaQueryWrapper<Cart> outerWrapper = Wrappers.lambdaQuery();
		applyOuterCartListFilter(
				outerWrapper,
				companyId,
				effectiveUserId,
				shopType,
				shopId,
				isShopScreen,
				promoterUserId,
				buyUserId);
		long totalCount = cartMapper.selectCount(outerWrapper);

		LinkedHashSet<Long> excludeIdSet = new LinkedHashSet<>();

		if (totalCount == 0L) {
			LambdaQueryWrapper<Cart> finalWrapper = Wrappers.lambdaQuery();
			applyOuterCartListFilter(
					finalWrapper,
					companyId,
					effectiveUserId,
					shopType,
					shopId,
					isShopScreen,
					promoterUserId,
					buyUserId);
			Map<String, Object> raw = cartMapper.selectAggregateCount(finalWrapper);
			long cartCount = longFromAggregate(raw.get("cart_count"));
			long itemCount = longFromAggregate(raw.get("item_count"));
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("cart_count", cartCount);
			result.put("item_count", itemCount);
			return result;
		}

		Page<Cart> page = new Page<>(1, 100, false);
		page.setOrders(List.of(OrderItem.desc("cart_id")));
		List<Cart> shopIdSourceRows = cartMapper.selectPage(page, outerWrapper).getRecords();

		LinkedHashSet<Long> shopIds = new LinkedHashSet<>();
		for (Cart row : shopIdSourceRows) {
			Long sid = row.getShopId();
			if (sid != null) {
				shopIds.add(sid);
			}
		}

		String ct = cartType == null ? "" : cartType.trim();
		for (long sid : shopIds) {
			List<Cart> basicRows;
			if ("cart".equals(ct)) {
				LambdaQueryWrapper<Cart> basicW = Wrappers.lambdaQuery();
				basicW.eq(Cart::getCompanyId, companyId)
						.eq(Cart::getUserId, effectiveUserId)
						.eq(Cart::getShopType, shopType)
						.eq(Cart::getShopId, sid);
				basicRows = cartMapper.selectList(basicW);
			} else if ("fastbuy".equals(ct)) {
				basicRows = buildFastBuyBasicRowsForCount(companyId, effectiveUserId, sid, shopType);
			} else {
				basicRows = List.of();
			}

			if (!basicRows.isEmpty()) {
				excludeIdSet.addAll(
						collectInvalidCartIdsAfterItemCountValidation(
								companyId,
								effectiveUserId,
								sid,
								shopType,
								iscrossborder,
								isShopScreen,
								basicRows));
			}
		}

		LambdaQueryWrapper<Cart> finalWrapper = Wrappers.lambdaQuery();
		applyOuterCartListFilter(
				finalWrapper,
				companyId,
				effectiveUserId,
				shopType,
				shopId,
				isShopScreen,
				promoterUserId,
				buyUserId);
		if (!excludeIdSet.isEmpty()) {
			finalWrapper.notIn(Cart::getCartId, excludeIdSet);
		}
		Map<String, Object> raw = cartMapper.selectAggregateCount(finalWrapper);
		long cartCount = longFromAggregate(raw.get("cart_count"));
		long itemCount = longFromAggregate(raw.get("item_count"));
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("cart_count", cartCount);
		result.put("item_count", itemCount);
		return result;
	}

	private void applyOuterCartListFilter(
			LambdaQueryWrapper<Cart> w,
			long companyId,
			long effectiveUserId,
			String shopType,
			long shopId,
			int isShopScreen,
			long promoterUserId,
			long buyUserId) {
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, effectiveUserId)
				.eq(Cart::getShopType, shopType);
		if (promoterUserId != 0L && buyUserId != 0L) {
			w.eq(Cart::getPromoterUserId, promoterUserId);
		}
		if (isShopScreen != 0 || shopId > 0L) {
			w.eq(Cart::getShopId, shopId);
		}
	}

	private List<Cart> buildFastBuyBasicRowsForCount(
			long companyId, long effectiveUserId, long sid, String shopType) {
		String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + effectiveUserId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			Map<String, Object> line = OM.readValue(raw, new TypeReference<>() {});
			long lineShop = longObj(line.get("shop_id"));
			if (sid > 0L && lineShop != sid) {
				return List.of();
			}
			if (StringUtils.hasText(shopType)) {
				String lst = stringVal(line.get("shop_type"));
				if (StringUtils.hasText(lst) && !shopType.equals(lst)) {
					return List.of();
				}
			}
			long itemId = longObj(line.get("item_id"));
			if (itemId <= 0L) {
				return List.of();
			}
			return List.of(cartFromFastBuyLineMap(line, companyId, effectiveUserId));
		} catch (Exception ignored) {
			return List.of();
		}
	}

	private static Cart cartFromFastBuyLineMap(Map<String, Object> line, long companyId, long effectiveUserId) {
		Cart c = new Cart();
		long lc = longObj(line.get("company_id"));
		c.setCompanyId(lc > 0L ? lc : companyId);
		long lu = longObj(line.get("user_id"));
		c.setUserId(lu > 0L ? lu : effectiveUserId);
		c.setCartId(longObj(line.get("cart_id")));
		c.setShopId(longObj(line.get("shop_id")));
		String st = stringVal(line.get("shop_type"));
		if (StringUtils.hasText(st)) {
			c.setShopType(st);
		}
		c.setItemId(longObj(line.get("item_id")));
		int n = intQty(line.get("num"));
		c.setNum(n > 0 ? n : 1);
		String it = stringVal(line.get("item_type"));
		c.setItemType(StringUtils.hasText(it) ? it : "normal");
		c.setItemsId(stringVal(line.get("items_id")));
		c.setItemName(stringVal(line.get("item_name")));
		c.setPics(stringVal(line.get("pics")));
		c.setPrice(intQty(line.get("price")));
		c.setPoint(intQty(line.get("point")));
		Object aid = line.get("activity_id");
		if (aid instanceof Number n1 && n1.longValue() > 0L) {
			c.setActivityId(n1.longValue());
		} else if (aid != null) {
			try {
				long av = Long.parseLong(aid.toString().trim());
				if (av > 0L) {
					c.setActivityId(av);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		c.setActivityType(stringVal(line.get("activity_type")));
		Object mid = line.get("marketing_id");
		if (mid instanceof Number n2 && n2.longValue() > 0L) {
			c.setMarketingId(n2.longValue());
		} else if (mid != null) {
			try {
				long mv = Long.parseLong(mid.toString().trim());
				if (mv > 0L) {
					c.setMarketingId(mv);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		c.setMarketingType(stringVal(line.get("marketing_type")));
		c.setIsChecked(true);
		return c;
	}

	private List<Long> collectInvalidCartIdsAfterItemCountValidation(
			long companyId,
			long effectiveUserId,
			long sid,
			String shopType,
			int iscrossborder,
			int isShopScreen,
			List<Cart> basicRows) {
		assertInjected();
		List<Long> shopIdsForValid =
				basicRows.stream().map(Cart::getShopId).filter(Objects::nonNull).distinct().toList();
		List<Long> validShopIds =
				handleValidCartValidShopIdsService.resolveValidShopIdsAfterDistributorAndMerchantRules(
						companyId, shopIdsForValid);
		Set<Long> validShopSet = new LinkedHashSet<>(validShopIds);

		List<Long> itemIds = basicRows.stream().map(Cart::getItemId).filter(Objects::nonNull).distinct().toList();
		Map<String, Object> skuPack =
				loadSkuItemsForCount(
						companyId, effectiveUserId, sid, shopType, itemIds, isShopScreen, null, null);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuPack.getOrDefault("list", List.of());
		Map<Long, Map<String, Object>> skuByItem = new LinkedHashMap<>();
		indexSkuRowsByItemId(skuList, skuByItem);

		List<Long> invalidCartIds = new ArrayList<>();
		List<Cart> invalidCarts = new ArrayList<>();
		for (Cart c : basicRows) {
			Map<String, Object> line =
					cartToLineMap(c, skuByItem.get(c.getItemId()), companyId, shopType, validShopSet, iscrossborder);
			String approve = stringVal(line.get("approve_status"));
			boolean sellable = "onsale".equals(approve) || "offline_sale".equals(approve);
			if (!sellable) {
				invalidCarts.add(c);
			}
		}
		for (Cart c : invalidCarts) {
			Long cid = c.getCartId();
			if (cid != null && cid > 0L) {
				invalidCartIds.add(cid);
				LambdaUpdateWrapper<Cart> u = new LambdaUpdateWrapper<>();
				u.eq(Cart::getCartId, c.getCartId()).set(Cart::getIsChecked, false);
				cartMapper.update(null, u);
			}
		}
		return invalidCartIds;
	}

	private static long longFromAggregate(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private List<Cart> buildOfflineCartsFromItems(
			List<Map<String, Object>> offlineItems,
			long companyId,
			long userId,
			String shopType,
			long defaultShopId,
			Map<String, Object> inputData) {
		List<Cart> rows = new ArrayList<>();
		Long promoter = null;
		if (inputData != null) {
			Object p = inputData.get("promoter_user_id");
			if (p instanceof Number n && n.longValue() != 0L) {
				promoter = n.longValue();
			} else if (p != null) {
				try {
					long pv = Long.parseLong(p.toString().trim());
					if (pv != 0L) {
						promoter = pv;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		for (Map<String, Object> item : offlineItems) {
			long itemId = longObj(item.get("item_id"));
			if (itemId <= 0L) {
				continue;
			}
			Cart c = new Cart();
			c.setCompanyId(companyId);
			c.setUserId(userId);
			c.setShopType(shopType);
			long sid = longObj(item.get("shop_id"));
			if (sid <= 0L) {
				sid = defaultShopId;
			}
			c.setShopId(sid);
			c.setItemId(itemId);
			int num = intQty(item.get("num"));
			c.setNum(num > 0 ? num : 1);
			c.setIsChecked(resolveLineChecked(item.get("is_checked")));
			if (promoter != null) {
				c.setPromoterUserId(promoter);
			}
			rows.add(c);
		}
		return rows;
	}

	private static boolean resolveLineChecked(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = raw.toString().trim();
		return !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private void assembleCartLinesIntoOut(
			long companyId,
			long userId,
			long shopId,
			String shopType,
			boolean isCheckout,
			int iscrossborder,
			int isShopScreen,
			String userDevice,
			Map<String, Object> inputData,
			List<Cart> rows,
			Map<String, Object> out) {
		String shopTypeForList = shopType == null ? "" : shopType.trim();
		assertShopTypeSupportedForList(shopTypeForList);
		List<Long> shopIds =
				rows.stream().map(Cart::getShopId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		List<Long> validShopIds = handleValidCartValidShopIdsService.resolveValidShopIdsAfterDistributorAndMerchantRules(companyId, shopIds);
		Set<Long> validShopSet = new LinkedHashSet<>(validShopIds);

		List<Long> itemIds = new ArrayList<>(rows.stream().map(Cart::getItemId).filter(Objects::nonNull).distinct().toList());
		itemIds.addAll(wxappH5PackageCartSupport.collectExtraItemIdsForSkuLoad(rows));
		Map<String, Object> skuPack =
				loadSkuItemsViaOrchestrator(
						companyId, userId, shopId, shopTypeForList, itemIds, isShopScreen, userDevice, inputData);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuPack.getOrDefault("list", List.of());
		Map<Long, Map<String, Object>> skuByItem = new LinkedHashMap<>();
		indexSkuRowsByItemId(skuList, skuByItem);

		List<Map<String, Object>> validLines = new ArrayList<>();
		List<Map<String, Object>> invalidLines = new ArrayList<>();
		List<Cart> invalidCarts = new ArrayList<>();
		for (Cart c : rows) {
			String rowShopType = c.getShopType() != null ? c.getShopType().trim() : shopTypeForList;
			if (shouldCheckMemberPreferenceForCartList(rowShopType)) {
				Long itemId = c.getItemId();
				if (itemId != null
						&& itemId > 0L
						&& !cartMemberpreferenceValidationService.isEligibleForCartList(
								companyId, userId, itemId)) {
					Map<String, Object> rawLine =
							new LinkedHashMap<>(OrdersCartColumnNamesMapSupport.toColumnNamesMap(c));
					normalizePicsToJsonString(rawLine);
					invalidLines.add(rawLine);
					continue;
				}
			}
			Map<String, Object> line = cartToLineMap(c, skuByItem.get(c.getItemId()), companyId, shopType, validShopSet, iscrossborder);
			String approve = stringVal(line.get("approve_status"));
			boolean sellable = "onsale".equals(approve) || "offline_sale".equals(approve);
			if (!sellable) {
				invalidLines.add(line);
				invalidCarts.add(c);
			} else {
				validLines.add(line);
			}
		}
		checkoutCartDistributorStoreOverlayService.applyForLines(companyId, shopTypeForList, validLines, skuByItem);
		if (!isCheckout) {
			itemLogisticsStoreEnricher.applyCartLinesDisplayStoreAfterOverlay(companyId, validLines, skuByItem);
		}
		List<Map<String, Object>> storeQuantityAdjustments =
				checkoutCartLogisticsSupplierStoreClampService.clampIfNeeded(
						companyId, isCheckout, inputData, validLines, invalidLines, skuByItem);
		if (storeQuantityAdjustments != null && !storeQuantityAdjustments.isEmpty()) {
			out.put("store_quantity_adjustments", storeQuantityAdjustments);
		}
		// 数量与商品库存都为 0 时不能按供应商库存留在有效列表
		partitionCheckoutZeroNumZeroItemStore(isCheckout, validLines, invalidLines, skuByItem);
		// 对齐 PHP：无可用库存（本地/供应商）的主品移入失效列表
		partitionZeroStoreValidLines(companyId, validLines, invalidLines, invalidCarts, rows);
		for (Cart c : invalidCarts) {
			Long cid = c.getCartId();
			if (cid == null || cid <= 0L) {
				continue;
			}
			LambdaUpdateWrapper<Cart> u = new LambdaUpdateWrapper<>();
			u.eq(Cart::getCartId, c.getCartId()).set(Cart::getIsChecked, false);
			cartMapper.update(null, u);
		}
		if ("distributor".equals(shopTypeForList) || "pointsmall".equals(shopTypeForList)) {
			List<Map<String, Object>> enrichedInvalid =
					invalidLines.stream().filter(l -> l.containsKey("approve_status")).toList();
			finalizeDistributorCartLineRows(enrichedInvalid);
		} else {
			for (Map<String, Object> line : invalidLines) {
				normalizePicsToJsonString(line);
			}
		}
		out.put("invalid_cart", invalidLines);
		boolean packageStoreChecked = partitionPackageValidLines(companyId, userId, validLines, invalidLines, skuByItem, userDevice);
		if (packageStoreChecked) {
			out.put("is_check_store", Boolean.TRUE);
		}
		applyLimitedTimeThenMemberPrices(companyId, userId, validLines, userDevice, shopTypeForList, isCheckout);
		if ("distributor".equals(shopTypeForList) || "pointsmall".equals(shopTypeForList)) {
			finalizeDistributorCartLineRows(validLines);
		} else {
			for (Map<String, Object> line : validLines) {
				normalizePicsToJsonString(line);
			}
		}
		if ("distributor".equals(shopTypeForList)) {
			Map<String, Object> promotionTotals =
					wxappH5DistributorCartPromotionService.applyMarketingAndTotals(
							companyId, userId, validLines, userDevice);
			List<Map<String, Object>> shopBlocks = groupValidByShop(validLines);
			enrichDistributorShopBlocks(companyId, shopBlocks);
			mergeDistributorPromotionTotalsIntoShopBlocks(shopBlocks, promotionTotals);
			// check_gift_store=true 时：结算按 receipt_type 下调；购物车列表无 receipt_type 时按展示可用库存剔除无货赠品
			adjustGiftActivitiesForCart(companyId, inputData, shopBlocks);
			out.put("valid_cart", shopBlocks);
		} else if ("pointsmall".equals(shopTypeForList)) {
			List<Map<String, Object>> shopBlocks = groupValidByShop(validLines);
			applyPointsmallShopBlockTotals(companyId, userId, shopBlocks);
			out.put("valid_cart", shopBlocks);
			out.put("is_check_store", Boolean.TRUE);
		} else {
			out.put("valid_cart", groupValidByShop(validLines));
			wxappH5CartListTotalAndPromotionAggregator.apply(companyId, out);
		}
	}

	private void enrichDistributorShopBlocks(long companyId, List<Map<String, Object>> shopBlocks) {
		if (shopBlocks == null || shopBlocks.isEmpty()) {
			return;
		}
		List<Long> shopIds = new ArrayList<>();
		for (Map<String, Object> block : shopBlocks) {
			long sid = longObj(block.get("shop_id"));
			if (sid > 0L) {
				shopIds.add(sid);
			}
		}
		if (shopIds.isEmpty()) {
			return;
		}
		List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, shopIds);
		Map<Long, Distributor> byId = new LinkedHashMap<>();
		if (distributors != null) {
			for (Distributor d : distributors) {
				if (d.getDistributorId() != null) {
					byId.put(d.getDistributorId(), d);
				}
			}
		}
		for (Map<String, Object> block : shopBlocks) {
			long sid = longObj(block.get("shop_id"));
			Distributor d = byId.get(sid);
			if (d != null) {
				block.put("shop_name", d.getName() != null ? d.getName() : "");
				block.put("address", d.getAddress() != null ? d.getAddress() : "");
				block.put("mobile", d.getMobile() != null ? d.getMobile() : "");
				block.put("lat", d.getLat() != null ? d.getLat() : "");
				block.put("lng", d.getLng() != null ? d.getLng() : "");
				block.put("hour", d.getHour() != null ? d.getHour() : "");
				block.put("is_ziti", Boolean.TRUE.equals(d.getIsZiti()));
				block.put("is_delivery", Boolean.TRUE.equals(d.getIsDelivery()));
			}
		}
	}

	private static void mergeDistributorPromotionTotalsIntoShopBlocks(
			List<Map<String, Object>> shopBlocks, Map<String, Object> promotionTotals) {
		if (shopBlocks == null || shopBlocks.isEmpty() || promotionTotals == null) {
			return;
		}
		for (Map<String, Object> shop : shopBlocks) {
			copyIfPresent(shop, promotionTotals, "item_fee");
			copyIfPresent(shop, promotionTotals, "discount_fee");
			copyIfPresent(shop, promotionTotals, "total_fee");
			copyIfPresent(shop, promotionTotals, "cart_total_price");
			copyIfPresent(shop, promotionTotals, "cart_total_num");
			copyIfPresent(shop, promotionTotals, "cart_total_count");
			copyIfPresent(shop, promotionTotals, "used_activity");
			copyIfPresent(shop, promotionTotals, "used_activity_ids");
			copyIfPresent(shop, promotionTotals, "activity_grouping");
			copyIfPresent(shop, promotionTotals, "gift_activity");
			copyIfPresent(shop, promotionTotals, "plus_buy_activity");
			shop.put("total_fee", Long.toString(longObj(shop.get("total_fee"))));
		}
	}

	@SuppressWarnings("unchecked")
	private void adjustGiftActivitiesForCart(
			long companyId, Map<String, Object> inputData, List<Map<String, Object>> shopBlocks) {
		if (shopBlocks == null || shopBlocks.isEmpty()) {
			return;
		}
		String receiptType =
				inputData == null ? "" : stringVal(inputData.get("receipt_type"));
		for (Map<String, Object> shop : shopBlocks) {
			Object raw = shop.get("gift_activity");
			if (!(raw instanceof List<?> list) || list.isEmpty()) {
				continue;
			}
			List<Map<String, Object>> activities = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					activities.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
			List<Map<String, Object>> cartLines = new ArrayList<>();
			Object listRaw = shop.get("list");
			if (listRaw instanceof List<?> shopList) {
				for (Object el : shopList) {
					if (el instanceof Map<?, ?> m) {
						cartLines.add((Map<String, Object>) m);
					}
				}
			}
			List<Map<String, Object>> adjusted =
					giftActivityStoreAdjustService.adjustGiftActivities(
							companyId, receiptType, activities, cartLines);
			shop.put("gift_activity", adjusted);
			attachGiftUnavailableTipToCartLines(shop, adjusted);
		}
	}

	/**
	 * 将「应送赠品但无货」提示挂到关联主品行（同一活动取最后一个关联主品），前端直接展示 {@code gift_unavailable_tip}。
	 */
	@SuppressWarnings("unchecked")
	private void attachGiftUnavailableTipToCartLines(
			Map<String, Object> shop, List<Map<String, Object>> giftActivities) {
		Object listRaw = shop.get("list");
		if (!(listRaw instanceof List<?> lines) || lines.isEmpty() || giftActivities == null) {
			return;
		}
		for (Object lineObj : lines) {
			if (lineObj instanceof Map<?, ?> lm) {
				((Map<String, Object>) lm).remove(GiftActivityStoreAdjustService.GIFT_UNAVAILABLE_TIP_KEY);
			}
		}
		for (Map<String, Object> act : giftActivities) {
			if (act == null) {
				continue;
			}
			String tip = stringVal(act.get(GiftActivityStoreAdjustService.GIFT_UNAVAILABLE_TIP_KEY));
			if (!StringUtils.hasText(tip)) {
				continue;
			}
			String activityId = stringVal(act.get("activity_id"));
			List<String> activityItemIds = normalizeIdList(act.get("activity_item_ids"));
			int lastIdx = -1;
			for (int i = 0; i < lines.size(); i++) {
				Object lineObj = lines.get(i);
				if (!(lineObj instanceof Map<?, ?> lm)) {
					continue;
				}
				Map<String, Object> line = (Map<String, Object>) lm;
				if (cartLineLinkedToGiftActivity(line, activityId, activityItemIds)) {
					lastIdx = i;
				}
			}
			if (lastIdx < 0) {
				continue;
			}
			Map<String, Object> target = (Map<String, Object>) lines.get(lastIdx);
			String existing = stringVal(target.get(GiftActivityStoreAdjustService.GIFT_UNAVAILABLE_TIP_KEY));
			if (StringUtils.hasText(existing) && !existing.equals(tip)) {
				target.put(GiftActivityStoreAdjustService.GIFT_UNAVAILABLE_TIP_KEY, existing + "；" + tip);
			} else {
				target.put(GiftActivityStoreAdjustService.GIFT_UNAVAILABLE_TIP_KEY, tip);
			}
		}
	}

	private static boolean cartLineLinkedToGiftActivity(
			Map<String, Object> line, String activityId, List<String> activityItemIds) {
		if (line == null) {
			return false;
		}
		if (StringUtils.hasText(activityId)) {
			for (String id : normalizeIdList(line.get("full_gift_id"))) {
				if (activityId.equals(id)) {
					return true;
				}
			}
		}
		String itemId = stringVal(line.get("item_id"));
		if (!StringUtils.hasText(itemId) || activityItemIds == null || activityItemIds.isEmpty()) {
			return false;
		}
		for (String aid : activityItemIds) {
			if (itemId.equals(aid)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> normalizeIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object el : list) {
				if (el == null) {
					continue;
				}
				String s = el.toString().trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out;
		}
		String s = raw.toString().trim();
		return s.isEmpty() ? List.of() : List.of(s);
	}

	private static void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
		if (source.containsKey(key)) {
			target.put(key, source.get(key));
		}
	}

	private Map<String, Object> loadSkuItemsViaOrchestrator(
			long companyId,
			long userId,
			long shopId,
			String shopType,
			List<Long> itemIds,
			int isShopScreen,
			String userDevice,
			Map<String, Object> inputData) {
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("total_count", 0);
		empty.put("list", List.of());
		if (itemIds == null || itemIds.isEmpty()) {
			return empty;
		}
		if ("pointsmall".equals(shopType)) {
			return pointsmallCartSkuLoadService.querySkuPack(companyId, itemIds);
		}
		LinkedHashMap<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		long orchUser = userId;
		if (inputData != null && inputData.get("user_id") != null) {
			orchUser = longObj(inputData.get("user_id"));
		}
		q.put("user_id", orchUser);
		q.put("item_id", itemIds);
		if (shopId > 0L) {
			q.put("distributor_id", shopId);
			String shopTypeNorm = shopType == null ? "" : shopType.trim();
			if ("distributor".equals(shopTypeNorm) || "drug".equals(shopTypeNorm)) {
				q.put("is_can_sale", Boolean.TRUE);
			}
		}
		q.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, "zh-CN");
		if (StringUtils.hasText(userDevice)) {
			q.put("user_device", userDevice);
		}
		if (inputData != null && inputData.get("promoter_user_id") != null) {
			Object p = inputData.get("promoter_user_id");
			if (longObj(p) != 0L) {
				q.put("promoter_user_id", p);
			}
		}
		Map<String, Object> pack = wxappGoodsItemsListQueryOrchestrator.querySkuItemsList(companyId, q, List.of());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) pack.get("list");
		if (skuList != null && !skuList.isEmpty()) {
			itemLogisticsStoreEnricher.enrichListRowsAndApplyDisplayTotal(companyId, shopId, skuList);
		}
		return pack;
	}

	/**
	 * Delegates to {@link #loadSkuItemsViaOrchestrator} for count/validation paths that cannot call
	 * it directly from outside this class.
	 */
	Map<String, Object> loadSkuItemsForCount(
			long companyId,
			long userIdForOrchestrator,
			long distributorIdForOrchestrator,
			String shopType,
			List<Long> itemIds,
			int isShopScreen,
			String userDevice,
			Map<String, Object> inputData) {
		String st = shopType == null ? "" : shopType.trim();
		return loadSkuItemsViaOrchestrator(
				companyId,
				userIdForOrchestrator,
				distributorIdForOrchestrator,
				st,
				itemIds,
				isShopScreen,
				userDevice,
				inputData);
	}

	private static int intQty(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int countLinesInValidCartBlocks(Map<String, Object> out) {
		Object vc = out.get("valid_cart");
		if (!(vc instanceof List<?> blocks)) {
			return 0;
		}
		int n = 0;
		for (Object b : blocks) {
			if (b instanceof Map<?, ?> bm && bm.get("list") instanceof List<?> lst) {
				n += lst.size();
			}
		}
		return n;
	}

	private static boolean shouldCheckMemberPreferenceForCartList(String rowShopType) {
		return !"pointsmall".equals(rowShopType) && !"shop_offline".equals(rowShopType);
	}

	private void assertShopTypeSupportedForList(String shopType) {
		if ("distributor".equals(shopType) || "drug".equals(shopType) || "pointsmall".equals(shopType)) {
			return;
		}
		throw new ResourceException("购物车商品类型不能混合");
	}

	private boolean partitionPackageValidLines(
			long companyId,
			long userId,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			Map<Long, Map<String, Object>> skuByItem,
			String userDevice) {
		boolean checked = false;
		List<Map<String, Object>> stillValid = new ArrayList<>();
		for (Map<String, Object> line : validLines) {
			if (!"package".equals(stringVal(line.get("activity_type")))) {
				stillValid.add(line);
				continue;
			}
			checked = true;
			if (wxappH5PackageCartSupport.enrichPackageCartLine(line, companyId, userId, skuByItem, userDevice)) {
				stillValid.add(line);
			} else {
				invalidLines.add(line);
			}
		}
		validLines.clear();
		validLines.addAll(stillValid);
		return checked;
	}

	/**
	 * 结算：购买数量与商品库存都为 0 的行移入失效列表。库存取商品 SKU {@code store}，不用供应商覆盖后的值。
	 */
	private static void partitionCheckoutZeroNumZeroItemStore(
			boolean isCheckout,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			Map<Long, Map<String, Object>> skuByItem) {
		if (!isCheckout || validLines == null || validLines.isEmpty()) {
			return;
		}
		List<Map<String, Object>> stillValid = new ArrayList<>(validLines.size());
		for (Map<String, Object> line : validLines) {
			if (line == null) {
				continue;
			}
			int qty = intQty(line.get("num")) + intQty(line.get("logistics_num"));
			if (qty > 0) {
				stillValid.add(line);
				continue;
			}
			long itemId = longObj(line.get("item_id"));
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			int itemStore = sku == null ? 0 : intQty(sku.get("store"));
			if (itemStore <= 0) {
				if (invalidLines != null) {
					invalidLines.add(line);
				}
			} else {
				stillValid.add(line);
			}
		}
		validLines.clear();
		validLines.addAll(stillValid);
	}

	/**
	 * 购物车列表：可用库存为 0 的普通行移入 invalid_cart（组合商品仍由 {@link #partitionPackageValidLines} 处理）。
	 * 可用库存与赠品/结算同源：取快递与非快递路径较大值。
	 */
	private void partitionZeroStoreValidLines(
			long companyId,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			List<Cart> invalidCarts,
			List<Cart> rows) {
		if (validLines == null || validLines.isEmpty()) {
			return;
		}
		Map<Long, Cart> cartById = new LinkedHashMap<>();
		if (rows != null) {
			for (Cart c : rows) {
				if (c != null && c.getCartId() != null && c.getCartId() > 0L) {
					cartById.put(c.getCartId(), c);
				}
			}
		}
		List<Map<String, Object>> stillValid = new ArrayList<>(validLines.size());
		for (Map<String, Object> line : validLines) {
			if (line == null) {
				continue;
			}
			if ("package".equals(stringVal(line.get("activity_type")))) {
				stillValid.add(line);
				continue;
			}
			int available = itemAvailableStoreResolver.resolveAvailableForCartDisplay(companyId, line);
			if (available <= 0) {
				line.put("store", 0);
				invalidLines.add(line);
				long cartId = longObj(line.get("cart_id"));
				Cart c = cartById.get(cartId);
				if (c != null) {
					invalidCarts.add(c);
				}
			} else {
				stillValid.add(line);
			}
		}
		validLines.clear();
		validLines.addAll(stillValid);
	}

	private void assertInjected() {
		Objects.requireNonNull(wxappGoodsItemsListQueryOrchestrator, "wxappGoodsItemsListQueryOrchestrator");
		Objects.requireNonNull(distributorItemsRelListCoreService, "distributorItemsRelListCoreService");
		Objects.requireNonNull(packagePromotionFrontPackageInfoService, "packagePromotionFrontPackageInfoService");
		Objects.requireNonNull(wxappH5PackageCartSupport, "wxappH5PackageCartSupport");
		Objects.requireNonNull(itemsTagActivityCheckService, "itemsTagActivityCheckService");
		Objects.requireNonNull(originCountryMapper, "originCountryMapper");
		Objects.requireNonNull(crossBorderSetMapper, "crossBorderSetMapper");
	}

	@SuppressWarnings("unchecked")
	private void applyDistributorPromotionsToValidCartBlocks(
			long companyId, long userId, String userDevice, Map<String, Object> out) {
		Object vc = out.get("valid_cart");
		if (!(vc instanceof List<?> blocks) || blocks.isEmpty()) {
			return;
		}
		List<Map<String, Object>> shopBlocks = (List<Map<String, Object>>) vc;
		List<Map<String, Object>> flatLines = new ArrayList<>();
		for (Object blockObj : blocks) {
			if (!(blockObj instanceof Map<?, ?> blockRaw)) {
				continue;
			}
			Object listObj = blockRaw.get("list");
			if (!(listObj instanceof List<?> lst)) {
				continue;
			}
			for (Object lineObj : lst) {
				if (!(lineObj instanceof Map<?, ?> lineRaw)) {
					continue;
				}
				Map<String, Object> line = (Map<String, Object>) lineRaw;
				long itemId = longObj(line.get("item_id"));
				if (itemId > 0L && longObj(line.get("cart_id")) <= 0L) {
					line.put("cart_id", itemId);
				}
				line.put("is_checked", Boolean.TRUE);
				flatLines.add(line);
			}
		}
		if (flatLines.isEmpty()) {
			return;
		}
		wxappH5DistributorCartPromotionService.applyLimitedTimeSaleActOnCartLines(
				companyId, userId, flatLines, userDevice, false);
		wxappH5CartMemberPriceEnrichmentService.apply(companyId, userId, flatLines);
		finalizeDistributorCartLineRows(flatLines);
		Map<String, Object> promotionTotals =
				wxappH5DistributorCartPromotionService.applyMarketingAndTotals(companyId, userId, flatLines, userDevice);
		enrichDistributorShopBlocks(companyId, shopBlocks);
		mergeDistributorPromotionTotalsIntoShopBlocks(shopBlocks, promotionTotals);
	}

	private List<Cart> loadFastBuyCartRows(long companyId, long userId, long shopId, String shopType) {
		return buildFastBuyBasicRowsForCount(companyId, userId, shopId, shopType);
	}

	private void applyPointsmallShopBlockTotals(long companyId, long userId, List<Map<String, Object>> shopBlocks) {
		if (shopBlocks == null) {
			return;
		}
		String guideDesc = resolvePointsmallVipGuideDesc(companyId, userId);
		for (Map<String, Object> shop : shopBlocks) {
			shop.putIfAbsent("is_ziti", Boolean.FALSE);
			shop.putIfAbsent("is_delivery", Boolean.TRUE);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list =
					shop.get("list") instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
			int itemFee = 0;
			int discountFee = 0;
			int itemNum = 0;
			int cartNum = 0;
			int memberDiscount = 0;
			for (Map<String, Object> cart : list) {
				if (!truthyChecked(cart.get("is_checked"))) {
					continue;
				}
				int price = intQty(cart.get("price"));
				int num = intQty(cart.get("num"));
				itemFee += price * num;
				itemNum += num;
				cartNum++;
				discountFee += intQty(cart.get("discount_fee"));
				memberDiscount += intQty(cart.get("member_discount"));
			}
			int totalFee = itemFee >= discountFee ? itemFee - discountFee : 0;
			shop.put("cart_total_price", itemFee);
			shop.put("item_fee", itemFee);
			shop.put("cart_total_num", itemNum);
			shop.put("cart_total_count", cartNum);
			shop.put("discount_fee", discountFee);
			shop.put("total_fee", totalFee);
			shop.put("member_discount", memberDiscount);
			shop.put("used_activity", new ArrayList<>());
			shop.put("used_activity_ids", new ArrayList<>());
			shop.put("activity_grouping", new ArrayList<>());
			shop.put("gift_activity", new ArrayList<>());
			shop.put("plus_buy_activity", new ArrayList<>());
			Map<String, Object> guide = new LinkedHashMap<>();
			guide.put("guide_title_desc", guideDesc);
			shop.put("vipgrade_guide_title", guide);
		}
	}

	private String resolvePointsmallVipGuideDesc(long companyId, long userId) {
		if (userId <= 0L) {
			return "";
		}
		Map<String, Object> vipRow = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (vipRow == null) {
			return "";
		}
		Object guide = vipRow.get("guide_title");
		return guide != null ? guide.toString() : "";
	}

	private static boolean truthyChecked(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		return !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private void loadFastBuyInto(long companyId, long userId, long shopId, String shopType, Map<String, Object> out) {
		String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + userId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return;
		}
		try {
			Map<String, Object> line = OM.readValue(raw, new TypeReference<>() {});
			long lineShop = longObj(line.get("shop_id"));
			if (shopId > 0L && lineShop != shopId) {
				return;
			}
			if (StringUtils.hasText(shopType)) {
				String lst = stringVal(line.get("shop_type"));
				if (StringUtils.hasText(lst) && !shopType.equals(lst)) {
					return;
				}
			}
			List<Map<String, Object>> list = new ArrayList<>();
			list.add(line);
			Map<String, Object> block = new LinkedHashMap<>();
			block.put("shop_id", lineShop);
			block.put("list", list);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> valid = (List<Map<String, Object>>) out.get("valid_cart");
			valid.add(block);
		} catch (Exception ignored) {
		}
	}

	private List<Map<String, Object>> groupValidByShop(List<Map<String, Object>> lines) {
		Map<Long, List<Map<String, Object>>> byShop = new LinkedHashMap<>();
		for (Map<String, Object> line : lines) {
			long sid = longObj(line.get("shop_id"));
			byShop.computeIfAbsent(sid, k -> new ArrayList<>()).add(line);
		}
		List<Map<String, Object>> blocks = new ArrayList<>();
		for (Map.Entry<Long, List<Map<String, Object>>> e : byShop.entrySet()) {
			Map<String, Object> b = new LinkedHashMap<>();
			b.put("shop_id", e.getKey());
			b.put("list", e.getValue());
			blocks.add(b);
		}
		return blocks;
	}

	private Map<String, Object> cartToLineMap(
			Cart c,
			Map<String, Object> sku,
			long companyId,
			String shopType,
			Set<Long> validShopSet,
			int iscrossborder) {
		Map<String, Object> line = new LinkedHashMap<>(OrdersCartColumnNamesMapSupport.toColumnNamesMap(c));
		if (c.getShopId() == null) {
			line.put("shop_id", 0L);
		}
		if (sku != null) {
			String[] skuKeys = skuNarrowKeysForShopType(shopType);
			for (String k : skuKeys) {
				if (sku.containsKey(k)) {
					line.put(k, sku.get(k));
				}
			}
		}
		line.put("item_type", c.getItemType() != null ? c.getItemType() : line.get("item_type"));
		long sid = c.getShopId() == null ? 0L : c.getShopId();
		if (sid > 0L && !validShopSet.contains(sid) && "distributor".equals(shopType)) {
			line.put("approve_status", "instock");
		}
		if (sid > 0L && c.getItemId() != null && !"community".equals(shopType)) {
			var row = distributorItemSkuInfoForStoreService.findRow(companyId, c.getItemId(), sid);
			if (row != null && row.getPrice() != null && row.getPrice() > 0L) {
				line.put("price", row.getPrice().intValue());
			}
		}
		if (iscrossborder == 1) {
			line.put("iscrossborder", 1);
		}
		return line;
	}

	/**
	 * Aligns cart line rows with PHP list output: first image URL string for {@code pics}, default keys,
	 * and {@code total_fee} (fen) as {@code price * num} after member pricing.
	 */
	private static void finalizeDistributorCartLineRows(List<Map<String, Object>> lines) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		for (Map<String, Object> line : lines) {
			applyFirstPicUrlPhpStyle(line);
			ensurePhpLineDefaultsForCartList(line);
			if ("package".equals(stringVal(line.get("activity_type")))
					&& line.get("packages") instanceof List<?> pkgs
					&& !pkgs.isEmpty()) {
				line.put("total_fee", Long.toString(WxappH5PackageCartSupport.packageLinePayFen(line)));
				continue;
			}
			int p = intQty(line.get("price"));
			int n = intQty(line.get("num"));
			long discountFen = longObj(line.get("discount_fee"));
			long payFen = (long) p * n - discountFen;
			line.put("total_fee", Long.toString(Math.max(0L, payFen)));
		}
	}

	private void applyLimitedTimeThenMemberPrices(
			long companyId,
			long userId,
			List<Map<String, Object>> validLines,
			String userDevice,
			String shopTypeForList,
			boolean isCheckout) {
		if ("distributor".equals(shopTypeForList)) {
			wxappH5DistributorCartPromotionService.applyLimitedTimeSaleActOnCartLines(
					companyId, userId, validLines, userDevice, isCheckout);
		}
		wxappH5CartMemberPriceEnrichmentService.apply(companyId, userId, validLines);
	}

	private static void applyFirstPicUrlPhpStyle(Map<String, Object> line) {
		Object pics = line.get("pics");
		if (pics instanceof Collection<?> coll && !coll.isEmpty()) {
			Object first;
			if (pics instanceof List<?> list) {
				first = list.get(0);
			} else {
				first = coll.iterator().next();
			}
			line.put("pics", first != null ? String.valueOf(first) : "");
			return;
		}
		if (pics instanceof String s) {
			String t = s.trim();
			if (t.startsWith("[")) {
				try {
					List<?> arr = OM.readValue(t, new TypeReference<List<?>>() {});
					if (arr != null && !arr.isEmpty()) {
						line.put("pics", String.valueOf(arr.get(0)));
						return;
					}
				} catch (Exception ignored) {
				}
			}
			line.put("pics", t.isEmpty() ? "" : t);
			return;
		}
		line.put("pics", "");
	}

	private static String[] skuNarrowKeysForShopType(String shopType) {
		if ("distributor".equals(shopType) || "drug".equals(shopType)) {
			return SKU_NARROW_KEYS_DISTRIBUTOR;
		}
		if ("pointsmall".equals(shopType)) {
			return SKU_NARROW_KEYS_POINTSMALL;
		}
		return SKU_NARROW_KEYS_BASE;
	}

	private static void ensurePhpLineDefaultsForCartList(Map<String, Object> line) {
		if (!line.containsKey("cart_id") || line.get("cart_id") == null) {
			line.put("cart_id", 0);
		}
		line.putIfAbsent("isAccumulate", Boolean.FALSE);
		line.putIfAbsent("is_last_price", Boolean.FALSE);
		if (!line.containsKey("discount_fee")) {
			line.put("discount_fee", 0);
		}
		line.putIfAbsent("activity_info", new ArrayList<>());
		line.putIfAbsent("parent_id", 0);
		int t = parseIntLoose(line.get("type"), 0);
		line.put("type", String.valueOf(t));
		if (t != 1) {
			line.put("origincountry_name", "");
			line.put("origincountry_img_url", "");
		} else {
			line.putIfAbsent("origincountry_name", "");
			line.putIfAbsent("origincountry_img_url", "");
		}
		Object mp = line.get("market_price");
		if (mp == null
				|| (mp instanceof Number n && n.longValue() == 0L)
				|| "0".equals(String.valueOf(mp).trim())) {
			line.put("market_price", "0");
		} else if (mp instanceof Number n) {
			line.put("market_price", String.valueOf(n.longValue()));
		}
		Object gid = line.get("goods_id");
		if (gid == null || "".equals(String.valueOf(gid).trim())) {
			Object iid = line.get("item_id");
			if (iid != null) {
				line.put("goods_id", String.valueOf(iid));
			}
		} else if (!(gid instanceof String)) {
			line.put("goods_id", String.valueOf(gid));
		}
		Object ic = line.get("item_category");
		if (ic != null && !(ic instanceof String)) {
			line.put("item_category", String.valueOf(ic));
		}
		if (!line.containsKey("item_spec_desc") || line.get("item_spec_desc") == null) {
			line.put("item_spec_desc", "");
		} else if (!(line.get("item_spec_desc") instanceof String)) {
			line.put("item_spec_desc", String.valueOf(line.get("item_spec_desc")));
		}
	}

	private static int parseIntLoose(Object raw, int dflt) {
		if (raw == null) {
			return dflt;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (Exception e) {
			return dflt;
		}
	}

	/**
	 * Some clients store {@code pics} as a JSON string (e.g. {@code "[]"}); Java lists serialize as JSON
	 * arrays in API responses unless coerced to string. Used for non-distributor cart list shop types.
	 */
	private static void normalizePicsToJsonString(Map<String, Object> line) {
		Object pics = line.get("pics");
		if (pics instanceof String) {
			return;
		}
		if (pics == null) {
			line.put("pics", "[]");
			return;
		}
		if (pics instanceof Collection<?> c) {
			if (c.isEmpty()) {
				line.put("pics", "[]");
			} else {
				line.put("pics", picsJsonString(pics));
			}
		}
	}

	private static String picsJsonString(Object pics) {
		try {
			return OM.writeValueAsString(pics);
		} catch (Exception e) {
			return "[]";
		}
	}

	private static void indexSkuRowsByItemId(List<Map<String, Object>> skuList, Map<Long, Map<String, Object>> skuByItem) {
		if (skuList == null || skuList.isEmpty()) {
			return;
		}
		for (Map<String, Object> sku : skuList) {
			long itemId = longObj(sku.get("item_id"));
			if (itemId <= 0L) {
				itemId = longObj(sku.get("itemId"));
			}
			if (itemId > 0L) {
				skuByItem.putIfAbsent(itemId, sku);
			}
		}
	}

	private static long longObj(Object v) {
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
